/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::ops::{Deref, DerefMut};

use crate::{DatumChar, DatumDecoder};

#[cfg(feature = "alloc")]
use alloc::vec::Vec;

const UTF8_DECODE_BUFFER: usize = 4;

/// Decoder for the Datum encoding layer.
#[derive(Clone, Copy, PartialEq, Eq, Debug, Default)]
pub struct DatumByteDecoder {
    /// Char-based decoder.
    /// Only access this if you're sure you're doing the right thing!
    pub decoder: DatumDecoder,
    /// UTF-8 decoding buffer
    buffer: [u8; UTF8_DECODE_BUFFER],
    /// UTF-8 decoding buffer length
    buffer_len: u8,
}

impl Deref for DatumByteDecoder {
    type Target = DatumDecoder;
    fn deref(&self) -> &Self::Target {
        &self.decoder
    }
}

impl DerefMut for DatumByteDecoder {
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.decoder
    }
}

impl DatumByteDecoder {
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
        self.decoder.allowed_to_eof() && self.buffer_len == 0
    }

    /// Given a byte, returns a resulting [DatumChar], if any.
    /// Multiple [DatumChar]s can be returned at once due to Unicode escapes.
    pub fn feed_byte(&mut self, byte: u8) -> Option<DatumChar> {
        if self.buffer_len >= (UTF8_DECODE_BUFFER as u8) {
            // this implies a UTF-8 character kept on continuing
            // and was not recognized as valid by Rust
            self.decoder.force_error();
            None
        } else if self.buffer_len == 0 {
            // first char of sequence, use special handling to catch errors early
            if byte <= 127 {
                // fast-path these
                self.feed_char(byte as char)
            } else if byte >= 0x80 && byte <= 0xBF {
                // can't start a sequence with a continuation
                self.decoder.force_error();
                None
            } else {
                // start bytes of multi-byte sequences
                self.buffer[0] = byte;
                self.buffer_len = 1;
                None
            }
        } else if byte < 0x80 || byte > 0xBF {
            // we're supposed to be adding continuations and suddenly this shows up?
            // (this path also catches if a character comes in that looks fine at a glance but from_utf8 doesn't like)
            self.decoder.force_error();
            None
        } else {
            self.buffer[self.buffer_len as usize] = byte;
            self.buffer_len += 1;
            // check it
            let res = core::str::from_utf8(&self.buffer[0..self.buffer_len as usize]);
            if let Ok(res2) = res {
                self.buffer_len = 0;
                if let Some(v) = res2.chars().next() {
                    self.feed_char(v)
                } else {
                    unreachable!()
                }
            } else {
                // could just mean the character hasn't finished yet
                None
            }
        }
    }

    /// Feeds into a vec from a byte slice. Remember to check for [DatumDecoder::has_error].
    /// ```
    /// use datum_rs::DatumByteDecoder;
    /// let mut decoder = datum_rs::DatumByteDecoder::default();
    /// let mut results = vec![];
    /// decoder.feed_bytes_into(&mut results, &*b"example text");
    /// assert_eq!(results.len(), 12);
    /// ```
    #[cfg(feature = "alloc")]
    #[inline]
    pub fn feed_bytes_into<'a, S: IntoIterator<Item = &'a u8>>(&mut self, target: &mut Vec<DatumChar>, source: S) {
        for v in source {
            if let Some(c) = self.feed_byte(*v) {
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
        let mut decoder = DatumByteDecoder::default();
        let mut output_iterator = output.chars();
        for v in input.bytes() {
            if let Some(c) = decoder.feed_byte(v) {
                assert_eq!(c.char(), output_iterator.next().expect("early output end"));
                assert_eq!(c.class(), out_class);
            }
        }
        assert!(!decoder.has_error());
        assert!(decoder.allowed_to_eof());
        assert_eq!(output_iterator.next(), None);
    }

    fn decoder_should_fail(input: &[u8]) {
        let mut decoder = DatumByteDecoder::default();
        for v in input {
            decoder.feed_byte(*v);
        }
        assert!(decoder.has_error());
    }

    fn decoder_should_not_allow_eof(input: &[u8]) {
        let mut decoder = DatumByteDecoder::default();
        for v in input {
            decoder.feed_byte(*v);
        }
        assert!(!decoder.allowed_to_eof());
    }

    #[test]
    fn byte_decoder_tests() {
        // -- also see decoder.rs:all_decoder_test_cases
        decoder_test("thequickbrownfoxjumpsoverthelazydog", "thequickbrownfoxjumpsoverthelazydog", DatumCharClass::Content);
        decoder_test("THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG", "THEQUICKBROWNFOXJUMPSOVERTHELAZYDOG", DatumCharClass::Content);
        decoder_test("!£$%^&*_+=[]{}~@:?/>.<,|", "!£$%^&*_+=[]{}~@:?/>.<,|", DatumCharClass::Content);
        // a few simple sanity checks
        decoder_test("\\n", "\n", DatumCharClass::Content);
        decoder_test("\\r", "\r", DatumCharClass::Content);
        decoder_test("\\t", "\t", DatumCharClass::Content);
        decoder_test("\n", "\n", DatumCharClass::Newline);
        decoder_test(";", ";", DatumCharClass::LineComment);
        decoder_test("\\;", ";", DatumCharClass::Content);
        // Hex escape check
        decoder_test("\\x0A;", "\n", DatumCharClass::Content);
        // UTF-8 encoding check
        decoder_test("\\xB9;", "¹", DatumCharClass::Content);
        decoder_test("\\x10FFff;", "\u{10FFFF}", DatumCharClass::Content);
        decoder_test("\u{10FFFF}", "\u{10FFFF}", DatumCharClass::Content);
        // --

        // failure tests
        // random continuation
        decoder_should_fail(&[0x80]);
        // start of sequence, but nothing else
        decoder_should_not_allow_eof(&[0xC0]);
        // it just keeps going and going!
        decoder_should_fail(&[0xC0, 0x80, 0x80, 0x80, 0x80]);
        // interrupted 'characters'
        decoder_should_fail(&[0xC0, 0xC0]);
    }
}
