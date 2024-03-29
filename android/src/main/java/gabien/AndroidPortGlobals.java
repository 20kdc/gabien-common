/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.view.Surface;

/**
 * Created on 27th July 2022 (past midnight so "sort of" 28th)
 */
public class AndroidPortGlobals {
    /**
     * This is initialized once and only once from MainActivity.
     * It should survive forever.
     */
    public static Context applicationContext;
    /**
     * This is the rendering surface. It also contains the GameThread-side peripheral code.
     */
    public static GrInDriver theMainWindow;
    /**
     * The activity can't be *completely* destroyed while a lock is held.
     * This is important as it ensures we don't keep any undead Activities around.
     */
    public static final ReentrantLock mainActivityLock = new ReentrantLock();
    /**
     * This is the activity. Careful with this, use mainActivityLock.
     */
    public static MainActivity mainActivity;
    /**
     * Debug flag.
     */
    public static boolean debugFlag = false;
    /**
     * This is a dedicated lock tied to the SurfaceHolder apparatus.
     */
    public static final ReentrantLock surfaceLock = new ReentrantLock();
    /**
     * Current surface. Secured by surfaceLock.
     */
    public static volatile Surface surface;
    /**
     * Flag used to indicate EGLWSI must be reset.
     * The actual Surface object seems to get reused, so this is how MainActivity has to notify GrInDriver.
     */
    public static volatile boolean mustResetEGLWSI = true;
    /**
     * Width and height of surface.
     */
    public static volatile int surfaceWidth, surfaceHeight;
}
