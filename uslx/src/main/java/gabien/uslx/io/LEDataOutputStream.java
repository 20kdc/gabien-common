/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Created on 6th June 2022 as part of project VE2Bun
 */
public class LEDataOutputStream extends FilterOutputStream implements LEDataOutput {
    /**
     * Maintains a tally of written bytes.
     */
    protected int written = 0;

    public LEDataOutputStream(@NonNull OutputStream base) {
        super(base);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        written++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        written += len;
    }
}
