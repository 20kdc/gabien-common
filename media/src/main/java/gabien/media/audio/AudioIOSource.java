/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.audio;

import java.io.Closeable;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.append.ArrayConversions;

/**
 * Abstract source of Wav audio.
 * May be co-opted for other readers/writers, but is designed around WAV.
 * 
 * Created on 6th June 2022 as part of project VE2Bun
 */
public abstract class AudioIOSource implements Closeable {
    public final @NonNull AudioIOCRSet crSet;
    // Format hint for copying.
    public final @Nullable AudioIOFormat formatHint;

    public AudioIOSource(@NonNull AudioIOCRSet cr, @Nullable AudioIOFormat hint) {
        crSet = cr;
        formatHint = hint;
    }

    /**
     * The amount of frames.
     * Note that this is not necessarily the amount of frames *remaining*.
     */
    public abstract int frameCount();

    /**
     * Retrieves the next frames into the given double array at the given position.
     * Length is channels doubles.
     */
    public abstract void nextFrames(@NonNull double[] frame, int at, int frames) throws IOException;

    /**
     * Retrieves the next frames into the given float array at the given position.
     * Length is channels floats.
     */
    public abstract void nextFrames(@NonNull float[] frame, int at, int frames) throws IOException;

    /**
     * Retrieves the next frames into the given int array at the given position, as signed 32-bit PCM.
     * Length is channels ints.
     */
    public abstract void nextFrames(@NonNull int[] frame, int at, int frames) throws IOException;

    /**
     * Retrieves the next frames into the given byte array in the given format at the given position.
     * Very inefficient (goes through F64 for everything AND usually allocs!).
     */
    public void nextFramesInFormat(@NonNull AudioIOFormat fmt, @NonNull byte[] frame, int at, int frames) throws IOException {
        double[] tmp = new double[frames * crSet.channels];
        nextFrames(tmp, 0, frames);
        for (int i = 0; i < tmp.length; i++) {
            fmt.ofF64(frame, at, tmp[i]);
            at += fmt.bytesPerSample;
        }
    }

    /**
     * Retrieves all frames as doubles.
     */
    public final double[] readAllAsF64() throws IOException {
        int frames = frameCount();
        double[] res = new double[frames * crSet.channels];
        nextFrames(res, 0, frames);
        return res;
    }

    /**
     * Retrieves all frames as floats.
     */
    public final float[] readAllAsF32() throws IOException {
        int frames = frameCount();
        float[] res = new float[frames * crSet.channels];
        nextFrames(res, 0, frames);
        return res;
    }

    /**
     * Retrieves all frames as signed 32-bit PCM.
     */
    public final int[] readAllAsS32() throws IOException {
        int frames = frameCount();
        int[] res = new int[frames * crSet.channels];
        nextFrames(res, 0, frames);
        return res;
    }

    /**
     * Closes the audio source.
     * This is useful for "reader" sources.
     * Automatically called from writeWAV.
     */
    @Override
    public void close() throws IOException {
        // nothing to do here!
    }

    /**
     * Implements conversion from byte-based sources.
     */
    public abstract static class SourceBytes extends AudioIOSource {
        private final byte[] tmpBuf;
        public final AudioIOFormat format;

        public SourceBytes(@NonNull AudioIOCRSet crSet, @NonNull AudioIOFormat format) {
            super(crSet, format);
            tmpBuf = new byte[crSet.channels * format.bytesPerSample];
            this.format = format;
        }

        /**
         * Retrieves the next frame into the given byte buffer at the given position.
         * Length is channels * format.bytesPerSample bytes.
         * Order is of course little-endian.
         */
        public abstract void nextFrames(@NonNull byte[] frame, int at, int frames) throws IOException;

        @Override
        public final void nextFrames(@NonNull double[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                for (int j = 0; j < crSet.channels; j++)
                    frame[at++] = format.asF64(tmpBuf, j * format.bytesPerSample);
            }
        }

        @Override
        public final void nextFrames(@NonNull float[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                for (int j = 0; j < crSet.channels; j++)
                    frame[at++] = (float) format.asF64(tmpBuf, j * format.bytesPerSample);
            }
        }

        @Override
        public final void nextFrames(@NonNull int[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                for (int j = 0; j < crSet.channels; j++)
                    frame[at++] = format.asS32(tmpBuf, j * format.bytesPerSample);
            }
        }

        @Override
        public void nextFramesInFormat(@NonNull AudioIOFormat fmt, @NonNull byte[] frame, int at, int frames) throws IOException {
            if (fmt == format) {
                nextFrames(frame, at, frames);
            } else {
                super.nextFramesInFormat(fmt, frame, at, frames);
            }
        }
    }

    /**
     * Implements conversion for easier writing of signed 16-bit PCM sources.
     * One of the main formats you'd realistically use (if you want more precision, use F32)
     */
    public abstract static class SourceS16 extends AudioIOSource {
        private final short[] tmpBuf;

        public SourceS16(@NonNull AudioIOCRSet crSet) {
            super(crSet, AudioIOFormat.F_S16);
            tmpBuf = new short[crSet.channels];
        }

        public abstract void nextFrames(short[] buffer, int at, int frames);

        @Override
        public void nextFrames(@NonNull double[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                PCMConversions.pcmS16ToF64(tmpBuf, 0, frame, at, crSet.channels);
                at += crSet.channels;
            }
        }

        @Override
        public void nextFrames(@NonNull float[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                PCMConversions.pcmS16ToF32(tmpBuf, 0, frame, at, crSet.channels);
                at += crSet.channels;
            }
        }

        @Override
        public void nextFrames(@NonNull int[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                PCMConversions.pcmS16ToS32(tmpBuf, 0, frame, at, crSet.channels);
                at += crSet.channels;
            }
        }
    }

    /**
     * Implements conversion for easier writing of signed 16-bit PCM sources.
     * One of the main formats you'd realistically use (if you want more precision, use F32)
     */
    public abstract static class SourceS32 extends AudioIOSource {
        private final int[] tmpBuf;

        public SourceS32(@NonNull AudioIOCRSet crSet) {
            super(crSet, AudioIOFormat.F_S32);
            tmpBuf = new int[crSet.channels];
        }

        @Override
        public void nextFrames(@NonNull double[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                PCMConversions.pcmS32ToF64(tmpBuf, 0, frame, at, crSet.channels);
                at += crSet.channels;
            }
        }

        @Override
        public void nextFrames(@NonNull float[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                PCMConversions.pcmS32ToF32(tmpBuf, 0, frame, at, crSet.channels);
                at += crSet.channels;
            }
        }
    }

    /**
     * Implements conversion for easier writing of floating-point sources.
     * One of the two formats you'd realistically use (if you want more compatibility, use S16)
     */
    public abstract static class SourceF32 extends AudioIOSource {
        private final float[] tmpBuf;

        public SourceF32(@NonNull AudioIOCRSet crSet) {
            super(crSet, AudioIOFormat.F_F32);
            tmpBuf = new float[crSet.channels];
        }

        @Override
        public void nextFrames(@NonNull double[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                ArrayConversions.castF32ToF64(tmpBuf, 0, frame, at, crSet.channels);
                at += crSet.channels;
            }
        }

        @Override
        public void nextFrames(@NonNull int[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                PCMConversions.pcmF32ToS32(tmpBuf, 0, frame, at, crSet.channels);
                at += crSet.channels;
            }
        }
    }

    /**
     * Exists mainly to keep the framework orthogonal.
     */
    public abstract static class SourceF64 extends AudioIOSource {
        private final double[] tmpBuf;

        public SourceF64(@NonNull AudioIOCRSet crSet) {
            super(crSet, AudioIOFormat.F_F64);
            tmpBuf = new double[crSet.channels];
        }

        @Override
        public void nextFrames(@NonNull float[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                ArrayConversions.castF64ToF32(tmpBuf, 0, frame, at, crSet.channels);
                at += crSet.channels;
            }
        }

        @Override
        public void nextFrames(@NonNull int[] frame, int at, int frames) throws IOException {
            for (int i = 0; i < frames; i++) {
                nextFrames(tmpBuf, 0, 1);
                PCMConversions.pcmF64ToS32(tmpBuf, 0, frame, at, crSet.channels);
                at += crSet.channels;
            }
        }
    }
}
