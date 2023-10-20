/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.IOException;

/**
 * Trying to decouple this stuff from DataOutputStream.
 * Created 20th October, 2023.
 */
public interface LEDataOutput extends DataOutputEx {
    @Override
    default void writeInt(int v) throws IOException {
        writeShort(v);
        writeShort(v >> 16);
    }

    @Override
    default void writeLong(long v) throws IOException {
        writeInt((int) v);
        writeInt((int) (v >> 32));
    }

    @Override
    default void writeShort(int v) throws IOException {
        write(v);
        write(v >> 8);
    }
}
