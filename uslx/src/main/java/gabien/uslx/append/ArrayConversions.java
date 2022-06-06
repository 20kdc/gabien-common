/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.uslx.append;

/**
 * Created on 6th June 2022 as part of project VE2Bun
 */
public final class ArrayConversions {
    // Upward PCM conversions
    public static void pcmS16ToS32(short[] source, int sourceOfs, int[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            int v = source[sourceOfs++] & 0xFFFF;
            dest[destOfs++] = (v ^ 0x8000) | (v << 16);
        }
    }
    public static void pcmS16ToS32(short[] source, int[] dest) {
        pcmS16ToS32(source, 0, dest, 0, source.length);
    }

    public static void pcmS8ToS32(byte[] source, int sourceOfs, int[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            int v = source[sourceOfs++] & 0xFF;
            int vX = v ^ 0x80;
            dest[destOfs++] = vX | (vX << 8) | (vX << 16) | (v << 24);
        }
    }
    public static void pcmSSToS32(byte[] source, int[] dest) {
        pcmS8ToS32(source, 0, dest, 0, source.length);
    }

    // Downward Integer Casts
    public static void castS32ToS16(int[] source, int sourceOfs, short[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++)
            dest[destOfs++] = (short) (source[sourceOfs++] >> 16);
    }
    public static void castS32ToS16(int[] source, short[] dest) {
        castS32ToS16(source, 0, dest, 0, source.length);
    }

    public static void castS32ToS8(int[] source, int sourceOfs, byte[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++)
            dest[destOfs++] = (byte) (source[sourceOfs++] >> 24);
    }
    public static void castS32ToS8(int[] source, byte[] dest) {
        castS32ToS8(source, 0, dest, 0, source.length);
    }

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

    /**
     * Flips the sign of a byte array. NOT negation, that works differently! May be done in-place (source == dest)
     */
    public static void flipSign(byte[] source, int sourceOfs, byte[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++)
            dest[destOfs++] = (byte) ((source[sourceOfs++]) ^ 0x80);
    }
}
