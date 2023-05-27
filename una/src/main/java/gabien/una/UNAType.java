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
    U8(1, false, false),
    I8(1, false, true),
    U16(2, false, false),
    I16(2, false, true),
    U32(4, false, false),
    I32(4, false, true),
    U64(8, false, false),
    I64(8, false, true),
    F32(4, true, false),
    F64(8, true, false);

    public final int bytes;
    public final boolean isFP;
    public final boolean signed;
    public final long mask, signBit;

    UNAType(int b, boolean isFP, boolean signed) {
        bytes = b;
        mask = ~(-1L << (b * 8));
        signBit = 1 << ((b * 8) - 1);
        this.isFP = isFP;
        this.signed = signed;
    }

    public long signExtend(long v) {
        v &= mask;
        if (signed)
            if ((v & signBit) != 0)
                v |= ~mask;
        return v;
    }
}
