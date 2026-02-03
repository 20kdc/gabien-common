/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utilities for InputStream & friends mirroring without creating a new stream.
 * This is good for code where you don't want to get complaints for failing to close a DataInputStream.
 * (Or just don't want the GC pressure.)
 * Created 3rd February, 2026
 */
public class InputStreamUtils {
    /**
     * @see java.io.DataInput#readFully(byte[])
     */
    public static void readFully(InputStream inp, byte[] b) throws IOException {
        readFully(inp, b, 0, b.length);
    }

    /**
     * @see java.io.DataInput#readFully(byte[], int, int)
     */
    public static void readFully(InputStream inp, byte[] b, int off, int len) throws IOException {
        while (len > 0) {
            int res = inp.read(b, off, len);
            if (res <= 0)
                throw new EOFException();
            off += res;
            len -= res;
        }
    }
}
