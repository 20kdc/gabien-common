/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

import java.io.ByteArrayOutputStream;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Collates segments in memory and packets them.
 * (As of 27th Oct 2023): Importantly, avoids copying whenever possible.
 * For typical Vorbis files, it seems pages are never crossed, though this is not an implementation guarantee.
 * In most cases this means taking data straight from sync window to OggPacketReceiver is possible.
 * While a copy happens there right now, this still saves time during scanning.
 * Created 19th October, 2023.
 */
public final class OggPacketsFromSegments implements OggSegmentReceiver.Discardable {
    // If non-null, we're in zero copy mode.
    // That said will also *usually* be null while packetLen == 0.
    private @Nullable byte[] zeroCopySrc;
    private int zeroCopyOfs;
    // Shared packet length (exists to disambiguate zero copy vs collation and such)
    private int packetLen;

    // Packet storage in "copy mode".
    // If empty, "zero copy mode" is active. See zeroCopySrc.
    private final ByteArrayOutputStream collation = new ByteArrayOutputStream();
    private final OggPacketReceiver output;

    public OggPacketsFromSegments(OggPacketReceiver o) {
        output = o;
    }

    @Override
    public final void segment(byte[] data, int ofs, byte len) {
        int leni = len & 0xFF;
        if (packetLen == 0) {
            // always start with zero-copy
            zeroCopySrc = data;
            zeroCopyOfs = ofs;
        } else if (leni == 0) {
            // we've already started a packet, so we don't need to init zero-copy
            // but, we're contributing no data
            // since we don't want an empty array to upset zero-copy, skip logic
        } else if (zeroCopySrc != null) {
            // zero-copy: possible to continue if-and-only-if this continues directly from last packet
            // (this should happen a lot due to how Ogg lacing works)
            if ((data != zeroCopySrc) || (ofs != (zeroCopyOfs + packetLen))) {
                // unable to zero-copy this, switch to collation buffer
                transferFromZeroCopy();
                collation.write(data, ofs, leni);
            }
            // otherwise, do nothing!
            // extending the packet length increases zero-copy source length
        } else {
            // using collation buffer
            collation.write(data, ofs, leni);
        }
        packetLen += leni;
        // end-of-packet logic
        if (leni != 255) {
            if (zeroCopySrc == null) {
                // have to copy
                byte[] total = collation.toByteArray();
                output.packet(total, 0, packetLen);
                collation.reset();
            } else {
                // can supply packet data directly
                output.packet(zeroCopySrc, zeroCopyOfs, packetLen);
                zeroCopySrc = null;
            }
            packetLen = 0;
        }
    }

    @Override
    public void invalidateStorage() {
        if (zeroCopySrc != null)
            transferFromZeroCopy();
    }

    private void transferFromZeroCopy() {
        collation.reset();
        collation.write(zeroCopySrc, zeroCopyOfs, packetLen);
        zeroCopySrc = null;
    }

    @Override
    public final void discard() {
        collation.reset();
        zeroCopySrc = null;
        packetLen = 0;
    }
}
