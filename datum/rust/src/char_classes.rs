/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::{fmt::{Display, Write}, ops::Deref};

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
    /// '('
    ListStart,
    /// ')'
    ListEnd,
    /// '#'
    SpecialID,
    /// '-'
    Sign,
    /// '0' - '9'
    Digit,
}

impl DatumCharClass {
    /// If this character class is a potential identifier.
    /// Note that this can be accessed via [DatumChar] via [DatumChar::deref].
    /// ```
    /// use datum_rs::DatumChar;
    /// assert!(DatumChar::identify('a').expect("not backslash").potential_identifier());
    /// ```
    #[inline]
    pub const fn potential_identifier(&self) -> bool {
        matches!(self, Self::Content | Self::Sign | Self::Digit | Self::SpecialID)
    }

    /// If this character class starts a number.
    /// Note that this can be accessed via [DatumChar] via [DatumChar::deref].
    /// ```
    /// use datum_rs::DatumChar;
    /// assert!(DatumChar::identify('0').expect("not backslash").numeric_start());
    /// ```
    #[inline]
    pub const fn numeric_start(&self) -> bool {
        matches!(self, Self::Sign | Self::Digit)
    }

    /// Identifies a character.
    /// Backslash is a 'meta-character' and doesn't count, returning [None].
    pub const fn identify(v: char) -> Option<Self> {
        if v == '\\' {
            None
        } else if v == '\n' {
            Some(DatumCharClass::Newline)
        } else if v <= ' ' || v == '\x7F' {
            Some(DatumCharClass::Whitespace)
        } else if v == ';' {
            Some(DatumCharClass::LineComment)
        } else if v == '"' {
            Some(DatumCharClass::String)
        } else if v == '(' {
            Some(DatumCharClass::ListStart)
        } else if v == ')' {
            Some(DatumCharClass::ListEnd)
        } else if v == '#' {
            Some(DatumCharClass::SpecialID)
        } else if v == '-' {
            Some(DatumCharClass::Sign)
        } else if v >= '0' && v <= '9' {
            Some(DatumCharClass::Digit)
        } else {
            Some(DatumCharClass::Content)
        }
    }
}

/// Datum character with class.
/// It is not possible to create an instance of this enum which cannot be emitted.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct DatumChar {
    /// The raw value of this character.
    char: char,
    /// The class of this character.
    class: DatumCharClass,
    /// How to emit this class/char pair reliably.
    emit_len: u8,
    emit_storage: [char;2]
}

impl DatumChar {
    /// Returns the byte in Datum's class/byte stream.
    /// ```
    /// use datum_rs::DatumChar;
    /// let list_start = DatumChar::identify('(').expect("not backslash");
    /// assert_eq!(list_start.char(), '(');
    /// ```
    #[inline]
    pub const fn char(&self) -> char {
        self.char
    }

    /// Returns the class in Datum's class/byte stream.
    /// ```
    /// use datum_rs::{DatumChar, DatumCharClass};
    /// let list_start = DatumChar::identify('(').expect("not backslash");
    /// assert_eq!(list_start.class(), DatumCharClass::ListStart);
    /// ```
    #[inline]
    pub const fn class(&self) -> DatumCharClass {
        self.class
    }

    /// Returns how to emit the character.
    /// ```
    /// use datum_rs::{DatumChar, DatumCharClass};
    /// let content_open_paren = DatumChar::content('(');
    /// assert_eq!(content_open_paren.emit(), ['\\', '(']);
    /// ```
    #[inline]
    pub fn emit(&self) -> &[char] {
        &self.emit_storage[0..self.emit_len as usize]
    }

    /// Write the necessary UTF-8 characters that will be read back as this [DatumChar].
    pub fn write(&self, f: &mut dyn Write) -> core::fmt::Result {
        if self.emit_len >= 1 {
            f.write_char(self.emit_storage[0])?;
        }
        if self.emit_len >= 2 {
            f.write_char(self.emit_storage[1])?;
        }
        // never exceeds this, so avoid unnecessary bounds-checking
        core::fmt::Result::Ok(())
    }

    /// Identifies an unescaped character and returns the corresponding [DatumChar].
    /// Backslash is special due to being the escape character, and this will return [None].
    /// ```
    /// use datum_rs::DatumChar;
    /// assert_eq!(DatumChar::identify('\\'), None);
    /// assert_ne!(DatumChar::identify('a'), None);
    /// ```
    #[inline]
    pub const fn identify(v: char) -> Option<DatumChar> {
        match DatumCharClass::identify(v) {
            None => None,
            Some(class) => Some(DatumChar { char: v, class, emit_len: 1, emit_storage: [v, '\x00'] })
        }
    }

    /// Creates a content character for the given value.
    pub const fn content(v: char) -> DatumChar {
        let emit: (u8, [char; 2]) = if v == '\n' {
            (2, ['\\', 'n'])
        } else if v == '\r' {
            (2, ['\\', 'r'])
        } else if v == '\t' {
            (2, ['\\', 't'])
        } else {
            match DatumCharClass::identify(v) {
                Some(DatumCharClass::Content) => (1, [v, '\x00']),
                _ => (2, ['\\', v])
            }
        };
        DatumChar { char: v, class: DatumCharClass::Content, emit_len: emit.0, emit_storage: emit.1 }
    }

    /// Creates a potential identifier character for the given value.
    pub const fn potential_identifier(v: char) -> DatumChar {
        match Self::identify(v) {
            None => Self::content(v),
            Some(rchr) => if rchr.class().potential_identifier() {
                rchr
            } else {
                Self::content(v)
            }
        }
    }
}

impl Display for DatumChar {
    fn fmt(&self, f: &mut core::fmt::Formatter<'_>) -> core::fmt::Result {
        self.write(f)
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
        DatumChar { char: ' ', class: DatumCharClass::Whitespace, emit_len: 1, emit_storage: [' ', '\x00'] }
    }
}
