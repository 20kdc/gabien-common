/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

import java.io.IOException;
import java.io.InputStream;

/**
 * Separates an InputStream into Ogg pages.
 * Created 20th October, 2023.
 */
public class OggBufferingInputStreamReader {
    private final byte[] chunkBuffer = new byte[512];
    private int chunkBufferPosition, chunkBufferRemaining;
    private final OggReader or = new OggReader();
    // can't really change this around safely due to the chunk buffer...
    private final InputStream inp;
    // ...but can change this!
    public OggPageReceiver out;

    public OggBufferingInputStreamReader(InputStream inp, OggPageReceiver opr) {
        this.inp = inp;
        out = opr;
    }

    /**
     * Reads initial BOS pages in a physical bitstream.
     * Notably, does NOT consume the first 'data page'.
     * Chaining is poorly-specified anyway.
     * Does the language imply the logical bitstreams are concatenated or the physical bitstreams?
     */
    public void readStartingBOSPages() throws IOException {
        while (true) {
            if (chunkBufferRemaining > 0) {
                or.addByteToSyncWindow(chunkBuffer[chunkBufferPosition++]);
                chunkBufferRemaining--;
                int len = OggPage.verifyAndGetLength(or.syncWindow, 0, or.amountOfDataInSyncWindow);
                if (len > 0) {
                    int flags = or.syncWindow[OggPage.FIELD_FLAGS];
                    boolean isBOS = (flags & OggPage.FLAG_BOS) != 0;
                    if (!isBOS)
                        return;
                    out.page(or.syncWindow, 0, len);
                    or.skipSyncWindow(len);
                }
            } else if (!refillChunkBuffer()) {
                return;
            }
        }
    }

    /**
     * Returns true if a page was read.
     */
    public boolean readNextPage() throws IOException {
        while (true) {
            if (chunkBufferRemaining > 0) {
                or.addByteToSyncWindow(chunkBuffer[chunkBufferPosition++]);
                chunkBufferRemaining--;
                int len = OggPage.verifyAndGetLength(or.syncWindow, 0, or.amountOfDataInSyncWindow);
                if (len > 0) {
                    out.page(or.syncWindow, 0, len);
                    or.skipSyncWindow(len);
                    return true;
                }
            } else if (!refillChunkBuffer()) {
                return false;
            }
        }
    }

    /**
     * Refills chunk buffer for more reading.
     */
    private boolean refillChunkBuffer() throws IOException {
        int amount = inp.read(chunkBuffer);
        if (amount == -1)
            return false;
        chunkBufferPosition = 0;
        chunkBufferRemaining = amount;
        return true;
    }
}
