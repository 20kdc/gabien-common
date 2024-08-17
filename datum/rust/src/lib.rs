/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

//! Datum is an S-expression format meant for quick implementation in various languages.
//! It's intended to be reasonably readable by R6RS readers, but not a strict subset (ultimately, this reduces complexity).
//! The specification is available at <https://github.com/20kdc/gabien-common/blob/master/datum/specification.md>.

#![forbid(unsafe_code)]

#![cfg_attr(not(feature = "std"), no_std)]

// so this extern crate business is being weird, let's deal with it

#[cfg(feature = "alloc")]
extern crate alloc;

// Meta

mod array;
pub use array::*;

mod pipeline;
pub use pipeline::*;

// Encoding

mod char_classes;
pub use char_classes::*;

mod decoder;
pub use decoder::*;

mod byte_decoder;
pub use byte_decoder::*;

// Tokenization

mod token_core;
pub use token_core::*;

mod token;
pub use token::*;

// Values

mod atom;
pub use atom::*;

// Writing

mod writer;
pub use writer::*;

// AST (alloc-only)

#[cfg(feature = "alloc")]
mod ast;
#[cfg(feature = "alloc")]
pub use ast::*;

#[cfg(feature = "alloc")]
mod pipelines;
#[cfg(feature = "alloc")]
pub use pipelines::*;

// Big test battery

#[cfg(feature = "alloc")]
#[cfg(test)]
mod tests;
