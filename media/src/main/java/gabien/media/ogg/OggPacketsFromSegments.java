/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

import java.io.ByteArrayOutputStream;

/**
 * Collates segments in memory and packets them.
 * Created 19th October, 2023.
 */
public final class OggPacketsFromSegments implements OggSegmentReceiver {
    private final ByteArrayOutputStream collation = new ByteArrayOutputStream();
    private final OggPacketReceiver output;

    public OggPacketsFromSegments(OggPacketReceiver o) {
        output = o;
    }

    @Override
    public final void segment(byte[] data, int ofs, byte len) {
        collation.write(data, ofs, len & 0xFF);
    }

    @Override
    public void invalidateStorage() {
    }

    @Override
    public final void end() {
        byte[] data = collation.toByteArray();
        output.packet(data, 0, data.length);
        collation.reset();
    }

    @Override
    public final void discard() {
        collation.reset();
    }
}
