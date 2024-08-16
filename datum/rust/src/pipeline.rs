/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use crate::{DatumPushable, DatumArray, DatumFixedArray};

/// Generic "input X, get Y" function
pub trait DatumPipe {
    type Input;
    type Output: Default;
    type Array: DatumArray<Self::Output>;
    const MAX_SIZE: usize;

    /// Feeds in I, and you may get up to the given amount of O.
    fn feed(&mut self, i: Self::Input) -> Self::Array;

    /// EOF. May trigger errors.
    fn eof(&mut self) -> Self::Array;

    /// Returns true if an error has occurred.
    fn has_error(&self) -> bool;

    /// Feeds into a vec or similar from a slice.
    /// Can also automatically trigger EOF.
    /// Remember to check for [DatumPipe::has_error].
    /// ```
    /// use datum_rs::{DatumDecoder, DatumPipe};
    /// let mut decoder = DatumDecoder::default();
    /// let mut results = vec![];
    /// decoder.feed_iter_to_vec(&mut results, "example text".chars(), true);
    /// assert_eq!(results.len(), 12);
    /// ```
    fn feed_iter_to_vec<S: IntoIterator<Item = Self::Input>, V: DatumArray<Self::Output>>(&mut self, target: &mut V, source: S, eof: bool) {
        for v in source {
            for o in self.feed(v) {
                target.push_unwrap(o);
            }
        }
        if eof {
            self.eof_to_vec(target);
        }
    }

    /// Returns EOF results into a vec or similar.
    fn eof_to_vec<V: DatumArray<Self::Output>>(&mut self, target: &mut V) {
        for o in self.eof() {
            target.push_unwrap(o);
        }
    }

    /// Composes with another pipeline.
    /// Look for DatumStringTokenizer for an example.
    fn compose<const MS: usize, P: DatumPipe<Input = Self::Output>>(self, other: P) -> impl DatumPipe<Input = Self::Input, Output = P::Output> where Self: Sized {
        let res: DatumComposePipe<_, _, MS> = DatumComposePipe(self, other);
        res
    }
}

/// Composed pipe.
/// Due to Rust limitations, you need to set MAX_SIZE manually to the multiplied max sizes of the input pipes, multiplied by 2.
/// For this reason, you should always be careful to provide and use non-generic max size constants.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct DatumComposePipe<A: DatumPipe, B: DatumPipe<Input = A::Output>, const MS: usize>(pub A, pub B);

impl<A: DatumPipe, B: DatumPipe<Input = A::Output>, const MS: usize> DatumPipe for DatumComposePipe<A, B, MS> {
    type Input = A::Input;
    type Output = B::Output;
    type Array = DatumFixedArray<Self::Output, MS>;
    // can be up to 2x because of EOF behaviour
    const MAX_SIZE: usize = A::MAX_SIZE * B::MAX_SIZE * 2;

    fn feed(&mut self, i: Self::Input) -> Self::Array {
        if Self::MAX_SIZE != MS {
            panic!("Self::MAX_SIZE is not the product of 2 * input max * output max. This would be a compile error if I could make it one, but I can't so it isn't.");
        }
        let mut res: DatumFixedArray<Self::Output, MS> = DatumFixedArray::default();
        for v in self.0.feed(i) {
            for v2 in self.1.feed(v) {
                res.push_unwrap(v2);
            }
        }
        res
    }

    fn eof(&mut self) -> Self::Array {
        if Self::MAX_SIZE != MS {
            panic!("Self::MAX_SIZE is not the product of 2 * input max * output max. This would be a compile error if I could make it one, but I can't so it isn't.");
        }
        let mut res: DatumFixedArray<Self::Output, MS> = DatumFixedArray::default();
        for v in self.0.eof() {
            for v2 in self.1.feed(v) {
                res.push_unwrap(v2);
            }
        }
        for v in self.1.eof() {
            res.push_unwrap(v);
        }
        res
    }

    #[inline]
    fn has_error(&self) -> bool {
        self.0.has_error() || self.1.has_error()
    }
}
