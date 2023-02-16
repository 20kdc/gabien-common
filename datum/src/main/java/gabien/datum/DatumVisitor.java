/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Mechanism for receiving Datums.
 * Created 15th February 2022.
 */
public abstract class DatumVisitor {
    // Atoms

    /**
     * Called to visit a string.
     */
    public abstract void visitString(String s);

    /**
     * Called to visit an identifier.
     */
    public abstract void visitId(String s);

    /**
     * Called to visit an undecodable numeric value.
     */
    public abstract void visitNumericUnknown(String s);

    /**
     * Called to visit an integer.
     */
    public abstract void visitInt(long value, String raw);

    /**
     * Called to visit an integer.
     */
    public final void visitInt(long value) {
        visitInt(value, Long.toString(value));
    }

    /**
     * Called to visit a float.
     */
    public abstract void visitFloat(double value, String raw);

    /**
     * Called to visit a float.
     */
    public final void visitFloat(double value) {
        visitFloat(value, Double.toString(value));
    }

    // List start/end

    /**
     * Called when entering a list.
     * The DatumVisitor returned will visit the entire list, then visitEnd will be called on it.
     */
    public abstract DatumVisitor visitList();

    /**
     * Called on the list DatumVisitor when leaving a list.
     * (Notably, this won't get called at root level.)
     */
    public abstract void visitEnd();

    // Utilties

    /**
     * This is distinct from visitTree for testing reasons.
     * Besides, it's unlikely a meaningful benefit could be gotten in real code from reusing the Object values.
     * (That would require code passing Datums to other code via DatumVisitor, without going via a write/read cycle. Only testing does this.)
     */
    @SuppressWarnings("unchecked")
    public final void visitTreeManually(Object obj) {
        if (obj instanceof String) {
            visitString((String) obj);
        } else if (obj instanceof DatumSymbol) {
            visitId(((DatumSymbol) obj).id);
        } else if (obj instanceof Byte) {
            visitInt((byte) (Byte) obj, obj.toString());
        } else if (obj instanceof Short) {
            visitInt((short) (Short) obj, obj.toString());
        } else if (obj instanceof Integer) {
            visitInt((int) (Integer) obj, obj.toString());
        } else if (obj instanceof Long) {
            visitInt((long) (Long) obj, obj.toString());
        } else if (obj instanceof Double) {
            visitFloat((double) (Double) obj, obj.toString());
        } else if (obj instanceof Float) {
            visitFloat((float) (Float) obj, obj.toString());
        } else if (obj instanceof List) {
            DatumVisitor sub = visitList();
            for (Object elm : (List<Object>) obj)
                sub.visitTreeManually(elm);
            sub.visitEnd();
        } else if (obj.getClass().isArray()) {
            DatumVisitor sub = visitList();
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++)
                sub.visitTreeManually(Array.get(obj, i));
            sub.visitEnd();
        } else {
            throw new RuntimeException("Cannot handle visiting datum " + obj);
        }
    }
}
