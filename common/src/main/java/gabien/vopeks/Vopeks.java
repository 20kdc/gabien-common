/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.Nullable;

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
    private final ArrayBlockingQueue<ITask> taskQueue = new ArrayBlockingQueue<>(TASK_QUEUE_SIZE);
    private final ArrayBlockingQueue<Runnable> cbQueue = new ArrayBlockingQueue<>(TASK_QUEUE_SIZE);
    public final VopeksFloatPool floatPool = new VopeksFloatPool();
    private volatile boolean shutdownPrimary;
    public final AtomicReference<Throwable> initFailure = new AtomicReference<Throwable>();
    public final AtomicBoolean initComplete = new AtomicBoolean();

    public Vopeks(final int newInstanceFlags, @Nullable TimeLogger timeLogger, boolean isCrashingVopeks) {
        this.timeLogger = timeLogger;
        timeLoggerReadPixelsTask = TimeLogger.optSource(timeLogger, "readPixelsTask");
        timeLoggerFlushTask = TimeLogger.optSource(timeLogger, "flushTask");
        timeLoggerFinishTask = TimeLogger.optSource(timeLogger, "finishTask");
        vopeksThread = new Thread("VOPEKS Thread") {
            @Override
            public void run() {
                BadGPU.Instance instance;
                try {
                    instance = BadGPU.newInstance(newInstanceFlags);
                    if (isCrashingVopeks)
                        throw new RuntimeException("Told to crash VOPEKS");
                } catch (Throwable t) {
                    initFailure.set(t);
                    throw t;
                } finally {
                    initComplete.set(true);
                }
                if (timeLogger != null) {
                    TimeLogger.Source vs = timeLogger.newSource("vopeks_main");
                    while (!shutdownPrimary) {
                        try {
                            ITask task = taskQueue.take();
                            // intentionally limited to this path
                            tasksBetweenFlushes++;
                            try (TimeLogger.Source vs2 = TimeLogger.open(vs)) {
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
        vopeksThread.start();
        vopeksCBThread.start();
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

    public void putCallback(Runnable object) {
        if (vopeksCBThread.shutdown)
            return;
        try {
            cbQueue.put(object);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void putBatchStatisticsTask() {
        if (timeLoggerFlushTask != null) {
            putTask((instance) -> {
                // this task also counts as a task
                tasksBetweenFlushes--;
                System.out.println("VOPEKS: Tasks between flushes: " + tasksBetweenFlushes);
                tasksBetweenFlushes = 0;
            });
        }
    }

    public void putFlushTask() {
        if (timeLoggerFlushTask != null) {
            putTask((instance) -> {
                try (TimeLogger.Source src = TimeLogger.open(timeLoggerFlushTask)) {
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
                try (TimeLogger.Source src = TimeLogger.open(timeLoggerFinishTask)) {
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
        try {
            taskQueue.put((instance) -> {});
            vopeksThread.join();
        } catch (InterruptedException ie) {}
        try {
            cbQueue.put(EmptyLambdas.emptyRunnable);
            vopeksCBThread.join();
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
                        try (TimeLogger.Source vs2 = TimeLogger.open(vs)) {
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
