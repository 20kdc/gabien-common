/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

/**
 * Does less verification than it really should, but whatever...
 * Created 20th October, 2023.
 */
public class OggBufferingInputStreamReader {
    public final LinkedList<byte[]> packets = new LinkedList<>();
    private final byte[] chunkBuffer = new byte[512];
    private int chunkBufferPosition, chunkBufferRemaining;
    private final OggReader or = new OggReader();
    private final InputStream inp;
    public long lastGranulePos;

    private final OggPacketsFromSegments opfs = new OggPacketsFromSegments((data, ofs, len) -> {
        byte[] res = new byte[len];
        System.arraycopy(data, ofs, res, 0, len);
        packets.add(res);
    });

    public OggBufferingInputStreamReader(InputStream inp) {
        this.inp = inp;
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
                    lastGranulePos = OggPage.getGranulePos(or.syncWindow, 0);
                    or.sendSegmentsTo(opfs, false);
                    or.skipSyncWindow(len);
                    return true;
                }
            } else {
                int amount = inp.read(chunkBuffer);
                if (amount == -1)
                    return false;
                chunkBufferPosition = 0;
                chunkBufferRemaining = amount;
            }
        }
    }
}
