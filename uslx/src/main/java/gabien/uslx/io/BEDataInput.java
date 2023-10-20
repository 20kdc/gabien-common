/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.IOException;

/**
 * Trying to decouple this stuff from DataInputStream.
 * Created 20th October, 2023.
 */
public interface BEDataInput extends DataInputEx {
    @Override
    default int readUnsignedShort() throws IOException {
        int bH = readUnsignedByte();
        int bL = readUnsignedByte();
        return (short) ((bL & 0xFF) | ((bH & 0xFF) << 8));
    }

    @Override
    default int readInt() throws IOException {
        int sH = readShort() & 0xFFFF;
        int sL = readShort() & 0xFFFF;
        return sL | (sH << 16);
    }

    @Override
    default long readLong() throws IOException {
        long iH = readInt() & 0xFFFFFFFFL;
        long iL = readInt() & 0xFFFFFFFFL;
        return iL | (iH << 32);
    }
}
