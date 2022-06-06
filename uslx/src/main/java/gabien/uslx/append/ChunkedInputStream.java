/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.uslx.append;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Base class for chunked file input streams.
 * Created on 6th June 2022 as part of project VE2Bun
 */
public abstract class ChunkedInputStream extends XEDataInputStream {
    public final int chunkLen;
    protected int chunkPos;
    protected int resetPos;
    private boolean duringInit = true;

    /**
     * A subclass of this class is expected to.
     */
    public ChunkedInputStream(@NonNull InputStream in, @Nullable Object indicator) throws IOException {
        super(in);
        chunkLen = readChunkHeader(indicator);
        duringInit = false;
    }

    /**
     * Reads the chunk header and returns the chunk length.
     * @param indicator Indication value in the event that grabbing the chunk length is a non-obvious procedure.
     * @return Chunk length.
     */
    protected abstract int readChunkHeader(Object indicator) throws IOException;

    /**
     * Gets remaining bytes in the chunk.
     * @return Remaining chunk bytes.
     */
    public final int remainingInChunk() {
        return chunkLen - chunkPos;
    }

    /**
     * Terminates the chunk (doesn't close parent stream).
     */
    @Override
    public void close() throws IOException {
        skipBytes(chunkLen - chunkPos);
    }

    @Override
    public int read() throws IOException {
        if (duringInit)
            return super.read();
        if (chunkPos >= chunkLen)
            return -1;
        int res = super.read();
        if (res != -1)
            chunkPos++;
        return res;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (duringInit)
            return super.read(b, off, len);
        int efAvailable = chunkLen - chunkPos;
        if (efAvailable < len)
            len = efAvailable;
        int res = super.read(b, off, len);
        if (res > 0)
            chunkPos += res;
        return res;
    }

    @Override
    public int available() throws IOException {
        if (duringInit)
            return super.available();
        int actualAvailable = super.available();
        int efAvailable = chunkLen - chunkPos;
        if (actualAvailable > efAvailable)
            return efAvailable;
        return actualAvailable;
    }

    @Override
    public synchronized void reset() throws IOException {
        chunkPos = resetPos;
        super.reset();
    }

    @Override
    public synchronized void mark(int readlimit) {
        resetPos = chunkPos;
        super.mark(readlimit);
    }
}