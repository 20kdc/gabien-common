/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use crate::{DatumChar, DatumPipe};

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

/// Maximum amount of characters [DatumDecoder] will return at once.
pub const DATUM_DECODER_MAX_SIZE: usize = 1;

impl Default for DatumDecoder {
    #[inline]
    fn default() -> DatumDecoder {
        DatumDecoder(DatumDecoderState::Normal)
    }
}

impl DatumPipe for DatumDecoder {
    type Input = char;
    type Output = DatumChar;
    type Array = Option<DatumChar>;
    const MAX_SIZE: usize = DATUM_DECODER_MAX_SIZE;


    fn eof(&mut self) -> Self::Array {
        if self.0 != DatumDecoderState::Normal {
            self.0 = DatumDecoderState::Error;
        }
        Self::Array::default()
    }

    fn has_error(&self) -> bool {
        self.0 == DatumDecoderState::Error
    }

    fn feed(&mut self, char: char) -> Self::Array {
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
}

#[cfg(test)]
mod tests {
    use crate::{DatumArray, DatumCharClass};

    use super::*;

    fn decoder_test(input: &str, output: &str, out_class: DatumCharClass) {
        let mut decoder = DatumDecoder::default();
        let mut output_iterator = output.chars();
        for v in input.chars() {
            if let Some(c) = decoder.feed(v) {
                assert_eq!(c.char(), output_iterator.next().expect("early output end"));
                assert_eq!(c.class(), out_class);
            }
        }
        assert!(!decoder.has_error());
        _ = decoder.eof();
        assert!(!decoder.has_error());
        assert_eq!(output_iterator.next(), None);
    }

    fn decoder_should_fail(input: &str) {
        let mut decoder = DatumDecoder::default();
        for v in input.chars() {
            decoder.feed(v);
        }
        assert!(decoder.has_error());
    }

    fn decoder_should_not_allow_eof(input: &str) {
        let mut decoder = DatumDecoder::default();
        for v in input.chars() {
            decoder.feed(v);
        }
        assert!(!decoder.has_error());
        _ = decoder.eof();
        assert!(decoder.has_error());
    }

    #[test]
    fn decoder_results_test() {
        let mut decoder = DatumDecoder::default();
        assert_eq!(decoder.feed('\\').pop(), None);
        assert_eq!(decoder.feed('x').pop(), None);
        assert_eq!(decoder.feed('1').pop(), None);
        assert_eq!(decoder.feed('0').pop(), None);
        assert_eq!(decoder.feed('F').pop(), None);
        assert_eq!(decoder.feed('F').pop(), None);
        assert_eq!(decoder.feed('F').pop(), None);
        assert_eq!(decoder.feed('F').pop(), None);
        assert_eq!(decoder.feed(';').pop(), Some(DatumChar::content('\u{10FFFF}' as char)));
        assert_eq!(decoder.feed('a').pop(), Some(DatumChar::content('a' as char)));
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
