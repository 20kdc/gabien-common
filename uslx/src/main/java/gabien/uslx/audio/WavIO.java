/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.uslx.audio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import gabien.uslx.append.LEDataOutputStream;
import gabien.uslx.append.RIFFOutputStream;

/**
 * Contains a streaming WAV writer.
 * For now, that's all.
 * Created on 23rd May, 2022 (LTCH:PPJ6)
 */
public class WavIO {
    public static final int FORMAT_PCM = 1;
    public static final int FORMAT_FLOAT = 3;

    public static void writeWAV(OutputStream fos, ISource dataSource) throws IOException {
        // Details of the format.
        int sampleRate = dataSource.sampleRate();
        int sampleBytes = dataSource.sampleBytes();
        int frameCount = dataSource.frameCount();
        int sampleBits = sampleBytes * 8;
        int channels = dataSource.channels();
        int frameBytes = channels * sampleBytes;
        int totalSampleBytes = frameCount * frameBytes;
        // Now we know exactly what we're going to generate, let's build a header!
        int interiorContent = 4;
        interiorContent += RIFFOutputStream.getInteriorChunkSize(0x10);
        interiorContent += RIFFOutputStream.getInteriorChunkSize(totalSampleBytes);
        RIFFOutputStream riffChunk = new RIFFOutputStream(fos, "RIFF", interiorContent);
        // Filetype
        riffChunk.writeBytes("WAVE");
        // fmt {
        RIFFOutputStream fmtChunk = new RIFFOutputStream(riffChunk, "fmt ", 0x10);
        fmtChunk.writeShort(dataSource.formatTag());
        fmtChunk.writeShort(channels);
        fmtChunk.writeInt(sampleRate);
        fmtChunk.writeInt(sampleRate * sampleBytes * channels);
        fmtChunk.writeShort(sampleBytes * channels);
        fmtChunk.writeShort(sampleBits);
        fmtChunk.close();
        // }
        // data {
        RIFFOutputStream dataChunk = new RIFFOutputStream(riffChunk, "data", totalSampleBytes);
        // And now for the sample data!
        ByteBuffer bb = ByteBuffer.allocate(frameBytes);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frameCount; i++) {
            // Stereo data.
            bb.position(0);
            dataSource.nextFrame(bb);
            dataChunk.write(bb.array(), 0, frameBytes);
        }
        dataChunk.close();
        // }
        // Close off.
        riffChunk.close();
    }

    public interface ISource {
        /**
         * The amount of channels (referred to as C later).
         */
        int channels();

        /**
         * Sample rate.
         */
        int sampleRate();

        /**
         * Format tag for this sample format (see FORMAT_ constants)
         */
        int formatTag();

        /**
         * Bytes per sample (referred to as B).
         */
        int sampleBytes();

        /**
         * The amount of frames (referred to as F).
         */
        int frameCount();

        /**
         * Retrieves the next frame.
         * @param frame Buffer to fill with the next frame of data. Length is C * B bytes. Order is of course little-endian.
         */
        void nextFrame(ByteBuffer frame);
    }

    /**
     * Implements conversion for easier writing of signed 16-bit PCM sources.
     * One of the two formats you'd realistically use (if you want more precision, use F32)
     */
    public abstract static class SourceS16 implements ISource {
        private short[] tmpBuf;

        @Override
        public int formatTag() {
            return FORMAT_PCM;
        }

        @Override
        public int sampleBytes() {
            return 2;
        }

        @Override
        public void nextFrame(ByteBuffer frame) {
            if (tmpBuf == null)
                tmpBuf = new short[channels()];
            nextFrame(tmpBuf);
            for (short s : tmpBuf)
                frame.putShort(s);
        }

        public abstract void nextFrame(short[] buffer);
    }

    /**
     * Implements conversion for easier writing of floating-point sources.
     * One of the two formats you'd realistically use (if you want more compatibility, use S16)
     */
    public abstract static class SourceF32 implements ISource {
        private float[] tmpBuf;

        @Override
        public int formatTag() {
            return FORMAT_FLOAT;
        }

        @Override
        public int sampleBytes() {
            return 4;
        }

        @Override
        public void nextFrame(ByteBuffer frame) {
            if (tmpBuf == null)
                tmpBuf = new float[channels()];
            nextFrame(tmpBuf);
            for (float s : tmpBuf)
                frame.putFloat(s);
        }

        public abstract void nextFrame(float[] buffer);
    }
}
