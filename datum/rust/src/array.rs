/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#[cfg(feature = "alloc")]
use alloc::vec::Vec;
#[cfg(feature = "alloc")]
use alloc::string::String;

/// Indicates there was no room in whatever you were trying to push the value into.
#[derive(Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Debug)]
pub struct DatumNoRoomError;

/// Fallible push
pub trait DatumPushable<V>: Extend<V> {
    /// Fallible push.
    fn push(&mut self, entry: V) -> Result<(), DatumNoRoomError>;
}

/// Array-like entity.
/// Implemented for vec and Datum's own fixed-array type.
pub trait DatumArray<V>: DatumPushable<V> + IntoIterator<Item = V> {
    /// Pops the last element.
    fn pop(&mut self) -> Option<V>;
    /// Returns the array length.
    fn len(&self) -> usize;
    /// Returns true if the array is empty.
    fn is_empty(&self) -> bool {
        self.len() == 0
    }
}

#[cfg(feature = "alloc")]
impl DatumPushable<char> for String {
    fn push(&mut self, entry: char) -> Result<(), DatumNoRoomError> {
        String::push(self, entry);
        Ok(())
    }
}

#[cfg(feature = "alloc")]
impl<V> DatumPushable<V> for Vec<V> {
    #[inline]
    fn push(&mut self, entry: V) -> Result<(), DatumNoRoomError> {
        Vec::push(self, entry);
        Ok(())
    }
}

#[cfg(feature = "alloc")]
impl<V> DatumArray<V> for Vec<V> {
    #[inline]
    fn pop(&mut self) -> Option<V> {
        Vec::pop(self)
    }
    #[inline]
    fn len(&self) -> usize {
        Vec::len(self)
    }
}

/// Fixed-array. Requires Default to fill in unused slots safely.
pub struct DatumFixedArray<V: Default, const SIZE: usize>(pub usize, pub [V; SIZE]);

impl<V: Default + Clone, const SIZE: usize> Clone for DatumFixedArray<V, SIZE> {
    #[inline]
    fn clone(&self) -> Self {
        Self(self.0, self.1.clone())
    }
}

impl<V: Default, const SIZE: usize> Default for DatumFixedArray<V, SIZE> {
    #[inline]
    fn default() -> Self {
        Self(0, [(); SIZE].map(|_| V::default()))
    }
}

impl<V: Default, const SIZE: usize> Extend<V> for DatumFixedArray<V, SIZE> {
    #[inline]
    fn extend<T: IntoIterator<Item = V>>(&mut self, iter: T) {
        for v in iter {
            self.1[self.0] = v;
            self.0 += 1;
        }
    }
}

impl<V: Default, const SIZE: usize> DatumPushable<V> for DatumFixedArray<V, SIZE> {
    #[inline]
    fn push(&mut self, entry: V) -> Result<(), DatumNoRoomError> {
        if self.0 >= SIZE {
            Err(DatumNoRoomError)
        } else {
            self.1[self.0] = entry;
            self.0 += 1;
            Ok(())
        }
    }
}

impl<V: Default, const SIZE: usize> DatumArray<V> for DatumFixedArray<V, SIZE> {
    #[inline]
    fn pop(&mut self) -> Option<V> {
        if self.0 == 0 {
            None
        } else {
            self.0 -= 1;
            Some(core::mem::take(&mut self.1[self.0]))
        }
    }
    #[inline]
    fn len(&self) -> usize {
        self.0
    }
}

/// Iterator for DatumFixedArray.
pub struct DatumFixedArrayIter<V: Default, const SIZE: usize>(usize, DatumFixedArray<V, SIZE>);

impl<V: Default, const SIZE: usize> IntoIterator for DatumFixedArray<V, SIZE> {
    type Item = V;
    type IntoIter = DatumFixedArrayIter<V, SIZE>;
    #[inline]
    fn into_iter(self) -> Self::IntoIter {
        DatumFixedArrayIter(0, self)
    }
}

impl<V: Default, const SIZE: usize> Iterator for DatumFixedArrayIter<V, SIZE> {
    type Item = V;
    #[inline]
    fn next(&mut self) -> Option<Self::Item> {
        if self.0 < self.1.0 {
            let res = core::mem::take(&mut self.1.1[self.0]);
            self.0 += 1;
            Some(res)
        } else {
            None
        }
    }
}

#[cfg(feature = "alloc")]
#[cfg(test)]
mod tests {
    use super::*;
    use alloc::vec::Vec;

    fn array_i_test<A: DatumArray<u8> + Clone>(mut array: A) {
        assert_eq!(array.len(), 0);
        assert_eq!(array.push(0), Ok(()));
        assert!(!array.is_empty());
        assert_eq!(array.len(), 1);
        assert_eq!(array.push(1), Ok(()));
        assert_eq!(array.len(), 2);
        assert_eq!(array.clone().pop(), Some(1));
        assert!(!array.is_empty());
        assert_eq!(array.len(), 2);
        assert_eq!(array.pop(), Some(1));
        assert_eq!(array.len(), 1);
        assert_eq!(array.pop(), Some(0));
        assert_eq!(array.len(), 0);
        assert_eq!(array.pop(), None);
        assert!(array.is_empty());
    }

    fn array_2_test<A: DatumArray<u8> + Clone>(mut array: A) {
        assert_eq!(array.len(), 0);
        assert_eq!(array.push(0), Ok(()));
        assert!(!array.is_empty());
        assert_eq!(array.len(), 1);
        assert_eq!(array.push(1), Ok(()));
        assert_eq!(array.len(), 2);
        assert_eq!(array.push(2), Err(DatumNoRoomError));
        assert_eq!(array.clone().pop(), Some(1));
        assert!(!array.is_empty());
        assert_eq!(array.len(), 2);
        assert_eq!(array.pop(), Some(1));
        assert_eq!(array.len(), 1);
        assert_eq!(array.pop(), Some(0));
        assert_eq!(array.len(), 0);
        assert_eq!(array.pop(), None);
        assert!(array.is_empty());
    }

    #[test]
    fn array_tests() {
        let array: DatumFixedArray<u8, 2> = DatumFixedArray::default();
        array_2_test(array);
        array_i_test(Vec::new());
    }
}
