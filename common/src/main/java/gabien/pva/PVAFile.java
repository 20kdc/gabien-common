/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.pva;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.zip.InflaterInputStream;

/**
 * PVA file structure.
 * For further details please see https://20kdc.gitlab.io/scrapheap/pva/
 * This would be in gabien-media, BUT: the rendering side of things requires it be here.
 * Besides, the Vector Icons project requires it be here.
 * Created 2nd October 2023.
 */
public class PVAFile {
    public final Header header = new Header();
    public SequenceElm[] sequence;
    public FrameElm[][] frames;
    public Triangle[] triangles;
    public Matrix[] matrices;
    public Loop[] loops;
    public PaletteElm[] palette;
    public Vertex[] vertices;
    public Texture[] textures;
    public ImageHeader[] imageHeaders;
    public byte[][] imageDatas;

    public void read(InputStream inp) throws IOException {
        readDecompressed(new InflaterInputStream(inp));
    }

    public void readDecompressed(InputStream inp) throws IOException {
        header.read(CABS.readChunk(inp));
        sequence = new SequenceElm().readArray(CABS.readChunk(inp)).toArray(new SequenceElm[0]);
        frames = new FrameElm[header.frameCount][];
        for (int i = 0; i < header.frameCount; i++)
            frames[i] = new FrameElm().readArray(CABS.readChunk(inp)).toArray(new FrameElm[0]);
        triangles = new Triangle().readArray(CABS.readChunk(inp)).toArray(new Triangle[0]);
        matrices = new Matrix().readArray(CABS.readChunk(inp)).toArray(new Matrix[0]);
        loops = new Loop().readArray(CABS.readChunk(inp)).toArray(new Loop[0]);
        palette = new PaletteElm().readArray(CABS.readChunk(inp)).toArray(new PaletteElm[0]);
        vertices = new Vertex().readArray(CABS.readChunk(inp)).toArray(new Vertex[0]);
        textures = new Texture().readArray(CABS.readChunk(inp)).toArray(new Texture[0]);
        imageHeaders = new ImageHeader().readArray(CABS.readChunk(inp)).toArray(new ImageHeader[0]);
        imageDatas = new byte[imageHeaders.length][];
        int imgCount = imageHeaders.length;
        for (int i = 0; i < imgCount; i++)
            imageDatas[i] = CABS.readChunk(inp);
    }

    /**
     * Gets the duration of the file in milliseconds.
     */
    public double getDuration() {
        double total = 0;
        for (SequenceElm se : sequence)
            total += se.delay;
        return total;
    }

    /**
     * Given a time in milliseconds, returns the corresponding frame index.
     * Returns -1 on failure.
     */
    public int frameOf(double time) {
        double frameStartTime = 0;
        for (SequenceElm se : sequence) {
            double frameEndTime = frameStartTime + se.delay;
            if (time < frameEndTime)
                return se.frameIndex;
            frameStartTime = frameEndTime;
        }
        return -1;
    }

    public static abstract class Struct<T extends Struct<T>> {
        public final void read(byte[] src) {
            ByteBuffer bb = ByteBuffer.wrap(src);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            read(bb, 0);
        }
        public abstract int read(ByteBuffer inp, int at);
        public abstract T make();
        public final ArrayList<T> readArray(byte[] src) {
            ArrayList<T> list = new ArrayList<T>();
            if (src.length == 0)
                return list;
            ByteBuffer bb = ByteBuffer.wrap(src);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int at = 0;
            while (at < src.length) {
                T tgt = make();
                at += tgt.read(bb, at);
                list.add(tgt);
            }
            return list;
        }
    }

    public static final class Header extends Struct<Header> {
        public int width, height, frameCount;

        @Override
        public int read(ByteBuffer inp, int at) {
            width = inp.getInt(at);
            height = inp.getInt(at + 4);
            frameCount = inp.getInt(at + 8);
            return 12;
        }

        @Override
        public Header make() {
            return new Header();
        }
    }

    public static final class SequenceElm extends Struct<SequenceElm> {
        public float delay;
        public int frameIndex;

        @Override
        public int read(ByteBuffer inp, int at) {
            delay = inp.getFloat(at);
            frameIndex = inp.getInt(at + 4);
            return 8;
        }

        @Override
        public SequenceElm make() {
            return new SequenceElm();
        }
    }

    public static final class FrameElm extends Struct<FrameElm> {
        public int mtxIndex, triIndex;

        @Override
        public int read(ByteBuffer inp, int at) {
            mtxIndex = inp.getInt(at);
            triIndex = inp.getInt(at + 4);
            return 8;
        }

        @Override
        public FrameElm make() {
            return new FrameElm();
        }
    }

    public static final class Triangle extends Struct<Triangle> {
        public int texIndex, aIndex, bIndex, cIndex;

        @Override
        public int read(ByteBuffer inp, int at) {
            texIndex = inp.getInt(at);
            aIndex = inp.getInt(at + 4);
            bIndex = inp.getInt(at + 8);
            cIndex = inp.getInt(at + 12);
            return 16;
        }

        @Override
        public Triangle make() {
            return new Triangle();
        }
    }

    public static final class Matrix extends Struct<Matrix> {
        public float[] content = new float[16];

        @Override
        public int read(ByteBuffer inp, int at) {
            for (int i = 0; i < 16; i++) {
                content[i] = inp.getFloat(at);
                at += 4;
            }
            return 64;
        }

        @Override
        public Matrix make() {
            return new Matrix();
        }

        /**
         * Calculate vertex X.
         */
        public float transformVertexX(Vertex vtx) {
            return (vtx.x * content[0]) + (vtx.y * content[1]) + (vtx.z * content[2]) + content[3];
        }

        /**
         * Calculate vertex Y.
         */
        public float transformVertexY(Vertex vtx) {
            return (vtx.x * content[4]) + (vtx.y * content[5]) + (vtx.z * content[6]) + content[7];
        }

        /**
         * Calculate vertex Z.
         */
        public float transformVertexZ(Vertex vtx) {
            return (vtx.x * content[8]) + (vtx.y * content[9]) + (vtx.z * content[10]) + content[11];
        }

        /**
         * Calculate vertex W.
         */
        public float transformVertexW(Vertex vtx) {
            return (vtx.x * content[12]) + (vtx.y * content[13]) + (vtx.z * content[14]) + content[15];
        }
    }

    public static final class Loop extends Struct<Loop> {
        public int vtxIndex, palIndex;
        public float u, v; 

        @Override
        public int read(ByteBuffer inp, int at) {
            vtxIndex = inp.getInt(at);
            palIndex = inp.getInt(at + 4);
            u = inp.getFloat(at + 8);
            v = inp.getFloat(at + 12);
            return 16;
        }

        @Override
        public Loop make() {
            return new Loop();
        }
    }

    public static final class PaletteElm extends Struct<PaletteElm> {
        public float r, g, b, a; 

        @Override
        public int read(ByteBuffer inp, int at) {
            r = (inp.getShort(at) & 0xFFFF) / 255.0f;
            g = (inp.getShort(at + 2) & 0xFFFF) / 255.0f;
            b = (inp.getShort(at + 4) & 0xFFFF) / 255.0f;
            a = (inp.get(at + 6) & 0xFF) / 255.0f;
            return 7;
        }

        @Override
        public PaletteElm make() {
            return new PaletteElm();
        }
    }

    public static final class Vertex extends Struct<Vertex> {
        public float x, y, z; 

        @Override
        public int read(ByteBuffer inp, int at) {
            x = inp.getFloat(at);
            y = inp.getFloat(at + 4);
            z = inp.getFloat(at + 8);
            return 12;
        }

        @Override
        public Vertex make() {
            return new Vertex();
        }
    }

    public static final class Texture extends Struct<Texture> {
        public static final int MODE_FILTER = 1;
        public static final int MODE_REPEAT = 2;

        public int mode, imgIndex;

        @Override
        public int read(ByteBuffer inp, int at) {
            mode = inp.getInt(at);
            imgIndex = inp.getInt(at + 4);
            return 8;
        }

        @Override
        public Texture make() {
            return new Texture();
        }
    }

    public static final class ImageHeader extends Struct<ImageHeader> {
        public int w, h;

        @Override
        public int read(ByteBuffer inp, int at) {
            w = inp.getInt(at);
            h = inp.getInt(at + 4);
            return 8;
        }

        @Override
        public ImageHeader make() {
            return new ImageHeader();
        }
    }
}
