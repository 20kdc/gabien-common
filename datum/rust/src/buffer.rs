/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::ops::{Deref, DerefMut};

/// Datum needs a place to store decoded tokens being built.
/// This library is also no-std, so may not have access to Vec.
/// Also, some users may want strict limits on the length of a token/etc.
pub trait DatumBuffer: Deref<Target = [u8]> + Clone {
    /// Truncates the buffer's contents to be at most the given length.
    /// Same semantics as [Vec::truncate].
    /// ```
    /// use datum_rs::{DatumFixedBuffer, DatumBuffer};
    /// let mut example: DatumFixedBuffer<3> = DatumFixedBuffer::default();
    ///
    /// assert!(example.push(1));
    /// assert!(example.push(2));
    /// example.truncate(0);
    /// assert_eq!(example.len(), 0);
    /// example.truncate(2); // does nothing
    /// assert_eq!(example.len(), 0);
    /// ```
    fn truncate(&mut self, len: usize);

    /// Attempts to push a value to the end of this buffer.
    /// The important takeaway here is 'attempts to'.
    /// It is possible to exceed the limits of a [DatumBuffer], as shown here:
    /// ```
    /// use datum_rs::{DatumFixedBuffer, DatumBuffer};
    /// let mut example: DatumFixedBuffer<3> = DatumFixedBuffer::default();
    ///
    /// assert!(example.push(1));
    /// assert!(example.push(2));
    /// assert!(example.push(3));
    ///
    /// assert!(!example.push(4)); // too many!
    ///
    /// assert_eq!(example.len(), 3);
    /// assert_eq!(example[0], 1);
    /// assert_eq!(example[1], 2);
    /// assert_eq!(example[2], 3);
    /// ```
    /// Returns true on success.
    #[must_use]
    fn push(&mut self, byte: u8) -> bool;
}

#[cfg(feature = "alloc")]
use alloc::vec::Vec;

#[cfg(feature = "alloc")]
impl DatumBuffer for Vec<u8> {
    fn truncate(&mut self, len: usize) {
        Vec::truncate(self, len);
    }
    fn push(&mut self, byte: u8) -> bool {
        Vec::push(self, byte);
        true
    }
}

/// Fixed-size DatumBuffer implementation.
/// It should be impossible to make this implementation panic.
/// If it somehow panics anyway, that is a bug.
#[derive(Clone, Copy, Debug)]
pub struct DatumFixedBuffer<const SIZE: usize> {
    len: usize,
    data: [u8;SIZE]
}
impl<const SIZE: usize> DatumBuffer for DatumFixedBuffer<SIZE> {
    #[inline]
    fn truncate(&mut self, len: usize) {
        if len < self.len {
            self.len = len;
        }
    }
    #[inline]
    fn push(&mut self, char: u8) -> bool {
        if self.len == self.data.len() {
            false
        } else {
            self.data[self.len] = char;
            self.len += 1;
            true
        }
    }
}
impl<const SIZE: usize> Default for DatumFixedBuffer<SIZE> {
    #[inline]
    fn default() -> Self {
        Self {
            len: 0,
            data: [0;SIZE]
        }
    }
}
impl<const SIZE: usize> Deref for DatumFixedBuffer<SIZE> {
    type Target = [u8];
    #[inline]
    fn deref(&self) -> &Self::Target {
        &self.data[..self.len]
    }
}
impl<const SIZE: usize> DerefMut for DatumFixedBuffer<SIZE> {
    #[inline]
    fn deref_mut(&mut self) -> &mut Self::Target {
        &mut self.data[..self.len]
    }
}

impl<'buffer, const SIZE: usize> IntoIterator for &'buffer DatumFixedBuffer<SIZE> {
    type Item = &'buffer u8;
    type IntoIter = core::slice::Iter<'buffer, u8>;
    #[inline]
    fn into_iter(self) -> Self::IntoIter {
        self.deref().into_iter()
    }
}

impl<'buffer, const SIZE: usize> IntoIterator for &'buffer mut DatumFixedBuffer<SIZE> {
    type Item = &'buffer mut u8;
    type IntoIter = core::slice::IterMut<'buffer, u8>;
    #[inline]
    fn into_iter(self) -> Self::IntoIter {
        self.deref_mut().into_iter()
    }
}
