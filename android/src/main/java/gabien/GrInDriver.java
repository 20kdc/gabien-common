/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.util.concurrent.Semaphore;

import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceHolder;
import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;
import gabien.uslx.append.TimeLogger;

public class GrInDriver implements IGrInDriver {
    public Peripherals peripherals;
    public boolean wantsShutdown = false;
    public Rect displayArea = new Rect(0, 0, 1, 1);
    public int wantedBackBufferW, wantedBackBufferH, wantedBackBufferWSetAsync, wantedBackBufferHSetAsync;
    private final TimeLogger.Source timeLoggerGameThread = TimeLogger.optSource(GaBIEn.timeLogger, "GameThread");
    private final TimeLogger.Source timeLoggerAndroidFlip = TimeLogger.optSource(GaBIEn.timeLogger, "AndroidFlip");
    public Semaphore waitingFrames = new Semaphore(1);
    public boolean isFirstFrame = true;

    private Surface currentEGLSurfaceJ = null;
    private long currentEGLSurface = 0;

    public GrInDriver(int w, int h) {
        wantedBackBufferW = w;
        wantedBackBufferH = h;
        wantedBackBufferWSetAsync = w;
        wantedBackBufferHSetAsync = h;
        peripherals = new Peripherals(this);
        timeLoggerGameThread.open();
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
        timeLoggerGameThread.close();

        waitingFrames.acquireUninterruptibly();
        timeLoggerAndroidFlip.open();
        GaBIEn.vopeks.putTask((instance) -> {
            try {
                doFlushLoop(instance, backBufferI);
            } finally {
                timeLoggerAndroidFlip.close();
                waitingFrames.release();
            }
        });
        if (isFirstFrame) {
            // Ensure the actual surface dimensions are here
            waitingFrames.acquireUninterruptibly();
            waitingFrames.release();
            isFirstFrame = false;
        }
        wantedBackBufferW = wantedBackBufferWSetAsync;
        wantedBackBufferH = wantedBackBufferHSetAsync;

        // So the timeline is a bit weird here.
        AndroidPortGlobals.mainActivityLock.lock();
        try {
            displayArea = new Rect(0, 0, backBufferI.getWidth(), backBufferI.getHeight());
            // These are set from within mainActivityLock, so...
            peripherals.gdUpdateTextboxHoldingMALock();
        } finally {
            AndroidPortGlobals.mainActivityLock.unlock();
        }
        timeLoggerGameThread.open();
    }
    private void doFlushLoop(BadGPU.Instance instance, IImage backBufferI) {
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
                    Rect surfaceFrame = sh.getSurfaceFrame();
                    int w = surfaceFrame.width();
                    int h = surfaceFrame.height();
                    wantedBackBufferWSetAsync = w;
                    wantedBackBufferHSetAsync = h;
                    if (sh != null) {
                        BadGPU.Texture tex = backBufferI.getTextureFromTask();
                        // oops, no texture!
                        if (tex == null)
                            return;
                        Surface ns = sh.getSurface();
                        if (ns != currentEGLSurfaceJ) {
                            //if (currentEGLSurface != 0)
                            //    BadGPUUnsafe.ANDdestroyEGLSurface(instance.pointer, currentEGLSurface);
                            currentEGLSurfaceJ = ns;
                            currentEGLSurface = BadGPUUnsafe.ANDcreateEGLSurface(instance.pointer, ns);
                            System.out.println("Created surface: " + currentEGLSurface);
                        }
                        if (currentEGLSurface != 0) {
                            System.out.println("Blitting to surface...");
                            BadGPUUnsafe.ANDblitToSurface(instance.pointer, tex.pointer, currentEGLSurface, w, h);
                            System.out.println("...done!");
                        }
                        return;
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
