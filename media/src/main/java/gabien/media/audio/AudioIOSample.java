/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.audio;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Think of this like AudioIOSource but in-memory.
 * Created on 10th June 2022 as part of WTFr7.
 */
public final class AudioIOSample extends DiscreteSample {
    public final AudioIOFormat format;
    public final byte[] data;
    public final int bytesPerFrame;

    public AudioIOSample(AudioIOSource src, AudioIOFormat fmt) throws IOException {
        this(src.crSet, fmt, src.frameCount());
        src.nextFramesInFormat(fmt, data, 0, data.length / bytesPerFrame);
    }

    public AudioIOSample(AudioIOCRSet crs, AudioIOFormat fmt, int len) {
        super(crs, len);
        format = fmt;
        bytesPerFrame = crs.channels * fmt.bytesPerSample;
        data = new byte[length * bytesPerFrame];
    }

    public AudioIOSource getSource() {
        return new AudioIOSource.SourceBytes(this, format) {
            int ptr = 0;

            @Override
            public void nextFrames(@NonNull byte[] frame, int at, int frames) throws IOException {
                System.arraycopy(data, ptr, frame, at, bytesPerFrame);
                ptr += bytesPerFrame;
            }

            @Override
            public int frameCount() {
                return length;
            }
        };
    }

    @Override
    public final void getF32(int frame, float[] buffer) {
        int at = frame * bytesPerFrame;
        for (int i = 0; i < channels; i++) {
            buffer[i] = (float) format.asF64(data, at);
            at += format.bytesPerSample;
        }
    }

    @Override
    public final void getS32(int frame, int[] buffer) {
        int at = frame * bytesPerFrame;
        for (int i = 0; i < channels; i++) {
            buffer[i] = format.asS32(data, at);
            at += format.bytesPerSample;
        }
    }

    public final void setF32(int frame, float[] buffer) {
        int at = frame * bytesPerFrame;
        for (int i = 0; i < channels; i++) {
            format.ofF64(data, at, buffer[i]);
            at += format.bytesPerSample;
        }
    }

    public final void setS32(int frame, int[] buffer) {
        int at = frame * bytesPerFrame;
        for (int i = 0; i < channels; i++) {
            format.ofS32(data, at, buffer[i]);
            at += format.bytesPerSample;
        }
    }
}
