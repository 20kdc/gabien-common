/*
 * gabien-datum-rs - Quick to implement S-expression format
 * Written starting in 2024 by contributors (see CREDITS.txt at repository's root)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

use core::{fmt::Write, ops::Deref};

use crate::{DatumAtom, DatumToken, DatumTokenType, DatumTreeReader, DatumValue, DatumTree};

#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum DatumWriterState {
    /// No indentation.
    None,
    /// Queued indentation.
    QueuedIndent,
    /// After a token. Will emit a space unless the token is a list end.
    AfterToken
}

impl Default for DatumWriterState {
    fn default() -> Self {
        Self::None
    }
}

/// General interface for formatting/printing Datum content.
#[derive(Clone, Copy, PartialEq, Eq, Debug, Default)]
pub struct DatumWriter {
    /// Indentation level (in tabs).
    pub indent: usize,
    /// Writer's state. Beware: Editing this improperly can create unreadable output.
    pub state: DatumWriterState
}

impl DatumWriter {
    /// Emits queued whitespace.
    /// If list_end is set, won't emit a single space.
    /// Will still emit indentation.
    pub fn emit_whitespace(&mut self, f: &mut dyn Write, list_end: bool) -> core::fmt::Result {
        match self.state {
            DatumWriterState::None => {},
            DatumWriterState::QueuedIndent => {
                let mut counter = self.indent;
                while counter > 0 {
                    f.write_char('\t')?;
                    counter -= 1;
                }
            },
            DatumWriterState::AfterToken => {
                if !list_end {
                    f.write_char(' ')?;
                }
            }
        }
        self.state = DatumWriterState::None;
        Ok(())
    }

    /// Writes a newline and prepares for it.
    pub fn write_newline(&mut self, f: &mut dyn Write) -> core::fmt::Result {
        f.write_char('\n')?;
        self.state = DatumWriterState::QueuedIndent;
        Ok(())
    }

    /// Writes a line comment. Newlines are converted into more line comments.
    pub fn write_comment(&mut self, f: &mut dyn Write, text: &str) -> core::fmt::Result {
        self.emit_whitespace(f, false)?;
        f.write_char(';')?;
        f.write_char(' ')?;
        for v in text.chars() {
            if v == '\n' {
                self.write_newline(f)?;
                self.emit_whitespace(f, false)?;
                f.write_char(';')?;
                f.write_char(' ')?;
            } else {
                f.write_char(v)?;
            }
        }
        self.write_newline(f)?;
        Ok(())
    }

    /// Writes a token.
    pub fn write_token<B: Deref<Target = str>>(&mut self, f: &mut dyn Write, token: &DatumToken<B>) -> core::fmt::Result {
        let token_type = token.token_type();
        self.emit_whitespace(f, token_type == DatumTokenType::ListEnd)?;
        token.write(f)?;
        if token_type != DatumTokenType::ListStart && token_type != DatumTokenType::Quote {
            self.state = DatumWriterState::AfterToken;
        } else {
            self.state = DatumWriterState::None;
        }
        Ok(())
    }

    /// Writes a value from AST atom.
    pub fn write_atom<B: Deref<Target = str>>(&mut self, f: &mut dyn Write, value: &DatumAtom<B>) -> core::fmt::Result {
        self.emit_whitespace(f, false)?;
        value.write(f)?;
        self.state = DatumWriterState::AfterToken;
        Ok(())
    }

    /// Writes a value from AST.
    pub fn write_tree_value<M: DatumTreeReader>(&mut self, f: &mut dyn Write, tree: &M, value: &DatumValue<M>) -> core::fmt::Result {

        match value {
            DatumValue::Atom(v) => {
                self.write_atom(f, v)?;
            },
            DatumValue::List(v) => {
                self.emit_whitespace(f, false)?;
                f.write_char('(')?;
                self.state = DatumWriterState::None;
                for i in 0..tree.len(v) {
                    let element = tree.get_value(v, i);
                    self.write_tree_value(f, tree, element)?;
                }
                self.emit_whitespace(f, true)?;
                f.write_char(')')?;
                self.state = DatumWriterState::AfterToken;
            }
        }
        Ok(())
    }

    /// Writes a value from AST where the AST uses dereferencable slices.
    pub fn write_value_deref_slices<M: DatumTree<ListRef = V>, V: Deref<Target = [DatumValue<M>]>>(&mut self, f: &mut dyn Write, value: &DatumValue<M>) -> core::fmt::Result {
        match value {
            DatumValue::Atom(v) => {
                self.write_atom(f, v)?;
            },
            DatumValue::List(v) => {
                self.emit_whitespace(f, false)?;
                f.write_char('(')?;
                self.state = DatumWriterState::None;
                for element in v.deref() {
                    self.write_value_deref_slices(f, element)?;
                }
                self.emit_whitespace(f, true)?;
                f.write_char(')')?;
                self.state = DatumWriterState::AfterToken;
            }
        }
        Ok(())
    }
}
