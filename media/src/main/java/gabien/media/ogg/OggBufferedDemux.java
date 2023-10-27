/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.ogg;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Separates a stream of assorted Ogg pages into separate Ogg logical bitstreams.
 * Created 27th October, 2023.
 */
public class OggBufferedDemux implements OggPageReceiver {
    public final LinkedList<Stream> streams = new LinkedList<>();
    public final HashMap<Integer, Stream> activeStreams = new HashMap<>();

    @Override
    public void page(byte[] data, int ofs, int len) {
        Stream theStream = null;
        int streamID = OggPage.getStreamID(data, ofs);
        int flags = data[ofs + OggPage.FIELD_FLAGS];
        boolean isBOS = (flags & OggPage.FLAG_BOS) != 0;
        boolean isEOS = (flags & OggPage.FLAG_EOS) != 0;
        if (isBOS) {
            theStream = new Stream(streamID);
            streams.add(theStream);
            activeStreams.put(streamID, theStream);
        }
        if (theStream != null) {
            theStream.pageStreamIDChecked(data, ofs);
            if (isEOS)
                activeStreams.remove(streamID);
        }
    }

    /**
     * Stream. Implements OggPageReceiver which allows it to be "decoupled" from the full demux.
     * A decoupled stream will still properly discriminate pages.
     */
    public static class Stream implements OggPageReceiver {
        public final LinkedList<byte[]> packets = new LinkedList<>();
        public final int streamID;
        public long lastGranulePos;
        public boolean hasBeenShutdown = false;

        private final OggPacketsFromSegments opfs = new OggPacketsFromSegments((data, ofs, len) -> {
            byte[] res = new byte[len];
            System.arraycopy(data, ofs, res, 0, len);
            packets.add(res);
        });

        public Stream(int id) {
            streamID = id;
        }

        @Override
        public void page(byte[] data, int ofs, int len) {
            if (OggPage.getStreamID(data, ofs) != streamID)
                return;
            pageStreamIDChecked(data, ofs);
        }

        /**
         * Skip past the stream ID check if called from the demux
         */
        private void pageStreamIDChecked(byte[] data, int ofs) {
            if (hasBeenShutdown)
                return;
            lastGranulePos = OggPage.getGranulePos(data, ofs);
            OggPage.sendSegmentsTo(data, ofs, opfs, false);
            if ((data[ofs + OggPage.FIELD_FLAGS] & OggPage.FLAG_EOS) != 0)
                hasBeenShutdown = true;
        }
    }
}
