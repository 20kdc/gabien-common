/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::{convert::TryFrom, fmt::Write, ops::Deref, str::FromStr};

use crate::DatumToken;

/// Atomic Datum AST value.
/// This enum also contains the functions that convert between tokens and atoms.
/// You can think of it as the bridge between Datum's tokenization model and value model.
#[derive(Clone, Copy, PartialEq, PartialOrd, Debug)]
pub enum DatumAtom<B: Deref<Target = str>> {
    String(B),
    ID(B),
    Integer(i64),
    Float(f64),
    Boolean(bool),
    Nil
}

impl<B: Deref<Target = str>> Default for DatumAtom<B> {
    fn default() -> Self {
        Self::Nil
    }
}

impl<B: Deref<Target = str>> TryFrom<DatumToken<B>> for DatumAtom<B> {
    type Error = ();

    /// Tries to convert from a DatumToken.
    /// Due to the strings involved, this has to be done via ownership transfer.
    fn try_from(token: DatumToken<B>) -> Result<DatumAtom<B>, ()> {
        match token {
            DatumToken::String(b) => Ok(DatumAtom::String(b)),
            DatumToken::ID(b) => Ok(DatumAtom::ID(b)),
            DatumToken::SpecialID(b) => {
                if b.eq_ignore_ascii_case("t") {
                    Ok(DatumAtom::Boolean(true))
                } else if b.eq_ignore_ascii_case("f") {
                    Ok(DatumAtom::Boolean(false))
                } else if b.eq_ignore_ascii_case("nil") {
                    Ok(DatumAtom::Nil)
                } else {
                    Err(())
                }
            },
            DatumToken::Numeric(b) => {
                if b.eq_ignore_ascii_case("+nan.0") {
                    Ok(DatumAtom::Float(f64::NAN))
                } else if b.eq_ignore_ascii_case("+inf.0") {
                    Ok(DatumAtom::Float(f64::INFINITY))
                } else if b.eq_ignore_ascii_case("-inf.0") {
                    Ok(DatumAtom::Float(f64::NEG_INFINITY))
                } else if let Ok(v) = i64::from_str_radix(&b, 10) {
                    Ok(DatumAtom::Integer(v))
                } else if let Ok(v) = f64::from_str(&b) {
                    Ok(DatumAtom::Float(v))
                } else {
                    Err(())
                }
            },
            _ => Err(())
        }
    }
}

impl<B: Deref<Target = str>> DatumAtom<B> {
    /// Writes a value from the atom.
    pub fn write(&self, f: &mut dyn Write) -> core::fmt::Result {
        match &self {
            DatumAtom::String(v) => {
                DatumToken::String(v.deref()).write(f)?;
            },
            DatumAtom::ID(v) => {
                DatumToken::ID(v.deref()).write(f)?;
            },
            DatumAtom::Integer(v) => {
                f.write_fmt(format_args!("{}", v))?;
            },
            DatumAtom::Float(v) => {
                if v.is_nan() {
                    f.write_str("#+nan.0")?;
                } else if v.is_infinite() {
                    if v.is_sign_positive() {
                        f.write_str("#+inf.0")?;
                    } else {
                        f.write_str("#-inf.0")?;
                    }
                } else {
                    f.write_fmt(format_args!("{}", v))?;
                }
            },
            DatumAtom::Boolean(v) => {
                if *v {
                    f.write_str("#t")?;
                } else {
                    f.write_str("#f")?;
                }
            },
            DatumAtom::Nil => {
                f.write_str("#nil")?;
            }
        }
        Ok(())
    }
}
