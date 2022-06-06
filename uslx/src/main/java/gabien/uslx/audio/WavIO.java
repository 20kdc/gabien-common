/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.uslx.audio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;

import gabien.uslx.append.*;

/**
 * Contains a streaming WAV reader and writer.
 * For now, that's all.
 * Created on 23rd May, 2022 (LTCH:PPJ6)
 * Reading support added 6th June, 2022
 */
public class WavIO {
    public static AudioIOSource readWAV(@NonNull final InputStream fis) throws IOException {
        RIFFInputStream ris = new RIFFInputStream(fis);
        if (!ris.chunkId.equals("RIFF"))
            throw new IOException("This 'WAV' isn't even a RIFF file");
        String rc = ris.readFourCC();
        if (!rc.equals("WAVE"))
            throw new IOException("This RIFF isn't a WAVE.");
        AudioIOFormat fmt = null;
        AudioIOCRSet cr = null;
        while (ris.available() > 0) {
            RIFFInputStream chk = new RIFFInputStream(ris);
            if (chk.chunkId.equals("fmt ")) {
                int fmtTag = chk.readUnsignedShort();
                int channels = chk.readUnsignedShort();
                int channelMask = 0;
                int sampleRate = chk.readInt();
                chk.readInt(); // bytes per second
                chk.readUnsignedShort();
                int sampleBits = chk.readUnsignedShort(); // sample bits
                if (fmtTag == 0xFFFE) {
                    if (chk.readUnsignedShort() < 0x16)
                        throw new IOException("Extensible WAVE format, but not enough header for it");
                    chk.readShort();
                    channelMask = chk.readInt();
                    fmtTag = chk.readUnsignedShort();
                }
                fmt = AudioIOFormat.detect(fmtTag, sampleBits);
                cr = new AudioIOCRSet(channels, channelMask, sampleRate);
            } else if (chk.chunkId.equals("data")) {
                final RIFFInputStream data = chk;
                final int frameBytes = fmt.bytesPerSample * cr.channels;
                final int frameCount = chk.chunkLen / frameBytes;
                return new AudioIOSource(cr, fmt) {
                    @Override
                    public int frameCount() {
                        return frameCount;
                    }

                    @Override
                    public void nextFrame(@NonNull ByteBuffer frame) throws IOException {
                        data.readFully(frame.array(), frame.arrayOffset(), frameBytes);
                    }

                    @Override
                    public void close() throws IOException {
                        // don't bother with a proper RIFF close
                        fis.close();
                    }
                };
            }
            chk.close();
        }
        throw new IOException("Never found data chunk");
    }

    public static void writeWAV(@NonNull OutputStream fos, @NonNull AudioIOSource dataSource) throws IOException {
        // Details of the format.
        AudioIOCRSet cr = dataSource.crSet;
        AudioIOFormat fmt = dataSource.format;
        int frameCount = dataSource.frameCount();
        int frameBytes = cr.channels * fmt.bytesPerSample;
        int totalSampleBytes = frameCount * frameBytes;
        // Expand requirements.
        int requirements = fmt.requirements;
        // channel mask implies EXT_MODE
        if (cr.channelMask != 0)
            requirements |= AudioIOFormat.REQ_EXT_MODE;
        // EXT_MODE implies EXT_SIZE and FACT
        if ((requirements & AudioIOFormat.REQ_EXT_MODE) != 0) {
            requirements |= AudioIOFormat.REQ_EXT_SIZE;
            requirements |= AudioIOFormat.REQ_FACT;
        }
        // Determine "fmt " chunk size from requirements.
        int fmtSize = 0x10;
        if ((requirements & AudioIOFormat.REQ_EXT_SIZE) != 0)
            fmtSize = 0x12;
        if ((requirements & AudioIOFormat.REQ_EXT_MODE) != 0)
            fmtSize = 0x28;
        // Now we know exactly what we're going to generate, let's build a header!
        int interiorContent = 4;
        interiorContent += RIFFOutputStream.getInteriorChunkSize(fmtSize);
        interiorContent += RIFFOutputStream.getInteriorChunkSize(totalSampleBytes);
        RIFFOutputStream riffChunk = new RIFFOutputStream(fos, "RIFF", interiorContent);
        // Filetype
        riffChunk.writeBytes("WAVE");
        // fmt {
        RIFFOutputStream fmtChunk = new RIFFOutputStream(riffChunk, "fmt ", fmtSize);
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
        if ((requirements & AudioIOFormat.REQ_FACT) != 0) {
            // fact {
            RIFFOutputStream factChunk = new RIFFOutputStream(riffChunk, "fact", 4);
            factChunk.writeInt(frameCount);
            factChunk.close();
            // }
        }
        // data {
        RIFFOutputStream dataChunk = new RIFFOutputStream(riffChunk, "data", totalSampleBytes);
        // And now for the sample data!
        byte[] data = new byte[frameBytes];
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < frameCount; i++) {
            // Stereo data.
            bb.position(0);
            dataSource.nextFrame(bb);
            dataChunk.write(data);
        }
        dataChunk.close();
        // }
        // Close off.
        riffChunk.close();
        dataSource.close();
    }
}
