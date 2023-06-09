/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import java.util.concurrent.ArrayBlockingQueue;

import org.eclipse.jdt.annotation.Nullable;

import gabien.IImage;
import gabien.natives.BadGPU;
import gabien.uslx.append.EmptyLambdas;
import gabien.uslx.append.TimeLogger;

/**
 * New renderers always have to be named in silly ways, right?
 * To be clear, this API isn't necessarily totally stable.
 * Direct interaction with it should be restrained to situations where you can reasonably say you need the performance boost.
 *
 * Created 7th June, 2023.
 */
public final class Vopeks {
    private final int TASK_QUEUE_SIZE = 65536;
    private int tasksBetweenFlushes = 0;

    public final @Nullable TimeLogger timeLogger;
    public final @Nullable TimeLogger.Source timeLoggerReadPixelsTask;
    public final @Nullable TimeLogger.Source timeLoggerFlushTask;
    public final @Nullable TimeLogger.Source timeLoggerFinishTask;
    public final Thread vopeksThread;
    public final CallbackRunner vopeksCBThread;
    public final CallbackRunner vopeksOTRThread;
    private final ArrayBlockingQueue<ITask> taskQueue = new ArrayBlockingQueue<>(TASK_QUEUE_SIZE);
    private final ArrayBlockingQueue<Runnable> cbQueue = new ArrayBlockingQueue<>(TASK_QUEUE_SIZE);
    private final ArrayBlockingQueue<Runnable> otrQueue = new ArrayBlockingQueue<>(TASK_QUEUE_SIZE);
    public final VopeksFloatPool floatPool = new VopeksFloatPool();
    private volatile boolean shutdownPrimary;
    private volatile boolean otrOK;

    public Vopeks(final int newInstanceFlags, @Nullable TimeLogger timeLogger) {
        this.timeLogger = timeLogger;
        timeLoggerReadPixelsTask = TimeLogger.optSource(timeLogger, "readPixelsTask");
        timeLoggerFlushTask = TimeLogger.optSource(timeLogger, "flushTask");
        timeLoggerFinishTask = TimeLogger.optSource(timeLogger, "finishTask");
        vopeksThread = new Thread("VOPEKS Thread") {
            @Override
            public void run() {
                BadGPU.Instance instance = BadGPU.newInstance(newInstanceFlags);
                otrOK = instance.supportsOffThread();
                if (!otrOK)
                    vopeksOTRThread.shutdown = true;
                if (timeLogger != null) {
                    TimeLogger.Source vs = timeLogger.newSource("vopeks_main");
                    while (!shutdownPrimary) {
                        try {
                            ITask task = taskQueue.take();
                            // intentionally limited to this path
                            tasksBetweenFlushes++;
                            try (TimeLogger.Source vs2 = vs.open()) {
                                task.run(instance);
                            }
                        } catch (Throwable t) {
                            System.err.println("Exception in VOPEKS:");
                            t.printStackTrace();
                        }
                    }
                } else {
                    while (!shutdownPrimary) {
                        try {
                            taskQueue.take().run(instance);
                        } catch (Throwable t) {
                            System.err.println("Exception in VOPEKS:");
                            t.printStackTrace();
                        }
                    }
                }
                instance.dispose();
            }
        };
        vopeksCBThread = new CallbackRunner("VOPEKS Callback Thread", cbQueue);
        vopeksOTRThread = new CallbackRunner("VOPEKS OTR Thread", otrQueue);
        vopeksThread.start();
        vopeksCBThread.start();
        vopeksOTRThread.start();
    }

    public void putTask(ITask object) {
        if (shutdownPrimary)
            return;
        try {
            taskQueue.put(object);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    /**
     * Performs an asynchronous int readPixels.
     */
    public void asyncReadPixels(IImage image, int x, int y, int w, int h, BadGPU.TextureLoadFormat format, int[] data, int dataOfs, Runnable onDone) {
        image.batchFlush();
        if (vopeksOTRThread.shutdown || !otrOK) {
            // OTR not initialized yet or impossible, use traditional approach
            putTask((instance) -> {
                try (TimeLogger.Source src = TimeLogger.optOpen(timeLoggerReadPixelsTask)) {
                    BadGPU.Texture texture = image.getTextureFromTask();
                    if (texture != null)
                        texture.readPixels(x, y, w, h, format, data, dataOfs);
                }
                putCallback(onDone);
            });
        } else {
            // OTR OK. Do shenanigans.
            putTask((instance) -> {
                // Ensure any outstanding GL operations are finished.
                try (TimeLogger.Source src = TimeLogger.optOpen(timeLoggerFinishTask)) {
                    instance.finish();
                }
                image.otrLock();
                BadGPU.Texture texture = image.getTextureFromTask();
                try {
                    otrQueue.put(() -> {
                        try (TimeLogger.Source src = TimeLogger.optOpen(timeLoggerReadPixelsTask)) {
                            if (texture != null)
                                texture.readPixelsOffThread(x, y, w, h, format, data, dataOfs);
                        }
                        image.otrUnlock();
                        putCallback(onDone);
                    });
                } catch (InterruptedException e) {
                    image.otrUnlock();
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Performs an asynchronous byte readPixels.
     */
    public void asyncReadPixels(IImage image, int x, int y, int w, int h, BadGPU.TextureLoadFormat format, byte[] data, int dataOfs, Runnable onDone) {
        image.batchFlush();
        if (vopeksOTRThread.shutdown || !otrOK) {
            // OTR not initialized yet or impossible, use traditional approach
            putTask((instance) -> {
                try (TimeLogger.Source src = TimeLogger.optOpen(timeLoggerReadPixelsTask)) {
                    BadGPU.Texture texture = image.getTextureFromTask();
                    if (texture != null)
                        texture.readPixels(x, y, w, h, format, data, dataOfs);
                }
                putCallback(onDone);
            });
        } else {
            // OTR OK. Do shenanigans.
            putTask((instance) -> {
                // Ensure any outstanding GL operations are finished.
                try (TimeLogger.Source src = TimeLogger.optOpen(timeLoggerFinishTask)) {
                    instance.finish();
                }
                image.otrLock();
                BadGPU.Texture texture = image.getTextureFromTask();
                try {
                    otrQueue.put(() -> {
                        try (TimeLogger.Source src = TimeLogger.optOpen(timeLoggerReadPixelsTask)) {
                            if (texture != null)
                                texture.readPixelsOffThread(x, y, w, h, format, data, dataOfs);
                        }
                        image.otrUnlock();
                        putCallback(onDone);
                    });
                } catch (InterruptedException e) {
                    image.otrUnlock();
                    e.printStackTrace();
                }
            });
        }
    }

    public void putCallback(Runnable object) {
        if (vopeksCBThread.shutdown)
            return;
        try {
            cbQueue.put(object);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void putFlushTask() {
        if (timeLoggerFlushTask != null) {
            putTask((instance) -> {
                try (TimeLogger.Source src = timeLoggerFlushTask.open()) {
                    if (tasksBetweenFlushes > 0) {
                        System.out.println("VOPEKS: Tasks between flushes: " + tasksBetweenFlushes);
                        tasksBetweenFlushes = 0;
                    }
                    instance.flush();
                }
            });
        } else {
            putTask((instance) -> {
                instance.flush();
            });
        }
    }

    public void putFinishTask() {
        if (timeLoggerFinishTask != null) {
            putTask((instance) -> {
                try (TimeLogger.Source src = timeLoggerFinishTask.open()) {
                    instance.finish();
                }
            });
        } else {
            putTask((instance) -> {
                instance.finish();
            });
        }
    }

    public void shutdown() {
        // Start shutdown, then wake up the thread.
        shutdownPrimary = true;
        vopeksCBThread.shutdown = true;
        vopeksOTRThread.shutdown = true;
        try {
            taskQueue.put((instance) -> {});
            vopeksThread.join();
        } catch (InterruptedException ie) {}
        try {
            cbQueue.put(EmptyLambdas.emptyRunnable);
            vopeksCBThread.join();
        } catch (InterruptedException ie) {}
        try {
            otrQueue.put(EmptyLambdas.emptyRunnable);
            vopeksOTRThread.join();
        } catch (InterruptedException ie) {}
    }

    public static interface ITask {
        void run(BadGPU.Instance instance);
    }

    private class CallbackRunner extends Thread {
        public volatile boolean shutdown;

        public final ArrayBlockingQueue<Runnable> queue;
        public CallbackRunner(String name, ArrayBlockingQueue<Runnable> queue) {
            super(name);
            this.queue = queue;
        }

        @Override
        public void run() {
            if (timeLogger != null) {
                TimeLogger.Source vs = timeLogger.newSource(getName());
                while (!shutdown) {
                    try {
                        Runnable task = queue.take();
                        try (TimeLogger.Source vs2 = vs.open()) {
                            task.run();
                        }
                    } catch (Throwable t) {
                        System.err.println("Exception in " + getName() + ":");
                        t.printStackTrace();
                    }
                }
            } else {
                while (!shutdown) {
                    try {
                        queue.take().run();
                    } catch (Throwable t) {
                        System.err.println("Exception in " + getName() + ":");
                        t.printStackTrace();
                    }
                }
            }
        }
    }
}
