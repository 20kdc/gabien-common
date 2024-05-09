/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created 9th May 2024
 */
public class ByteArrayMemoryish extends MemoryishRW {
    public final byte[] data;

    public ByteArrayMemoryish(byte[] data) {
        super(data.length);
        this.data = data;
    }

    @Override
    public void set8(long at, int v) {
        data[(int) at] = (byte) v;
    }

    @Override
    public void setBulk(long at, byte[] data, int offset, int length) {
        System.arraycopy(data, offset, this.data, (int) at, length);
    }

    @Override
    public final byte getS8(long at) {
        return data[(int) at];
    }

    @Override
    public void getBulk(long at, byte[] data, int offset, int length) {
        System.arraycopy(this.data, (int) at, data, offset, length);
    }

    @Override
    public void getBulk(long at, OutputStream os, int length) throws IOException {
        os.write(data, (int) at, length);
    }
}
