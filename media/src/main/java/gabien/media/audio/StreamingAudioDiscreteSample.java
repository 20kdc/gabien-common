/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

/**
 * Audio player memory usage and such optimization
 * Created on 2nd August 2022.
 * Migrated to gabien-media on the 11th August 2022.
 */
public class StreamingAudioDiscreteSample extends DiscreteSample {
    public final AudioIOSource baseSource;
    /**
     * List of chunk byte buffers.
     * All chunks except the last must contain CHUNK_SIZE frames.
     * All chunks are little-endian.
     * The positions are used to track frames being read in.
     */
    private final LinkedList<ByteBuffer> chunks = new LinkedList<ByteBuffer>();
    private final int frameSize;
    private final int chunkSize;
    private int framesCached;
    private int remainingFramesToCache;

    public StreamingAudioDiscreteSample(AudioIOSource source) {
        this(source, 0x1000);
    }

    public StreamingAudioDiscreteSample(AudioIOSource source, int cs) {
        super(source.crSet, source.frameCount());
        chunkSize = cs;
        frameSize = channels * source.format.bytesPerSample;
        baseSource = source;
        framesCached = 0;
        remainingFramesToCache = length;
    }

    private boolean addChunk() {
        int framesInThisChunk = chunkSize;
        if (remainingFramesToCache < framesInThisChunk)
            framesInThisChunk = remainingFramesToCache;
        if (framesInThisChunk <= 0)
            return false;
        ByteBuffer chunkContent = ByteBuffer.allocate(framesInThisChunk * frameSize);
        chunkContent.order(ByteOrder.LITTLE_ENDIAN);
        try {
            int positionInStagingBuffer = 0;
            for (int i = 0; i < framesInThisChunk; i++) {
                baseSource.nextFrame(chunkContent, positionInStagingBuffer);
                positionInStagingBuffer += frameSize;
            }
        } catch (IOException ioe) {
            // well, we tried
        }
        chunks.add(chunkContent);
        remainingFramesToCache -= framesInThisChunk;
        framesCached += framesInThisChunk;
        return true;
    }

    private ByteBuffer locateFrame(int frame) {
        while (frame >= framesCached) {
            if (!addChunk())
                return null;
        }
        int chunkIdx = frame / chunkSize;
        if ((chunkIdx < 0) || (chunkIdx >= chunks.size()))
            return null;
        ByteBuffer chk = chunks.get(chunkIdx);
        frame -= chunkIdx * chunkSize;
        int chunkFrames = chk.limit() / frameSize;
        if (frame >= chunkFrames)
            return null;
        chk.position(frame * frameSize);
        return chk;
    }

    @Override
    public void getF32(int frame, float[] buffer) {
        synchronized (this) {
            ByteBuffer target = locateFrame(frame);
            if (target == null)
                return;
            int pos = target.position();
            for (int i = 0; i < channels; i++) {
                buffer[i] = (float) baseSource.format.asF64(target, pos);
                pos += baseSource.format.bytesPerSample;
            }
        }
    }

    @Override
    public void getS32(int frame, int[] buffer) {
        synchronized (this) {
            ByteBuffer target = locateFrame(frame);
            if (target == null)
                return;
            int pos = target.position();
            for (int i = 0; i < channels; i++) {
                buffer[i] = baseSource.format.asS32(target, pos);
                pos += baseSource.format.bytesPerSample;
            }
        }
    }
}