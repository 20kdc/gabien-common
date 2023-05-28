/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.una;

import java.util.LinkedList;

/**
 * Data about the local system's type system.
 * Created 27th May 2023.
 */
public final class UNASysTypeInfo {
    public final UNAType pointerI, pointerU;

    static UNASysTypeInfo si32 = new UNASysTypeInfo(UNAType.I32, UNAType.U32);
    static UNASysTypeInfo si64 = new UNASysTypeInfo(UNAType.I64, UNAType.U64);

    UNASysTypeInfo(UNAType pI, UNAType pU) {
        pointerI = pI;
        pointerU = pU;
    }

    /**
     * Decodes a textual signature.
     * Textual signatures are of the form "v(II|I)".
     * This indicates void (*)(int, int, ...) called with a third int parameter.
     */
    public UNAProto sig(String sig) {
        int stage = 0;
        UNAType ret = null;
        LinkedList<UNAType> args = new LinkedList<>();
        int vaIdx = -1;
        for (char c : sig.toCharArray()) {
            if (c == '(') {
                if (stage != 1)
                    throw new RuntimeException("Signature \"" + sig + "\": ) can only be used in AREADY stage");
                stage = 2;
            } else if (c == ')') {
                if (stage != 2)
                    throw new RuntimeException("Signature \"" + sig + "\": ) can only be used in ARGS stage");
                stage = 3;
            } else if (c == '|') {
                if (stage != 2)
                    throw new RuntimeException("Signature \"" + sig + "\": | can only be used in ARGS stage");
                if (vaIdx != -1)
                    throw new RuntimeException("Signature \"" + sig + "\": | can only be used once");
                vaIdx = args.size();
            } else if (stage == 0) {
                ret = charType(c);
                stage = 1;
            } else if (stage == 1) {
                throw new RuntimeException("Signature \"" + sig + "\": No args allowed in AREADY stage");
            } else if (stage == 2) {
                args.add(charType(c));
            } else if (stage == 3) {
                throw new RuntimeException("Signature \"" + sig + "\": No args allowed in END stage");
            }
        }
        if (stage != 3)
            throw new RuntimeException("Signature \"" + sig + "\": incomplete, was at stage");
        return new UNAProto(ret, args.toArray(new UNAType[0]), vaIdx);
    }

    /**
     * Decodes a signature character into the corresponding UNAType.
     * Setup must have been called for pointer to be decoded.
     */
    public UNAType charType(char chr) {
        if (chr == 'b')
            return UNAType.I8;
        if (chr == 'B')
            return UNAType.U8;
        if (chr == 's')
            return UNAType.I16;
        if (chr == 'S')
            return UNAType.U16;
        if (chr == 'i')
            return UNAType.I32;
        if (chr == 'I')
            return UNAType.U32;
        if (chr == 'l')
            return UNAType.I64;
        if (chr == 'L')
            return UNAType.U64;
        if (chr == 'f')
            return UNAType.F32;
        if (chr == 'd')
            return UNAType.F64;
        if (chr == 'p' || chr == 'v') {
            return pointerI;
        } else if (chr == 'P' || chr == 'V') {
            return pointerU;
        }
        throw new RuntimeException("Unknown UNAType specifier " + chr);
    }
}
