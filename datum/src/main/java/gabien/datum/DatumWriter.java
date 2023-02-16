/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.io.IOException;
import java.io.Writer;

/**
 * Writes out a Datum (or a stream of them) to a Writer.
 * Created 15th February 2023.
 */
public class DatumWriter extends DatumEncodingVisitor {
    protected final Writer base;
    protected boolean needSpacing = false;

    public DatumWriter(Writer base) {
        this.base = base;
    }

    protected void putChar(char c) {
        try {
            base.append(c);
        } catch (IOException e) {
            throw new DatumRuntimeIOException(e);
        }
    }

    protected void emitSpacingIfNeeded() {
        if (needSpacing) {
            putChar(' ');
            needSpacing = false;
        }
    }

    /**
     * It's not really sensible to try to write a general "write a character direct or indirect" function.
     * (Mainly because writing "n" indirectly is painful.)
     * Luckily, it's also never necessary.
     * However, when a character must be of the content class is a case that often comes up, particularly re: identifiers...
     */
    protected void putCharContent(char c) {
        if (!DatumCharacters.isContent(c))
            putChar('\\');
        putChar(c);
    }

    private void putStringContent(String content, char delimiter) {
        for (char c : content.toCharArray()) {
            if (c == delimiter) {
                putChar('\\');
                putChar(c);
            } else {
                putChar(c);
            }
        }
    }

    public void visitComment(String comment) {
        emitSpacingIfNeeded();
        putChar(';');
        putStringContent(comment, '\n');
        putChar('\n');
        needSpacing = false;
    }

    @Override
    public void visitString(String s) {
        putChar('"');
        putStringContent(s, '"');
        putChar('"');
        needSpacing = false;
    }

    @Override
    public void visitId(String s) {
        emitSpacingIfNeeded();
        if (s.length() == 0) {
            // Emit #{}# to work around this
            putChar('#');
            putChar('{');
            putChar('}');
            putChar('#');
            needSpacing = true;
            return;
        }
        boolean isFirst = true;
        for (char c : s.toCharArray()) {
            // stop numeric or special start from being first!
            if (isFirst && (DatumCharacters.isNumericStart(c) || c == '#')) {
                putChar('\\');
                putChar(c);
            } else {
                putCharContent(c);
            }
            isFirst = false;
        }
        needSpacing = true;
    }

    @Override
    public void visitNumericUnknown(String s) {
        if (s.length() == 0)
            throw new RuntimeException("Cannot write an empty numeric.");
        if (!DatumCharacters.isNumericStart(s.charAt(0)))
            throw new RuntimeException("Cannot write a numeric with '" + s.charAt(0) + "' at the start.");
        emitSpacingIfNeeded();
        // Write directly as content
        for (char c : s.toCharArray())
            putCharContent(c);
        needSpacing = true;
    }

    @Override
    public void visitSpecialUnknown(String s) {
        if (s.length() == 0)
            throw new RuntimeException("Cannot write an empty special identifier.");
        if (s.charAt(0) != '#')
            throw new RuntimeException("Cannot write a special identifier with '" + s.charAt(0) + "' at the start.");
        emitSpacingIfNeeded();
        // Write directly as content
        for (char c : s.toCharArray())
            putCharContent(c);
        needSpacing = true;
    }

    @Override
    public void visitBoolean(boolean value) {
        visitSpecialUnknown(value ? "#t" : "#f");
    }

    @Override
    public void visitInt(long value, String raw) {
        visitNumericUnknown(raw);
    }

    @Override
    public void visitFloat(double value, String raw) {
        visitNumericUnknown(raw);
    }

    @Override
    public DatumVisitor visitList() {
        putChar('(');
        needSpacing = false;
        return this;
    }

    @Override
    public void visitEnd() {
        putChar(')');
        needSpacing = false;
    }
}
