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
 * Encodes from tree to the various visit submethods.
 * Created 16th February 2023.
 */
public abstract class DatumEncodingVisitor extends DatumVisitor {
    public DatumEncodingVisitor() {
        
    }

    @Override
    @SuppressWarnings("unchecked")
    public void visitTree(Object obj) {
        if (obj == null) {
            visitNull();
        } else if (obj instanceof String) {
            visitString((String) obj);
        } else if (obj instanceof DatumSymbol) {
            visitId(((DatumSymbol) obj).id);
        } else if (obj instanceof Number) {
            if (obj instanceof Byte) {
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
            } else {
                throw new RuntimeException("Cannot handle visiting number " + obj);
            }
        } else if (obj instanceof Boolean) {
            visitBoolean((Boolean) obj);
        } else if (obj instanceof List) {
            DatumVisitor sub = visitList();
            for (Object elm : (List<Object>) obj)
                sub.visitTree(elm);
            sub.visitEnd();
        } else if (obj.getClass().isArray()) {
            DatumVisitor sub = visitList();
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++)
                sub.visitTree(Array.get(obj, i));
            sub.visitEnd();
        } else {
            throw new RuntimeException("Cannot handle visiting datum " + obj);
        }
    }
}
