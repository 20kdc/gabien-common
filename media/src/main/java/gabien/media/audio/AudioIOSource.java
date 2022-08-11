/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.media.audio;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Abstract source of Wav audio.
 * May be co-opted for other readers/writers, but is designed around WAV.
 * 
 * Created on 6th June 2022 as part of project VE2Bun
 */
public abstract class AudioIOSource implements Closeable {
    public final @NonNull AudioIOCRSet crSet;
    public final @NonNull AudioIOFormat format;

    public AudioIOSource(@NonNull AudioIOCRSet cr, @NonNull AudioIOFormat fmt) {
        crSet = cr;
        format = fmt;
    }

    /**
     * The amount of frames.
     * Note that this is not necessarily the amount of frames *remaining*.
     */
    public abstract int frameCount();

    /**
     * Retrieves the next frame into the given byte buffer at the given position.
     * Length is channels * format.bytesPerSample bytes.
     * Order is of course little-endian.
     * @param frame Buffer to fill with the next frame of data. Must support array().
     */
    public abstract void nextFrame(@NonNull ByteBuffer frame, int at) throws IOException;

    /**
     * Retrieves all frames as signed 32-bit PCM.
     * @return data
     * @throws IOException
     */
    public int[] readAllAsS32() throws IOException {
        int frames = frameCount();
        int[] res = new int[frames * crSet.channels];
        byte[] frame = new byte[format.bytesPerSample * crSet.channels];
        ByteBuffer frameBuffer = ByteBuffer.wrap(frame);
        frameBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int resPtr = 0;
        for (int i = 0; i < frames; i++) {
            nextFrame(frameBuffer, 0);
            for (int c = 0; c < crSet.channels; c++)
                res[resPtr++] = format.asS32(frameBuffer, c * format.bytesPerSample);
        }
        return res;
    }

    /**
     * Retrieves all frames as doubles.
     * @return data
     * @throws IOException
     */
    public double[] readAllAsF64() throws IOException {
        int frames = frameCount();
        double[] res = new double[frames * crSet.channels];
        byte[] frame = new byte[format.bytesPerSample * crSet.channels];
        ByteBuffer frameBuffer = ByteBuffer.wrap(frame);
        frameBuffer.order(ByteOrder.LITTLE_ENDIAN);
        int resPtr = 0;
        for (int i = 0; i < frames; i++) {
            nextFrame(frameBuffer, 0);
            for (int c = 0; c < crSet.channels; c++)
                res[resPtr++] = format.asF64(frameBuffer, c * format.bytesPerSample);
        }
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
     * Implements conversion for easier writing of signed 16-bit PCM sources.
     * One of the two formats you'd realistically use (if you want more precision, use F32)
     */
    public abstract static class SourceS16 extends AudioIOSource {
        private short[] tmpBuf;

        public SourceS16(@NonNull AudioIOCRSet crSet) {
            super(crSet, AudioIOFormat.F_S16);
            tmpBuf = new short[crSet.channels];
        }

        @Override
        public void nextFrame(@NonNull ByteBuffer frame, int at) {
            nextFrame(tmpBuf);
            for (short s : tmpBuf) {
                frame.putShort(at, s);
                at += 2;
            }
        }

        public abstract void nextFrame(short[] buffer);
    }

    /**
     * Implements conversion for easier writing of floating-point sources.
     * One of the two formats you'd realistically use (if you want more compatibility, use S16)
     */
    public abstract static class SourceF32 extends AudioIOSource {
        private float[] tmpBuf;

        public SourceF32(@NonNull AudioIOCRSet crSet) {
            super(crSet, AudioIOFormat.F_F32);
            tmpBuf = new float[crSet.channels];
        }

        @Override
        public void nextFrame(@NonNull ByteBuffer frame, int at) {
            nextFrame(tmpBuf);
            for (float s : tmpBuf) {
                frame.putFloat(at, s);
                at += 4;
            }
        }

        public abstract void nextFrame(float[] buffer);
    }
}
