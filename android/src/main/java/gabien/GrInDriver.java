/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.util.concurrent.Semaphore;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.SurfaceHolder;

public class GrInDriver implements IGrInDriver {
    public Peripherals peripherals;
    public boolean wantsShutdown = false;
    public Rect displayArea = new Rect(0, 0, 1, 1);
    public int wantedBackBufferW, wantedBackBufferH;
    public int wantedBackBufferWSetAsync, wantedBackBufferHSetAsync;
    public boolean isFirstFrame = true;
    private int[] backBufferDownload = new int[0];
    private WSIImageDriver backBufferDownloadWSI = new WSIImageDriver(new int[0], 0, 0);
    private Paint globalPaint = new Paint();
    private Semaphore waitingFrames = new Semaphore(1);

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
        /*
         * Big explanation of how the threading works here:
         * So the original idea was to use lockCanvas as the lock, but it doesn't actually work that way.
         * So doing the waitingFrames from JSE, but with added spice.
         */
        waitingFrames.acquireUninterruptibly();
        // Grab these from the last frame.
        wantedBackBufferW = wantedBackBufferWSetAsync;
        wantedBackBufferH = wantedBackBufferHSetAsync;
        AndroidPortGlobals.mainActivityLock.lock();
        try {
            peripherals.gdUpdateTextboxHoldingMALock();
        } finally {
            AndroidPortGlobals.mainActivityLock.unlock();
        }
        // Ensure the buffers are the right size.
        if (backBufferDownloadWSI.getWidth() != backBufferI.getWidth() || backBufferDownloadWSI.getHeight() != backBufferI.getHeight()) {
            backBufferDownload = new int[backBufferI.getWidth() * backBufferI.getHeight()];
            backBufferDownloadWSI = new WSIImageDriver(backBufferDownload, backBufferI.getWidth(), backBufferI.getHeight());
        }
        backBufferI.getPixelsAsync(backBufferDownload, () -> {
            backBufferDownloadWSI.setPixels(backBufferDownload);
            doFlushLoop();
            waitingFrames.release();
        });
        if (isFirstFrame) {
            // This is so that the first flush always sets up the correct w/h for the game thread.
            // It has no other use.
            waitingFrames.acquireUninterruptibly();
            wantedBackBufferW = wantedBackBufferWSetAsync;
            wantedBackBufferH = wantedBackBufferHSetAsync;
            waitingFrames.release();
            isFirstFrame = false;
        }
    }
    private void doFlushLoop() {
        while (true) {
            AndroidPortGlobals.mainActivityLock.lock();
            try {
                MainActivity last = AndroidPortGlobals.mainActivity;
                if (last != null) {
                    try {
                        SurfaceHolder sh = last.mySurface.getHolder();
                        if (sh != null) {
                            Canvas c = sh.lockCanvas();
                            if (c != null) {
                                flushWithLockedCanvas(sh, c);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                AndroidPortGlobals.mainActivityLock.unlock();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
    private void flushWithLockedCanvas(SurfaceHolder sh, Canvas c) {
        Rect r = sh.getSurfaceFrame();
        
        wantedBackBufferWSetAsync = r.width();
        wantedBackBufferHSetAsync = r.height();

        WSIImageDriver backBuffer = backBufferDownloadWSI;
        int letterboxing2 = 0;
        int bW = backBuffer.w;
        int bH = backBuffer.h;
        double realAspectRatio = bW / (double) bH;
        int goodWidth = (int)(realAspectRatio * r.height());
        // work out letterboxing from widths
        int letterboxing = (r.width() - goodWidth) / 2;

        displayArea = new Rect(letterboxing, letterboxing2, r.width() - letterboxing, r.height() - letterboxing2);
        if (backBuffer.bitmap != null)
            c.drawBitmap(backBuffer.bitmap, new Rect(0, 0, bW, bH), displayArea, globalPaint);

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
}
