/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Created on 6th June 2022 as part of project VE2Bun
 */
public class XEDataOutputStream extends FilterOutputStream implements DataOutput {
    /**
     * Current byte order. Defaults to little-endian, because if you wanted BE you'd just use DataOutputStream.
     */
    public ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private @NonNull final DataOutputStream baseDataOutput;
    /**
     * Maintains a tally of written bytes.
     */
    protected int written = 0;

    public XEDataOutputStream(@NonNull OutputStream base) {
        super(base);
        baseDataOutput = new DataOutputStream(this);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        written++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        written += len;
    }

    @Override
    public final void writeBoolean(boolean v) throws IOException {
        baseDataOutput.writeBoolean(v);
    }

    @Override
    public final void writeByte(int v) throws IOException {
        out.write(v);
        written++;
    }

    @Override
    public final void writeBytes(String s) throws IOException {
        baseDataOutput.writeBytes(s);
    }

    @Override
    public final void writeChar(int v) throws IOException {
        writeShort(v);
    }

    @Override
    public final void writeChars(String s) throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            baseDataOutput.writeChars(s);
        } else {
            int len = s.length();
            for (int i = 0; i < len; i++)
                writeShort(s.charAt(i));
        }
    }

    @Override
    public final void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public final void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public final void writeInt(int v) throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            baseDataOutput.writeInt(v);
        } else {
            writeShort(v);
            writeShort(v >> 16);
        }
    }

    @Override
    public final void writeLong(long v) throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            baseDataOutput.writeLong(v);
        } else {
            writeInt((int) v);
            writeInt((int) (v >> 32));
        }
    }

    @Override
    public final void writeShort(int v) throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            baseDataOutput.writeShort(v);
        } else {
            out.write(v);
            out.write(v >> 8);
            written += 2;
        }
    }

    @Override
    public void writeUTF(String s) throws IOException {
        baseDataOutput.writeUTF(s);
    }
}
