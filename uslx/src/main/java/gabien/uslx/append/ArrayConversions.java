/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

/**
 * Created on 6th June 2022 as part of project VE2Bun
 */
public final class ArrayConversions {
    // Float Casts
    public static void castF64ToF32(double[] source, int sourceOfs, float[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++)
            dest[destOfs++] = (float) source[sourceOfs++];
    }
    public static void castF64ToF32(double[] source, float[] dest) {
        castF64ToF32(source, 0, dest, 0, source.length);
    }

    public static void castF32ToF64(float[] source, int sourceOfs, double[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++)
            dest[destOfs++] = source[sourceOfs++];
    }
    public static void castF32ToF64(float[] source, double[] dest) {
        castF32ToF64(source, 0, dest, 0, source.length);
    }
}
