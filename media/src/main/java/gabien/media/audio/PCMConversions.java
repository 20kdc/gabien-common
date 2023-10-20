/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.audio;

import gabien.uslx.append.MathsX;

/**
 * Created 20th October, 2023.
 */
public final class PCMConversions {
    private PCMConversions() {
    }

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
    public static void pcmS8ToS32(byte[] source, int[] dest) {
        pcmS8ToS32(source, 0, dest, 0, source.length);
    }

    public static void pcmS8ToS16(byte[] source, int sourceOfs, short[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            int v = source[sourceOfs++] & 0xFF;
            int vX = v ^ 0x80;
            dest[destOfs++] = (short) (vX | (vX << 8));
        }
    }
    public static void pcmS8ToS16(byte[] source, short[] dest) {
        pcmS8ToS16(source, 0, dest, 0, source.length);
    }

    // Downward PCM conversions
    public static void pcmS32ToS16(int[] source, int sourceOfs, short[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++)
            dest[destOfs++] = (short) (source[sourceOfs++] >> 16);
    }
    public static void pcmS32ToS16(int[] source, short[] dest) {
        pcmS32ToS16(source, 0, dest, 0, source.length);
    }

    public static void pcmS32ToS8(int[] source, int sourceOfs, byte[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++)
            dest[destOfs++] = (byte) (source[sourceOfs++] >> 24);
    }
    public static void pcmS32ToS8(int[] source, byte[] dest) {
        pcmS32ToS8(source, 0, dest, 0, source.length);
    }

    public static void pcmS16ToS8(short[] source, int sourceOfs, byte[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++)
            dest[destOfs++] = (byte) (source[sourceOfs++] >> 8);
    }
    public static void pcmS16ToS8(short[] source, byte[] dest) {
        pcmS16ToS8(source, 0, dest, 0, source.length);
    }

    // PCM x Float casts, F64/S16
    public static void pcmF64ToS16(double[] source, int sourceOfs, short[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            double flt = source[sourceOfs++];
            flt *= flt < 0 ? 32768d : 32767d;
            flt = MathsX.clamp(flt, -32768, 32767);
            dest[destOfs++] = (short) flt;
        }
    }
    public static void pcmF64ToS16(double[] source, short[] dest) {
        pcmF64ToS16(source, 0, dest, 0, source.length);
    }
    public static void pcmS16ToF64(short[] source, int sourceOfs, double[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            double flt = source[sourceOfs++];
            flt /= flt < 0 ? 32768d : 32767d;
            dest[destOfs++] = flt;
        }
    }
    public static void pcmS16ToF64(short[] source, double[] dest) {
        pcmS16ToF64(source, 0, dest, 0, source.length);
    }

    // PCM x Float casts, F64/S32
    public static void pcmF64ToS32(double[] source, int sourceOfs, int[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            double flt = source[sourceOfs++];
            flt *= flt < 0 ? 2147483648d : 2147483647d;
            flt = MathsX.clamp(flt, -2147483648, 2147483647);
            dest[destOfs++] = (int) flt;
        }
    }
    public static void pcmF64ToS32(double[] source, int[] dest) {
        pcmF64ToS32(source, 0, dest, 0, source.length);
    }
    public static void pcmS32ToF64(int[] source, int sourceOfs, double[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            double flt = source[sourceOfs++];
            flt /= flt < 0 ? 2147483648d : 2147483647d;
            dest[destOfs++] = (int) flt;
        }
    }
    public static void pcmS32ToF64(int[] source, double[] dest) {
        pcmS32ToF64(source, 0, dest, 0, source.length);
    }

    // PCM x Float casts, F32/S16
    public static void pcmF32ToS16(float[] source, int sourceOfs, short[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            float flt = source[sourceOfs++];
            flt *= flt < 0 ? 32768f : 32767f;
            flt = MathsX.clamp(flt, -32768, 32767);
            dest[destOfs++] = (short) flt;
        }
    }
    public static void pcmF32ToS16(float[] source, short[] dest) {
        pcmF32ToS16(source, 0, dest, 0, source.length);
    }
    public static void pcmS16ToF32(short[] source, int sourceOfs, float[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            float flt = source[sourceOfs++];
            flt /= flt < 0 ? 32768f : 32767f;
            dest[destOfs++] = flt;
        }
    }
    public static void pcmS16ToF32(short[] source, float[] dest) {
        pcmS16ToF32(source, 0, dest, 0, source.length);
    }

    // PCM x Float casts, F32/S32
    public static void pcmF32ToS32(float[] source, int sourceOfs, int[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            float flt = source[sourceOfs++];
            flt *= flt < 0 ? 2147483648d : 2147483647d;
            flt = MathsX.clamp(flt, -2147483648, 2147483647);
            dest[destOfs++] = (short) flt;
        }
    }
    public static void pcmF32ToS32(float[] source, int[] dest) {
        pcmF32ToS32(source, 0, dest, 0, source.length);
    }
    public static void pcmS32ToF32(int[] source, int sourceOfs, float[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++) {
            float flt = source[sourceOfs++];
            flt /= flt < 0 ? 2147483648d : 2147483647d;
            dest[destOfs++] = flt;
        }
    }
    public static void pcmS32ToF32(int[] source, float[] dest) {
        pcmS32ToF32(source, 0, dest, 0, source.length);
    }

    /**
     * Flips the sign of a byte array. NOT negation, that works differently! May be done in-place (source == dest)
     */
    public static void flipSign(byte[] source, int sourceOfs, byte[] dest, int destOfs, int sourceLen) {
        for (int i = 0; i < sourceLen; i++)
            dest[destOfs++] = (byte) ((source[sourceOfs++]) ^ 0x80);
    }
}
