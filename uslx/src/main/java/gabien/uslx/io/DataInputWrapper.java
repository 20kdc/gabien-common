/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.DataInput;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Wraps another DataInput to replace the format primitives.
 * Created 20th October 2023
 */
public abstract class DataInputWrapper implements DataInput {
    protected @NonNull final DataInput baseDataInput;
    public DataInputWrapper(@NonNull DataInput in) {
        baseDataInput = in;
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
    public final byte readByte() throws IOException {
        return baseDataInput.readByte();
    }

    @Override
    public final int readUnsignedByte() throws IOException {
        return baseDataInput.readUnsignedByte();
    }

    /**
     * Wraps another DataInput to "convert" it to little-endian.
     * Good for use on RandomAccessFile etc.
     */
    public static class LE extends DataInputWrapper implements LEDataInput {
        public LE(@NonNull DataInput in) {
            super(in);
        }
    }

    /**
     * Wraps another DataInput to "convert" it to big-endian.
     * Shouldn't be necessary but could be in particularly mixed-endian cases.
     */
    public static class BE extends DataInputWrapper implements BEDataInput {
        public BE(@NonNull DataInput in) {
            super(in);
        }
    }
}
