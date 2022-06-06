/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.uslx.append;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created on 6th June 2022 as part of project VE2Bun
 */
public class LEDataOutputStream extends FilterOutputStream implements DataOutput {
    private final DataOutputStream baseDataOutput;
    public LEDataOutputStream(OutputStream base) {
        super(base);
        baseDataOutput = new DataOutputStream(base);
    }

    @Override
    public final void writeBoolean(boolean v) throws IOException {
        baseDataOutput.writeBoolean(v);
    }

    @Override
    public final void writeByte(int v) throws IOException {
        out.write(v);
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
        int len = s.length();
        for (int i = 0; i < len; i++)
            writeShort(s.charAt(i));
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
        writeShort(v);
        writeShort(v >> 16);
    }

    @Override
    public final void writeLong(long v) throws IOException {
        writeInt((int) v);
        writeInt((int) (v >> 32));
    }

    @Override
    public final void writeShort(int v) throws IOException {
        out.write(v);
        out.write(v >> 8);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        baseDataOutput.writeUTF(s);
    }
}
