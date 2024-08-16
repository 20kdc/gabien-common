/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::convert::TryFrom;
use core::ops::Deref;
use core::fmt::Debug;

use crate::{DatumAtom, DatumToken, DatumTokenType};

/// Datum AST node common
pub trait DatumTree {
    /// Buffer (string substitute)
    type Buffer: Deref<Target = str>;
    /// List reference.
    /// This may in fact be the actual list itself.
    /// The API is designed to work either way.
    type ListRef;
}

/// Datum AST node builder.
/// This is expected to manage the memory for lists.
/// Something to note is that for memory truly managed by the instance, you 'should' be implementing this on a reference type, i.e. `impl DatumTree for &mut MyType`.
/// Singleton implementations meanwhile should implement it directly and be copy/clone.
pub trait DatumTreeBuilder: DatumTree {
    /// Gets the constant string "quote".
    fn get_quote(&mut self) -> Result<Self::Buffer, ()>;

    /// Creates a new list reference.
    fn new_list(&mut self) -> Result<Self::ListRef, ()>;
    /// Pushes a value to a list.
    /// True on success.
    #[must_use]
    fn push(&mut self, list: &mut Self::ListRef, v: DatumValue<Self>) -> Result<(), ()>;
}

/// Datum AST node accessor (for use by, say, formatting/writing code)
pub trait DatumTreeReader: DatumTree {
    /// Gets a list's length.
    fn len(&self, list: &Self::ListRef) -> usize;
    /// Gets a list value.
    fn get_value<'a: 'b, 'b>(&'b self, list: &'a Self::ListRef, i: usize) -> &'a DatumValue<Self>;
}

/// Entry on Datum parser stack.
#[derive(Debug)]
pub struct DatumParserStackEntry<M: DatumTree + ?Sized>(DatumParserState<M>);

impl<M: DatumTree + ?Sized> Default for DatumParserStackEntry<M> {
    fn default() -> Self {
        // this is not a very good value, but it helps fixed-array impls cope without unsafe
        Self(DatumParserState::InQuote)
    }
}

impl<M: DatumTree<ListRef = L> + ?Sized, L: Clone> Clone for DatumParserStackEntry<M> {
    fn clone(&self) -> Self {
        DatumParserStackEntry(self.0.clone())
    }
}

/// Datum parser stack.
pub trait DatumParserStack<M: DatumTree + ?Sized> {
    /// See Vec::push. However, this can fail (indicating stack overflow).
    #[must_use]
    fn push(&mut self, entry: DatumParserStackEntry<M>) -> Result<(), ()>;
    /// See Vec::pop
    fn pop(&mut self) -> Option<DatumParserStackEntry<M>>;
    /// See Vec::is_empty
    fn is_empty(&self) -> bool;
}

/// Datum AST node / value.
#[derive(Clone, PartialEq, PartialOrd, Debug)]
pub enum DatumValue<M: DatumTree + ?Sized> {
    Atom(DatumAtom<M::Buffer>),
    List(M::ListRef)
}

impl<M: DatumTreeBuilder + ?Sized> Default for DatumValue<M> {
    fn default() -> Self {
        Self::Atom(DatumAtom::Nil)
    }
}

/// Result of the Datum parser.
pub type DatumParserResult<M> = Result<Option<DatumValue<M>>, ()>;

/// These states represent ways of handling an emitted value.
enum DatumParserState<M: DatumTree + ?Sized> {
    InList(M::ListRef),
    InQuote
}

impl<M: DatumTree + ?Sized> Debug for DatumParserState<M> {
    fn fmt(&self, f: &mut core::fmt::Formatter<'_>) -> core::fmt::Result {
        match self {
            Self::InList(_) => f.write_str("InList"),
            Self::InQuote => f.write_str("InQuote")
        }
    }
}

impl<M: DatumTree<ListRef = L> + ?Sized, L: Clone> Clone for DatumParserState<M> {
    fn clone(&self) -> Self {
        match self {
            Self::InList(list) => Self::InList(list.clone()),
            Self::InQuote => Self::InQuote
        }
    }
}


/// Datum parser (from tokens into values).
pub struct DatumParser<M: DatumTreeBuilder, S: DatumParserStack<M>> {
    manager: M,
    stack: S
}

impl<M: DatumTreeBuilder, S: DatumParserStack<M>> DatumParser<M, S> {
    /// Creates a new DatumParser.
    pub fn new(manager: M, stack: S) -> DatumParser<M, S> {
        DatumParser {
            manager,
            stack
        }
    }

    /// Returns the manager.
    /// This can be useful if you've got, say, a fixed-memory-array manager implementation,
    ///  and that implementation is owned by the parser struct.
    pub fn manager(&self) -> &M {
        &self.manager
    }

    /// Returns the manager, mutably.
    pub fn manager_mut(&mut self) -> &mut M {
        &mut self.manager
    }

    /// Given a token, feed it into the parser.
    /// If the token completes a value, that value is returned.
    pub fn feed(&mut self, token: DatumToken<M::Buffer>) -> DatumParserResult<M> {
        match token.token_type() {
            DatumTokenType::Quote => {
                self.stack.push(DatumParserStackEntry(DatumParserState::InQuote))?;
                Ok(None)
            },
            DatumTokenType::ListStart => {
                self.stack.push(DatumParserStackEntry(DatumParserState::InList(self.manager.new_list()?)))?;
                Ok(None)
            },
            DatumTokenType::ListEnd => {
                let res = self.stack.pop();
                if let Some(DatumParserStackEntry(DatumParserState::InList(v))) = res {
                    self.feed_value(DatumValue::List(v))
                } else {
                    Err(())
                }
            },
            _ => match DatumAtom::try_from(token) {
                Err(_) => Err(()),
                Ok(v) => self.feed_value(DatumValue::Atom(v)),
            }
        }
    }

    /// If EOF is allowed without causing an error.
    pub fn is_eof_allowed(&self) -> bool {
        self.stack.is_empty()
    }

    fn feed_value(&mut self, mut v: DatumValue<M>) -> DatumParserResult<M> {
        loop {
            match self.stack.pop() {
                None => {
                    return Ok(Some(v));
                },
                Some(DatumParserStackEntry(DatumParserState::InList(mut list))) => {
                    self.manager.push(&mut list, v)?;
                    self.stack.push(DatumParserStackEntry(DatumParserState::InList(list)))?;
                    return Ok(None);
                },
                Some(DatumParserStackEntry(DatumParserState::InQuote)) => {
                    let mut list = self.manager.new_list()?;
                    let quote = DatumValue::Atom(DatumAtom::ID(self.manager.get_quote()?));
                    self.manager.push(&mut list, quote)?;
                    self.manager.push(&mut list, v)?;
                    v = DatumValue::List(list);
                    // and continue
                }
            }
        }
    }
}

// for any "self-managing" pair. implement Default
impl<M: Default + DatumTreeBuilder, S: Default + DatumParserStack<M>> Default for DatumParser<M, S> {
    fn default() -> Self {
        Self {
            manager: M::default(),
            stack: S::default()
        }
    }
}

// under similar conditions, implement Clone
impl<M: Clone + DatumTreeBuilder, S: Clone + DatumParserStack<M>> Clone for DatumParser<M, S> {
    fn clone(&self) -> Self {
        Self {
            manager: self.manager.clone(),
            stack: self.stack.clone()
        }
    }
}
