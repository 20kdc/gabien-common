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

impl Default for DatumDecoder {
    #[inline]
    fn default() -> DatumDecoder {
        DatumDecoder(DatumDecoderState::Normal)
    }
}

impl DatumPipe for DatumDecoder {
    type Input = char;
    type Output = DatumChar;

    fn eof<F: FnMut(DatumChar)>(&mut self, _f: &mut F) {
        if self.0 != DatumDecoderState::Normal {
            self.0 = DatumDecoderState::Error;
        }
    }

    fn has_error(&self) -> bool {
        self.0 == DatumDecoderState::Error
    }

    fn feed<F: FnMut(DatumChar)>(&mut self, char: char, f: &mut F) {
        self.0 = match self.0 {
            DatumDecoderState::Normal => {
                let val = DatumChar::identify(char);
                match val {
                    Some(v) => {
                        f(v);
                        DatumDecoderState::Normal
                    },
                    None => DatumDecoderState::Escaping
                }
            },
            DatumDecoderState::Escaping => {
                match char {
                    'r' => {
                        f(DatumChar::content('\r'));
                        DatumDecoderState::Normal
                    },
                    'n' => {
                        f(DatumChar::content('\n'));
                        DatumDecoderState::Normal
                    },
                    't' => {
                        f(DatumChar::content('\t'));
                        DatumDecoderState::Normal
                    },
                    'x' => {
                        DatumDecoderState::HexEscape(0)
                    },
                    _ => {
                        f(DatumChar::content(char));
                        DatumDecoderState::Normal
                    }
                }
            },
            DatumDecoderState::HexEscape(v) => {
                if char == ';' {
                    if let Some(rustchar) = char::from_u32(v) {
                        f(DatumChar::content(rustchar));
                        DatumDecoderState::Normal
                    } else {
                        DatumDecoderState::Error
                    }
                } else {
                    let mut v_new = v;
                    v_new <<= 4;
                    if let Some(digit) = char.to_digit(16) {
                        v_new |= digit;
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
    }
}

#[cfg(feature = "alloc")]
#[cfg(test)]
mod tests {
    use crate::DatumCharClass;
    use alloc::vec::Vec;

    use super::*;

    fn decoder_test(input: &str, output: &str, out_class: DatumCharClass) {
        let mut decoder = DatumDecoder::default();
        let mut output_iterator = output.chars();
        for v in input.chars() {
            decoder.feed(v, &mut |c| {
                assert_eq!(c.char(), output_iterator.next().expect("early output end"));
                assert_eq!(c.class(), out_class);
            });
        }
        assert!(!decoder.has_error());
        _ = decoder.eof(&mut |_| {});
        assert!(!decoder.has_error());
        assert_eq!(output_iterator.next(), None);
    }

    fn decoder_should_fail(input: &str) {
        let mut decoder = DatumDecoder::default();
        for v in input.chars() {
            decoder.feed(v, &mut |_| {});
        }
        assert!(decoder.has_error());
    }

    fn decoder_should_not_allow_eof(input: &str) {
        let mut decoder = DatumDecoder::default();
        for v in input.chars() {
            decoder.feed(v, &mut |_| {});
        }
        assert!(!decoder.has_error());
        _ = decoder.eof(&mut |_| {});
        assert!(decoder.has_error());
    }

    #[test]
    fn decoder_results_test() {
        let mut decoder = DatumDecoder::default();
        decoder.feed('\\', &mut |_| {panic!("NO")});
        decoder.feed('x', &mut |_| {panic!("NO")});
        decoder.feed('1', &mut |_| {panic!("NO")});
        decoder.feed('0', &mut |_| {panic!("NO")});
        decoder.feed('F', &mut |_| {panic!("NO")});
        decoder.feed('F', &mut |_| {panic!("NO")});
        decoder.feed('F', &mut |_| {panic!("NO")});
        decoder.feed('F', &mut |_| {panic!("NO")});
        let out = [DatumChar::content('\u{10FFFF}' as char), DatumChar::content('a' as char)];
        let mut tmp = Vec::new();
        decoder.feed_iter_to_vec(&mut tmp, [';', 'a'], true);
        assert_eq!(tmp, out);
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
