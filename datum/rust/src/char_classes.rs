/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::ops::Deref;

/// Datum character class.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumCharClass {
    /// Escaped characters, anything else
    Content,
    /// 0-32 and 127 but not 10
    Whitespace,
    /// 10
    Newline,
    /// ';'
    LineComment,
    /// '"'
    String,
    /// '\''
    Quote,
    /// '('
    ListStart,
    /// ')'
    ListEnd,
    /// '#'
    SpecialID,
    /// '0' - '9', '-'
    NumericStart,
}

impl DatumCharClass {
    /// If this character class is a potential identifier.
    #[inline]
    pub const fn potential_identifier(&self) -> bool {
        match self {
            Self::Content => true,
            Self::NumericStart => true,
            Self::SpecialID => true,
            _ => false
        }
    }

    /// If this character class is non-printing.
    #[inline]
    pub const fn non_printing(&self) -> bool {
        match self {
            Self::Whitespace => true,
            Self::Newline => true,
            _ => false
        }
    }
}

/// Datum character with class.
/// It is not possible to create an instance of this enum which cannot be emitted.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct DatumChar {
    /// The raw value of this character.
    byte: u8,
    /// The class of this character.
    class: DatumCharClass,
    /// How to emit this class/byte pair reliably.
    emit_len: u8,
    emit_storage: [u8;2]
}

impl DatumChar {
    /// Returns the byte in Datum's class/byte stream.
    /// ```
    /// use datum_rs::DatumChar;
    /// let list_start = DatumChar::byte_to_char(b'(').expect("not backslash");
    /// assert_eq!(list_start.byte(), b'(');
    /// ```
    #[inline]
    pub const fn byte(&self) -> u8 {
        self.byte
    }

    /// Returns the class in Datum's class/byte stream.
    /// ```
    /// use datum_rs::{DatumChar, DatumCharClass};
    /// let list_start = DatumChar::byte_to_char(b'(').expect("not backslash");
    /// assert_eq!(list_start.class(), DatumCharClass::ListStart);
    /// ```
    #[inline]
    pub const fn class(&self) -> DatumCharClass {
        self.class
    }

    /// Returns how to emit the character.
    /// ```
    /// use datum_rs::{DatumChar, DatumCharClass};
    /// let content_open_paren = DatumChar::byte_to_content_char(b'(');
    /// assert_eq!(content_open_paren.emit(), [b'\\', b'(']);
    /// ```
    #[inline]
    pub fn emit(&self) -> &[u8] {
        &self.emit_storage[0..self.emit_len as usize]
    }

    const fn init_normal(v: u8) -> Option<DatumChar> {
        if v == ('\\' as u8) {
            None
        } else {
            let class = if v == 10 {
                DatumCharClass::Newline
            } else if v <= 32 || v == 127 {
                DatumCharClass::Whitespace
            } else if v == b';' {
                DatumCharClass::LineComment
            } else if v == b'"' {
                DatumCharClass::String
            } else if v == b'\'' {
                DatumCharClass::Quote
            } else if v == b'(' {
                DatumCharClass::ListStart
            } else if v == b')' {
                DatumCharClass::ListEnd
            } else if v == b'#' {
                DatumCharClass::SpecialID
            } else if (v >= b'0' && v <= b'9') || v == b'-' {
                DatumCharClass::NumericStart
            } else {
                DatumCharClass::Content
            };

            Some(DatumChar { byte: v, class, emit_len: 1, emit_storage: [v, 0] })
        }
    }
    const fn init_normal_array() -> [Option<DatumChar>; 256] {
        let mut data: [Option<DatumChar>; 256] = [None; 256];
        let mut i: usize = 0;
        while i < 256 {
            data[i] = DatumChar::init_normal(i as u8);
            i += 1;
        }
        data
    }
    const fn init_content(v: u8) -> DatumChar {
        let emit: (u8, [u8; 2]) = if v == 10 {
            (2, [b'\\', b'n'])
        } else if v == 13 {
            (2, [b'\\', b'r'])
        } else if v == 9 {
            (2, [b'\\', b't'])
        } else {
            match DatumChar::init_normal(v) {
                None => (2, [b'\\', v]),
                Some(rchr) => match rchr.class {
                    DatumCharClass::Content => (1, [v, 0]),
                    _ => (2, [b'\\', v])
                }
            }
        };
        DatumChar { byte: v, class: DatumCharClass::Content, emit_len: emit.0, emit_storage: emit.1 }
    }
    const fn init_content_array() -> [DatumChar; 256] {
        let dummy = DatumChar { byte: 0, class: DatumCharClass::Content, emit_len: 0, emit_storage: [0, 0] };
        let mut data: [DatumChar; 256] = [dummy; 256];
        let mut i: usize = 0;
        while i < 256 {
            data[i] = DatumChar::init_content(i as u8);
            i += 1;
        }
        data
    }
}

impl Deref for DatumChar {
    type Target = DatumCharClass;
    fn deref(&self) -> &Self::Target {
        &self.class
    }
}

impl Default for DatumChar {
    fn default() -> Self {
        // Whitespace ' ' should avoid messing up whatever somehow receives this value.
        DatumChar { byte: 32, class: DatumCharClass::Whitespace, emit_len: 1, emit_storage: [32, 0] }
    }
}

/// The Central Character Table, used for parsing, etc.
static DATUM_CHARS: [Option<DatumChar>; 256] = DatumChar::init_normal_array();
/// Table of all content DatumChars.
static DATUM_CONTENT_CHARS: [DatumChar; 256] = DatumChar::init_content_array();

impl DatumChar {
    /// Converts a byte to the character it would be recognized as.
    /// Note that backslash won't convert (being the escape character).
    pub fn byte_to_char(b: u8) -> Option<DatumChar> {
        DATUM_CHARS[b as usize]
    }
    /// Converts a byte to the equivalent content character.
    pub fn byte_to_content_char(b: u8) -> DatumChar {
        DATUM_CONTENT_CHARS[b as usize]
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn sanity_check_tables() {
        for i in 0..256 {
            // Assert .byte() is always correct
            assert_eq!(DATUM_CONTENT_CHARS[i].byte(), i as u8);
            if i != ('\\' as usize) {
                assert_eq!(DATUM_CHARS[i].unwrap().byte(), i as u8);
            } else {
                assert_eq!(DATUM_CHARS[i], None);
            }
        }
    }
}
