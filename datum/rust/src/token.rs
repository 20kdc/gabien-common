/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::{fmt::{Display, Write}, ops::Deref};

#[cfg(feature = "alloc")]
use alloc::string::String;

use crate::{DatumPushable, DatumTokenizer, DatumChar, DatumCharClass, DatumTokenType, DatumPipe, DatumTokenizerAction};

/// Datum token with integrated string.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumToken<B: Deref<Target = str>> {
    /// String. Buffer contents are the unescaped string contents.
    String(B),
    /// ID. Buffer contents are the symbol.
    ID(B),
    /// Special ID. Buffer contents are the symbol (text after, but not including, '#').
    SpecialID(B),
    /// Numeric. Buffer contents are simply the token.
    Numeric(B),
    ListStart,
    ListEnd
}

impl<B: Deref<Target = str>> Default for DatumToken<B> {
    fn default() -> Self {
        // arbitrarily chosen
        // this is array filler
        Self::ListEnd
    }
}

impl<B: Deref<Target = str>> DatumToken<B> {
    /// Creates a DatumToken using a String or something that can be converted into it (such as [&str]).
    /// It is possible to serialize anything you pass here into a token that can be read back, except that the three alone-group characters do not have buffers (so these will be lost).
    pub fn new<T: Into<B>>(tt: DatumTokenType, text: T) -> Self {
        match tt {
            DatumTokenType::String => Self::String(text.into()),
            DatumTokenType::ID => Self::ID(text.into()),
            DatumTokenType::SpecialID => Self::SpecialID(text.into()),
            DatumTokenType::Numeric => Self::Numeric(text.into()),
            DatumTokenType::ListStart => Self::ListStart,
            DatumTokenType::ListEnd => Self::ListEnd,
        }
    }
    /// Similar to [DatumToken::new] but doesn't perform .into (prevents type issues sometimes)
    pub fn new_not_into(tt: DatumTokenType, text: B) -> Self {
        match tt {
            DatumTokenType::String => Self::String(text),
            DatumTokenType::ID => Self::ID(text),
            DatumTokenType::SpecialID => Self::SpecialID(text),
            DatumTokenType::Numeric => Self::Numeric(text),
            DatumTokenType::ListStart => Self::ListStart,
            DatumTokenType::ListEnd => Self::ListEnd,
        }
    }

    /// Return the token type of this token.
    #[cfg(not(tarpaulin_include))]
    pub fn token_type(&self) -> DatumTokenType {
        match self {
            Self::String(_) => DatumTokenType::String,
            Self::ID(_) => DatumTokenType::ID,
            Self::SpecialID(_) => DatumTokenType::SpecialID,
            Self::Numeric(_) => DatumTokenType::Numeric,
            Self::ListStart => DatumTokenType::ListStart,
            Self::ListEnd => DatumTokenType::ListEnd,
        }
    }

    /// Return the buffer of this token, if the type has one.
    #[cfg(not(tarpaulin_include))]
    pub fn buffer(&self) -> Option<&B> {
        match &self {
            Self::String(b) => Some(b),
            Self::ID(b) => Some(b),
            Self::SpecialID(b) => Some(b),
            Self::Numeric(b) => Some(b),
            _ => None
        }
    }

    /// Writes this value as a valid, parsable Datum token.
    pub fn write(&self, f: &mut dyn Write) -> core::fmt::Result {
        match self {
            Self::String(b) => {
                f.write_char('\"')?;
                for v in b.deref().chars() {
                    if v == '\\' {
                        f.write_char('\\')?;
                        f.write_char('\\')?;
                    } else if v == '\r' {
                        f.write_char('\\')?;
                        f.write_char('r')?;
                    } else if v == '\n' {
                        f.write_char('\\')?;
                        f.write_char('n')?;
                    } else if v == '\t' {
                        f.write_char('\\')?;
                        f.write_char('t')?;
                    } else {
                        f.write_char(v)?;
                    }
                }
                f.write_char('\"')?;
            },
            Self::ID(b) => {
                let mut chars = b.chars();
                match chars.next() {
                    Some(v) => {
                        if DatumCharClass::identify(v) == Some(DatumCharClass::Sign) {
                            match chars.next() {
                                Some(v2) => {
                                    // business as usual
                                    DatumChar::content(v).write(f)?;
                                    DatumChar::content(v2).write(f)?;
                                },
                                None => {
                                    // lone sign
                                    return f.write_char(v);
                                }
                            }
                        } else {
                            DatumChar::content(v).write(f)?;
                        }
                        for remainder in chars {
                            DatumChar::potential_identifier(remainder).write(f)?;
                        }
                    },
                    None => {
                        f.write_char('#')?;
                        f.write_char('{')?;
                        f.write_char('}')?;
                        f.write_char('#')?;
                    }
                }
            },
            Self::SpecialID(b) => {
                f.write_char('#')?;
                let chars = b.chars();
                for remainder in chars {
                    DatumChar::potential_identifier(remainder).write(f)?;
                }
            },
            Self::Numeric(b) => {
                let mut chars = b.chars();
                match chars.next() {
                    Some(v0) => {
                        let v0dc = DatumChar::identify(v0);
                        if let Some(v0dc) = v0dc {
                            if v0dc.numeric_start() {
                                match chars.next() {
                                    Some(v1) => {
                                        // success
                                        v0dc.write(f)?;
                                        DatumChar::potential_identifier(v1).write(f)?;
                                        for remainder in chars {
                                            DatumChar::potential_identifier(remainder).write(f)?;
                                        }
                                    },
                                    None => {
                                        if v0dc.class() == DatumCharClass::Sign {
                                            // fallback as lone sign
                                            f.write_char('#')?;
                                            f.write_char('i')?;
                                            f.write_char(v0dc.char())?;
                                        } else {
                                            // success as lone digit
                                            f.write_char(v0dc.char())?;
                                        }
                                    }
                                }
                            } else {
                                // fallback as start is not numeric
                                f.write_char('#')?;
                                f.write_char('i')?;
                                DatumChar::potential_identifier(v0).write(f)?;
                                for remainder in chars {
                                    DatumChar::potential_identifier(remainder).write(f)?;
                                }
                            }
                        } else {
                            // fallback as start is backslash
                            f.write_char('#')?;
                            f.write_char('i')?;
                            DatumChar::potential_identifier(v0).write(f)?;
                            for remainder in chars {
                                DatumChar::potential_identifier(remainder).write(f)?;
                            }
                        }
                    },
                    None => {
                        // fallback as empty
                        f.write_char('#')?;
                        f.write_char('i')?;
                    }
                }
            },
            Self::ListStart => {
                f.write_char('(')?;
            },
            Self::ListEnd => {
                f.write_char(')')?;
            }
        }
        core::fmt::Result::Ok(())
    }
}

impl<B: Deref<Target = str>> Display for DatumToken<B> {
    fn fmt(&self, f: &mut core::fmt::Formatter<'_>) -> core::fmt::Result {
        self.write(f)
    }
}

/// Tokenizer that uses String as an internal buffer and spits out DatumToken.
/// ```
/// use datum_rs::{DatumDecoder, DatumToken, DatumStringTokenizer, DatumComposePipe, DatumPipe};
/// let mut decoder = DatumDecoder::default().compose(DatumStringTokenizer::default());
/// let mut out = Vec::new();
/// decoder.feed_iter_to_vec(&mut out, ("these become test symbols").chars(), true);
/// ```
#[derive(Clone, Default, Debug)]
pub struct DatumPipeTokenizer<B: DatumPushable<char> + Deref<Target = str> + Default>(B, DatumTokenizer, bool);

#[cfg(feature = "alloc")]
pub type DatumStringTokenizer = DatumPipeTokenizer<String>;

impl<B: DatumPushable<char> + Deref<Target = str> + Default> DatumPipe for DatumPipeTokenizer<B> {
    type Input = DatumChar;
    type Output = DatumToken<B>;

    fn feed<F: FnMut(Self::Output)>(&mut self, i: Self::Input, f: &mut F) {
        let m0 = &mut self.0;
        let m2 = &mut self.2;
        self.1.feed(i.class(), &mut |v| {
            Self::transform_action(m0, m2, i.char(), v, f)
        })
    }

    fn eof<F: FnMut(Self::Output)>(&mut self, f: &mut F) {
        let m0 = &mut self.0;
        let m2 = &mut self.2;
        self.1.eof(&mut |v| {
            Self::transform_action(m0, m2, ' ', v, f)
        })
    }

    fn has_error(&self) -> bool {
        self.1.has_error() || self.2
    }
}

impl<B: DatumPushable<char> + Deref<Target = str> + Default> DatumPipeTokenizer<B> {
    fn transform_action<F: FnMut(DatumToken<B>)>(buffer: &mut B, error: &mut bool, char: char, action: DatumTokenizerAction, f: &mut F) {
        match action {
            DatumTokenizerAction::Push => {
                if buffer.push(char).is_err() {
                    *error = true;
                }
            },
            DatumTokenizerAction::Token(v) => {
                f(DatumToken::new(v, core::mem::take(buffer)));
            }
        }
    }
}
