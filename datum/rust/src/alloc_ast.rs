/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::usize;

use crate::{DatumParser, DatumParserStack, DatumParserStackEntry, DatumTree, DatumTreeBuilder, DatumTreeReader, DatumValue};

use alloc::vec::Vec;
use alloc::string::{String, ToString};

/// Vec/String-based DatumTreeManager.
#[derive(Clone, Copy, Debug)]
pub struct AllocDatumTree;

impl Default for AllocDatumTree {
    fn default() -> Self {
        AllocDatumTree
    }
}

impl DatumTree for AllocDatumTree {
    type Buffer = String;
    type ListRef = Vec<DatumValue<Self>>;
}
impl DatumTreeBuilder for AllocDatumTree {
    fn get_quote(&mut self) -> Result<Self::Buffer, ()> {
        Ok("quote".to_string())
    }
    fn new_list(&mut self) -> Result<Self::ListRef, ()> {
        Ok(Self::ListRef::new())
    }
    fn push(&mut self, list: &mut Self::ListRef, v: DatumValue<AllocDatumTree>) -> Result<(), ()> {
        list.push(v);
        Ok(())
    }
}
impl DatumTreeReader for AllocDatumTree {
    fn len(&self, list: &Self::ListRef) -> usize {
        list.len()
    }
    fn get_value<'a: 'b, 'b>(&'b self, list: &'a Self::ListRef, i: usize) -> &'a DatumValue<Self> {
        &list[i]
    }
}

/// Vec-based parser stack.
pub struct VecDatumParserStack<M: DatumTree + ?Sized>(pub Vec<DatumParserStackEntry<M>>);

impl<M: DatumTree<ListRef = E> + ?Sized, E: Clone> Clone for VecDatumParserStack<M> {
    fn clone(&self) -> Self {
        Self(self.0.clone())
    }
}

impl<M: DatumTree + ?Sized> Default for VecDatumParserStack<M> {
    fn default() -> Self {
        Self(Vec::new())
    }
}

impl<M: DatumTree + ?Sized> DatumParserStack<M> for VecDatumParserStack<M> {
    fn push(&mut self, entry: DatumParserStackEntry<M>) -> Result<(), ()> {
        self.0.push(entry);
        Ok(())
    }
    fn pop(&mut self) -> Option<DatumParserStackEntry<M>> {
        self.0.pop()
    }
    fn is_empty(&self) -> bool {
        self.0.is_empty()
    }
}

/// DatumValue based on Vec/String.
pub type AllocDatumValue = DatumValue<AllocDatumTree>;
/// Pairs with AllocDatumValue.
pub type AllocDatumParserStack = VecDatumParserStack<AllocDatumTree>;
/// Finished parser from DatumParserStack.
pub type AllocDatumParser = DatumParser<AllocDatumTree, AllocDatumParserStack>;
