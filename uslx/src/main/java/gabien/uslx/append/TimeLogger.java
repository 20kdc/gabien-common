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
import java.util.concurrent.ArrayBlockingQueue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Meant for profiling.
 * Created 9th June, 2023.
 */
public final class TimeLogger implements AutoCloseable {
    private DataOutputStream output;
    private int nextSourceID;
    private ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(256);

    private Thread runner = new Thread("TimeLogger") {
        public void run() {
            while (true) {
                try {
                    queue.take().run();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    };

    public TimeLogger(OutputStream os) {
        output = new DataOutputStream(os);
        runner.start();
    }

    public static @Nullable TimeLogger.Source optSource(@Nullable TimeLogger logger, String name) {
        if (logger == null)
            return null;
        return logger.newSource(name);
    }

    /**
     * Opens a Source. Importantly, this returns null if the source is null.
     */
    public static @Nullable Source open(@Nullable Source src) {
        if (src == null)
            return null;
        return src.open();
    }

    /**
     * Closes a Source. Importantly, this returns null if the source is null.
     */
    public static void close(@Nullable Source src) {
        if (src == null)
            return;
        src.close();
    }

    private synchronized void event(int type, int sourceID, @Nullable String addendum, long now) {
        if (output == null)
            return;
        try {
            output.write(type);
            output.writeInt(sourceID);
            output.writeLong(now);
            if (addendum != null)
                output.writeUTF(addendum);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public synchronized Source newSource(@NonNull String id) {
        int sid = nextSourceID++;
        long now = System.nanoTime();
        queue.add(() -> event(0, sid, id, now));
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

        private Source open() {
            try {
                long now = System.nanoTime();
                queue.put(() -> event(1, id, null, now));
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            return this;
        }

        /**
         * This is marked as deprecated so that you never directly call it.
         * Use TimeLogger.close instead as this will perform a null check.
         * Alternatively, use via try-with-resources (which is why this method exists).
         */
        @Deprecated
        @Override
        public void close() {
            try {
                long now = System.nanoTime();
                queue.put(() -> event(2, id, null, now));
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    public synchronized void flush() {
        if (output != null)
            try {
                output.flush();
            } catch (IOException e) {
                // :(
            }
    }
}
