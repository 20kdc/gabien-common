/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/// Generic "input X, get Y" function
pub trait DatumPipe {
    type Input;
    type Output;

    /// Feeds in I, and you may get up to the given amount of O.
    fn feed<F: FnMut(Self::Output)>(&mut self, i: Self::Input, f: &mut F);

    /// EOF. May trigger errors.
    fn eof<F: FnMut(Self::Output)>(&mut self, f: &mut F);

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
    fn feed_iter_to_vec<S: IntoIterator<Item = Self::Input>, V: Extend<Self::Output>>(&mut self, target: &mut V, source: S, eof: bool) {
        for v in source {
            self.feed(v, &mut |o| target.extend(Some(o)));
        }
        if eof {
            self.eof_to_vec(target);
        }
    }

    /// Returns EOF results into a vec or similar.
    fn eof_to_vec<V: Extend<Self::Output>>(&mut self, target: &mut V) {
        self.eof(&mut |o| target.extend(Some(o)));
    }

    /// Composes with another pipeline.
    fn compose<P: DatumPipe<Input = Self::Output>>(self, other: P) -> impl DatumPipe<Input = Self::Input, Output = P::Output> where Self: Sized {
        DatumComposePipe(self, other)
    }
}

/// Composed pipe.
/// Due to Rust limitations, you need to set MAX_SIZE manually to the multiplied max sizes of the input pipes, multiplied by 2.
/// For this reason, you should always be careful to provide and use non-generic max size constants.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub struct DatumComposePipe<A: DatumPipe, B: DatumPipe<Input = A::Output>>(pub A, pub B);

impl<A: DatumPipe, B: DatumPipe<Input = A::Output>> DatumPipe for DatumComposePipe<A, B> {
    type Input = A::Input;
    type Output = B::Output;

    fn feed<F: FnMut(Self::Output)>(&mut self, i: Self::Input, f: &mut F) {
        let m0 = &mut self.0;
        let m1 = &mut self.1;
        m0.feed(i, &mut |v| {
            m1.feed(v, f);
        });
    }

    fn eof<F: FnMut(Self::Output)>(&mut self, f: &mut F) {
        let m0 = &mut self.0;
        let m1 = &mut self.1;
        m0.eof(&mut |v| {
            m1.feed(v, f);
        });
        m1.eof(f);
    }

    #[inline]
    fn has_error(&self) -> bool {
        self.0.has_error() || self.1.has_error()
    }
}
