/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use crate::{DatumParserStack, DatumParserStackEntry, DatumTree};

/// Fixed-array-based parser stack.
pub struct FixedDatumParserStack<M: DatumTree + ?Sized, const SIZE: usize>(pub usize, pub [DatumParserStackEntry<M>; SIZE]);

impl<M: DatumTree<ListRef = E> + ?Sized, const SIZE: usize, E: Clone> Clone for FixedDatumParserStack<M, SIZE> {
    fn clone(&self) -> Self {
        Self(self.0, self.1.clone())
    }
}

impl<M: DatumTree + ?Sized, const SIZE: usize> Default for FixedDatumParserStack<M, SIZE> {
    fn default() -> Self {
        Self(0, [(); SIZE].map(|_| DatumParserStackEntry::default()))
    }
}

impl<M: DatumTree + ?Sized, const SIZE: usize> DatumParserStack<M> for FixedDatumParserStack<M, SIZE> {
    fn push(&mut self, entry: DatumParserStackEntry<M>) -> Result<(), ()> {
        if self.0 >= SIZE {
            Err(())
        } else {
            self.1[self.0] = entry;
            self.0 += 1;
            Ok(())
        }
    }
    fn pop(&mut self) -> Option<DatumParserStackEntry<M>> {
        if self.0 == 0 {
            None
        } else {
            self.0 -= 1;
            Some(core::mem::take(&mut self.1[self.0 - 1]))
        }
    }
    fn is_empty(&self) -> bool {
        self.0 == 0
    }
}
