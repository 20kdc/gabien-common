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

/// Provides a target that data can be pushed to.
/// This is similar to [Extend], and that trait is required (as an unwrapped version).
/// Unlike [Extend], however, pushing to [DatumPushable] is single-item-at-a-time, and can fail.
pub trait DatumPushable<V>: Extend<V> {
    /// Fallible push.
    fn push(&mut self, entry: V) -> Result<(), DatumNoRoomError>;
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
    // trivial
    #[cfg(not(tarpaulin_include))]
    #[inline]
    fn push(&mut self, entry: V) -> Result<(), DatumNoRoomError> {
        Vec::push(self, entry);
        Ok(())
    }
}
