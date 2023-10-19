/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

/**
 * Receives segments.
 * Created 19th October, 2023.
 */
public interface OggSegmentReceiver {
    /**
     * Receives a segment.
     * A segment may only be up to 255 bytes long.
     * Beware the sign of len!
     * The data array will remain untouched until invalidateStorage is called.
     * (This may allow zero-copy shenanigans.)
     */
    void segment(byte[] data, int ofs, byte len);

    /**
     * The storage used by segment will become invalid when this function returns.
     */
    void invalidateStorage();

    /**
     * Ends a packet and submits it.
     */
    void end();

    /**
     * Discard the current packet, if any.
     * This is used on any page that doesn't announce a continued packet.
     * It's also a general anti-corruption measure.
     */
    void discard();
}
