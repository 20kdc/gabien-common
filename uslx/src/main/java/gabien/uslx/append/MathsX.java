/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

/**
 * Extended maths library
 * Created on 10th June 2022 as part of project WTFr7
 */
public final class MathsX {
    /**
     * PI2 is useful for conversions that treat 0-1 as a cycle.
     */
    public static final double PI2 = Math.PI * 2;

    /**
     * Extracts the fractional part of the given value.
     */
    public static float fract(float val) {
        return (float) (val - Math.floor(val));
    }

    /**
     * Linearly interpolates between two values.
     * Values outside the range 0-1 continue the 'line'.
     */
    public static float lerpUnclamped(float from, float to, float mix) {
        return from + ((to - from) * mix);
    }

    /**
     * Samples a linear float array.
     * Note that coordinate normalization is not included.
    */
    public static float linearSample1d(float frame, float[] buffer, boolean loop) {
        int frame1 = (int) Math.floor(frame);
        float frac = (float) (frame - frame1);
        int frame2 = frame1 + 1;
        if (loop) {
            frame1 = seqModulo(frame1, buffer.length);
            frame2 = seqModulo(frame2, buffer.length);
        } else {
            frame1 = clamp(frame1, 0, buffer.length - 1);
            frame2 = clamp(frame2, 0, buffer.length - 1);
        }
        return lerpUnclamped(buffer[frame1], buffer[frame2], frac);
    }

    // Clamps

    /**
     * Clamps a value between two other values.
     */
    public static int clamp(int val, int min, int max) {
        return Math.min(Math.max(val, min), max);
    }

    /**
     * Clamps a value between two other values.
     */
    public static long clamp(long val, long min, long max) {
        return Math.min(Math.max(val, min), max);
    }

    /**
     * Clamps a value between two other values.
     */
    public static float clamp(float val, float min, float max) {
        return Math.min(Math.max(val, min), max);
    }

    /**
     * Clamps a value between two other values.
     */
    public static double clamp(double val, double min, double max) {
        return Math.min(Math.max(val, min), max);
    }

    /**
     * Modulo, but handling negative values in the first parameter in a way that makes sense for positions in looped sequences.
     * For example, seqModulo(-1, 4) is 3.
     */
    public static int seqModulo(int x, int length) {
        int base = x % length;
        if (x < 0) {
            base += length;
            if (base >= length)
                base -= length;
        }
        return base;
    }

    /**
     * Modulo, but handling negative values in the first parameter in a way that makes sense for positions in looped sequences.
     * For example, saneModulo(-1, 4) is 3.
     */
    public static long seqModulo(long x, long length) {
        long base = x % length;
        if (x < 0) {
            base += length;
            if (base >= length)
                base -= length;
        }
        return base;
    }

    /**
     * Modulo, but handling negative values in the first parameter in a way that makes sense for positions in looped sequences.
     * For example, saneModulo(-1, 4) is 3.
     */
    public static float seqModulo(float x, float length) {
        float base = x % length;
        if (x < 0) {
            base += length;
            if (base >= length)
                base -= length;
        }
        return base;
    }

    /**
     * Modulo, but handling negative values in the first parameter in a way that makes sense for positions in looped sequences.
     * For example, saneModulo(-1, 4) is 3.
     */
    public static double seqModulo(double x, double length) {
        double base = x % length;
        if (x < 0) {
            base += length;
            if (base >= length)
                base -= length;
        }
        return base;
    }

}
