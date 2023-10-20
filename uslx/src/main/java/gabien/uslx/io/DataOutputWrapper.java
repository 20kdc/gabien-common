/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.DataOutput;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Created 20th October, 2023
 */
public abstract class DataOutputWrapper implements DataOutput {
    protected @NonNull final DataOutput baseDataOutput;

    /**
     * Maintains a tally of written bytes.
     */
    protected int written = 0;

    public DataOutputWrapper(@NonNull DataOutput base) {
        baseDataOutput = base;
    }

    @Override
    public void write(int b) throws IOException {
        baseDataOutput.write(b);
        written++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        baseDataOutput.write(b, off, len);
        written += len;
    }

    /**
     * Wraps a DataOutput for little-endian output.
     */
    public static class LE extends DataOutputWrapper implements LEDataOutput {
        public LE(@NonNull DataOutput in) {
            super(in);
        }
    }

    /**
     * Wraps a DataOutput for big-endian output.
     */
    public static class BE extends DataOutputWrapper implements BEDataOutput {
        public BE(@NonNull DataOutput in) {
            super(in);
        }
    }
}
