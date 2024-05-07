/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

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
    private final @Nullable TimeLogger.Source[] timeLoggerHeld;
    private final @Nullable Object[] canon;
    private final ArrayBlockingQueue<T> queue;

    public WSIDownloadPair(String n, int capacity) {
        try {
            queue = new ArrayBlockingQueue<>(capacity);
            timeLoggerAcquire = TimeLogger.optSource(GaBIEn.timeLogger, n + ".ACQ");
            if (GaBIEn.timeLogger != null) {
                timeLoggerHeld = new TimeLogger.Source[capacity];
                canon = new Object[capacity];
                for (int i = 0; i < capacity; i++) {
                    timeLoggerHeld[i] = TimeLogger.optSource(GaBIEn.timeLogger, n + "." + i + ".HLD");
                    T gen = genBuffer(0, 0);
                    canon[i] = gen;
                    queue.put(gen);
                }
            } else {
                timeLoggerHeld = null;
                canon = null;
                // the queue must be filled with something and it can't be null
                for (int i = 0; i < capacity; i++)
                    queue.put(genBuffer(1, 1));
            }
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    private int indexOf(T res) {
        int foundIndex = -1;
        for (int i = 0; i < canon.length; i++) {
            if (canon[i] == res) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex == -1)
            throw new RuntimeException("Contaminant in queue not supposed to be there");
        return foundIndex;
    }

    public final T acquire(int width, int height) {
        try {
            T res;
            if (timeLoggerAcquire != null) {
                try (TimeLogger.Source src = TimeLogger.open(timeLoggerAcquire)) {
                    res = queue.take();
                }
                int foundIndex = indexOf(res);
                TimeLogger.open(timeLoggerHeld[foundIndex]);
                // Replace the buffer if it doesn't match the width/height.
                if (!bufferMatchesSize(res, width, height)) {
                    res = genBuffer(width, height);
                    canon[foundIndex] = res;
                }
            } else {
                res = queue.take();
                // Replace the buffer if it doesn't match the width/height.
                if (!bufferMatchesSize(res, width, height))
                    res = genBuffer(width, height);
            }
            return res;
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public final void release(T buffer) {
        try {
            if (timeLoggerHeld != null) {
                int foundIndex = indexOf(buffer);
                TimeLogger.close(timeLoggerHeld[foundIndex]);
            }
            queue.put(buffer);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    public abstract boolean bufferMatchesSize(T buffer, int width, int height);
    public abstract T genBuffer(int width, int height);
}
