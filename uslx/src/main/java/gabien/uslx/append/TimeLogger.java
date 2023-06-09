/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Meant for profiling.
 * Created 9th June, 2023.
 */
public final class TimeLogger implements AutoCloseable {
    private DataOutputStream output;
    private int nextSourceID;

    public TimeLogger(OutputStream os) {
        output = new DataOutputStream(os);
    }

    public static @Nullable TimeLogger.Source optSource(@Nullable TimeLogger logger, String name) {
        if (logger == null)
            return null;
        return logger.newSource(name);
    }

    public static @Nullable Source optOpen(@Nullable Source src) {
        if (src == null)
            return null;
        return src.open();
    }

    private synchronized void event(int type, int sourceID, @Nullable String addendum) {
        if (output == null)
            return;
        try {
            output.write(type);
            output.writeInt(sourceID);
            output.writeLong(System.nanoTime());
            if (addendum != null)
                output.writeUTF(addendum);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized Source newSource(@NonNull String id) {
        int sid = nextSourceID++;
        event(0, sid, id);
        return new Source(sid);
    }

    @Override
    public synchronized void close() throws IOException {
        if (output != null) {
            output.close();
            output = null;
        }
    }

    @Override
    protected synchronized void finalize() throws IOException {
        close();
    }

    public final class Source implements AutoCloseable {
        public final int id;
        public Source(int id) {
            this.id = id;
        }

        public Source open() {
            event(1, id, null);
            return this;
        }

        @Override
        public void close() {
            event(2, id, null);
        }
    }
}
