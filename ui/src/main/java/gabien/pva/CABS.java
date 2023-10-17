/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.pva;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Compression-Aware Bulk Storage reader.
 * Created 2nd October 2023.
 */
public class CABS {
    /**
     * Reads a CABS chunk.
     */
    public static byte[] readChunk(InputStream is) throws IOException {
        int a = is.read();
        if (a == -1)
            throw new EOFException("EOF at start of CABS");
        int b = is.read();
        if (b == -1)
            throw new IOException("EOF during CABS");
        int c = is.read();
        if (c == -1)
            throw new IOException("EOF during CABS");
        int d = is.read();
        if (d == -1)
            throw new IOException("EOF during CABS");
        int e = is.read();
        if (e == -1)
            throw new IOException("EOF during CABS");
        int len = (a << 0) | (b << 8) | (c << 16) | (d << 24);
        switch (e) {
        case 0:
            // nothing!
            {
                byte[] buf1 = new byte[len - 5];
                new DataInputStream(is).readFully(buf1);
                return buf1;
            }
        case 1:
            // transposed!
            {
                int columns = is.read();
                byte[] buf1 = new byte[len - 6];
                new DataInputStream(is).readFully(buf1);
                byte[] buf2 = new byte[len - 6];
                transpose(buf1, buf2, columns);
                return buf2;
            }
        default:
            throw new IOException("CABS: Invalid chunk type: " + e);
        }
    }

    private static void transpose(byte[] buf1, byte[] buf2, int columns) {
        int rows = buf2.length / columns;
        for (int i = 0; i < buf2.length; i++) {
            int column = i % columns;
            int row = i / columns;
            int srcIdx = (column * rows) + row;
            buf2[i] = buf1[srcIdx];
        }
    }
}
