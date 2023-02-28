/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.io.IOException;
import java.io.StringWriter;
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

    public static String objectToString(Object obj) {
        StringWriter sw = new StringWriter();
        DatumWriter dw = new DatumWriter(sw);
        dw.visitTree(obj);
        return sw.toString();
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
        putChar(' ');
        for (char c : comment.toCharArray()) {
            putChar(c);
            if (c == '\n') {
                putChar(';');
                putChar(' ');
            }
        }
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
            // only content-class can be in first character of a regular ID
            DatumCharClass cc = DatumCharClass.identify(c);
            boolean escape = false;
            if (isFirst) {
                escape = cc != DatumCharClass.Content;
            } else {
                escape = !cc.isValidPID;
            } 
            if (escape)
                putChar('\\');
            putChar(c);
            isFirst = false;
        }
        needSpacing = true;
    }

    private void visitUnknownPID(DatumCharClass dcc, String s) {
        if (s.length() == 0)
            throw new RuntimeException("Cannot write an empty " + dcc + ".");
        if (DatumCharClass.identify(s.charAt(0)) != dcc)
            throw new RuntimeException("Cannot write a " + dcc + " with '" + s.charAt(0) + "' at the start.");
        emitSpacingIfNeeded();
        // Write directly
        for (char c : s.toCharArray()) {
            DatumCharClass cc = DatumCharClass.identify(c);
            if (!cc.isValidPID)
                putChar('\\');
            putChar(c);
        }
        needSpacing = true;
    }

    @Override
    public void visitNumericUnknown(String s) {
        visitUnknownPID(DatumCharClass.NumericStart, s);
    }

    @Override
    public void visitSpecialUnknown(String s) {
        visitUnknownPID(DatumCharClass.SpecialID, s);
    }

    @Override
    public void visitBoolean(boolean value) {
        visitSpecialUnknown(value ? "#t" : "#f");
    }

    @Override
    public void visitNull() {
        visitSpecialUnknown("#nil");
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
