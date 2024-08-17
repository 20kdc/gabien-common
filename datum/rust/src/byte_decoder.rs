/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use crate::{DatumFixedArray, DatumPipe};

const UTF8_DECODE_BUFFER: usize = 4;

pub const DATUM_UTF8_DECODER_MAX_SIZE: usize = 1;

/// UTF-8 stream decoder.
#[derive(Clone, Copy, PartialEq, Eq, Debug, Default)]
pub struct DatumUTF8Decoder {
    /// UTF-8 decoding buffer
    buffer: [u8; UTF8_DECODE_BUFFER],
    /// UTF-8 decoding buffer length
    buffer_len: u8,
    has_error: bool
}

impl DatumPipe for DatumUTF8Decoder {
    type Input = u8;
    type Output = char;
    type Array = DatumFixedArray<char, DATUM_UTF8_DECODER_MAX_SIZE>;
    const MAX_SIZE: usize = DATUM_UTF8_DECODER_MAX_SIZE;

    #[inline]
    fn eof(&mut self) -> Self::Array {
        if self.buffer_len != 0 {
            self.has_error = true;
        }
        DatumFixedArray::default()
    }

    #[inline]
    fn has_error(&self) -> bool {
        self.has_error
    }

    /// Given a [u8], returns a resulting [char], if any.
    fn feed(&mut self, byte: u8) -> Self::Array {
        let mut arr = DatumFixedArray::default();
        if self.buffer_len >= (UTF8_DECODE_BUFFER as u8) {
            // this implies a UTF-8 character kept on continuing
            // and was not recognized as valid by Rust
            self.has_error = true;
            DatumFixedArray::default()
        } else if self.buffer_len == 0 {
            // first char of sequence, use special handling to catch errors early
            if byte <= 127 {
                // fast-path these
                arr.extend(Some(byte as char));
                arr
            } else if (0x80..=0xBF).contains(&byte) {
                // can't start a sequence with a continuation
                self.has_error = true;
                arr
            } else {
                // start bytes of multi-byte sequences
                self.buffer[0] = byte;
                self.buffer_len = 1;
                arr
            }
        } else if !(0x80..=0xBF).contains(&byte) {
            // we're supposed to be adding continuations and suddenly this shows up?
            // (this path also catches if a character comes in that looks fine at a glance but from_utf8 doesn't like)
            self.has_error = true;
            arr
        } else {
            self.buffer[self.buffer_len as usize] = byte;
            self.buffer_len += 1;
            // check it
            let res = core::str::from_utf8(&self.buffer[0..self.buffer_len as usize]);
            if let Ok(res2) = res {
                self.buffer_len = 0;
                if let Some(v) = res2.chars().next() {
                    arr.extend(Some(v));
                    arr
                } else {
                    unreachable!()
                }
            } else {
                // could just mean the character hasn't finished yet
                arr
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn decoder_should_fail(input: &[u8]) {
        let mut decoder = DatumUTF8Decoder::default();
        for v in input {
            decoder.feed(*v);
        }
        assert!(decoder.has_error());
    }

    fn decoder_should_not_allow_eof(input: &[u8]) {
        let mut decoder = DatumUTF8Decoder::default();
        for v in input {
            decoder.feed(*v);
        }
        assert!(!decoder.has_error());
        _ = decoder.eof();
        assert!(decoder.has_error());
    }

    #[test]
    fn byte_decoder_tests() {
        // failure tests
        // random continuation
        decoder_should_fail(&[0x80]);
        // start of sequence, but nothing else
        decoder_should_not_allow_eof(&[0xC2]);
        // it just keeps going and going!
        decoder_should_fail(&[0xC2, 0x80, 0x80, 0x80, 0x80]);
        // interrupted 'characters'
        decoder_should_fail(&[0xC2, 0xC2]);
    }
}
