/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.uslx.audio;

import gabien.uslx.append.*;

/**
 * Represents the abstract concept of a sample.
 * Note that length is not directly specified - this sample may be generative and/or infinite.
 * Note that this is fully floatified.
 */
public interface ISample {
    float get(double at);

    /**
     * Array sample. Note: No interpolation!
     */
    public static class ArrayF32 implements ISample {
        public float[] base;

        public ArrayF32(float[] data) {
            base = data;
        }

        @Override
        public float get(double at) {
            int index = (int) at;
            if (index < 0)
                return 0;
            if (index >= base.length)
                return 0;
            return base[index];
        }
    }

    /**
     * Looping sample. Note that interpolation should go outside a loop.
     */
    public static class Looped implements ISample {
        public ISample base;
        public double loopStart;
        public double loopLength;

        public Looped(ISample b, double loopS, double loopL) {
            base = b;
            loopStart = loopS;
            loopLength = loopL;
        }

        @Override
        public float get(double at) {
            at -= loopStart;
            at -= Math.floor(at / loopLength) * loopLength;
            at += loopStart;
            return base.get(at);
        }
    }

    /**
     * Linear interpolation wrapper.
     */
    public static class Lerped implements ISample {
        public ISample base;

        public Lerped(ISample b) {
            base = b;
        }

        @Override
        public float get(double at) {
            double af = Math.floor(at); 
            double ofs = at - af;
            float a = base.get(af);
            float b = base.get(af + 1);
            float diff = b - a;
            diff *= ofs;
            return a + diff;
        }
    }
}
