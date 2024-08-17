/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

// A heads up to future people reading this.
// This entire feature was made alloc-only because of the following error:
//
// error[E0275]: overflow evaluating the requirement `Vec<ast::DatumValue<AllocDatumTree>>: Clone`
//    --> src/tests.rs:14:153
//     |
// 14  | ...der::default(), DatumComposePipe(DatumStringTokenizer::default(), AllocDatumParser::default()));
//     |                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//     |
// note: required for `ast::DatumValue<AllocDatumTree>` to implement `Clone`
//    --> src/ast.rs:83:87
//     |
// 83  | impl<M: DatumTree<Buffer = B, ListRef = R>, B: Clone + Deref<Target = str>, R: Clone> Clone for DatumValue<M> {
//     |                                                                                -----  ^^^^^     ^^^^^^^^^^^^^
//     |                                                                                |
//     |                                                                                unsatisfied trait bound introduced here
//     = note: 2 redundant requirements hidden
//     = note: required for `VecDatumParserStack<AllocDatumTree>` to implement `Clone`
//
// In addition to this, the memory structure became kind of a kudzu.
// Basically, the assumption was you'd be using fixed-size buffers for your strings and then have a manager object handling list allocations, but the more I think about it the more wasteful the string handling seems.
// Also, there was a specific array needed to handle the parser's internal stack.
// That needed a layer of indirection to protect parser internal details from callers while still allowing them to supply their own allocators.
// Finally, there were a ton of cases which were like "if this specific internal array runs out of memory, bring the parser to an error state" - repeat for practically every line in the parser...
// Sorry. - 20kdc

use core::{convert::TryFrom, fmt::Write};
use core::fmt::Debug;
use alloc::string::{String, ToString};
use alloc::vec::Vec;
use alloc::vec;

use crate::{DatumAtom, DatumFixedArray, DatumPipe, DatumToken, DatumTokenType, DatumWriter};

/// Datum AST node / value.
#[derive(Clone, PartialEq, PartialOrd, Debug)]
pub enum DatumValue {
    Atom(DatumAtom<String>),
    List(Vec<DatumValue>)
}

impl Default for DatumValue {
    fn default() -> Self {
        Self::Atom(DatumAtom::Nil)
    }
}

impl DatumValue {
    /// Writes a value from AST.
    pub fn write_to(&self, f: &mut dyn Write, writer: &mut DatumWriter) -> core::fmt::Result {
        match self {
            DatumValue::Atom(v) => {
                writer.write_atom(f, v)?;
            },
            DatumValue::List(v) => {
                let ls: DatumToken<&str> = DatumToken::ListStart;
                let le: DatumToken<&str> = DatumToken::ListEnd;
                writer.write_token(f, &ls)?;
                for e in v {
                    e.write_to(f, writer)?;
                }
                writer.write_token(f, &le)?;
            }
        }
        Ok(())
    }
}

/// These states represent ways of handling an emitted value.
#[derive(Clone, Debug)]
enum DatumParserState {
    InList(Vec<DatumValue>),
    InQuote
}

/// Maximum values that can be output from the parser per feed call
pub const DATUM_PARSER_MAX_SIZE: usize = 1;

/// Datum parser (from tokens into values).
#[derive(Clone, Debug, Default)]
pub struct DatumParser {
    stack: Vec<DatumParserState>,
    error: bool
}

impl DatumPipe for DatumParser {
    type Input = DatumToken<String>;
    type Output = DatumValue;
    type Array = DatumFixedArray<Self::Output, DATUM_PARSER_MAX_SIZE>;
    const MAX_SIZE: usize = DATUM_PARSER_MAX_SIZE;

    fn feed(&mut self, token: Self::Input) -> Self::Array {
        match token.token_type() {
            DatumTokenType::Quote => {
                self.stack.push(DatumParserState::InQuote);
                Self::Array::default()
            },
            DatumTokenType::ListStart => {
                let list = Vec::new();
                self.stack.push(DatumParserState::InList(list));
                Self::Array::default()
            },
            DatumTokenType::ListEnd => {
                let res = self.stack.pop();
                if let Some(DatumParserState::InList(v)) = res {
                    self.feed_value(DatumValue::List(v))
                } else {
                    self.error = true;
                    Self::Array::default()
                }
            },
            _ => match DatumAtom::try_from(token) {
                Err(_) => {
                    self.error = true;
                    Self::Array::default()
                },
                Ok(v) => self.feed_value(DatumValue::Atom(v)),
            }
        }
    }

    fn eof(&mut self) -> Self::Array {
        if !self.stack.is_empty() {
            self.error = true;
        }
        Self::Array::default()
    }

    fn has_error(&self) -> bool {
        self.error
    }
}

impl DatumParser {
    fn feed_value(&mut self, mut v: DatumValue) -> DatumFixedArray<DatumValue, DATUM_PARSER_MAX_SIZE> {
        loop {
            match self.stack.pop() {
                None => {
                    let mut res = DatumFixedArray::default();
                    res.extend(Some(v));
                    return res;
                },
                Some(DatumParserState::InList(mut list)) => {
                    list.push(v);
                    self.stack.push(DatumParserState::InList(list));
                    return DatumFixedArray::default();
                },
                Some(DatumParserState::InQuote) => {
                    v = DatumValue::List(vec![
                        DatumValue::Atom(DatumAtom::ID("quote".to_string())),
                        v
                    ]);
                    // and continue
                }
            }
        }
    }
}
