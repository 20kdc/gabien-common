/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import java.util.concurrent.ArrayBlockingQueue;

import gabien.natives.BadGPU;

/**
 * New renderers always have to be named in silly ways, right?
 * To be clear, this API isn't necessarily totally stable.
 * Direct interaction with it should be restrained to situations where you can reasonably say you need the performance boost.
 *
 * Created 7th June, 2023.
 */
public final class Vopeks {
    private final int TASK_QUEUE_SIZE = 65536;
    public final Thread vopeksThread;
    private final ArrayBlockingQueue<ITask> taskQueue = new ArrayBlockingQueue<>(TASK_QUEUE_SIZE);
    public final VopeksFloatPool floatPool = new VopeksFloatPool();
    private volatile boolean shutdown;

    public Vopeks(final int newInstanceFlags) {
        vopeksThread = new Thread("VOPEKS Thread") {
            @Override
            public void run() {
                BadGPU.Instance instance = BadGPU.newInstance(newInstanceFlags);
                while (!shutdown) {
                    try {
                        taskQueue.take().run(instance);
                    } catch (Throwable t) {
                        System.err.println("Exception in VOPEKS:");
                        t.printStackTrace();
                    }
                }
                instance.dispose();
            }
        };
        vopeksThread.start();
    }

    public void putTask(ITask object) {
        try {
            taskQueue.put(object);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void shutdown() {
        // Start shutdown, then wake up the thread.
        shutdown = true;
        taskQueue.add((instance) -> {});
    }

    public static interface ITask {
        void run(BadGPU.Instance instance);
    }
}
