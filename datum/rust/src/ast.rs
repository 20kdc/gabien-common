/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::{cell::Cell, convert::TryFrom};

use alloc::boxed::Box;
use alloc::vec::Vec;
use alloc::vec;
use alloc::string::{String, ToString};

use crate::{DatumAtom, DatumToken, DatumTokenType};

/// Datum AST node / value.
#[derive(Clone, PartialEq, PartialOrd, Debug)]
pub enum DatumValue {
    Atom(DatumAtom<String>),
    List(Vec<DatumValue>)
}

impl DatumValue {
    /// Quotes this value.
    pub fn quote(self) -> DatumValue {
        DatumValue::List(vec![DatumValue::Atom(DatumAtom::ID("quote".to_string())), self])
    }
}

/// Result of the Datum parser.
pub enum DatumParserResult {
    Continue,
    Value(DatumValue),
    Error
}

enum DatumParserState {
    None,
    InList(Vec<DatumValue>, Box<Cell<DatumParserState>>),
    InQuote(Box<Cell<DatumParserState>>)
}
impl Default for DatumParserState {
    fn default() -> Self {
        Self::None
    }
}

impl DatumParserState {
    fn feed(self, token: DatumToken<String>) -> (Self, DatumParserResult) {
        match self {
            Self::None => {
                match token.token_type() {
                    DatumTokenType::Quote => {
                        (Self::InQuote(Box::new(Cell::new(Self::None))), DatumParserResult::Continue)
                    },
                    DatumTokenType::ListStart => {
                        (Self::InList(Vec::new(), Box::new(Cell::new(Self::None))), DatumParserResult::Continue)
                    },
                    _ => match DatumAtom::try_from(token) {
                        Err(_) => (Self::None, DatumParserResult::Error),
                        Ok(v) => (Self::None, DatumParserResult::Value(DatumValue::Atom(v))),
                    }
                }
            },
            Self::InList(mut list, substate) => {
                let token_type = token.token_type();
                let substate_original = substate.replace(Self::None);
                let substate_original_idle = if let DatumParserState::None = &substate_original { true } else { false };
                let res = substate_original.feed(token);
                match res.1 {
                    DatumParserResult::Continue => {
                        // this cycle of taking/replacing the substate keeps the Box intact
                        substate.replace(res.0);
                        (Self::InList(list, substate), DatumParserResult::Continue)
                    }
                    DatumParserResult::Value(v) => {
                        list.push(v);
                        substate.replace(res.0);
                        (Self::InList(list, substate), DatumParserResult::Continue)
                    },
                    DatumParserResult::Error => {
                        // if the error is explicitly caused by a list end token,
                        // AND the substate is idle
                        // then this is the list stack underflow error
                        if token_type == DatumTokenType::ListEnd && substate_original_idle {
                            // which means, return the list
                            (Self::None, DatumParserResult::Value(DatumValue::List(list)))
                        } else {
                            // otherwise it's a regular error
                            (Self::None, DatumParserResult::Error)
                        }
                    }
                }
            },
            Self::InQuote(substate) => {
                let res = substate.replace(Self::None).feed(token);
                match res.1 {
                    DatumParserResult::Continue => {
                        // this cycle of taking/replacing the substate keeps the Box intact
                        substate.replace(res.0);
                        (Self::InQuote(substate), DatumParserResult::Continue)
                    }
                    DatumParserResult::Value(v) => (Self::None, DatumParserResult::Value(v.quote())),
                    DatumParserResult::Error => (Self::None, DatumParserResult::Error)
                }
            }
        }
    }
}

/// Datum parser (from tokens into values).
#[derive(Default)]
pub struct DatumParser(DatumParserState);

impl DatumParser {
    /// Given a token, feed it into the parser.
    /// If the token completes a value, that value is returned.
    pub fn feed(&mut self, token: DatumToken<String>) -> DatumParserResult {
        let state = core::mem::take(&mut self.0);
        let res = state.feed(token);
        self.0 = res.0;
        res.1
    }
}
