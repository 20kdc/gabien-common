/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Created on 6th June 2022 as part of project VE2Bun
 */
public class LEDataInputStream extends FilterInputStream implements LEDataInput {
    private @NonNull final DataInputStream baseDataInput;
    public LEDataInputStream(@NonNull InputStream in) {
        super(in);
        baseDataInput = new DataInputStream(this);
    }

    public final byte[] readToEnd() throws IOException {
        int av = available();
        // System.out.println("RTE Av" + av);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(av);
        while (av > 0) {
            byte[] data = new byte[av];
            readFully(data);
            baos.write(data);
            av = available();
        }
        return baos.toByteArray();
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
}
