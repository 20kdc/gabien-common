/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.uslx.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Think of this like AudioIOSource but in-memory.
 * Created on 10th June 2022 as part of WTFr7.
 */
public final class AudioIOSample extends DiscreteSample {
    public final AudioIOFormat format;
    public final ByteBuffer data;
    public final int bytesPerFrame;

    public AudioIOSample(AudioIOSource src) throws IOException {
        this(src.crSet, src.format, src.frameCount());
        int at = 0;
        int cap = data.capacity();
        while (at < cap) {
            src.nextFrame(data, at);
            at += bytesPerFrame;
        }
    }

    public AudioIOSample(AudioIOCRSet crs, AudioIOFormat fmt, int len) {
        super(crs, len);
        format = fmt;
        bytesPerFrame = crs.channels * fmt.bytesPerSample;
        data = ByteBuffer.allocate(length * bytesPerFrame);
        data.order(ByteOrder.LITTLE_ENDIAN);
    }

    public AudioIOSource getSource() {
        return new AudioIOSource(this, format) {
            int ptr = 0;
            @Override
            public void nextFrame(@NonNull ByteBuffer frame, int at) throws IOException {
                data.position(ptr);
                data.get(frame.array(), frame.arrayOffset() + at, bytesPerFrame);
                ptr += bytesPerFrame;
            }
            
            @Override
            public int frameCount() {
                return length;
            }
        };
    }

    @Override
    public void getF32(int frame, float[] buffer) {
        int at = frame * bytesPerFrame;
        for (int i = 0; i < channels; i++) {
            buffer[i] = (float) format.asF64(data, at);
            at += format.bytesPerSample;
        }
    }

    @Override
    public void getS32(int frame, int[] buffer) {
        int at = frame * bytesPerFrame;
        for (int i = 0; i < channels; i++) {
            buffer[i] = format.asS32(data, at);
            at += format.bytesPerSample;
        }
    }

    /**
     * Why, what a temporary holdover this is! Would be a shame if it were to disappear in the next commit...
     * Gets a 32-bit PCM sample given a position (as if indexing a flattened [length][channels] array)
     */
    public int getS32(int at) {
        return format.asS32(data, format.bytesPerSample * at);
    }
    /**
     * Why, what a temporary holdover this is! Would be a shame if it were to disappear in the next commit...
     * Gets a 64-bit floating point sample given a position (as if indexing a flattened [length][channels] array)
     */
    public double getF64(int at) {
        return format.asF64(data, format.bytesPerSample * at);
    }
}
