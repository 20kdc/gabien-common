/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package gabien.backendhelp;

import gabien.IGrDriver;
import gabien.IGrInDriver;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Backend assistance: Forwards commands to another thread.
 * Apart from OsbDriver, images are immutable.
 * Created on 08/06/17.
 */
public class ThreadForwardingGrDriver<T extends IGrDriver> implements IGrDriver {
    // Wiped by forwardingThread
    public LinkedRunnable firstCommand = new LinkedRunnable(new Runnable() {
        @Override
        public void run() {
            // Do nothing.
        }
    });
    public LinkedRunnable lastCommand = firstCommand;

    public AtomicBoolean shutdownThread = new AtomicBoolean(false);

    public Thread forwardingThread = new Thread() {
        @Override
        public void run() {
            LinkedRunnable current = firstCommand;
            firstCommand = null;
            while (!shutdownThread.get()) {
                LinkedRunnable maybeNext = current.next;
                if (maybeNext != null) {
                    current.next = null;
                    current = maybeNext;
                    current.doIt.run();
                }
            }
            target.shutdown();
        }
    };

    public int clientWidth = 0;
    public int clientHeight = 0;
    public final T target;
    public Runnable waitingLock;

    public ThreadForwardingGrDriver(T targ) {
        forwardingThread.start();
        target = targ;
    }

    @Override
    public int getWidth() {
        return clientWidth;
    }

    @Override
    public int getHeight() {
        return clientHeight;
    }

    public void flushCmdBuf() {
        Runnable[] block = createBlock();
        block[0].run();
        waitingLock = block[1];
        // refresh visible width and height
        clientWidth = target.getWidth();
        clientHeight = target.getHeight();
    }

    public void shutdown() {
        if (shutdownThread.get())
            return;
        flushCmdBuf();
        shutdownThread.set(true);
    }

    public void cmdUnwait() {
        if (waitingLock != null) {
            waitingLock.run();
            waitingLock = null;
        }
    }
    public void cmdSubmitCore(Runnable r) {
        LinkedRunnable lr = new LinkedRunnable(r);
        lastCommand.next = lr;
        lastCommand = lr;
    }
    // Used to talk between ThreadForwardingGrDriver instances.
    // Both returned Runnables have to be used on the calling TFGD's worker thread.
    // The target worker will stop when it hits the block.
    // The first function ensures the target worker has stopped.
    // The second function lets it go on after the work is done.
    public Runnable[] createBlock() {
        cmdUnwait();
        final Semaphore lA = new Semaphore(1);
        final Semaphore lB = new Semaphore(1);
        // 1. Acquire LA & LB on this thread...
        lA.acquireUninterruptibly();
        lB.acquireUninterruptibly();
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                lB.release();
                // 2. ...to block this thread...
                lA.acquireUninterruptibly();
                // 5. It continues
            }
        });
        return new Runnable[] {new Runnable() {
            @Override
            public void run() {
                // 2. ...to block this thread...
                lB.acquireUninterruptibly();
                // 3. Released by the target worker's lB release, lA still held, do work
            }
        }, new Runnable() {
            @Override
            public void run() {
                // 4. The work is done, release lA so the target worker can continue
                lA.release();
            }
        }};
    }

    // basic operations

    @Override
    public void blitImage(final int srcx, final int srcy, final int srcw, final int srch, final int x, final int y, final IGrInDriver.IImage i) {
        if (i == null)
            throw new NullPointerException();
        cmdUnwait();
        final Runnable[] block;
        if (i instanceof ThreadForwardingGrDriver) {
            block = ((ThreadForwardingGrDriver) i).createBlock();
        } else {
            block = null;
        }
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                if (block != null)
                    block[0].run();
                target.blitImage(srcx, srcy, srcw, srch, x, y, i);
                if (block != null)
                    block[1].run();
            }
        });
    }

    @Override
    public void blitScaledImage(final int srcx, final int srcy, final int srcw, final int srch, final int x, final int y, final int acw, final int ach, final IGrInDriver.IImage i) {
        if (i == null)
            throw new NullPointerException();
        cmdUnwait();
        final Runnable[] block;
        if (i instanceof ThreadForwardingGrDriver) {
            block = ((ThreadForwardingGrDriver) i).createBlock();
        } else {
            block = null;
        }
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                if (block != null)
                    block[0].run();
                target.blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i);
                if (block != null)
                    block[1].run();
            }
        });
    }

    @Override
    public void blitRotatedScaledImage(final int srcx, final int srcy, final int srcw, final int srch, final int x, final int y, final int acw, final int ach, final int angle, final IGrInDriver.IImage i) {
        if (i == null)
            throw new NullPointerException();
        cmdUnwait();
        final Runnable[] block;
        if (i instanceof ThreadForwardingGrDriver) {
            block = ((ThreadForwardingGrDriver) i).createBlock();
        } else {
            block = null;
        }
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                if (block != null)
                    block[0].run();
                target.blitRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i);
                if (block != null)
                    block[1].run();
            }
        });
    }

    @Override
    public void drawText(final int x, final int y, final int r, final int g, final int b, final int i, final String text) {
        cmdUnwait();
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                target.drawText(x, y, r, g, b, i, text);
            }
        });
    }

    @Override
    public void clearAll(final int i, final int i0, final int i1) {
        cmdUnwait();
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                target.clearAll(i, i0, i1);
            }
        });
    }

    @Override
    public void clearRect(final int r, final int g, final int b, final int x, final int y, final int width, final int height) {
        cmdUnwait();
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                target.clearRect(r, g, b, x, y, width, height);
            }
        });
    }

    public static class LinkedRunnable {
        public Runnable doIt;
        public LinkedRunnable next;

        public LinkedRunnable(Runnable runnable) {
            doIt = runnable;
        }
    }
}
