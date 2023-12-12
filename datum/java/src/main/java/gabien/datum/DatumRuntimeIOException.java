/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.io.IOException;

/**
 * Wraps an IOException as a RuntimeException in a catchable way.
 * Created 16th February 2023.
 */
@SuppressWarnings("serial")
public class DatumRuntimeIOException extends RuntimeException {
    public final IOException exception;
    public DatumRuntimeIOException(IOException ioe) {
        super(ioe);
        exception = ioe;
    }
}
