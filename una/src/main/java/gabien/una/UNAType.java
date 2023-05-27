/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

/**
 * Type enum that doesn't contain anything particularly opinionated (I hope).
 * Created 27th May, 2023.
 */
public enum UNAType {
    I8('b', 1, false, true),
    U8('B', 1, false, false),
    I16('s', 2, false, true),
    U16('S', 2, false, false),
    I32('i', 4, false, true),
    U32('I', 4, false, false),
    I64('l', 8, false, true),
    U64('L', 8, false, false),
    F32('f', 4, true, false),
    F64('d', 8, true, false);
    // see charType in UNAProto for metatypes

    public final char protoChar;
    public final int bytes;
    public final boolean isFP;
    public final boolean signed, doExtension;
    public final long mask, invMask, signBit;

    UNAType(char c, int b, boolean isFP, boolean signed) {
        protoChar = c;
        bytes = b;
        if (b == 8) {
            mask = -1;
            invMask = 0;
            signBit = 0x8000000000000000L;
            doExtension = false;
        } else {
            invMask = -1L << (b * 8);
            mask = ~invMask;
            signBit = 1 << ((b * 8) - 1);
            doExtension = true;
        }
        this.isFP = isFP;
        this.signed = signed;
    }

    public long signExtend(long v) {
        if (doExtension) {
            v &= mask;
            if (signed)
                if ((v & signBit) != 0)
                    v |= invMask;
        }
        return v;
    }
}
