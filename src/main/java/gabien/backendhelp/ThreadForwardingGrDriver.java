/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.backendhelp;

import gabien.IGrDriver;
import gabien.IImage;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Backend assistance: Forwards commands to another thread.
 * Apart from OsbDriver, images are immutable.
 * Created on 08/06/17.
 */
public class ThreadForwardingGrDriver<T extends IGrDriver> implements IGrDriver, INativeImageHolder {
    public int[] localSTBuffer = new int[6];
    // Wiped by forwardingThread
    public LinkedRunnable firstCommand = new LinkedRunnable(new Runnable() {
        @Override
        public void run() {
            // Do nothing.
        }
    });
    public LinkedRunnable lastCommand = firstCommand;

    public AtomicBoolean shutdownThread = new AtomicBoolean(false);

    public Thread forwardingThread = new Thread(this.toString()) {
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

    @Override
    public int[] getPixels() {
        Runnable r = flushCmdBufAndLock();
        int[] bp = target.getPixels();
        r.run();
        return bp;
    }

    @Override
    public byte[] createPNG() {
        Runnable r = flushCmdBufAndLock();
        byte[] bp = target.createPNG();
        r.run();
        return bp;
    }

    protected Runnable flushCmdBufAndLock() {
        Runnable[] block = getLockingSequenceN();
        block[0].run();
        // refresh visible width and height
        clientWidth = target.getWidth();
        clientHeight = target.getHeight();
        // sync ST values
        int[] remoteST = target.getLocalST();
        for (int i = 0; i < localSTBuffer.length; i++)
            localSTBuffer[i] = remoteST[i];
        return block[1];
    }

    public void shutdown() {
        if (shutdownThread.get())
            return;
        Runnable r = flushCmdBufAndLock();
        shutdownThread.set(true);
        r.run();
    }

    public void cmdSubmitCore(Runnable r) {
        LinkedRunnable lr = new LinkedRunnable(r);
        lastCommand.next = lr;
        lastCommand = lr;
    }

    // -- DO NOT ACTUALLY USE THIS, just use getLockingSequence --
    // Used to talk between ThreadForwardingGrDriver instances.
    // Both returned Runnables have to be used on the calling TFGD's worker thread.
    // The target worker will stop when it hits the block.
    // The first function ensures the target worker has stopped.
    // The second function lets it go on after the work is done.
    private Runnable[] createBlockIntern() {
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
                acquireImpl(lA);
                // 5. It continues
            }
        });
        return new Runnable[] {new Runnable() {
            @Override
            public void run() {
                // 2. ...to block this thread...
                acquireImpl(lB);
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

    private void acquireImpl(Semaphore lA) {
        if (true) {
            // Always use this!!!
            lA.acquireUninterruptibly();
        } else {
            // For deadlock debugging only, not totally reliable
            try {
                if (!lA.tryAcquire(2, TimeUnit.SECONDS))
                    throw new RuntimeException("The thread " + Thread.currentThread() + " failed to acquire " + this);
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }
    }

    @Override
    public Runnable[] getLockingSequenceN() {
        final Runnable[] base = createBlockIntern();
        final Runnable[] b2 = ensureTargetImageStable(target);
        if (b2 == null)
            return base;
        return new Runnable[] {
                new Runnable() {
                    @Override
                    public void run() {
                        base[0].run();
                        b2[0].run();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        b2[1].run();
                        base[1].run();
                    }
                }
        };
    }

    // basic operations

    private Runnable[] ensureTargetImageStable(IImage i) {
        return ((INativeImageHolder) i).getLockingSequenceN();
    }

    @Override
    public void blitImage(final int srcx, final int srcy, final int srcw, final int srch, final int x, final int y, final IImage i) {
        if (i == null)
            throw new NullPointerException();
        final Runnable[] r = ensureTargetImageStable(i);
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                if (r != null)
                    r[0].run();
                target.blitImage(srcx, srcy, srcw, srch, x, y, i);
                if (r != null)
                    r[1].run();
            }
        });
    }

    @Override
    public void blitTiledImage(final int x, final int y, final int w, final int h, final IImage i) {
        if (i == null)
            throw new NullPointerException();
        final Runnable[] r = ensureTargetImageStable(i);
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                if (r != null)
                    r[0].run();
                target.blitTiledImage(x, y, w, h, i);
                if (r != null)
                    r[1].run();
            }
        });
    }

    @Override
    public void blitScaledImage(final int srcx, final int srcy, final int srcw, final int srch, final int x, final int y, final int acw, final int ach, final IImage i) {
        if (i == null)
            throw new NullPointerException();
        final Runnable[] r = ensureTargetImageStable(i);
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                if (r != null)
                    r[0].run();
                target.blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i);
                if (r != null)
                    r[1].run();
            }
        });
    }

    @Override
    public void blitRotatedScaledImage(final int srcx, final int srcy, final int srcw, final int srch, final int x, final int y, final int acw, final int ach, final int angle, final IImage i) {
        if (i == null)
            throw new NullPointerException();
        final Runnable[] r = ensureTargetImageStable(i);
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                if (r != null)
                    r[0].run();
                target.blitRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i);
                if (r != null)
                    r[1].run();
            }
        });
    }

    @Override
    public void blendRotatedScaledImage(final int srcx, final int srcy, final int srcw, final int srch, final int x, final int y, final int acw, final int ach, final int angle, final IImage i, final boolean blendSub) {
        if (i == null)
            throw new NullPointerException();
        final Runnable[] r = ensureTargetImageStable(i);
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                if (r != null)
                    r[0].run();
                target.blendRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, blendSub);
                if (r != null)
                    r[1].run();
            }
        });
    }

    @Override
    public void drawText(final int x, final int y, final int r, final int g, final int b, final int i, final String text) {
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                target.drawText(x, y, r, g, b, i, text);
            }
        });
    }

    @Override
    public void clearAll(final int i, final int i0, final int i1) {
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                target.clearAll(i, i0, i1);
            }
        });
    }

    @Override
    public void clearRect(final int r, final int g, final int b, final int x, final int y, final int width, final int height) {
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                target.clearRect(r, g, b, x, y, width, height);
            }
        });
    }

    @Override
    public int[] getLocalST() {
        return localSTBuffer;
    }

    @Override
    public void updateST() {
        final int a, b, c, d, e, f;
        a = localSTBuffer[0];
        b = localSTBuffer[1];
        c = localSTBuffer[2];
        d = localSTBuffer[3];
        e = localSTBuffer[4];
        f = localSTBuffer[5];
        cmdSubmitCore(new Runnable() {
            @Override
            public void run() {
                int[] lst = target.getLocalST();
                lst[0] = a;
                lst[1] = b;
                lst[2] = c;
                lst[3] = d;
                lst[4] = e;
                lst[5] = f;
                target.updateST();
            }
        });
    }

    @Override
    public Object getNative() {
        return ((INativeImageHolder) target).getNative();
    }

    public static class LinkedRunnable {
        public Runnable doIt;
        public LinkedRunnable next;

        public LinkedRunnable(Runnable runnable) {
            doIt = runnable;
        }
    }
}
