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

public class GrInDriver implements IGrInDriver {
    public Peripherals peripherals;
    public boolean wantsShutdown = false;
    public Rect displayArea = new Rect(0, 0, 1, 1);
    public int wantedBackBufferW, wantedBackBufferH;
    private int[] backBufferDownload;
    private WSIImageDriver backBufferDownloadWSI;
    private Paint globalPaint = new Paint();

    public GrInDriver(int w, int h) {
        wantedBackBufferW = w;
        wantedBackBufferH = h;
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
         * So SurfaceHolder.lockCanvas acts as a natural limiter on frames.
         * This is because we pass control of the lock over to the render thread.
         * The render thread therefore owns the canvas lock, and if we try to flush another frame, we get blocked on the existing lock.
         * Meanwhile we're still able to send the surface details back down to respond to changes.
         */
        while (true) {
            AndroidPortGlobals.mainActivityLock.lock();
            peripherals.gdUpdateTextboxHoldingMALock();
            try {
                MainActivity last = AndroidPortGlobals.mainActivity;
                if (last != null) {
                    try {
                        SurfaceHolder sh = last.mySurface.getHolder();
                        if (sh != null) {
                            Canvas c = sh.lockCanvas();
                            flushWithLockedCanvas(c, sh, backBufferI);
                            return;
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
    private void flushWithLockedCanvas(Canvas c, SurfaceHolder sh, IImage backBufferI) {
        Rect r = sh.getSurfaceFrame();
        
        // Ensure the buffers are the right size.
        if (backBufferDownloadWSI.getWidth() != backBufferI.getWidth() || backBufferDownloadWSI.getHeight() != backBufferI.getHeight()) {
            backBufferDownload = new int[backBufferI.getWidth() * backBufferI.getHeight()];
            backBufferDownloadWSI = new WSIImageDriver(backBufferDownload, backBufferI.getWidth(), backBufferI.getHeight());
        }
        backBufferI.getPixelsAsync(backBufferDownload, () -> {
            WSIImageDriver backBuffer = backBufferDownloadWSI;
            int letterboxing2 = 0;
            int bW = backBuffer.w;
            int bH = backBuffer.h;
            double realAspectRatio = bW / (double) bH;
            int goodWidth = (int)(realAspectRatio * r.height());
            // work out letterboxing from widths
            int letterboxing = (r.width() - goodWidth) / 2;

            displayArea = new Rect(letterboxing, letterboxing2, r.width() - letterboxing, r.height() - letterboxing2);
            c.drawBitmap(backBuffer.bitmap, new Rect(0, 0, bW, bH), displayArea, globalPaint);

            sh.unlockCanvasAndPost(c);
        });

        wantedBackBufferW = r.width();
        wantedBackBufferH = r.height();
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
