/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use crate::DatumCharClass;

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
    SpecialID
}

/// Action output by the tokenizer.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumTokenizerAction {
    /// Stop iterating over actions in this [DatumTokenizerResult].
    Break,
    /// Push this character to buffer.
    Push,
    /// Take token, then clear buffer.
    Token(DatumTokenType)
}

/// Actions output by the tokenizer for a character.
pub type DatumTokenizerResult = [DatumTokenizerAction; 2];

/// Continue result.
pub(crate) const TRES_CONTINUE: DatumTokenizerResult = [DatumTokenizerAction::Break, DatumTokenizerAction::Break];
/// Push/continue result.
pub(crate) const TRES_PUSH: DatumTokenizerResult = [DatumTokenizerAction::Push, DatumTokenizerAction::Break];
/// String/continue result.
pub(crate) const TRES_STRING: DatumTokenizerResult = [DatumTokenizerAction::Token(DatumTokenType::String), DatumTokenizerAction::Break];

/// Result on EOF.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumTokenizerEOFResult {
    Nothing,
    Error,
    Token(DatumTokenType),
}

/// Datum tokenizer state machine.
/// This API is a little harder to use, but allows complete control over buffer allocation/etc.
/// In particular, it works with char classes, and expects you to keep track of bytes it sends your way with the [DatumTokenizerAction::Push] action.
/// When a token is complete, you will receive the [DatumTokenizerAction::Token] action.
/// You should also call [DatumTokenizer::eof] when relevant to get any token at the very end of the file.
/// ```
/// use datum_rs::{DatumDecoder, DatumTokenizer, DatumTokenizerAction, DatumTokenizerEOFResult, DatumTokenType};
/// let example = "some-symbol ; ignored comment";
/// let mut decoder = DatumDecoder::default();
/// let mut tokenizer = DatumTokenizer::default();
/// // use u8 for example's sake since we know this is all ASCII
/// // in practice you'd either use String or a proper on-stack string library
/// let mut token: [u8; 11] = [0; 11];
/// let mut token_len: usize = 0;
/// for b in example.chars() {
///     for c in decoder.feed_char(b) {
///         for a in tokenizer.feed(c.class()) {
///             match a {
///                 DatumTokenizerAction::Break => break,
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
/// assert!(decoder.allowed_to_eof());
/// match tokenizer.eof() {
///     DatumTokenizerEOFResult::Nothing => {},
///     DatumTokenizerEOFResult::Error => assert!(false),
///     DatumTokenizerEOFResult::Token(tt) => {
///         assert_eq!(tt, DatumTokenType::ID);
///         assert_eq!(&token[..token_len], b"some-symbol");
///         token_len = 0;
///     },
/// }
/// ```

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct DatumTokenizer(DatumTokenizerState);

impl Default for DatumTokenizer {
    fn default() -> Self {
        Self(DatumTokenizerState::Start)
    }
}

impl DatumTokenizer {
    /// Given an incoming character class, returns the resulting actions.
    pub fn feed(&mut self, class: DatumCharClass) -> DatumTokenizerResult {
        let res = match self.0 {
            DatumTokenizerState::Start => {
                let res2 = Self::start_feed(class);
                (res2.0, [res2.1, DatumTokenizerAction::Break])
            },
            DatumTokenizerState::LineComment => {
                if class == DatumCharClass::Newline {
                    (DatumTokenizerState::Start, TRES_CONTINUE)
                } else {
                    (DatumTokenizerState::LineComment, TRES_CONTINUE)
                }
            },
            DatumTokenizerState::String => {
                if class == DatumCharClass::String {
                    (DatumTokenizerState::Start, TRES_STRING)
                } else {
                    (DatumTokenizerState::String, TRES_PUSH)
                }
            },
            DatumTokenizerState::ID => {
                if class.potential_identifier() {
                    (DatumTokenizerState::ID, TRES_PUSH)
                } else {
                    Self::token_then_repeat(class, DatumTokenType::ID)
                }
            },
            DatumTokenizerState::NumericSign => {
                if class.potential_identifier() {
                    (DatumTokenizerState::Numeric, TRES_PUSH)
                } else {
                    // just a sign, so interpret as ID
                    Self::token_then_repeat(class, DatumTokenType::ID)
                }
            },
            DatumTokenizerState::Numeric => {
                if class.potential_identifier() {
                    (DatumTokenizerState::Numeric, TRES_PUSH)
                } else {
                    // if just "-", an ID, else numeric
                    Self::token_then_repeat(class, DatumTokenType::Numeric)
                }
            },
            DatumTokenizerState::SpecialID => {
                if class.potential_identifier() {
                    (DatumTokenizerState::SpecialID, TRES_PUSH)
                } else {
                    Self::token_then_repeat(class, DatumTokenType::SpecialID)
                }
            }
        };
        self.0 = res.0;
        res.1
    }

    fn token_then_repeat(class: DatumCharClass, token: DatumTokenType) -> (DatumTokenizerState, DatumTokenizerResult) {
        let res = Self::start_feed(class);
        (res.0, [DatumTokenizerAction::Token(token), res.1])
    }

    /// Handling for the start state.
    /// This is used both in that state and when going 'through' that state when leaving another state.
    fn start_feed(class: DatumCharClass) -> (DatumTokenizerState, DatumTokenizerAction) {
        match class {
            DatumCharClass::Content => {
                (DatumTokenizerState::ID, DatumTokenizerAction::Push)
            },
            DatumCharClass::Whitespace => {
                (DatumTokenizerState::Start, DatumTokenizerAction::Break)
            },
            DatumCharClass::Newline => {
                (DatumTokenizerState::Start, DatumTokenizerAction::Break)
            },
            DatumCharClass::LineComment => {
                (DatumTokenizerState::LineComment, DatumTokenizerAction::Break)
            },
            DatumCharClass::String => {
                (DatumTokenizerState::String, DatumTokenizerAction::Break)
            },
            DatumCharClass::Quote => {
                (DatumTokenizerState::Start, DatumTokenizerAction::Token(DatumTokenType::Quote))
            },
            DatumCharClass::ListStart => {
                (DatumTokenizerState::Start, DatumTokenizerAction::Token(DatumTokenType::ListStart))
            },
            DatumCharClass::ListEnd => {
                (DatumTokenizerState::Start, DatumTokenizerAction::Token(DatumTokenType::ListEnd))
            },
            DatumCharClass::SpecialID => {
                (DatumTokenizerState::SpecialID, DatumTokenizerAction::Break)
            },
            DatumCharClass::Sign => {
                (DatumTokenizerState::NumericSign, DatumTokenizerAction::Push)
            },
            DatumCharClass::Digit => {
                (DatumTokenizerState::Numeric, DatumTokenizerAction::Push)
            },
        }
    }

    /// Feeds an EOF to the tokenizer.
    /// This also resets it.
    pub fn eof(&mut self) -> DatumTokenizerEOFResult {
        let res = match self.0 {
            DatumTokenizerState::Start => DatumTokenizerEOFResult::Nothing,
            DatumTokenizerState::LineComment => DatumTokenizerEOFResult::Nothing,
            DatumTokenizerState::String => DatumTokenizerEOFResult::Error,
            DatumTokenizerState::ID => DatumTokenizerEOFResult::Token(DatumTokenType::ID),
            DatumTokenizerState::NumericSign => DatumTokenizerEOFResult::Token(DatumTokenType::ID),
            DatumTokenizerState::Numeric => DatumTokenizerEOFResult::Token(DatumTokenType::Numeric),
            DatumTokenizerState::SpecialID => DatumTokenizerEOFResult::Token(DatumTokenType::SpecialID),
        };
        self.0 = DatumTokenizerState::Start;
        res
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn verify_reset(dt: &mut DatumTokenizer) {
        assert_eq!(dt, &DatumTokenizer::default());
    }
    fn check_eof_case(dt: &mut DatumTokenizer, eof: DatumTokenizerEOFResult) {
        assert_eq!(dt.eof(), eof);
        verify_reset(dt);
    }

    #[test]
    fn check_tests() {
        let mut dt = DatumTokenizer::default();

        // "" (tests EOF when tokenizer is reset)
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Nothing);

        // " "
        assert_eq!(dt.feed(DatumCharClass::Whitespace), TRES_CONTINUE);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Nothing);

        // "\n"
        assert_eq!(dt.feed(DatumCharClass::Newline), TRES_CONTINUE);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Nothing);

        // ";"
        assert_eq!(dt.feed(DatumCharClass::LineComment), TRES_CONTINUE);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Nothing);

        // ";\n"
        assert_eq!(dt.feed(DatumCharClass::LineComment), TRES_CONTINUE);
        assert_eq!(dt.feed(DatumCharClass::Newline), TRES_CONTINUE);
        verify_reset(&mut dt);

        // ";a\n"
        assert_eq!(dt.feed(DatumCharClass::LineComment), TRES_CONTINUE);
        let snapshot_lc = dt;
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_CONTINUE);
        // should still be in LC state
        assert_eq!(dt, snapshot_lc);
        assert_eq!(dt.feed(DatumCharClass::Newline), TRES_CONTINUE);
        verify_reset(&mut dt);

        // "\""
        assert_eq!(dt.feed(DatumCharClass::String), TRES_CONTINUE);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Error);

        // "\"a"
        assert_eq!(dt.feed(DatumCharClass::String), TRES_CONTINUE);
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_PUSH);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Error);

        // "\"a\"""
        assert_eq!(dt.feed(DatumCharClass::String), TRES_CONTINUE);
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::String), TRES_STRING);
        verify_reset(&mut dt);

        // "a"
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_PUSH);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Token(DatumTokenType::ID));

        // "aa"
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_PUSH);
        let snapshot_id = dt;
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_PUSH);
        // should still be in ID state
        assert_eq!(dt, snapshot_id);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Token(DatumTokenType::ID));

        // "a "
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::Whitespace), [DatumTokenizerAction::Token(DatumTokenType::ID), DatumTokenizerAction::Break]);
        verify_reset(&mut dt);

        // "-"
        assert_eq!(dt.feed(DatumCharClass::Sign), TRES_PUSH);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Token(DatumTokenType::ID));

        // "--"
        assert_eq!(dt.feed(DatumCharClass::Sign), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::Sign), TRES_PUSH);
        // yes, this is expected by Datum spec - it saves a lot of complicated and hard to get right logic, particularly with infinities and such around
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Token(DatumTokenType::Numeric));

        // "- "
        assert_eq!(dt.feed(DatumCharClass::Sign), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::Whitespace), [DatumTokenizerAction::Token(DatumTokenType::ID), DatumTokenizerAction::Break]);
        verify_reset(&mut dt);

        // "0"
        assert_eq!(dt.feed(DatumCharClass::Digit), TRES_PUSH);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Token(DatumTokenType::Numeric));

        // "10"
        assert_eq!(dt.feed(DatumCharClass::Digit), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::Digit), TRES_PUSH);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Token(DatumTokenType::Numeric));

        // "0 "
        assert_eq!(dt.feed(DatumCharClass::Digit), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::Whitespace), [DatumTokenizerAction::Token(DatumTokenType::Numeric), DatumTokenizerAction::Break]);
        verify_reset(&mut dt);

        // "-0"
        assert_eq!(dt.feed(DatumCharClass::Sign), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::Digit), TRES_PUSH);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Token(DatumTokenType::Numeric));

        // "#"
        assert_eq!(dt.feed(DatumCharClass::SpecialID), TRES_CONTINUE);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Token(DatumTokenType::SpecialID));

        // "##"
        assert_eq!(dt.feed(DatumCharClass::SpecialID), TRES_CONTINUE);
        assert_eq!(dt.feed(DatumCharClass::SpecialID), TRES_PUSH);
        check_eof_case(&mut dt, DatumTokenizerEOFResult::Token(DatumTokenType::SpecialID));

        // "# "
        assert_eq!(dt.feed(DatumCharClass::SpecialID), TRES_CONTINUE);
        assert_eq!(dt.feed(DatumCharClass::Whitespace), [DatumTokenizerAction::Token(DatumTokenType::SpecialID), DatumTokenizerAction::Break]);
        verify_reset(&mut dt);

        // alone
        assert_eq!(dt.feed(DatumCharClass::Quote), [DatumTokenizerAction::Token(DatumTokenType::Quote), DatumTokenizerAction::Break]);
        verify_reset(&mut dt);
        assert_eq!(dt.feed(DatumCharClass::ListStart), [DatumTokenizerAction::Token(DatumTokenType::ListStart), DatumTokenizerAction::Break]);
        verify_reset(&mut dt);
        assert_eq!(dt.feed(DatumCharClass::ListEnd), [DatumTokenizerAction::Token(DatumTokenType::ListEnd), DatumTokenizerAction::Break]);
        verify_reset(&mut dt);

        // alone, interrupting
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::Quote), [DatumTokenizerAction::Token(DatumTokenType::ID), DatumTokenizerAction::Token(DatumTokenType::Quote)]);
        verify_reset(&mut dt);
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::ListStart), [DatumTokenizerAction::Token(DatumTokenType::ID), DatumTokenizerAction::Token(DatumTokenType::ListStart)]);
        verify_reset(&mut dt);
        assert_eq!(dt.feed(DatumCharClass::Content), TRES_PUSH);
        assert_eq!(dt.feed(DatumCharClass::ListEnd), [DatumTokenizerAction::Token(DatumTokenType::ID), DatumTokenizerAction::Token(DatumTokenType::ListEnd)]);
        verify_reset(&mut dt);
    }
}
