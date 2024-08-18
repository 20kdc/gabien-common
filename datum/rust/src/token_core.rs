/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use crate::{DatumCharClass, DatumPipe};

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
    SpecialID
}

/// Action output by the tokenizer.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumTokenizerAction {
    /// Push this character to buffer.
    Push,
    /// Take token, then clear buffer.
    Token(DatumTokenType)
}

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
///     decoder.feed(b, &mut |c| {
///         tokenizer.feed(c.class(), &mut |a| {
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
///         });
///     });
/// }
/// // At the end, you have to process EOF, check for errors, etc.
/// // If you're really in a rush and don't care about errors (unterminated strings, invalid hex escapes):
/// //  Adding a single newline character to the end of your input is stable and reliable
/// // That said, if you do it, you keep the pieces
/// decoder.eof(&mut |_| {});
/// assert!(!decoder.has_error());
/// tokenizer.eof(&mut |a| {
///     match a {
///         DatumTokenizerAction::Push => {},
///         DatumTokenizerAction::Token(tt) => {
///             assert_eq!(tt, DatumTokenType::ID);
///             assert_eq!(&token[..token_len], b"some-symbol");
///             token_len = 0;
///         },
///     }
/// });
/// assert!(!tokenizer.has_error());
/// ```

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct DatumTokenizer(DatumTokenizerState, bool);

impl Default for DatumTokenizer {
    fn default() -> Self {
        Self(DatumTokenizerState::Start, false)
    }
}

impl DatumPipe for DatumTokenizer {
    // this is a bit awkward since it has to be kept in sync
    // ...which doesn't really fit the DatumPipe style!
    /// but it should work
    type Input = DatumCharClass;
    type Output = DatumTokenizerAction;

    fn has_error(&self) -> bool {
        self.1
    }

    /// Feeds an EOF to the tokenizer.
    fn eof<F: FnMut(DatumTokenizerAction)>(&mut self, f: &mut F) {
        self.0 = match self.0 {
            DatumTokenizerState::Start => DatumTokenizerState::Start,
            DatumTokenizerState::LineComment => DatumTokenizerState::Start,
            DatumTokenizerState::String => {
                self.1 = true;
                DatumTokenizerState::Start
            },
            DatumTokenizerState::ID => {
                f(DatumTokenizerAction::Token(DatumTokenType::ID));
                DatumTokenizerState::Start
            },
            DatumTokenizerState::NumericSign => {
                f(DatumTokenizerAction::Token(DatumTokenType::ID));
                DatumTokenizerState::Start
            },
            DatumTokenizerState::Numeric => {
                f(DatumTokenizerAction::Token(DatumTokenType::Numeric));
                DatumTokenizerState::Start
            },
            DatumTokenizerState::SpecialID => {
                f(DatumTokenizerAction::Token(DatumTokenType::SpecialID));
                DatumTokenizerState::Start
            }
        };
    }

    /// Given an incoming character class, returns the resulting actions.
    fn feed<F: FnMut(DatumTokenizerAction)>(&mut self, class: DatumCharClass, f: &mut F) {
        self.0 = match self.0 {
            DatumTokenizerState::Start => Self::start_feed(f, class),
            DatumTokenizerState::LineComment => {
                if class == DatumCharClass::Newline {
                    DatumTokenizerState::Start
                } else {
                    DatumTokenizerState::LineComment
                }
            },
            DatumTokenizerState::String => {
                if class == DatumCharClass::String {
                    f(DatumTokenizerAction::Token(DatumTokenType::String));
                    DatumTokenizerState::Start
                } else {
                    f(DatumTokenizerAction::Push);
                    DatumTokenizerState::String
                }
            },
            DatumTokenizerState::ID => {
                if class.potential_identifier() {
                    f(DatumTokenizerAction::Push);
                    DatumTokenizerState::ID
                } else {
                    f(DatumTokenizerAction::Token(DatumTokenType::ID));
                    Self::start_feed(f, class)
                }
            },
            DatumTokenizerState::NumericSign => {
                if class.potential_identifier() {
                    f(DatumTokenizerAction::Push);
                    DatumTokenizerState::Numeric
                } else {
                    // just a sign, so interpret as ID
                    f(DatumTokenizerAction::Token(DatumTokenType::ID));
                    Self::start_feed(f, class)
                }
            },
            DatumTokenizerState::Numeric => {
                if class.potential_identifier() {
                    f(DatumTokenizerAction::Push);
                    DatumTokenizerState::Numeric
                } else {
                    // if just "-", an ID, else numeric
                    f(DatumTokenizerAction::Token(DatumTokenType::Numeric));
                    Self::start_feed(f, class)
                }
            },
            DatumTokenizerState::SpecialID => {
                if class.potential_identifier() {
                    f(DatumTokenizerAction::Push);
                    DatumTokenizerState::SpecialID
                } else {
                    f(DatumTokenizerAction::Token(DatumTokenType::SpecialID));
                    Self::start_feed(f, class)
                }
            }
        };
    }
}

impl DatumTokenizer {
    /// Handling for the start state.
    /// This is used both in that state and when going 'through' that state when leaving another state.
    fn start_feed<F: FnMut(DatumTokenizerAction)>(f: &mut F, class: DatumCharClass) -> DatumTokenizerState {
        match class {
            DatumCharClass::Content => {
                f(DatumTokenizerAction::Push);
                DatumTokenizerState::ID
            },
            DatumCharClass::Whitespace => DatumTokenizerState::Start,
            DatumCharClass::Newline => DatumTokenizerState::Start,
            DatumCharClass::LineComment => DatumTokenizerState::LineComment,
            DatumCharClass::String => DatumTokenizerState::String,
            DatumCharClass::ListStart => {
                f(DatumTokenizerAction::Token(DatumTokenType::ListStart));
                DatumTokenizerState::Start
            },
            DatumCharClass::ListEnd => {
                f(DatumTokenizerAction::Token(DatumTokenType::ListEnd));
                DatumTokenizerState::Start
            },
            DatumCharClass::SpecialID => {
                DatumTokenizerState::SpecialID
            },
            DatumCharClass::Sign => {
                f(DatumTokenizerAction::Push);
                DatumTokenizerState::NumericSign
            },
            DatumCharClass::Digit => {
                f(DatumTokenizerAction::Push);
                DatumTokenizerState::Numeric
            },
        }
    }
}
