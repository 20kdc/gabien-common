/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.audio.fileio;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import org.eclipse.jdt.annotation.NonNull;

import gabien.media.audio.AudioIOCRSet;
import gabien.media.audio.AudioIOSource;
import gabien.media.ogg.OggBufferedDemux;
import gabien.media.ogg.OggBufferingInputStreamReader;
import gabien.natives.VorbisDecoder;

/**
 * Created 20th October 2023.
 */
public class OggVorbisSource extends AudioIOSource.SourceF32 {
    private final VorbisDecoder res;
    private final int frameCount;
    int framesToDiscard;
    private final LinkedList<byte[]> packets;
    private final float[] buffer;
    private int bufferPosition = 0;
    private int bufferRemainingFrames = 0;
    private OggVorbisSource(VorbisDecoder res, LinkedList<byte[]> packets, long igp) throws IOException {
        super(new AudioIOCRSet(res.channels, res.sampleRate));
        this.res = res;
        this.packets = packets;
        this.buffer = new float[res.outputLength];
        if (igp < 0)
            framesToDiscard = (int) -igp;
        boolean isFirst = true;
        // Since we have all samples, we can measure the stream by inspecting packet headers.
        // This doesn't require we actually *decode* it.
        // The code to do this could be written in Java.
        // However, treating stb_vorbis_g as a semi-independent project, having it there makes more sense.
        // stb_vorbis_g doesn't know about Ogg and we don't know about Vorbis.
        int tfc = 0;
        for (byte[] b : packets) {
            if (isFirst) {
                isFirst = false;
                continue;
            }
            tfc += res.getPacketSampleCount(b, 0, b.length);
        }
        tfc -= framesToDiscard;
        frameCount = tfc;
    }

    public static OggVorbisSource fromInputStream(InputStream inp, boolean close) throws IOException {
        try {
            OggBufferingInputStreamReader isr;
            OggBufferedDemux.Stream stream;
            {
                OggBufferedDemux demux = new OggBufferedDemux();
                isr = new OggBufferingInputStreamReader(inp, demux);
                // Assume a compliant unchained stream.
                // This procedure should be able to deal with (read: ignore) Ogg Skeleton and other odd cases.
                // It can't handle chained streams, but chained streams are weird.
                isr.readStartingBOSPages();
                // Attempt to identify a Vorbis stream (i.e. the one we'll be using)
                // Do this by peeking at first packet of each stream
                stream = null;
                for (OggBufferedDemux.Stream st : demux.streams) {
                    if (st.packets.size() > 0) {
                        byte[] packet1 = st.packets.getFirst();
                        if (packet1.length < 7)
                            continue;
                        if (packet1[0] != 1)
                            continue;
                        if (packet1[1] != 'v')
                            continue;
                        if (packet1[2] != 'o')
                            continue;
                        if (packet1[3] != 'r')
                            continue;
                        if (packet1[4] != 'b')
                            continue;
                        if (packet1[5] != 'i')
                            continue;
                        if (packet1[6] != 's')
                            continue;
                        stream = st;
                        break;
                    }
                }
                // That stream is now the only stream we will buffer
                // (all other streams can now be GC'd)
                if (stream == null)
                    throw new IOException("No Vorbis stream could be identified from the BOS pages.");
                isr.out = stream;
            }
            // Finish setup
            LinkedList<byte[]> packets = stream.packets;
            while (packets.size() < 4) {
                if (!isr.readNextPage())
                    throw new IOException("Was unable to get header packets and a single data packet");
            }
            // granule pos of packet 4 (1st audio packet)
            long initGranulePos = stream.lastGranulePos;
            // grab all packets
            while (isr.readNextPage());
            // do setup
            byte[] id = packets.removeFirst();
            packets.removeFirst();
            byte[] setup = packets.removeFirst();
            VorbisDecoder res = new VorbisDecoder(id, 0, id.length, setup, 0, setup.length);
            return new OggVorbisSource(res, packets, initGranulePos);
        } finally {
            if (close)
                inp.close();
        }
    }

    @Override
    public int frameCount() {
        return frameCount;
    }

    @Override
    public void nextFrames(@NonNull float[] frame, int at, int frames) throws IOException {
        while (frames > 0) {
            if (bufferRemainingFrames > 0) {
                if (framesToDiscard > 0) {
                    // don't advance!
                    bufferPosition += res.channels;
                } else {
                    for (int i = 0; i < res.channels; i++)
                        frame[at++] = buffer[bufferPosition++];
                    frames--;
                }
                bufferRemainingFrames--;
            } else if (packets.size() > 0) {
                bufferPosition = 0;
                byte[] packet = packets.removeFirst();
                bufferRemainingFrames = res.decodeFrame(packet, 0, packet.length, buffer, 0);
            } else {
                throw new EOFException("Out of frames!");
            }
        }
    }
}
