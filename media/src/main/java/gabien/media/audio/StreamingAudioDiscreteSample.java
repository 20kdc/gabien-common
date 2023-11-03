/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.audio;

import java.util.LinkedList;

/**
 * Audio player memory usage and such optimization
 * Created on 2nd August 2022.
 * Migrated to gabien-media on the 11th August 2022.
 */
public class StreamingAudioDiscreteSample extends DiscreteSample {
    public final AudioIOSource baseSource;
    private final AudioIOFormat storageFormat;

    /**
     * List of chunk byte buffers.
     * All chunks except the last must contain CHUNK_SIZE frames.
     * All chunks are little-endian.
     * The positions are used to track frames being read in.
     */
    private final LinkedList<byte[]> chunks = new LinkedList<byte[]>();
    private final int frameSize;
    private final int chunkSize;
    private int framesCached;
    private int remainingFramesToCache;

    public StreamingAudioDiscreteSample(AudioIOSource source, AudioIOFormat fmt) {
        this(source, fmt, 0x1000);
    }

    public StreamingAudioDiscreteSample(AudioIOSource source, AudioIOFormat fmt, int cs) {
        super(source.crSet, source.frameCount());
        storageFormat = fmt;
        chunkSize = cs;
        frameSize = channels * fmt.bytesPerSample;
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
        byte[] chunkContent = new byte[framesInThisChunk * frameSize];
        try {
            baseSource.nextFramesInFormat(storageFormat, chunkContent, 0, framesInThisChunk);
        } catch (Exception ioe) {
            // well, we tried
            // ioe.printStackTrace();
        }
        chunks.add(chunkContent);
        remainingFramesToCache -= framesInThisChunk;
        framesCached += framesInThisChunk;
        return true;
    }

    private void locateFrame(int frame, Object buffer, boolean wantsF32) {
        while (frame >= framesCached) {
            if (!addChunk())
                return;
        }
        int chunkIdx = frame / chunkSize;
        if ((chunkIdx < 0) || (chunkIdx >= chunks.size()))
            return;
        byte[] chk = chunks.get(chunkIdx);
        frame -= chunkIdx * chunkSize;
        int chunkFrames = chk.length / frameSize;
        if (frame >= chunkFrames)
            return;
        int chkOfs = frame * frameSize;
        if (wantsF32) {
            float[] buf = (float[]) buffer;
            for (int i = 0; i < channels; i++) {
                buf[i] = (float) storageFormat.asF64(chk, chkOfs);
                chkOfs += storageFormat.bytesPerSample;
            }
        } else {
            int[] buf = (int[]) buffer;
            for (int i = 0; i < channels; i++) {
                buf[i] = storageFormat.asS32(chk, chkOfs);
                chkOfs += storageFormat.bytesPerSample;
            }
        }
    }

    @Override
    public void getF32(int frame, float[] buffer) {
        synchronized (this) {
            locateFrame(frame, buffer, true);
        }
    }

    @Override
    public void getS32(int frame, int[] buffer) {
        synchronized (this) {
            locateFrame(frame, buffer, false);
        }
    }
}
