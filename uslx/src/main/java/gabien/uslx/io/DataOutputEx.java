/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * DataOutput with endian-independent operations implemented as default methods.
 * Created 20th October, 2023.
 */
public interface DataOutputEx extends DataOutput {
    @Override
    default void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    default void writeBoolean(boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    @Override
    default void writeByte(int v) throws IOException {
        write(v);
    }

    @Override
    default void writeBytes(String s) throws IOException {
        int l = s.length();
        for (int i = 0; i < l; i++)
            write(s.charAt(i) & 0xFF);
    }

    @Override
    default void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    default void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    default void writeChar(int v) throws IOException {
        writeShort(v);
    }

    @Override
    default void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++)
            writeShort(s.charAt(i));
    }

    @Override
    default void writeUTF(String s) throws IOException {
        // OPT: This should almost never be called
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF(s);
        byte[] ba = baos.toByteArray();
        // most of this is flat bytes, but there is one short here; correct it
        writeShort(ba.length - 2);
        write(ba, 2, ba.length - 2);
    }
}
