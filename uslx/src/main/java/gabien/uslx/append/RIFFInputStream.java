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
public class RIFFInputStream extends ChunkedInputStream {
    public @NonNull final String chunkId;
    private String chunkIdTmp;
    private int chunkPaddedLen;

    /**
     * Reads chunk header and prepares for further reading.
     */
    public RIFFInputStream(@NonNull InputStream in) throws IOException {
        super(in, null);
        chunkId = chunkIdTmp;
    }

    @Override
    protected int readChunkHeader(Object indicator) throws IOException {
        chunkIdTmp = readFourCC();
        int len = super.readInt();
        chunkPaddedLen = len + (len & 1);
        return len;
    }

    /**
     * Terminates the chunk (doesn't close parent stream).
     */
    @Override
    public void close() throws IOException {
        // override again because of padding
        skipBytes(chunkPaddedLen - chunkPos);
    }
}
