/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.audio;

import gabien.uslx.append.MathsX;
import gabien.uslx.append.TemporaryResultsBuffer;

/**
 * Samples, but discrete!
 * Created on 10th June 2022 as part of project WTFr7
 */
public abstract class DiscreteSample extends AudioIOCRSet {
    private final TemporaryResultsBuffer.F32 tmpFloats;

    /**
     * Length in frames
     */
    public final int length;
    public DiscreteSample(AudioIOCRSet crs, int l) {
        super(crs);
        tmpFloats = new TemporaryResultsBuffer.F32(channels);
        length = l;
    }

    /**
     * Writes the contents of each channel on the given frame into the buffer.
     */
    public abstract void getS32(int frame, int[] buffer);

    /**
     * Writes the contents of each channel on the given frame into the buffer.
     */
    public abstract void getF32(int frame, float[] buffer);

    private final void getExtendedF32(int frame, float[] buffer, boolean loop) {
        if (loop) {
            if (length != 0) {
                frame %= length;
            } else {
                frame = 0;
            }
        } else {
            if ((frame < 0) || (frame >= length)) {
                for (int i = 0; i < channels; i++)
                    buffer[i] = 0;
                return;
            }
        }
        getF32(frame, buffer);
    }

    public final void getInterpolatedF32(double frame, float[] buffer, boolean loop) {
        if (loop) {
            if (length != 0) {
                frame %= length;
            } else {
                frame = 0;
            }
        }
        int frame1 = (int) Math.floor(frame);
        float frac = (float) (frame - frame1);
        int frame2 = frame1 + 1;
        float[] tmpIB = tmpFloats.get();
        getExtendedF32(frame1, buffer, loop);
        getExtendedF32(frame2, tmpIB, loop);
        for (int i = 0; i < channels; i++)
            buffer[i] = MathsX.lerpUnclamped(buffer[i], tmpIB[i], frac);
    }

    /**
     * A (bad, but usable) resampler.
     */
    public final AudioIOSample resample(AudioIOCRSet set, boolean loop) {
        float[] tmp = new float[channels];
        float[] tmp2 = new float[set.channels];
        AudioIOSample res = new AudioIOSample(set, AudioIOFormat.F_F32, (int) ((set.sampleRate * (long) length) / sampleRate));
        double cvt = sampleRate / (double) set.sampleRate;
        if (set.channels != 0) {
            for (int i = 0; i < res.length; i++) {
                getInterpolatedF32(i * cvt, tmp, loop);
                if (channels > 1) {
                    tmp2[0] = tmp[0];
                    if (tmp2.length > 1)
                        tmp2[1] = tmp[1];
                } else if (channels > 0) {
                    tmp2[0] = tmp[0];
                    if (tmp2.length > 1)
                        tmp2[1] = tmp[0];
                }
                res.setF32(i, tmp2);
            }
        }
        return res;
    }
}
