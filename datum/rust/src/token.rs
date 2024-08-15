/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use crate::{DatumBuffer, DatumChar, DatumCharClass, DatumDecoder, DatumDecoderResults};

/// Datum token type.
/// This is paired with some state in the DatumBuffer implementation.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumTokenType {
    /// String. Buffer contents are the unescaped string contents.
    String,
    /// ID. Buffer contents are the symbol.
    ID,
    /// Special ID. Buffer contents are the symbol (text after, but not including, '#').
    SpecialID,
    /// Numeric
    Numeric,
    /// Quote. Buffer is empty.
    Quote,
    /// List start. Buffer is empty.
    ListStart,
    /// List end. Buffer is empty.
    ListEnd
}

/// Datum token with integrated type.
/// TODO: Actually use this
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumToken<B: DatumBuffer> {
    /// String. Buffer contents are the unescaped string contents.
    String(B),
    /// ID. Buffer contents are the symbol.
    ID(B),
    /// Special ID. Buffer contents are the symbol (text after, but not including, '#').
    SpecialID(B),
    /// Numeric. Buffer contents are simply the token.
    Numeric(B),
    Quote,
    ListStart,
    ListEnd
}

impl<B: DatumBuffer> DatumToken<B> {
    /// Return the token type of this token.
    pub fn token_type(&self) -> DatumTokenType {
        match self {
            Self::String(_) => DatumTokenType::String,
            Self::ID(_) => DatumTokenType::ID,
            Self::SpecialID(_) => DatumTokenType::SpecialID,
            Self::Numeric(_) => DatumTokenType::Numeric,
            Self::Quote => DatumTokenType::Quote,
            Self::ListStart => DatumTokenType::ListStart,
            Self::ListEnd => DatumTokenType::ListEnd,
        }
    }
    /// Return the buffer of this token, if the type has one.
    pub fn buffer<'a>(&'a self) -> Option<&'a B> {
        match &self {
            Self::String(b) => Some(b),
            Self::ID(b) => Some(b),
            Self::SpecialID(b) => Some(b),
            Self::Numeric(b) => Some(b),
            _ => None
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum DatumTokenizerState {
    /// start of whitespace_skipping block
    Start,
    /// comment block, not_newline
    LineComment,
    /// string block, not_"
    String,
    /// potential identifier block, content_class
    ID,
    /// potential identifier block, numeric_start_class_and_45/numeric_start_class_but_not_45
    /// Boolean flag indicates 'minus'
    Numeric(bool),
    /// potential identifier block
    SpecialID
}
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum DatumTokenizerEOFResult {
    Nothing,
    Error,
    Token(DatumTokenType),
}
impl DatumTokenizerState {
    /// Returns (new state, repeat this char, truncation warning, resulting token)
    /// If there is a resulting token, caller truncates buffer
    fn advance<B: DatumBuffer>(self, char: DatumChar, buffer: &mut B) -> (Self, bool, bool, Option<DatumTokenType>) {
        match self {
            Self::Start => {
                match char.class() {
                    DatumCharClass::Content => {
                        let t = !buffer.push(char.byte());
                        (Self::ID, false, t, None)
                    },
                    DatumCharClass::Whitespace => {
                        (Self::Start, false, false, None)
                    },
                    DatumCharClass::Newline => {
                        (Self::Start, false, false, None)
                    },
                    DatumCharClass::LineComment => {
                        (Self::LineComment, false, false, None)
                    },
                    DatumCharClass::String => {
                        (Self::String, false, false, None)
                    },
                    DatumCharClass::Quote => {
                        (Self::Start, false, false, Some(DatumTokenType::Quote))
                    },
                    DatumCharClass::ListStart => {
                        (Self::Start, false, false, Some(DatumTokenType::ListStart))
                    },
                    DatumCharClass::ListEnd => {
                        (Self::Start, false, false, Some(DatumTokenType::ListEnd))
                    },
                    DatumCharClass::SpecialID => {
                        (Self::SpecialID, false, false, None)
                    },
                    DatumCharClass::NumericStart => {
                        let t = !buffer.push(char.byte());
                        (Self::Numeric(char.byte() == b'-'), false, t, None)
                    },
                }
            },
            Self::LineComment => {
                if char.class() == DatumCharClass::Newline {
                    (Self::Start, false, false, None)
                } else {
                    (Self::LineComment, false, false, None)
                }
            },
            Self::String => {
                if char.class() == DatumCharClass::String {
                    (Self::Start, false, false, Some(DatumTokenType::String))
                } else {
                    let t = !buffer.push(char.byte());
                    (Self::String, false, t, None)
                }
            },
            Self::ID => {
                if char.class().potential_identifier() {
                    let t = !buffer.push(char.byte());
                    (Self::ID, false, t, None)
                } else {
                    (Self::Start, true, false, Some(DatumTokenType::ID))
                }
            },
            Self::Numeric(minus) => {
                if char.class().potential_identifier() {
                    let t = !buffer.push(char.byte());
                    (Self::Numeric(false), false, t, None)
                } else {
                    // if just "-", an ID, else numeric
                    let res = if minus { DatumTokenType::ID } else {DatumTokenType::Numeric};
                    (Self::Start, true, false, Some(res))
                }
            },
            Self::SpecialID => {
                if char.class().potential_identifier() {
                    let t = !buffer.push(char.byte());
                    (Self::SpecialID, false, t, None)
                } else {
                    (Self::Start, true, false, Some(DatumTokenType::SpecialID))
                }
            }
        }
    }
    fn eof(&mut self) -> DatumTokenizerEOFResult {
        let res = match self {
            Self::Start => DatumTokenizerEOFResult::Nothing,
            Self::LineComment => DatumTokenizerEOFResult::Nothing,
            Self::String => DatumTokenizerEOFResult::Error,
            Self::ID => DatumTokenizerEOFResult::Token(DatumTokenType::ID),
            Self::Numeric(minus) => {
                let res = if *minus { DatumTokenType::ID } else {DatumTokenType::Numeric};
                DatumTokenizerEOFResult::Token(res)
            },
            Self::SpecialID => DatumTokenizerEOFResult::Token(DatumTokenType::SpecialID),
        };
        *self = Self::Start;
        res
    }
}

/// Datum tokenizer.
#[derive(Clone, Copy, Debug)]
pub struct DatumTokenizer<'byte, I: Iterator<Item = &'byte u8>, B: DatumBuffer> {
    /// Decoder,
    decoder: DatumDecoder,
    /// Decoder results
    decoder_results: DatumDecoderResults,
    /// char repeat
    char_buffer: Option<DatumChar>,
    /// Source
    source: I,
    /// Buffer
    buffer: B,
    /// Tokenizer state
    state: DatumTokenizerState,
}

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
enum DatumTokenizerDecoderResult {
    Ok(DatumChar),
    EOF,
    Error,
}

/// Result of the tokenizer.
/// The actual result has a borrow to the buffer stapled onto it with a tuple.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumTokenizerResult<B: DatumBuffer> {
    /// Got a token
    Token(B, DatumTokenType),
    /// Truncated due to not enough buffer space.
    TruncatedToken(B, DatumTokenType),
    /// Tokenization error (unterminated string, invalid hex escape...)
    Error
}

impl<'byte, I: Iterator<Item = &'byte u8>, B: DatumBuffer> DatumTokenizer<'byte, I, B> {
    pub fn new(source: I, buffer: B) -> Self {
        Self {
            decoder: DatumDecoder::new(),
            decoder_results: DatumDecoderResults::default(),
            char_buffer: None,
            source,
            buffer,
            state: DatumTokenizerState::Start
        }
    }
    fn get_next_datum_char(&mut self) -> DatumTokenizerDecoderResult {
        if let Some(char) = self.char_buffer {
            self.char_buffer = None;
            DatumTokenizerDecoderResult::Ok(char)
        } else {
            loop {
                if let Some(char) = self.decoder_results.next() {
                    return DatumTokenizerDecoderResult::Ok(char);
                } else {
                    match self.source.next() {
                        Some(v) => {
                            self.decoder_results = self.decoder.feed(*v);
                        },
                        None => {
                            if !self.decoder.allowed_to_eof() {
                                // reset decoder so we don't loop endlessly, return error
                                self.decoder = DatumDecoder::new();
                                return DatumTokenizerDecoderResult::Error;
                            } else {
                                return DatumTokenizerDecoderResult::EOF;
                            }
                        }
                    }
                }
            }
        }
    }
}

impl<'byte, I: Iterator<Item = &'byte u8>, B: DatumBuffer> Iterator for DatumTokenizer<'byte, I, B> {
    type Item = DatumTokenizerResult<B>;
    fn next(&mut self) -> Option<Self::Item> {
        let mut truncation_error = false;
        loop {
            match self.get_next_datum_char() {
                DatumTokenizerDecoderResult::Ok(char) => {
                    let res = self.state.advance(char, &mut self.buffer);
                    self.state = res.0;
                    // repeat?
                    if res.1 {
                        self.char_buffer = Some(char);
                    }
                    // truncated?
                    if res.2 {
                        // yes - set error flag
                        // when we reach the end of the token, the error is returned
                        truncation_error = true;
                    }
                    if let Some(token) = res.3 {
                        let finale = if truncation_error {
                            Some(DatumTokenizerResult::TruncatedToken(self.buffer.clone(), token))
                        } else {
                            Some(DatumTokenizerResult::Token(self.buffer.clone(), token))
                        };
                        self.buffer.truncate(0);
                        return finale;
                    }
                },
                DatumTokenizerDecoderResult::EOF => {
                    return match self.state.eof() {
                        DatumTokenizerEOFResult::Token(token) => {
                            let finale = if truncation_error {
                                Some(DatumTokenizerResult::TruncatedToken(self.buffer.clone(), token))
                            } else {
                                Some(DatumTokenizerResult::Token(self.buffer.clone(), token))
                            };
                            self.buffer.truncate(0);
                            finale
                        },
                        DatumTokenizerEOFResult::Nothing => None,
                        DatumTokenizerEOFResult::Error => Some(DatumTokenizerResult::Error)
                    }
                },
                DatumTokenizerDecoderResult::Error => {
                    self.decoder = DatumDecoder::new();
                    return Some(DatumTokenizerResult::Error);
                },
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use crate::DatumFixedBuffer;

    use super::*;

    fn unwrap_token<'a, const SIZE: usize>(res: &'a DatumTokenizerResult<DatumFixedBuffer<SIZE>>, ty: DatumTokenType) -> &'a [u8] {
        if let DatumTokenizerResult::Token(text, tt) = res {
            assert_eq!(*tt, ty);
            text
        } else {
            panic!("bad! expected token, got {:?}", res);
        }
    }

    #[test]
    fn check_simple() {
        let test_slice: &[u8] = b"test(world)";
        let buf: DatumFixedBuffer<8> = DatumFixedBuffer::default();
        let mut res = DatumTokenizer::new(test_slice.iter(), buf);
        let mut tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"test");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListStart), b"");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"world");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListEnd), b"");
        assert!(res.next().is_none());
    }

    #[test]
    fn check_complex() {
        let test_slice: &[u8] = b"(\n; Symbols & lists test\n(moku sina)\nli\n(pona)\n(tawa mi)\n; Exceptional cases\n#t #f #{}# \\#escapethis \\1234 #nil\n; Floats, strings\n0.125 \"Hello\\r\\n\\t\\x5000;\\x10000;\" 'hi\n)";
        let buf: DatumFixedBuffer<32> = DatumFixedBuffer::default();
        let mut res = DatumTokenizer::new(test_slice.iter(), buf);
        let mut tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListStart), b"");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListStart), b"");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"moku");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"sina");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListEnd), b"");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"li");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListStart), b"");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"pona");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListEnd), b"");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListStart), b"");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"tawa");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"mi");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListEnd), b"");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::SpecialID), b"t");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::SpecialID), b"f");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::SpecialID), b"{}#");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"#escapethis");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"1234");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::SpecialID), b"nil");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::Numeric), b"0.125");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::String), "Hello\r\n\t\u{5000}\u{10000}".as_bytes());
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::Quote), b"");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ID), b"hi");
        tmp = res.next().unwrap();
        assert_eq!(unwrap_token(&tmp, DatumTokenType::ListEnd), b"");
        assert!(res.next().is_none());
    }

    #[test]
    fn check_truncation() {
        let test_slice: &[u8] = b"thisisaverylongtoken #anotherverylongtoken 123456789";
        let buf: DatumFixedBuffer<8> = DatumFixedBuffer::default();
        let mut res = DatumTokenizer::new(test_slice.iter(), buf);
        let mut tmp = res.next().unwrap();
        if let DatumTokenizerResult::TruncatedToken(buf, typ) = tmp {
            assert_eq!(typ, DatumTokenType::ID);
            assert_eq!(&*buf, b"thisisav");
        } else {
            panic!("unexpected");
        }
        tmp = res.next().unwrap();
        if let DatumTokenizerResult::TruncatedToken(buf, typ) = tmp {
            assert_eq!(typ, DatumTokenType::SpecialID);
            assert_eq!(&*buf, b"anotherv");
        } else {
            panic!("unexpected");
        }
        tmp = res.next().unwrap();
        if let DatumTokenizerResult::TruncatedToken(buf, typ) = tmp {
            assert_eq!(typ, DatumTokenType::Numeric);
            assert_eq!(&*buf, b"12345678");
        } else {
            panic!("unexpected");
        }
    }
}
