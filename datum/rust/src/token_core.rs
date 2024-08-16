/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use crate::{DatumPushable, DatumCharClass, DatumFixedArray, DatumPipe};

/// Datum token type.
/// This is paired with the token contents, if any.
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
    /// potential identifier block, sign_class
    NumericSign,
    /// potential identifier block, digit_class
    Numeric,
    /// potential identifier block
    SpecialID,
    Error
}

/// Action output by the tokenizer.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumTokenizerAction {
    /// Push this character to buffer.
    Push,
    /// Take token, then clear buffer.
    Token(DatumTokenType)
}

impl Default for DatumTokenizerAction {
    fn default() -> Self {
        Self::Push
    }
}

pub const DATUM_TOKENIZER_MAX_SIZE: usize = 2;

/// Datum tokenizer state machine.
/// This API is a little harder to use, but allows complete control over buffer allocation/etc.
/// In particular, it works with char classes, and expects you to keep track of bytes it sends your way with the [DatumTokenizerAction::Push] action.
/// When a token is complete, you will receive the [DatumTokenizerAction::Token] action.
/// You should also call [DatumTokenizer::eof] when relevant to get any token at the very end of the file.
/// ```
/// use datum_rs::{DatumDecoder, DatumPipe, DatumTokenizer, DatumTokenizerAction, DatumTokenType};
/// let example = "some-symbol ; ignored comment";
/// let mut decoder = DatumDecoder::default();
/// let mut tokenizer = DatumTokenizer::default();
/// // use u8 for example's sake since we know this is all ASCII
/// // in practice you'd either use String or a proper on-stack string library
/// let mut token: [u8; 11] = [0; 11];
/// let mut token_len: usize = 0;
/// for b in example.chars() {
///     for c in decoder.feed(b) {
///         for a in tokenizer.feed(c.class()) {
///             match a {
///                 DatumTokenizerAction::Push => {
///                     token[token_len] = c.char() as u8;
///                     token_len += 1;
///                 },
///                 DatumTokenizerAction::Token(tt) => {
///                     // Example 'parser': only accepts sequences of this one symbol
///                     assert_eq!(tt, DatumTokenType::ID);
///                     assert_eq!(&token[..token_len], b"some-symbol");
///                     token_len = 0;
///                 },
///             }
///         }
///     }
/// }
/// // At the end, you have to process EOF, check for errors, etc.
/// // If you're really in a rush and don't care about errors (unterminated strings, invalid hex escapes):
/// //  Adding a single newline character to the end of your input is stable and reliable
/// // That said, if you do it, you keep the pieces
/// _ = decoder.eof();
/// assert!(!decoder.has_error());
/// for a in tokenizer.eof() {
///     match a {
///         DatumTokenizerAction::Push => {},
///         DatumTokenizerAction::Token(tt) => {
///             assert_eq!(tt, DatumTokenType::ID);
///             assert_eq!(&token[..token_len], b"some-symbol");
///             token_len = 0;
///         },
///     }
/// }
/// assert!(!tokenizer.has_error());
/// ```

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct DatumTokenizer(DatumTokenizerState);

impl Default for DatumTokenizer {
    fn default() -> Self {
        Self(DatumTokenizerState::Start)
    }
}

impl DatumPipe for DatumTokenizer {
    // this is a bit awkward since it has to be kept in sync
    // ...which doesn't really fit the DatumPipe style!
    /// but it should work
    type Input = DatumCharClass;
    type Output = DatumTokenizerAction;
    type Array = DatumFixedArray<Self::Output, DATUM_TOKENIZER_MAX_SIZE>;
    const MAX_SIZE: usize = DATUM_TOKENIZER_MAX_SIZE;

    fn has_error(&self) -> bool {
        self.0 == DatumTokenizerState::Error
    }

    /// Feeds an EOF to the tokenizer.
    /// This also resets it.
    fn eof(&mut self) -> Self::Array {
        let mut arr: Self::Array = DatumFixedArray::default();
        self.0 = match self.0 {
            DatumTokenizerState::Start => {
                DatumTokenizerState::Start
            },
            DatumTokenizerState::LineComment => {
                DatumTokenizerState::Start
            },
            DatumTokenizerState::String => {
                DatumTokenizerState::Error
            },
            DatumTokenizerState::ID => {
                arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::ID));
                DatumTokenizerState::Start
            },
            DatumTokenizerState::NumericSign => {
                arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::ID));
                DatumTokenizerState::Start
            },
            DatumTokenizerState::Numeric => {
                arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::Numeric));
                DatumTokenizerState::Start
            },
            DatumTokenizerState::SpecialID => {
                arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::SpecialID));
                DatumTokenizerState::Start
            },
            DatumTokenizerState::Error => {
                DatumTokenizerState::Error
            }
        };
        arr
    }

    /// Given an incoming character class, returns the resulting actions.
    fn feed(&mut self, class: DatumCharClass) -> Self::Array {
        let mut arr: DatumFixedArray<Self::Output, DATUM_TOKENIZER_MAX_SIZE> = DatumFixedArray::default();
        self.0 = match self.0 {
            DatumTokenizerState::Start => Self::start_feed(&mut arr, class),
            DatumTokenizerState::LineComment => {
                if class == DatumCharClass::Newline {
                    DatumTokenizerState::Start
                } else {
                    DatumTokenizerState::LineComment
                }
            },
            DatumTokenizerState::String => {
                if class == DatumCharClass::String {
                    arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::String));
                    DatumTokenizerState::Start
                } else {
                    arr.push_unwrap(DatumTokenizerAction::Push);
                    DatumTokenizerState::String
                }
            },
            DatumTokenizerState::ID => {
                if class.potential_identifier() {
                    arr.push_unwrap(DatumTokenizerAction::Push);
                    DatumTokenizerState::ID
                } else {
                    arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::ID));
                    Self::start_feed(&mut arr, class)
                }
            },
            DatumTokenizerState::NumericSign => {
                if class.potential_identifier() {
                    arr.push_unwrap(DatumTokenizerAction::Push);
                    DatumTokenizerState::Numeric
                } else {
                    // just a sign, so interpret as ID
                    arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::ID));
                    Self::start_feed(&mut arr, class)
                }
            },
            DatumTokenizerState::Numeric => {
                if class.potential_identifier() {
                    arr.push_unwrap(DatumTokenizerAction::Push);
                    DatumTokenizerState::Numeric
                } else {
                    // if just "-", an ID, else numeric
                    arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::Numeric));
                    Self::start_feed(&mut arr, class)
                }
            },
            DatumTokenizerState::SpecialID => {
                if class.potential_identifier() {
                    arr.push_unwrap(DatumTokenizerAction::Push);
                    DatumTokenizerState::SpecialID
                } else {
                    arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::SpecialID));
                    Self::start_feed(&mut arr, class)
                }
            },
            DatumTokenizerState::Error => DatumTokenizerState::Error
        };
        arr
    }
}

impl DatumTokenizer {
    /// Handling for the start state.
    /// This is used both in that state and when going 'through' that state when leaving another state.
    fn start_feed(arr: &mut DatumFixedArray<DatumTokenizerAction, DATUM_TOKENIZER_MAX_SIZE>, class: DatumCharClass) -> DatumTokenizerState {
        match class {
            DatumCharClass::Content => {
                arr.push_unwrap(DatumTokenizerAction::Push);
                DatumTokenizerState::ID
            },
            DatumCharClass::Whitespace => DatumTokenizerState::Start,
            DatumCharClass::Newline => DatumTokenizerState::Start,
            DatumCharClass::LineComment => DatumTokenizerState::LineComment,
            DatumCharClass::String => DatumTokenizerState::String,
            DatumCharClass::Quote => {
                arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::Quote));
                DatumTokenizerState::Start
            },
            DatumCharClass::ListStart => {
                arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::ListStart));
                DatumTokenizerState::Start
            },
            DatumCharClass::ListEnd => {
                arr.push_unwrap(DatumTokenizerAction::Token(DatumTokenType::ListEnd));
                DatumTokenizerState::Start
            },
            DatumCharClass::SpecialID => {
                DatumTokenizerState::SpecialID
            },
            DatumCharClass::Sign => {
                arr.push_unwrap(DatumTokenizerAction::Push);
                DatumTokenizerState::NumericSign
            },
            DatumCharClass::Digit => {
                arr.push_unwrap(DatumTokenizerAction::Push);
                DatumTokenizerState::Numeric
            },
        }
    }
}
