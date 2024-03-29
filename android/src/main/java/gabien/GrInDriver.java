/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.util.concurrent.Semaphore;

import org.eclipse.jdt.annotation.Nullable;

import android.graphics.Rect;
import android.view.Surface;
import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;
import gabien.render.IImage;
import gabien.uslx.append.TimeLogger;
import gabien.wsi.IGrInDriver;
import gabien.wsi.IPeripherals;

public class GrInDriver implements IGrInDriver {
    public Peripherals peripherals;
    public boolean wantsShutdown = false;
    public Rect displayArea = new Rect(0, 0, 1, 1);
    public int wantedBackBufferW, wantedBackBufferH;
    private final @Nullable TimeLogger.Source timeLoggerGameThread = TimeLogger.optSource(GaBIEn.timeLogger, "GameThread");
    private final @Nullable TimeLogger.Source timeLoggerAndroidFlip = TimeLogger.optSource(GaBIEn.timeLogger, "AndroidFlip");
    public Semaphore waitingFrames = new Semaphore(1);

    // Accessed only from VOPEKS thread
    private long currentEGLSurface = 0;

    public GrInDriver(int w, int h) {
        wantedBackBufferW = w;
        wantedBackBufferH = h;
        peripherals = new Peripherals(this);
        TimeLogger.open(timeLoggerGameThread);
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
        TimeLogger.close(timeLoggerGameThread);

        waitingFrames.acquireUninterruptibly();
        GaBIEn.vopeks.putTask((instance) -> {
            TimeLogger.open(timeLoggerAndroidFlip);
            try {
                doFlushLoop(instance, backBufferI);
            } finally {
                TimeLogger.close(timeLoggerAndroidFlip);
                waitingFrames.release();
            }
        });
        AndroidPortGlobals.surfaceLock.lock();
        if (AndroidPortGlobals.surface != null) {
            wantedBackBufferW = AndroidPortGlobals.surfaceWidth;
            wantedBackBufferH = AndroidPortGlobals.surfaceHeight;
        }
        AndroidPortGlobals.surfaceLock.unlock();

        // So the timeline is a bit weird here.
        AndroidPortGlobals.mainActivityLock.lock();
        try {
            displayArea = new Rect(0, 0, backBufferI.getWidth(), backBufferI.getHeight());
            // These are set from within mainActivityLock, so...
            peripherals.gdUpdateTextboxHoldingMALock();
        } finally {
            AndroidPortGlobals.mainActivityLock.unlock();
        }
        TimeLogger.open(timeLoggerGameThread);
    }
    private void doFlushLoop(BadGPU.Instance instance, IImage backBufferI) {
        while (true) {
            AndroidPortGlobals.surfaceLock.lock();
            try {
                Surface surface = AndroidPortGlobals.surface;
                if (surface != null) {
                    BadGPU.Texture tex = backBufferI.getTextureFromTask();
                    // oops, no texture!
                    if (tex == null)
                        return;
                    if (!surface.isValid()) {
                        if (AndroidPortGlobals.debugFlag)
                            System.out.println("Surface invalid, waiting...");
                        continue;
                    }
                    if (AndroidPortGlobals.mustResetEGLWSI) {
                        AndroidPortGlobals.mustResetEGLWSI = false;
                        if (currentEGLSurface != 0)
                            BadGPUUnsafe.ANDdestroyEGLSurface(instance.pointer, currentEGLSurface);
                        currentEGLSurface = BadGPUUnsafe.ANDcreateEGLSurface(instance.pointer, surface);
                        BadGPUUnsafe.ANDoverrideSurface(instance.pointer, currentEGLSurface);
                        if (AndroidPortGlobals.debugFlag)
                            System.out.println("Created surface: " + currentEGLSurface);
                    }
                    if (currentEGLSurface != 0) {
                        if (AndroidPortGlobals.debugFlag)
                            System.out.println("Blitting to surface...");
                        BadGPUUnsafe.ANDblitToSurface(instance.pointer, tex.pointer, currentEGLSurface, AndroidPortGlobals.surfaceWidth, AndroidPortGlobals.surfaceHeight, 0, 1, 1, 0);
                        if (AndroidPortGlobals.debugFlag)
                            System.out.println("...done!");
                    }
                    return;
                } else {
                    if (AndroidPortGlobals.debugFlag)
                        System.out.println("Surface null, waiting...");
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                AndroidPortGlobals.surfaceLock.unlock();
            }
            try {
                Thread.sleep(500);
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
