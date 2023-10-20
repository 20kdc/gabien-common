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
    private OggVorbisSource(VorbisDecoder res, LinkedList<byte[]> packets, long igp, long lgp) throws IOException {
        super(new AudioIOCRSet(res.channels, res.sampleRate));
        this.res = res;
        this.packets = packets;
        this.buffer = new float[res.outputLength];
        if (igp < 0)
            framesToDiscard = (int) -igp;
        frameCount = (int) (lgp - igp);
    }

    public static OggVorbisSource fromInputStream(InputStream inp, boolean close) throws IOException {
        try {
            OggBufferingInputStreamReader isr = new OggBufferingInputStreamReader(inp);
            LinkedList<byte[]> packets = isr.packets;
            while (isr.packets.size() < 4) {
                if (!isr.readNextPage())
                    throw new IOException("Was unable to get header packets and a single data packet");
            }
            // granule pos of packet 4 (1st audio packet)
            long initGranulePos = isr.lastGranulePos;
            // grab all packets
            while (isr.readNextPage());
            long lastGranulePos = isr.lastGranulePos;
            // do setup
            byte[] id = isr.packets.removeFirst();
            isr.packets.removeFirst();
            byte[] setup = isr.packets.removeFirst();
            VorbisDecoder res = new VorbisDecoder(id, 0, id.length, setup, 0, setup.length);
            return new OggVorbisSource(res, packets, initGranulePos, lastGranulePos);
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
