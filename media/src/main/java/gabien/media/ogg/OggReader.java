/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

/**
 * Ogg page reader.
 * Some definitions:
 * Sync window: area from syncWindow[0] to syncWindow[amountOfDataInSyncWindow - 1].
 * Current page: The page at the head of the sync window.
 * Created 19th October, 2023
 */
public final class OggReader {
    /**
     * Ogg page
     */
    public final byte[] syncWindow = new byte[OggPage.MAX_PAGE_LENGTH];

    /**
     * Before the sync window is full, it may have uninitialized (zero) areas.
     * To prevent a CRC32 collision, from accidentally treating these as valid, this is recorded.
     */
    public int amountOfDataInSyncWindow = 0;

    public OggReader() {
    }

    /**
     * Resets the sync window.
     */
    public void reset() {
        amountOfDataInSyncWindow = 0;
    }

    /**
     * Adds a byte of data to the sync window.
     */
    public void addByteToSyncWindow(byte b) {
        if (amountOfDataInSyncWindow < syncWindow.length) {
            syncWindow[amountOfDataInSyncWindow++] = b;
        } else {
            System.arraycopy(syncWindow, 1, syncWindow, 0, syncWindow.length - 1);
            syncWindow[syncWindow.length - 1] = b;
        }
    }

    /**
     * Consume data from the sync window (to consume a known-valid page).
     */
    public void skipSyncWindow(int len) {
        if (amountOfDataInSyncWindow < len)
            throw new RuntimeException("Can't skip more bytes than are in the sync window!");
        System.arraycopy(syncWindow, len, syncWindow, 0, amountOfDataInSyncWindow - len);
        amountOfDataInSyncWindow -= len;
    }
}
