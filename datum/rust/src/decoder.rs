/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::ops::{Deref, DerefMut};

use crate::DatumChar;

/// Decoder's state machine
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum DatumDecoderState {
    Normal,
    Escaping,
    HexEscape(u32),
    InvalidEscapeError
}

// rationale for not generalizing DatumBuffer for this:
// it isn't a good idea to expose the implementation detail that this is 4 characters in length
// that in mind, we don't want any code outside of this file to push to this
// in theory, it would be good to ditch push here in favour of preinitializing, also
// but the push API is more convenient

/// Possible outputs of [DatumDecoder] given a char.
#[derive(Clone, Copy, Default, Debug)]
pub struct DatumDecoderResults {
    output_len: usize,
    output: [DatumChar;4]
}
impl DatumDecoderResults {
    #[inline]
    fn push(&mut self, char: DatumChar) {
        self.output[self.output_len] = char;
        self.output_len += 1;
    }
}
impl Deref for DatumDecoderResults {
    type Target = [DatumChar];
    #[inline]
    fn deref(&self) -> &Self::Target {
        &self.output[..self.output_len]
    }
}
impl DerefMut for DatumDecoderResults {
    #[inline]
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.output[..self.output_len]
    }
}
impl Iterator for DatumDecoderResults {
    type Item = DatumChar;
    #[inline]
    fn next(&mut self) -> Option<Self::Item> {
        if self.output_len != 0 {
            let val = self.output[0];
            self.output_len -= 1;
            self.output.copy_within(1..(1 + self.output_len), 0);
            Some(val)
        } else {
            None
        }
    }
}

/// Decoder for the Datum encoding layer.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct DatumDecoder(DatumDecoderState);

impl DatumDecoder {
    #[inline]
    pub fn new() -> DatumDecoder {
        DatumDecoder(DatumDecoderState::Normal)
    }

    /// Returns true if EOF is allowed.
    /// In other words, if an EOF were to happen now, would any error (including invalid escape errors) have occurred?
    /// ```
    /// use datum_rs::DatumDecoder;
    /// let mut decoder = DatumDecoder::new();
    /// assert_eq!(decoder.allowed_to_eof(), true);
    /// decoder.feed(b'\\');
    /// assert_eq!(decoder.allowed_to_eof(), false);
    /// decoder.feed(b'n');
    /// assert_eq!(decoder.allowed_to_eof(), true);
    /// ```
    #[inline]
    pub fn allowed_to_eof(&self) -> bool {
        self.0 == DatumDecoderState::Normal
    }

    /// True if the decoder encountered an invalid escape.
    /// The decoder will NOP once this occurs.
    /// If you want to continue anyway, simply reconstruct the [DatumDecoder].
    #[inline]
    pub fn has_invalid_escape_error(&self) -> bool {
        self.0 == DatumDecoderState::InvalidEscapeError
    }

    /// Given a byte, returns the resulting DatumChars.
    /// Multiple [DatumChar]s can be returned at once due to Unicode escapes.
    pub fn feed(&mut self, byte: u8) -> DatumDecoderResults {
        let mut res = DatumDecoderResults::default();
        self.0 = match self.0 {
            DatumDecoderState::Normal => {
                let maybe_char = DatumChar::byte_to_char(byte);
                match maybe_char {
                    Some(v) => {
                        res.push(v);
                        DatumDecoderState::Normal
                    },
                    None => {
                        DatumDecoderState::Escaping
                    }
                }
            },
            DatumDecoderState::Escaping => {
                match byte {
                    b'r' => {
                        res.push(DatumChar::byte_to_content_char(13));
                        DatumDecoderState::Normal
                    },
                    b'n' => {
                        res.push(DatumChar::byte_to_content_char(10));
                        DatumDecoderState::Normal
                    },
                    b't' => {
                        res.push(DatumChar::byte_to_content_char(9));
                        DatumDecoderState::Normal
                    },
                    b'x' => {
                        DatumDecoderState::HexEscape(0)
                    },
                    _ => {
                        res.push(DatumChar::byte_to_content_char(byte));
                        DatumDecoderState::Normal
                    }
                }
            },
            DatumDecoderState::HexEscape(v) => {
                if byte == b';' {
                    if let Some(rustchar) = char::from_u32(v) {
                        let mut data: [u8;4] = [0;4];
                        for v in rustchar.encode_utf8(&mut data).bytes() {
                            res.push(DatumChar::byte_to_content_char(v));
                        }
                        DatumDecoderState::Normal
                    } else {
                        DatumDecoderState::InvalidEscapeError
                    }
                } else {
                    let mut v_new = v;
                    v_new <<= 4;
                    if byte >= b'0' && byte <= b'9' {
                        v_new |= (byte - b'0') as u32;
                        DatumDecoderState::HexEscape(v_new)
                    } else if byte >= b'A' && byte <= b'F' {
                        v_new |= ((byte - b'A') as u32) + 0xA;
                        DatumDecoderState::HexEscape(v_new)
                    } else if byte >= b'a' && byte <= b'f' {
                        v_new |= ((byte - b'a') as u32) + 0xA;
                        DatumDecoderState::HexEscape(v_new)
                    } else {
                        DatumDecoderState::InvalidEscapeError
                    }
                }
            },
            DatumDecoderState::InvalidEscapeError => {
                DatumDecoderState::InvalidEscapeError
            }
        };
        res
    }
}

#[cfg(test)]
mod tests {
    use crate::DatumCharClass;

    use super::*;

    fn decoder_test(input: &str, output: &str, out_class: DatumCharClass) {
        let mut decoder = DatumDecoder::new();
        let mut i: usize = 0;
        for v in input.bytes() {
            for c in decoder.feed(v) {
                assert_eq!(c.byte(), output.as_bytes()[i]);
                assert_eq!(c.class(), out_class);
                i += 1;
            }
        }
        assert!(!decoder.has_invalid_escape_error());
        assert!(decoder.allowed_to_eof());
        assert_eq!(i, output.len());
    }

    fn decoder_should_fail(input: &str) {
        let mut decoder = DatumDecoder::new();
        for v in input.bytes() {
            decoder.feed(v);
        }
        assert!(decoder.has_invalid_escape_error());
    }

    fn decoder_should_not_allow_eof(input: &str) {
        let mut decoder = DatumDecoder::new();
        for v in input.bytes() {
            decoder.feed(v);
        }
        assert!(!decoder.allowed_to_eof());
    }

    #[test]
    fn all_decoder_test_cases() {
        decoder_test("thequickbrownfoxjumpsoverthelazydog", "thequickbrownfoxjumpsoverthelazydog", DatumCharClass::Content);
        decoder_test("THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG", "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG", DatumCharClass::Content);
        decoder_test("!£$%^&*_+=[]{}~@:?/>.<,|", "!£$%^&*_+=[]{}~@:?/>.<,|", DatumCharClass::Content);
        // a few simple sanity checks
        decoder_test("\\n", "\n", DatumCharClass::Content);
        decoder_test("\n", "\n", DatumCharClass::Newline);
        decoder_test(";", ";", DatumCharClass::LineComment);
        decoder_test("\\;", ";", DatumCharClass::Content);
        // Hex escape check
        decoder_test("\\x0A;", "\n", DatumCharClass::Content);
        // UTF-8 encoding check
        decoder_test("\\xB9;", "¹", DatumCharClass::Content);
        decoder_test("\\x10FFFF;", "\u{10FFFF}", DatumCharClass::Content);
        // failure tests
        decoder_should_fail("\\x-");
        decoder_should_not_allow_eof("\\");
        decoder_should_not_allow_eof("\\x");
        decoder_should_not_allow_eof("\\xA");
    }
}
