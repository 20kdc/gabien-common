/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.audio.fileio;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNull;

import gabien.media.audio.AudioIOCRSet;
import gabien.media.audio.AudioIOSource;
import gabien.natives.MP3Decoder;

/**
 * Created 3rd November 2023.
 */
public class MP3Source extends AudioIOSource.SourceF32 {
    private final MP3Decoder res;
    private final int frameCount;

    private final byte[] data;
    private int dataPosition = 0;

    private final float[] buffer = new float[MP3Decoder.MAX_SAMPLES_PER_FRAME];
    private int bufferPosition = 0;
    private int bufferRemainingFrames = 0;

    private MP3Source(MP3Decoder res, int channels, int sampleRate, int frameCount, byte[] data) throws IOException {
        super(new AudioIOCRSet(channels, sampleRate));
        this.res = res;
        this.data = data;
        this.frameCount = frameCount;
    }

    public static MP3Source fromInputStream(InputStream inp, boolean close) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] chk = new byte[4096];
            while (true) {
                int len = inp.read(chk);
                if (len < 0)
                    break;
                baos.write(chk, 0, len);
            }
            byte[] data = baos.toByteArray();
            baos = null;
            // measure stream length and first packet setup
            boolean isFirst = true;
            int frameCount = 0;
            int sampleRate = 22050;
            int channels = 1;
            int dataPosition = 0;
            MP3Decoder res = new MP3Decoder();
            while (true) {
                int frames = res.decodeFrame(data, dataPosition, data.length - dataPosition, null, 0);
                if (frames == 0) {
                    dataPosition += 4096;
                    if (dataPosition >= data.length)
                        break;
                    continue;
                } else {
                    frameCount += frames;
                    if (isFirst) {
                        sampleRate = res.getLastFrameSampleRate();
                        channels = res.getLastFrameChannels();
                        isFirst = false;
                    }
                    dataPosition += res.getLastFrameBytes();
                }
            }
            if (frameCount == 0) {
                res.close();
                throw new IOException("Not an MP3");
            }
            res.reset();
            return new MP3Source(res, channels, sampleRate, frameCount, data);
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
                for (int i = 0; i < crSet.channels; i++)
                    frame[at++] = buffer[bufferPosition++];
                frames--;
                bufferRemainingFrames--;
            } else {
                bufferPosition = 0;
                while (true) {
                    bufferRemainingFrames = res.decodeFrame(data, dataPosition, data.length - dataPosition, buffer, 0);
                    if (bufferRemainingFrames == 0) {
                        dataPosition += 4096;
                        if (dataPosition >= data.length)
                            throw new EOFException("Out of frames!");
                    } else {
                        dataPosition += res.getLastFrameBytes();
                        break;
                    }
                }
            }
        }
    }
}
