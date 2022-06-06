/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.uslx.append;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created on 6th June 2022 as part of project VE2Bun
 */
public class LEDataInputStream extends FilterInputStream implements DataInput {
    private final DataInputStream baseDataInput;
    protected LEDataInputStream(InputStream in) {
        super(in);
        baseDataInput = new DataInputStream(in);
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
        int sL = readShort() & 0xFFFF;
        int sH = readShort() & 0xFFFF;
        return sL | (sH << 16);
    }
    
    @Override
    public final long readLong() throws IOException {
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
