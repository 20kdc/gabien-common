/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.DataInput;
import java.io.IOException;

/**
 * DataInput with some additional common datatypes and functions implemented as default methods.
 * In addition, endian-independent operations are implemented here.
 * (we don't live in the pre-Java 8 era anymore)
 * Created 20th October, 2023.
 */
public interface DataInputEx extends DataInput {
    default String readFourCC() throws IOException {
        char a = (char) readUnsignedByte();
        char b = (char) readUnsignedByte();
        char c = (char) readUnsignedByte();
        char d = (char) readUnsignedByte();
        return new String(new char[] {a, b, c, d});
    }

    @Override
    default boolean readBoolean() throws IOException {
        return readUnsignedByte() != 0;
    }

    @Override
    default float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    default double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    default short readShort() throws IOException {
        return (short) readUnsignedShort();
    }

    @Override
    default char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    @Override
    default String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            int b = readUnsignedByte();
            // technically this is misuse of a wording error in the Java specification
            // this error, however, only affects classic Mac OS line endings
            // and supporting them is the only part of this interface that requires supporting unread
            if (b == 13)
                continue;
            if (b == 10)
                break;
            sb.append((char) b);
        }
        return sb.toString();
    }

    @Override
    default String readUTF() throws IOException {
        StringBuilder sb = new StringBuilder();
        int r = readUnsignedShort();
        while (r > 0) {
            int b = readUnsignedByte();
            if (b < 128) {
                sb.append((char) b);
                r--;
            } else if (b >= 0xE0) {
                int c = readUnsignedByte();
                int d = readUnsignedByte();
                int res = ((b & 0x0F) << 12) | ((c & 0x3F) << 6) | (d & 0x3F);
                sb.append((char) res);
                r -= 3;
            } else if (b >= 0xC0) {
                int d = readUnsignedByte();
                int res = ((b & 0x1F) << 6) | (d & 0x3F);
                sb.append((char) res);
                r -= 2;
            } else {
                throw new IOException("invalid MUTF-8");
            }
        }
        return sb.toString();
    }
}
