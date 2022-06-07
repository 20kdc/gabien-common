/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.uslx.append;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Created on 6th June 2022 as part of project VE2Bun
 */
public class XEDataInputStream extends FilterInputStream implements DataInput {
    /**
     * Current byte order. Defaults to little-endian, because if you wanted BE you'd just use DataOutputStream.
     */
    public ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;
    private @NonNull final DataInputStream baseDataInput;
    protected XEDataInputStream(@NonNull InputStream in) {
        super(in);
        baseDataInput = new DataInputStream(this);
    }

    public final String readFourCC() throws IOException {
        char a = (char) readUnsignedByte();
        char b = (char) readUnsignedByte();
        char c = (char) readUnsignedByte();
        char d = (char) readUnsignedByte();
        return new String(new char[] {a, b, c, d});
    }

    public final byte[] readToEnd() throws IOException {
        int av = available();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(av);
        while (av > 0) {
            byte[] data = new byte[av];
            readFully(data);
            baos.write(data);
            av = available();
        }
        return baos.toByteArray();
    }

    @Override
    public final void readFully(byte[] b) throws IOException {
        baseDataInput.readFully(b);
    }

    @Override
    public final void readFully(byte[] b, int off, int len) throws IOException {
        baseDataInput.readFully(b, off, len);
    }
    
    @Override
    public final int skipBytes(int n) throws IOException {
        return baseDataInput.skipBytes(n);
    }
    
    @Override
    public final boolean readBoolean() throws IOException {
        return baseDataInput.readBoolean();
    }
    
    @Override
    public final byte readByte() throws IOException {
        return baseDataInput.readByte();
    }
    
    @Override
    public final int readUnsignedByte() throws IOException {
        return baseDataInput.readUnsignedByte();
    }
    
    @Override
    public final short readShort() throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN)
            return baseDataInput.readShort();
        byte bL = baseDataInput.readByte();
        byte bH = baseDataInput.readByte();
        return (short) ((bL & 0xFF) | ((bH & 0xFF) << 8));
    }
    
    @Override
    public final int readUnsignedShort() throws IOException {
        return readShort() & 0xFFFF;
    }
    
    @Override
    public final char readChar() throws IOException {
        return (char) readUnsignedShort();
    }
    
    @Override
    public final int readInt() throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN)
            return baseDataInput.readInt();
        int sL = readShort() & 0xFFFF;
        int sH = readShort() & 0xFFFF;
        return sL | (sH << 16);
    }
    
    @Override
    public final long readLong() throws IOException {
        if (byteOrder == ByteOrder.BIG_ENDIAN)
            return baseDataInput.readLong();
        long iL = readInt() & 0xFFFFFFFFL;
        long iH = readInt() & 0xFFFFFFFFL;
        return iL | (iH << 32);
    }
    
    @Override
    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }
    
    @Override
    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public final String readLine() throws IOException {
        return baseDataInput.readLine();
    }
    
    @Override
    public final String readUTF() throws IOException {
        return baseDataInput.readUTF();
    }
}
