/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media;

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
        chunkPaddedLen = len;
        if (RIFFOutputStream.shouldPad(len))
            chunkPaddedLen++;
        return len;
    }

    public void readListOrRiffTypeAndVerify(String expectedRIFFHead, String expectedLISTHead) throws IOException {
        String rc = readFourCC();
        boolean isRIFFWAVE = chunkId.equals("RIFF") && rc.equals(expectedRIFFHead);
        boolean isLISTwave = chunkId.equals("LIST") && rc.equals(expectedLISTHead);
        if (!(isRIFFWAVE || isLISTwave))
            throw new IOException("File expected to be RIFF;" + expectedRIFFHead + " or LIST;" + expectedLISTHead + " but was actually " + chunkId + ";" + rc);
    }

    /**
     * Terminates the chunk (doesn't close parent stream).
     */
    @Override
    public void close() throws IOException {
        // override again because of padding
        holdoffLimiters = true;
        skipBytes(chunkPaddedLen - chunkPos);
        holdoffLimiters = false;
    }
}
