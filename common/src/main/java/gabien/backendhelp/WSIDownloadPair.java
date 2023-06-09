/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backendhelp;

import java.util.concurrent.ArrayBlockingQueue;

import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.uslx.append.TimeLogger;

/**
 * Used to manage a specific part of the WSI download/transfer process.
 * Note that multiple of these will show up for multi-stage transfer processes.
 * Created 9th June, 2023.
 */
public abstract class WSIDownloadPair<T> {
    private final @Nullable TimeLogger.Source timeLoggerAcquire;
    private final ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(2);

    public WSIDownloadPair(String n) {
        timeLoggerAcquire = TimeLogger.optSource(GaBIEn.timeLogger, n + ".ACQ");
        try {
            queue.put(genBuffer(0, 0));
            queue.put(genBuffer(0, 0));
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public final T acquire(int width, int height) {
        try {
            T res;
            if (timeLoggerAcquire != null) {
                try (TimeLogger.Source src = timeLoggerAcquire.open()) {
                    res = queue.take();
                }
            } else {
                res = queue.take();
            }
            // Replace the buffer if it doesn't match the width/height.
            if (!bufferMatchesSize(res, width, height))
                res = genBuffer(width, height);
            return res;
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public final void release(T buffer) {
        try {
            queue.put(buffer);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public abstract boolean bufferMatchesSize(T buffer, int width, int height);
    public abstract T genBuffer(int width, int height);
}
