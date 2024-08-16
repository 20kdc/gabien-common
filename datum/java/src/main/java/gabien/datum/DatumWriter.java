/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Writes out a Datum (or a stream of them) to a Writer.
 * Includes utilities for pretty-printed writing.
 * But due to the formatting-varying nature of Datum, will not pretty-print totally automatically.
 * Created 15th February 2023.
 */
public class DatumWriter extends DatumEncodingVisitor {
    protected final Appendable base;
    protected SpacingState queued = SpacingState.None;

    /**
     * Current indentation level. Turns into tabs/etc.
     */
    public int indent = 0;

    public DatumWriter(Appendable base) {
        this.base = base;
    }

    /**
     * Converts an object to string with the minimum required spacing.
     */
    public static String objectToString(Object obj) {
        StringWriter sw = new StringWriter();
        DatumWriter dw = new DatumWriter(sw);
        dw.visitTree(obj, DatumSrcLoc.NONE);
        return sw.toString();
    }

    protected void putChar(char c) {
        try {
            base.append(c);
        } catch (IOException e) {
            throw new DatumRuntimeIOException(e);
        }
    }

    protected void emitQueued(boolean listEnd) {
        if (queued == SpacingState.QueuedIndent) {
            for (int i = 0; i < indent; i++)
                putChar('\t');
        } else if (queued == SpacingState.AfterToken && !listEnd) {
            putChar(' ');
        }
        queued = SpacingState.None;
    }

    private void putStringContent(String content, char delimiter) {
        for (char c : content.toCharArray()) {
            if (c == delimiter) {
                putChar('\\');
                putChar(c);
            } else if (c == '\r') {
                putChar('\\');
                putChar('r');
            } else if (c == '\n') {
                putChar('\\');
                putChar('n');
            } else if (c == '\t') {
                putChar('\\');
                putChar('t');
            } else {
                putChar(c);
            }
        }
    }

    /**
     * Writes a line comment (can contain newlines, these will be handled), followed by newline.
     */
    public void visitComment(String comment) {
        emitQueued(false);
        putChar(';');
        putChar(' ');
        for (char c : comment.toCharArray()) {
            if (c == '\n') {
                visitNewline();
                emitQueued(false);
                putChar(';');
                putChar(' ');
            } else {
                putChar(c);
            }
        }
        visitNewline();
    }

    /**
     * Writes a newline.
     */
    public void visitNewline() {
        putChar('\n');
        queued = SpacingState.QueuedIndent;
    }

    @Override
    public void visitString(String s, DatumSrcLoc srcLoc) {
        emitQueued(false);
        putChar('"');
        putStringContent(s, '"');
        putChar('"');
        queued = SpacingState.AfterToken;
    }

    @Override
    public void visitId(String s, DatumSrcLoc srcLoc) {
        emitQueued(false);
        if (s.length() == 0) {
            // Emit #{}# to work around this
            putChar('#');
            putChar('{');
            putChar('}');
            putChar('#');
            queued = SpacingState.AfterToken;
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
        queued = SpacingState.AfterToken;
    }

    @Override
    public void visitNumericUnknown(String s, DatumSrcLoc srcLoc) {
        emitQueued(false);
        if (s.length() == 0 || s.equals("-") || (DatumCharClass.identify(s.charAt(0)) != DatumCharClass.NumericStart)) {
            putChar('#');
            putChar('i');
            for (char c : s.toCharArray()) {
                DatumCharClass cc = DatumCharClass.identify(c);
                if (!cc.isValidPID)
                    putChar('\\');
                putChar(c);
            }
        } else {
            for (char c : s.toCharArray()) {
                DatumCharClass cc = DatumCharClass.identify(c);
                if (!cc.isValidPID)
                    putChar('\\');
                putChar(c);
            }
        }
        queued = SpacingState.AfterToken;
    }

    @Override
    public void visitSpecialUnknown(String s, DatumSrcLoc srcLoc) {
        emitQueued(false);
        putChar('#');
        for (char c : s.toCharArray()) {
            DatumCharClass cc = DatumCharClass.identify(c);
            if (!cc.isValidPID)
                putChar('\\');
            putChar(c);
        }
        queued = SpacingState.AfterToken;
    }

    @Override
    public void visitBoolean(boolean value, DatumSrcLoc srcLoc) {
        visitSpecialUnknown(value ? "t" : "f", srcLoc);
    }

    @Override
    public void visitNull(DatumSrcLoc srcLoc) {
        visitSpecialUnknown("nil", srcLoc);
    }

    @Override
    public void visitInt(long value, String raw, DatumSrcLoc srcLoc) {
        visitNumericUnknown(raw, srcLoc);
    }

    @Override
    public void visitFloat(double value, String raw, DatumSrcLoc srcLoc) {
        visitNumericUnknown(raw, srcLoc);
    }

    /**
     * Visits a list.
     * For DatumWriter there is an API guarantee that the returned writer will always be the callee.
     */
    @Override
    public DatumWriter visitList(DatumSrcLoc srcLoc) {
        emitQueued(false);
        putChar('(');
        queued = SpacingState.None;
        return this;
    }

    /**
     * Ends a list.
     */
    @Override
    public void visitEnd(DatumSrcLoc srcLoc) {
        emitQueued(true);
        putChar(')');
        queued = SpacingState.AfterToken;
    }

    protected enum SpacingState {
        None,
        QueuedIndent,
        // After a token (that isn't a list start, for "(example)" kinda thing)
        AfterToken
    }
}
