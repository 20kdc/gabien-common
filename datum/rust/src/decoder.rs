/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use crate::DatumChar;

#[cfg(feature = "alloc")]
use alloc::vec::Vec;

/// Decoder's state machine
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum DatumDecoderState {
    Normal,
    Escaping,
    HexEscape(u32),
    Error,
}

/// Decoder for the Datum encoding layer.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct DatumDecoder(DatumDecoderState);

impl Default for DatumDecoder {
    #[inline]
    fn default() -> DatumDecoder {
        DatumDecoder(DatumDecoderState::Normal)
    }
}

impl DatumDecoder {
    /// Returns true if EOF is allowed.
    /// In other words, if an EOF were to happen now, would any error (including invalid escape errors) have occurred?
    /// ```
    /// use datum_rs::DatumByteDecoder;
    /// let mut decoder = DatumByteDecoder::default();
    /// assert_eq!(decoder.allowed_to_eof(), true);
    /// decoder.feed_byte(b'\\');
    /// assert_eq!(decoder.allowed_to_eof(), false);
    /// decoder.feed_byte(b'n');
    /// assert_eq!(decoder.allowed_to_eof(), true);
    /// ```
    #[inline]
    pub fn allowed_to_eof(&self) -> bool {
        self.0 == DatumDecoderState::Normal
    }

    /// True if the decoder encountered invalid UTF-8 or an invalid escape.
    /// The decoder will NOP once this occurs.
    /// If you want to continue anyway, reconstruct the [DatumDecoder].
    #[inline]
    pub fn has_error(&self) -> bool {
        self.0 == DatumDecoderState::Error
    }

    /// Forces an error.
    /// This is useful for things that might wrap the decoder.
    #[inline]
    pub fn force_error(&mut self) {
        self.0 = DatumDecoderState::Error;
    }

    /// Given a char, returns a resulting [DatumChar], if any.
    /// Beware: This should not be mixed with `feed_byte` unless the byte decoder (not this decoder!) is safe to EOF, which implies no UTF-8 sequences are buffered.
    /// Multiple [DatumChar]s can be returned at once due to Unicode escapes.
    pub fn feed_char(&mut self, char: char) -> Option<DatumChar> {
        let mut res = None;
        self.0 = match self.0 {
            DatumDecoderState::Normal => {
                res = DatumChar::identify(char);
                match res {
                    Some(_) => DatumDecoderState::Normal,
                    None => DatumDecoderState::Escaping
                }
            },
            DatumDecoderState::Escaping => {
                match char {
                    'r' => {
                        res = Some(DatumChar::content('\r'));
                        DatumDecoderState::Normal
                    },
                    'n' => {
                        res = Some(DatumChar::content('\n'));
                        DatumDecoderState::Normal
                    },
                    't' => {
                        res = Some(DatumChar::content('\t'));
                        DatumDecoderState::Normal
                    },
                    'x' => {
                        DatumDecoderState::HexEscape(0)
                    },
                    _ => {
                        res = Some(DatumChar::content(char));
                        DatumDecoderState::Normal
                    }
                }
            },
            DatumDecoderState::HexEscape(v) => {
                if char == ';' {
                    if let Some(rustchar) = char::from_u32(v) {
                        res = Some(DatumChar::content(rustchar));
                        DatumDecoderState::Normal
                    } else {
                        DatumDecoderState::Error
                    }
                } else {
                    let mut v_new = v;
                    v_new <<= 4;
                    if char >= '0' && char <= '9' {
                        v_new |= (char as u32) - ('0' as u32);
                        DatumDecoderState::HexEscape(v_new)
                    } else if char >= 'A' && char <= 'F' {
                        v_new |= ((char as u32) - ('A' as u32)) + 0xA;
                        DatumDecoderState::HexEscape(v_new)
                    } else if char >= 'a' && char <= 'f' {
                        v_new |= ((char as u32) - ('a' as u32)) + 0xA;
                        DatumDecoderState::HexEscape(v_new)
                    } else {
                        DatumDecoderState::Error
                    }
                }
            },
            DatumDecoderState::Error => {
                DatumDecoderState::Error
            }
        };
        res
    }

    /// Feeds into a vec from a byte slice. Remember to check for [DatumDecoder::has_error].
    /// ```
    /// use datum_rs::DatumDecoder;
    /// let mut decoder = DatumDecoder::default();
    /// let mut results = vec![];
    /// decoder.feed_chars_into(&mut results, "example text".chars());
    /// assert_eq!(results.len(), 12);
    /// ```
    #[cfg(feature = "alloc")]
    #[inline]
    pub fn feed_chars_into<S: IntoIterator<Item = char>>(&mut self, target: &mut Vec<DatumChar>, source: S) {
        for v in source {
            if let Some(c) = self.feed_char(v) {
                target.push(c);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use crate::DatumCharClass;

    use super::*;

    fn decoder_test(input: &str, output: &str, out_class: DatumCharClass) {
        let mut decoder = DatumDecoder::default();
        let mut output_iterator = output.chars();
        for v in input.chars() {
            if let Some(c) = decoder.feed_char(v) {
                assert_eq!(c.char(), output_iterator.next().expect("early output end"));
                assert_eq!(c.class(), out_class);
            }
        }
        assert!(!decoder.has_error());
        assert!(decoder.allowed_to_eof());
        assert_eq!(output_iterator.next(), None);
    }

    fn decoder_should_fail(input: &str) {
        let mut decoder = DatumDecoder::default();
        for v in input.chars() {
            decoder.feed_char(v);
        }
        assert!(decoder.has_error());
    }

    fn decoder_should_not_allow_eof(input: &str) {
        let mut decoder = DatumDecoder::default();
        for v in input.chars() {
            decoder.feed_char(v);
        }
        assert!(!decoder.allowed_to_eof());
    }

    #[test]
    fn decoder_results_test() {
        let mut decoder = DatumDecoder::default();
        assert_eq!(decoder.feed_char('\\'), None);
        assert_eq!(decoder.feed_char('x'), None);
        assert_eq!(decoder.feed_char('1'), None);
        assert_eq!(decoder.feed_char('0'), None);
        assert_eq!(decoder.feed_char('F'), None);
        assert_eq!(decoder.feed_char('F'), None);
        assert_eq!(decoder.feed_char('F'), None);
        assert_eq!(decoder.feed_char('F'), None);
        assert_eq!(decoder.feed_char(';'), Some(DatumChar::content('\u{10FFFF}' as char)));
        assert_eq!(decoder.feed_char('a'), Some(DatumChar::content('a' as char)));
    }

    #[test]
    fn all_decoder_test_cases() {
        // -- also see byte_decoder.rs:byte_decoder_tests
        decoder_test("thequickbrownfoxjumpsoverthelazydog", "thequickbrownfoxjumpsoverthelazydog", DatumCharClass::Content);
        decoder_test("THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG", "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG", DatumCharClass::Content);
        decoder_test("!£$%^&*_+=[]{}~@:?/>.<,|", "!£$%^&*_+=[]{}~@:?/>.<,|", DatumCharClass::Content);
        // a few simple sanity checks
        decoder_test("\\n", "\n", DatumCharClass::Content);
        decoder_test("\\r", "\r", DatumCharClass::Content);
        decoder_test("\\t", "\t", DatumCharClass::Content);
        decoder_test("\n", "\n", DatumCharClass::Newline);
        decoder_test(";", ";", DatumCharClass::LineComment);
        decoder_test("\"", "\"", DatumCharClass::String);
        decoder_test("'", "'", DatumCharClass::Quote);
        decoder_test("(", "(", DatumCharClass::ListStart);
        decoder_test(")", ")", DatumCharClass::ListEnd);
        decoder_test("#", "#", DatumCharClass::SpecialID);
        decoder_test("\\;", ";", DatumCharClass::Content);
        // Hex escape check
        decoder_test("\\x0A;", "\n", DatumCharClass::Content);
        // UTF-8 encoding check
        decoder_test("\\xB9;", "¹", DatumCharClass::Content);
        decoder_test("\\x10FFff;", "\u{10FFFF}", DatumCharClass::Content);
        decoder_test("\u{10FFFF}", "\u{10FFFF}", DatumCharClass::Content);
        // --

        // failure tests
        decoder_should_fail("\\x-");
        decoder_should_fail("\\xFFFFFF;A");
        decoder_should_not_allow_eof("\\");
        decoder_should_not_allow_eof("\\x");
        decoder_should_not_allow_eof("\\xA");
    }
}
