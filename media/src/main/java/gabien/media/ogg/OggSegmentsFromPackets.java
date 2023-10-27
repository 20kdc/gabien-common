/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

/**
 * Splits packets into segments.
 * Created 19th October, 2023.
 */
public final class OggSegmentsFromPackets implements OggPacketReceiver {
    private final OggSegmentReceiver output;

    public OggSegmentsFromPackets(OggSegmentReceiver o) {
        output = o;
    }

    @Override
    public void packet(byte[] data, int ofs, int len) {
        // handle chunks of 255
        while (len >= 255) {
            output.segment(data, ofs, (byte) 255);
            len -= 255;
            ofs += 255;
        }
        // handle the remaining *non-255* len (including 0)
        output.segment(data, ofs, (byte) len);
    }
}