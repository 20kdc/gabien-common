/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import gabien.backendhelp.WSIDownloadPair;

public class GrInDriver implements IGrInDriver {
    public Peripherals peripherals;
    public boolean wantsShutdown = false;
    public Rect displayArea = new Rect(0, 0, 1, 1);
    public int wantedBackBufferW, wantedBackBufferH;
    public int wantedBackBufferWSetAsync, wantedBackBufferHSetAsync;
    public boolean isFirstFrame = true;
    private DLIAPair dlIA = new DLIAPair("Android");
    private Paint globalPaint = new Paint();

    public GrInDriver(int w, int h) {
        wantedBackBufferW = w;
        wantedBackBufferH = h;
        wantedBackBufferWSetAsync = w;
        wantedBackBufferHSetAsync = h;
        peripherals = new Peripherals(this);
    }

    @Override
    public boolean stillRunning() {
        return !wantsShutdown;
    }

    @Override
    public int getWidth() {
        return wantedBackBufferW;
    }

    @Override
    public int getHeight() {
        return wantedBackBufferH;
    }

    @Override
    public void flush(IImage backBufferI) {
        backBufferI.batchFlush();
        GaBIEn.vopeks.putFlushTask();
        /*
         * Big explanation of how the threading works here:
         * So the original idea was to use lockCanvas as the lock, but it doesn't actually work that way.
         * So doing the waitingFrames from JSE, but with added spice.
         */
        // Ensure the buffers are the right size.
        int bW = backBufferI.getWidth();
        int bH = backBufferI.getHeight();
        if (isFirstFrame) {
            // Synchronous, so that the first flush always sets up the correct w/h for the game thread.
            int[] backBufferDownload = dlIA.acquire(bW, bH);
            backBufferI.getPixels(backBufferDownload);
            doFlushLoop(backBufferDownload, bW, bH);
            dlIA.release(backBufferDownload);
            isFirstFrame = false;
        } else {
            int[] backBufferDownload = dlIA.acquire(bW, bH);
            backBufferI.getPixelsAsync(backBufferDownload, () -> {
                doFlushLoop(backBufferDownload, bW, bH);
                dlIA.release(backBufferDownload);
            });
        }
        // So the timeline is a bit weird here.
        // Hypothetically an infinitely-fast readPixels could lock this first, but it won't.
        AndroidPortGlobals.mainActivityLock.lock();
        try {
            // These are set from within mainActivityLock, so...
            wantedBackBufferW = wantedBackBufferWSetAsync;
            wantedBackBufferH = wantedBackBufferHSetAsync;
            peripherals.gdUpdateTextboxHoldingMALock();
        } finally {
            AndroidPortGlobals.mainActivityLock.unlock();
        }
    }
    private void doFlushLoop(int[] backBufferDownload, int bW, int bH) {
        while (true) {
            AndroidPortGlobals.mainActivityLock.lock();
            MainActivity last = AndroidPortGlobals.mainActivity;
            if (last != null) {
                // Transfer locks.
                try {
                    last.surfaceLock.lock();
                } finally {
                    AndroidPortGlobals.mainActivityLock.unlock();
                }
                // We're now holding surfaceLock and not holding the MAL.
                // So flush() code can do its thing.
                try {
                    SurfaceHolder sh = last.mySurface.getHolder();
                    if (sh != null) {
                        Canvas c = sh.lockCanvas();
                        if (c != null) {
                            flushWithLockedCanvas(sh, c, backBufferDownload, bW, bH);
                            return;
                        }
                    }
                } catch (Throwable e) {
                } finally {
                    last.surfaceLock.unlock();
                }
            } else {
                AndroidPortGlobals.mainActivityLock.unlock();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
    private void flushWithLockedCanvas(SurfaceHolder sh, Canvas c, int[] backBufferDownload, int bW, int bH) {
        Rect r = sh.getSurfaceFrame();
        
        wantedBackBufferWSetAsync = r.width();
        wantedBackBufferHSetAsync = r.height();

        /*
        int letterboxing2 = 0;
        double realAspectRatio = bW / (double) bH;
        int goodWidth = (int)(realAspectRatio * r.height());
        // work out letterboxing from widths
        int letterboxing = (r.width() - goodWidth) / 2;
        displayArea = new Rect(letterboxing, letterboxing2, r.width() - letterboxing, r.height() - letterboxing2);
        */

        // currently ignoring the whole scaling thing so that this works w/ acceptable perf maybe
        displayArea = new Rect(0, 0, bW, bH);
        if (bW != 0 && bH != 0)
            c.drawBitmap(backBufferDownload, 0, bW, 0, 0, bW, bH, true, globalPaint);

        sh.unlockCanvasAndPost(c);
    }

    @Override
    public IPeripherals getPeripherals() {
        return peripherals;
    }

    @Override
    public void shutdown() {
        // Bye-bye, GrInDriver.
        wantsShutdown = true;
    }

    @Override
    public int estimateUIScaleTenths() {
        return Math.max(10, Math.min(wantedBackBufferW, wantedBackBufferH) / 30);
    }

    private class DLIAPair extends WSIDownloadPair<int[]> {
        public DLIAPair(String n) {
            super(n, 2);
        }

        @Override
        public boolean bufferMatchesSize(int[] buffer, int width, int height) {
            return buffer.length == (width * height);
        }
        @Override
        public int[] genBuffer(int width, int height) {
            return new int[width * height];
        }
    }
}
