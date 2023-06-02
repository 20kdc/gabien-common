/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.SurfaceHolder;

public class GrInDriver extends OsbDriver implements IGrInDriver {
    public Peripherals peripherals;
    public boolean wantsShutdown = false;
    public Rect displayArea = new Rect(0, 0, 1, 1);

    public GrInDriver(int w, int h) {
        super(w, h, false);
        peripherals = new Peripherals(this);
    }

    @Override
    public boolean stillRunning() {
        return !wantsShutdown;
    }

    @Override
    public IGrDriver getBackBuffer() {
        return this;
    }

    @Override
    public void flush() {
        boolean first = true;
        while (true) {
            peripherals.gdUpdateTextbox(first);
            first = false;
            AndroidPortGlobals.mainActivityLock.lock();
            try {
                MainActivity last = AndroidPortGlobals.mainActivity;
                if (last != null) {
                    try {
                        SurfaceHolder sh = last.mySurface.getHolder();
                        if (sh != null) {
                            Canvas c = sh.lockCanvas();
                            Rect r = sh.getSurfaceFrame();
    
                            int letterboxing2 = 0;
                            double realAspectRatio = w / (double) h;
                            int goodWidth = (int)(realAspectRatio * r.height());
                            // work out letterboxing from widths
                            int letterboxing = (r.width() - goodWidth) / 2;
    
                            displayArea = new Rect(letterboxing, letterboxing2, r.width() - letterboxing, r.height() - letterboxing2);
                            c.drawBitmap(bitmap, new Rect(0, 0, w, h), displayArea, globalPaint);
    
                            sh.unlockCanvasAndPost(c);
                            if ((r.width() != w) || (r.height() != h))
                                resize(r.width(), r.height());
                        }
                    } catch (Exception e) {
    
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
        return Math.max(10, Math.min(w, h) / 30);
    }
}
