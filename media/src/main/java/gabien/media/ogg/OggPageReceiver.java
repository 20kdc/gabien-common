/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

/**
 * Receives pages.
 * Created 27th October, 2023.
 */
public interface OggPageReceiver {
    /**
     * Submits a page.
     * The page is asserted to be valid.
     * As such, the length is only given to save on wasteful computation.
     */
    void page(byte[] data, int ofs, int len);
}
