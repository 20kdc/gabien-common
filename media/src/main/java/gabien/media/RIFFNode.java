/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Created 19th June 2023 as part of gabien-tools's RIFF editor.
 */
public abstract class RIFFNode {
    public String chunkId = "test";

    public RIFFNode() {
        
    }

    public abstract RIFFNode copy();

    public static RIFFNode read(InputStream xe) throws IOException {
        try (RIFFInputStream ris = new RIFFInputStream(xe)) {
            if (ris.chunkId.equals("RIFF") || ris.chunkId.equals("LIST")) {
                return new CList(ris);
            } else {
                return new CData(ris);
            }
        }
    }
    public abstract void write(OutputStream xe) throws IOException;

    public static final class CData extends RIFFNode {
        public byte[] contents;

        public CData() {
            contents = new byte[0];
        }

        public CData(RIFFInputStream ris) throws IOException {
            chunkId = ris.chunkId;
            contents = new byte[ris.chunkLen];
            ris.readFully(contents);
        }

        @Override
        public RIFFNode copy() {
            CData n = new CData();
            n.chunkId = chunkId;
            n.contents = contents.clone();
            return n;
        }

        @Override
        public void write(OutputStream xe) throws IOException {
            RIFFOutputStream.putChunk(xe, chunkId, contents);
        }
    }

    public static final class CList extends RIFFNode {
        public String subChunkId = "test";
        public final LinkedList<RIFFNode> contents = new LinkedList<>();

        public CList(String cid, String scid) {
            chunkId = cid;
            subChunkId = scid;
        }

        public CList(RIFFInputStream ris) throws IOException {
            chunkId = ris.chunkId;
            subChunkId = ris.readFourCC();
            while (ris.available() > 0)
                contents.add(read(ris));
        }

        @Override
        public RIFFNode copy() {
            CList n = new CList(chunkId, subChunkId);
            for (RIFFNode rn : contents)
                n.contents.add(rn.copy());
            return n;
        }

        @Override
        public void write(OutputStream xe) throws IOException {
            try (RIFFOutputStream ros = new RIFFOutputStream(xe, chunkId)) {
                if (subChunkId.length() != 4)
                    throw new IOException("RIFF subchunk ID must be 4 characters.");
                ros.writeBytes(subChunkId);
                for (RIFFNode rn : contents)
                    rn.write(ros);
            }
        }
    }
}
