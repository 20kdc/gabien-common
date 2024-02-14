/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created February 14th, 2024.
 */
public final class MIDISequence {
    public final short division;
    public final byte[][] tracks;

    public MIDISequence(short div, byte[][] tracks) {
        division = div;
        this.tracks = tracks;
    }

    public static MIDISequence[] from(InputStream inp) throws IOException {
        DataInputStream dis = new DataInputStream(inp);
        if (dis.readInt() != 0x4d546864)
            throw new IOException("not MThd");
        if (dis.readInt() != 6)
            throw new IOException("MThd should have 6 bytes");
        int fmt = dis.readUnsignedShort();
        int trk = dis.readUnsignedShort();
        short div = dis.readShort();
        byte[][] trks = new byte[trk][];
        for (int i = 0; i < trk; i++) {
            if (dis.readInt() != 0x4d54726b)
                throw new IOException("not MTrk");
            int len = dis.readInt();
            byte[] trkData = new byte[len];
            dis.readFully(trkData);
            trks[i] = trkData;
        }
        if (fmt == 2) {
            // "patterns" form
            MIDISequence[] res = new MIDISequence[trks.length];
            for (int i = 0; i < res.length; i++)
                res[i] = new MIDISequence(div, new byte[][] {trks[i]});
            return res;
        } else {
            // "single sequence" form
            return new MIDISequence[] {new MIDISequence(div, trks)};
        }
    }
}
