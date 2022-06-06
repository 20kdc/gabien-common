/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.uslx.append;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Created on 6th June 2022 as part of project VE2Bun
 */
public class RIFFInputStream extends LEDataInputStream {
    public @NonNull final String chunkId;
    public final int chunkLen;
    private int chunkPos;
    private int resetPos;

    /**
     * Reads chunk header and prepares for further reading.
     */
    public RIFFInputStream(InputStream in) throws IOException {
        super(in);
        char a = (char) super.readUnsignedByte();
        char b = (char) super.readUnsignedByte();
        char c = (char) super.readUnsignedByte();
        char d = (char) super.readUnsignedByte();
        chunkId = new String(new char[] {a, b, c, d});
        chunkLen = super.readInt();
    }

    /**
     * Terminates the chunk (doesn't close parent stream).
     */
    @Override
    public void close() throws IOException {
        if ((chunkLen & 1) != 0) {
            skipBytes((chunkLen + 1) - chunkPos);
        } else {
            skipBytes(chunkLen - chunkPos);
        }
    }

    private final void ensure(int amount) throws EOFException {
        if (chunkPos + amount > chunkLen)
            throw new EOFException("Attempted to consume " + amount + " bytes in RIFF chunk " + chunkId + " at " + chunkPos + " but chunk was only " + chunkLen + " bytes long.");
    }

    @Override
    public int available() throws IOException {
        int actualAvailable = super.available();
        int efAvailable = chunkLen - chunkPos;
        if (actualAvailable > efAvailable)
            return efAvailable;
        return actualAvailable;
    }

    @Override
    public int read() throws IOException {
        ensure(1);
        int res = super.read();
        if (res == -1)
            chunkPos++;
        return res;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int efAvailable = chunkLen - chunkPos;
        if (efAvailable < len)
            len = efAvailable;
        int res = super.read(b, off, len);
        if (res > 0)
            chunkPos += res;
        return res;
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
