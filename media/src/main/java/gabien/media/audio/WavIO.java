/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.media.audio;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.media.append.*;
import gabien.uslx.append.XEDataInputStream;

/**
 * Contains a streaming WAV reader and writer.
 * For now, that's all.
 * Created on 23rd May, 2022 (LTCH:PPJ6)
 * Reading support added 6th June, 2022
 */
public class WavIO {
    public static AudioIOSource readWAV(@NonNull final InputStream fis, final boolean close) throws IOException {
        RIFFInputStream ris = new RIFFInputStream(fis);
        ris.readListOrRiffTypeAndVerify("WAVE", "wave");
        return readWAVInterior(ris, close ? fis : null);
    }
    public static AudioIOSource readWAVInterior(@NonNull final RIFFInputStream ris, @Nullable final Closeable closeMe) throws IOException {
        AudioIOCRFmt fmt = null;
        while (ris.available() > 0) {
            RIFFInputStream chk = new RIFFInputStream(ris);
            if (chk.chunkId.equals("fmt ")) {
                fmt = readFMT(chk);
            } else if (chk.chunkId.equals("data")) {
                if (fmt == null)
                    throw new IOException("Got 'data' chunk before 'fmt ' chunk");
                return readDATA(chk, fmt, closeMe);
            }
            chk.close();
        }
        throw new IOException("Never found 'data' chunk");
    }
    public static AudioIOCRFmt readFMT(@NonNull XEDataInputStream fmtChk) throws IOException {
        int fmtTag = fmtChk.readUnsignedShort();
        int channels = fmtChk.readUnsignedShort();
        int channelMask = 0;
        int sampleRate = fmtChk.readInt();
        fmtChk.readInt(); // bytes per second
        fmtChk.readUnsignedShort();
        int sampleBits = fmtChk.readUnsignedShort(); // sample bits
        if (fmtTag == 0xFFFE) {
            if (fmtChk.readUnsignedShort() < 0x16)
                throw new IOException("Extensible WAVE format, but not enough header for it");
            fmtChk.readShort();
            channelMask = fmtChk.readInt();
            fmtTag = fmtChk.readUnsignedShort();
        }
        return new AudioIOCRFmt(AudioIOFormat.detect(fmtTag, sampleBits), channels, channelMask, sampleRate);
    }
    public static AudioIOSource readDATA(@NonNull final RIFFInputStream data, @NonNull AudioIOCRFmt fmt, @Nullable final Closeable closeMe) throws IOException {
        final int frameBytes = fmt.format.bytesPerSample * fmt.channels;
        final int frameCount = data.chunkLen / frameBytes;
        return new AudioIOSource(fmt, fmt.format) {
            @Override
            public int frameCount() {
                return frameCount;
            }

            @Override
            public void nextFrame(@NonNull ByteBuffer frame, int at) throws IOException {
                data.readFully(frame.array(), frame.arrayOffset() + at, frameBytes);
            }

            @Override
            public void close() throws IOException {
                if (closeMe != null)
                    closeMe.close();
            }
        };
    }

    public static void writeWAV(@NonNull OutputStream fos, @NonNull AudioIOSource dataSource) throws IOException {
        int interiorContent = 4 + sizeWAVInterior(dataSource);
        RIFFOutputStream riffChunk = new RIFFOutputStream(fos, "RIFF", interiorContent);
        // Filetype
        riffChunk.writeBytes("WAVE");
        writeWAVInterior(riffChunk, dataSource);
        // Close off.
        riffChunk.close();
        dataSource.close();
    }

    public static void writeWAVInterior(@NonNull OutputStream fos, @NonNull AudioIOSource dataSource) throws IOException {
        // Details of the format.
        int requirements = inferFullRequirements(dataSource);
        int fmtSize = fmtSizeFromRequirements(requirements);
        writeWAVInteriorFMT(fos, dataSource, fmtSize);
        if ((requirements & AudioIOFormat.REQ_FACT) != 0)
            writeWAVInteriorFACT(fos, dataSource);
        writeWAVInteriorDATA(fos, dataSource);
    }

    public static int sizeWAVInterior(@NonNull AudioIOSource dataSource) throws IOException {
        // Details of the format.
        AudioIOCRSet cr = dataSource.crSet;
        AudioIOFormat fmt = dataSource.format;
        int frameCount = dataSource.frameCount();
        int frameBytes = cr.channels * fmt.bytesPerSample;
        int totalSampleBytes = frameCount * frameBytes;
        int requirements = inferFullRequirements(dataSource);
        int fmtSize = fmtSizeFromRequirements(requirements);
        // Now we know exactly what we're going to generate, let's build a header!
        int interiorContent = 0;
        interiorContent += RIFFOutputStream.getInteriorChunkSize(fmtSize);
        if ((requirements & AudioIOFormat.REQ_FACT) != 0)
            interiorContent += RIFFOutputStream.getInteriorChunkSize(4);
        interiorContent += RIFFOutputStream.getInteriorChunkSize(totalSampleBytes);
        return interiorContent;
    }

    public static void writeWAVInteriorFMT(@NonNull OutputStream fos, @NonNull AudioIOSource dataSource, int fmtSize) throws IOException {
        // Details of the format.
        AudioIOCRSet cr = dataSource.crSet;
        AudioIOFormat fmt = dataSource.format;
        // fmt {
        RIFFOutputStream fmtChunk = new RIFFOutputStream(fos, "fmt ", fmtSize);
        fmtChunk.writeShort(fmtSize == 0x28 ? 0xFFFE : fmt.formatCode);
        fmtChunk.writeShort(cr.channels);
        fmtChunk.writeInt(cr.sampleRate);
        fmtChunk.writeInt(cr.sampleRate * fmt.bytesPerSample * cr.channels);
        fmtChunk.writeShort(fmt.bytesPerSample * cr.channels);
        fmtChunk.writeShort(fmt.bitsPerSample);
        if (fmtSize == 0x10) {
            // nothing to do here!
        } else if (fmtSize == 0x12) {
            fmtChunk.writeShort(0);
        } else if (fmtSize == 0x28) {
            fmtChunk.writeShort(0x16);
            fmtChunk.writeShort(fmt.bitsPerSample);
            fmtChunk.writeInt(cr.channelMask);
            // apparently a GUID
            fmtChunk.writeInt(fmt.formatCode);
            fmtChunk.writeInt(0);
            fmtChunk.writeLong(0);
        } else {
            throw new UnsupportedOperationException("How'd you do this then, hmm?");
        }
        fmtChunk.close();
        // }
    }

    public static void writeWAVInteriorFACT(@NonNull OutputStream fos, @NonNull AudioIOSource dataSource) throws IOException {
        int frameCount = dataSource.frameCount();
        // fact {
        RIFFOutputStream factChunk = new RIFFOutputStream(fos, "fact", 4);
        factChunk.writeInt(frameCount);
        factChunk.close();
        // }
    }

    public static void writeWAVInteriorDATA(@NonNull OutputStream fos, @NonNull AudioIOSource dataSource) throws IOException {
        // Details of the format.
        AudioIOCRSet cr = dataSource.crSet;
        AudioIOFormat fmt = dataSource.format;
        int frameCount = dataSource.frameCount();
        int frameBytes = cr.channels * fmt.bytesPerSample;
        int totalSampleBytes = frameCount * frameBytes;
        // data {
        RIFFOutputStream dataChunk = new RIFFOutputStream(fos, "data", totalSampleBytes);
        // And now for the sample data!
        byte[] data = new byte[frameBytes];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frameCount; i++) {
            // Stereo data.
            dataSource.nextFrame(bb, 0);
            dataChunk.write(data);
        }
        dataChunk.close();
        // }
    }

    private static int inferFullRequirements(@NonNull AudioIOSource dataSource) throws IOException {
        // Expand requirements.
        int requirements = dataSource.format.requirements;
        // channel mask implies EXT_MODE
        if (dataSource.crSet.channelMask != 0)
            requirements |= AudioIOFormat.REQ_EXT_MODE;
        // EXT_MODE implies EXT_SIZE and FACT
        if ((requirements & AudioIOFormat.REQ_EXT_MODE) != 0) {
            requirements |= AudioIOFormat.REQ_EXT_SIZE;
            requirements |= AudioIOFormat.REQ_FACT;
        }
        return requirements;
    }

    private static int fmtSizeFromRequirements(int requirements) throws IOException {
        // Determine "fmt " chunk size from requirements.
        int fmtSize = 0x10;
        if ((requirements & AudioIOFormat.REQ_EXT_SIZE) != 0)
            fmtSize = 0x12;
        if ((requirements & AudioIOFormat.REQ_EXT_MODE) != 0)
            fmtSize = 0x28;
        return fmtSize;
    }
}
