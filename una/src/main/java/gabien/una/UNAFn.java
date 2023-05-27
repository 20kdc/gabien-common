/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.una;

/**
 * Bound function.
 * Mainly good for convenience, because creating these is higher overhead.
 * Created 27th May, 2023
 */
public final class UNAFn {
    public final long code;
    public final IUNAFnType type;
    public UNAFn(long code, IUNAFnType type) {
        this.code = code;
        this.type = type;
    }

    @Override
    public String toString() {
        return code + ":" + type;
    }

    public long call(
    ) {
        return type.call(code, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0
    ) {
        return type.call(code, i0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1
    ) {
        return type.call(code, i0, i1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2
    ) {
        return type.call(code, i0, i1, i2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3
    ) {
        return type.call(code, i0, i1, i2, i3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4
    ) {
        return type.call(code, i0, i1, i2, i3, i4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, i7, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, i7, i8, 0, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8, long i9
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, i7, i8, i9, 0, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8, long i9, long iA
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, i7, i8, i9, iA, 0, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8, long i9, long iA, long iB
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, i7, i8, i9, iA, iB, 0, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8, long i9, long iA, long iB, long iC
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, i7, i8, i9, iA, iB, iC, 0, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8, long i9, long iA, long iB, long iC, long iD
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, i7, i8, i9, iA, iB, iC, iD, 0, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8, long i9, long iA, long iB, long iC, long iD, long iE
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, i7, i8, i9, iA, iB, iC, iD, iE, 0);
    }
    public long call(
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8, long i9, long iA, long iB, long iC, long iD, long iE, long iF
    ) {
        return type.call(code, i0, i1, i2, i3, i4, i5, i6, i7, i8, i9, iA, iB, iC, iD, iE, iF);
    }
}
