/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

import gabien.uslx.io.CRC32Forward;

/**
 * Ogg Page utilities.
 * Created 27th October, 2023.
 */
public abstract class OggPage {
    /**
     * Ogg page
     * 27 header + 255 lacing values + 255 segments of 255 bytes
     */
    public static final int MAX_PAGE_LENGTH = 65307;

    public static final int FIELD_FLAGS = 5;
    public static final int FIELD_GRANULEPOS = 6;
    public static final int FIELD_STREAMID = 14;
    public static final int FIELD_SEQNUM = 18;
    public static final int FIELD_CRC32 = 22;

    public static final int FLAG_CONTINUED = 1;
    public static final int FLAG_BOS = 2;
    public static final int FLAG_EOS = 4;

    private OggPage() {
    }

    /**
     * Gets the absolute granule position from the page.
     * Assumes the page is otherwise valid.
     */
    public static long getGranulePos(byte[] syncWindow, int at) {
        at += FIELD_GRANULEPOS;
        long a = syncWindow[at++] & 0xFFL;
        long b = syncWindow[at++] & 0xFFL;
        long c = syncWindow[at++] & 0xFFL;
        long d = syncWindow[at++] & 0xFFL;
        long e = syncWindow[at++] & 0xFFL;
        long f = syncWindow[at++] & 0xFFL;
        long g = syncWindow[at++] & 0xFFL;
        long h = syncWindow[at] & 0xFFL;
        a |= b << 8;
        a |= c << 16;
        a |= d << 24;
        a |= e << 32;
        a |= f << 40;
        a |= g << 48;
        a |= h << 56;
        return a;
    }

    /**
     * Gets the stream ID from the page.
     * Assumes the page is otherwise valid.
     */
    public static int getStreamID(byte[] syncWindow, int at) {
        at += FIELD_STREAMID;
        int a = syncWindow[at++] & 0xFF;
        int b = syncWindow[at++] & 0xFF;
        int c = syncWindow[at++] & 0xFF;
        int d = syncWindow[at] & 0xFF;
        a |= b << 8;
        a |= c << 16;
        a |= d << 24;
        return a;
    }

    /**
     * Gets the sequence number from the page.
     * Assumes the page is otherwise valid.
     */
    public static int getSeqNum(byte[] syncWindow, int at) {
        at += FIELD_SEQNUM;
        int a = syncWindow[at++] & 0xFF;
        int b = syncWindow[at++] & 0xFF;
        int c = syncWindow[at++] & 0xFF;
        int d = syncWindow[at] & 0xFF;
        a |= b << 8;
        a |= c << 16;
        a |= d << 24;
        return a;
    }

    /**
     * Gets the CRC32 from the page.
     * Assumes the page is otherwise valid.
     */
    public static int getCRC32(byte[] syncWindow, int at) {
        at += FIELD_CRC32;
        int a = syncWindow[at++] & 0xFF;
        int b = syncWindow[at++] & 0xFF;
        int c = syncWindow[at++] & 0xFF;
        int d = syncWindow[at] & 0xFF;
        a |= b << 8;
        a |= c << 16;
        a |= d << 24;
        return a;
    }

    /**
     * Returns the length of the current page.
     * Note that this does not verify said page is valid.
     * It only returns the length of a hypothetical page.
     */
    public static int getLength(byte[] syncWindow, int at) {
        int segmentCount = syncWindow[at + 26] & 0xFF;
        int totalLength = 27 + segmentCount;
        for (int i = 0; i < segmentCount; i++)
            totalLength += syncWindow[at + 27 + i] & 0xFF;
        return totalLength;
    }

    /**
     * Returns 0 if the current page is invalid.
     * Otherwise see getLength.
     */
    public static int verifyAndGetLength(byte[] syncWindow, int at, int amountOfDataInSyncWindow) {
        // The page cannot be valid if the header is missing.
        // However, an empty page is still valid.
        if (amountOfDataInSyncWindow < 27)
            return 0;
        // Check for sync pattern & version.
        if (syncWindow[at] != 'O')
            return 0;
        if (syncWindow[at + 1] != 'g')
            return 0;
        if (syncWindow[at + 2] != 'g')
            return 0;
        if (syncWindow[at + 3] != 'S')
            return 0;
        if (syncWindow[at + 4] != 0)
            return 0;
        int length = getLength(syncWindow, at);
        // Check length makes sense.
        if (amountOfDataInSyncWindow < length)
            return 0;
        // Ok, so we have a complete byte array that looks like an Ogg page.
        // This means it's probably an Ogg page.
        // Check CRC32.
        int crc32 = 0;
        CRC32Forward checksum = CRC32Forward.CRC32_04C11DB7;
        crc32 = checksum.update(crc32, syncWindow, at, 22);
        crc32 = checksum.update(crc32, (byte) 0);
        crc32 = checksum.update(crc32, (byte) 0);
        crc32 = checksum.update(crc32, (byte) 0);
        crc32 = checksum.update(crc32, (byte) 0);
        crc32 = checksum.update(crc32, syncWindow, at + 26, length - 26);
        if (crc32 != getCRC32(syncWindow, at))
            return 0;
        return length;
    }
}
