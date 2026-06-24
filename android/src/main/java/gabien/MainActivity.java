/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.view.Window;
import gabien.wsi.IPointer;

public class MainActivity extends Activity {
    public TextboxImplObject myTIO;
    public SurfaceView mySurface;

    private final static int MTA_DOWNMOVE = 0;
    private final static int MTA_UP = 1;
    private final static int MTA_MOUSEWHEEL = 2;
    private final static int MTA_HOVER = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // THIS IS THE VERY FIRST CODE THAT RUNS!
        // Therefore:
        // + gabien MAY OR MAY NOT have been setup yet
        // + We need to setup the debug flag for all other debug logging
        AndroidPortGlobals.debugFlag = new File("/sdcard/gabien_android_enable_debug").exists();
        //
        if (AndroidPortGlobals.debugFlag)
            System.out.println(" ~~~ onCreate ~~~ ");
        // Just get this over with as early as possible
        // (we need it for assets)
        if (AndroidPortGlobals.applicationContext == null)
            AndroidPortGlobals.applicationContext = getApplicationContext();
        // Actual MainActivity stuff
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        SurfaceView surfaceview = new SurfaceView(this);
        surfaceview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                GrInDriver theMainWindow = AndroidPortGlobals.theMainWindow;
                if (theMainWindow == null)
                    return true;
                int acto = arg1.getAction();
                int act = (acto & MotionEvent.ACTION_MASK);
                // ACTION_POINTER_INDEX_MASK
                int ptrI = (acto >> 8) & 0xFF;
                switch (act) {
                    case MotionEvent.ACTION_DOWN:
                        mapToArea(theMainWindow, MTA_DOWNMOVE, arg1, 0);
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mapToArea(theMainWindow, MTA_DOWNMOVE, arg1, ptrI);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mapToArea(theMainWindow, MTA_DOWNMOVE, arg1, ptrI);
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        mapToArea(theMainWindow, MTA_UP, arg1, ptrI);
                        break;
                    // Sent "when the last pointer leaves the screen".
                    // I hope you aren't lying.
                    case MotionEvent.ACTION_UP:
                        mapToArea(theMainWindow, MTA_UP, arg1, 0);
                        theMainWindow.peripherals.gdResetPointers();
                        break;
                }
                return true;
            }
        });
        try {
            surfaceview.setOnGenericMotionListener(new OnGenericMotionListener() {            
                @Override
                public boolean onGenericMotion(View v, MotionEvent arg1) {
                    GrInDriver theMainWindow = AndroidPortGlobals.theMainWindow;
                    if (theMainWindow == null)
                        return false;
                    int acto = arg1.getAction();
                    int act = (acto & MotionEvent.ACTION_MASK);
                    // ACTION_POINTER_INDEX_MASK
                    int ptrI = (acto >> 8) & 0xFF;
                    switch (act) {
                    case MotionEvent.ACTION_SCROLL:
                        mapToArea(theMainWindow, MTA_MOUSEWHEEL, arg1, ptrI);
                        return true;
                    case MotionEvent.ACTION_HOVER_MOVE:
                        mapToArea(theMainWindow, MTA_HOVER, arg1, ptrI);
                        return true;
                    }
                    return false;
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
            // try to catch if the function doesn't exist because API 7
        }
        SurfaceHolder sh = surfaceview.getHolder();
        sh.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (AndroidPortGlobals.debugFlag)
                    System.out.println("surfaceChanged...");
                AndroidPortGlobals.surfaceLock.lock();
                AndroidPortGlobals.mustResetEGLWSI = true;
                AndroidPortGlobals.surface = holder.getSurface();
                AndroidPortGlobals.surfaceWidth = width;
                AndroidPortGlobals.surfaceHeight = height;
                AndroidPortGlobals.surfaceLock.unlock();
                if (AndroidPortGlobals.debugFlag)
                    System.out.println("surfaceChanged [OK]");
            }
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (AndroidPortGlobals.debugFlag)
                    System.out.println("surfaceCreated");
            }
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (AndroidPortGlobals.debugFlag)
                    System.out.println("surfaceDestroyed...");
                AndroidPortGlobals.surfaceLock.lock();
                AndroidPortGlobals.mustResetEGLWSI = true;
                AndroidPortGlobals.surface = null;
                AndroidPortGlobals.surfaceLock.unlock();
                if (AndroidPortGlobals.debugFlag)
                    System.out.println("surfaceDestroyed [OK]");
            }
        });
        setContentView(surfaceview);
        myTIO = new TextboxImplObject(this);
        mySurface = surfaceview;
        if (AndroidPortGlobals.debugFlag)
            System.out.println("onCreate: Updating mainActivity...");
        AndroidPortGlobals.mainActivityLock.lock();
        AndroidPortGlobals.mainActivity = this;
        AndroidPortGlobals.mainActivityLock.unlock();
        if (AndroidPortGlobals.debugFlag)
            System.out.println("onCreate: Updated mainActivity");
        GameThread.ensureStartedFromUIThread();
    }

    private void mapToArea(GrInDriver owner, int mode, MotionEvent arg1, int ptrI) {
        float x = arg1.getX(ptrI);
        float y = arg1.getY(ptrI);
        x -= owner.displayArea.left;
        y -= owner.displayArea.top;
        x /= owner.displayArea.width();
        y /= owner.displayArea.height();
        x *= owner.wantedBackBufferW;
        y *= owner.wantedBackBufferH;
        if (mode == MTA_DOWNMOVE) {
            IPointer.PointerType typeGuess = IPointer.PointerType.Generic;
            try {
                int res = (int) arg1.getClass().getMethod("getButtonState").invoke(arg1);
                if (res == 2)
                    typeGuess = IPointer.PointerType.Right;
                if (res == 4)
                    typeGuess = IPointer.PointerType.Middle;
            } catch (Exception ex) {
                // do nothing! it may just not be supported on this OS version, that's that
            }
            owner.peripherals.gdPushPointerDownOrMove(arg1.getPointerId(ptrI), (int) x, (int) y, typeGuess);
        } else if (mode == MTA_UP) {
            owner.peripherals.gdPushPointerUp(arg1.getPointerId(ptrI), (int) x, (int) y);
        } else if (mode == MTA_MOUSEWHEEL) {
            // this should only be called if this is valid
            // AXIS_VSCROLL = 9
            float axisV = arg1.getAxisValue(9, ptrI);
            if (axisV < 0) {
                owner.peripherals.gdMouseForward((int) x, (int) y, 1, false);
            } else if (axisV > 0) {
                owner.peripherals.gdMouseForward((int) x, (int) y, -1, false);
            }
        } else if (mode == MTA_HOVER) {
            owner.peripherals.gdMouseForward((int) x, (int) y, 0, true);
        } else {
            // ?
        }
    }

    @Override
    public void onBackPressed() {
        if (myTIO.inTextboxMode) {
            myTIO.setInactive();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        // remove self so we don't raise the dead
        AndroidPortGlobals.mainActivityLock.lock();
        if (AndroidPortGlobals.mainActivity == this)
            AndroidPortGlobals.mainActivity = null;
        AndroidPortGlobals.mainActivityLock.unlock();
        // then call super
        super.onDestroy();
    }
}
