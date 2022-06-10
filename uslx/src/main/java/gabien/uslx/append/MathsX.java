/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.uslx.append;

/**
 * Extended maths library
 * Created on 10th June 2022 as part of project WTFr7
 */
public final class MathsX {
    /**
     * Extracts the fractional part of the given value.
     */
    public static float fract(float val) {
        return (float) (val - Math.floor(val));
    }

    /**
     * Clamps a value between two other values.
     */
    public static float clamp(float val, float min, float max) {
        return Math.min(Math.max(val, min), max);
    }

    /**
     * Linearly interpolates between two values.
     * Values outside the range 0-1 continue the 'line'.
     */
    public static float lerpUnclamped(float from, float to, float mix) {
        return from + ((to - from) * mix);
    }
}
