/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;

public class MainActivity extends Activity {
	public TextboxImplObject myTIO;
    public SurfaceView mySurface;
    // Once you have the MainActivity (using mainActivityLock) you can transfer to this lock.
    // By transferring to this lock you can avoid holding up stuff using the mainActivityLock while doing stuff with the surface.
    // It's very important to hold that precise order, though. Never lock the surfaceLock first.
    // Otherwise deadlocks are possible.
    public final ReentrantLock surfaceLock = new ReentrantLock();

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
                        mapToArea(theMainWindow, true, arg1, 0);
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mapToArea(theMainWindow, true, arg1, ptrI);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        for (int i = 0; i < arg1.getPointerCount(); i++)
                            mapToArea(theMainWindow, true, arg1, i);
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        mapToArea(theMainWindow, false, arg1, ptrI);
                        break;
                    // Sent "when the last pointer leaves the screen".
                    // I hope you aren't lying.
                    case MotionEvent.ACTION_UP:
                        mapToArea(theMainWindow, false, arg1, 0);
                        theMainWindow.peripherals.gdResetPointers();
                        break;
                }
                return true;
            }

            private void mapToArea(GrInDriver owner, boolean mode, MotionEvent arg1, int ptrI) {
                float x = arg1.getX(ptrI);
                float y = arg1.getY(ptrI);
                x -= owner.displayArea.left;
                y -= owner.displayArea.top;
                x /= owner.displayArea.width();
                y /= owner.displayArea.height();
                x *= owner.wantedBackBufferW;
                y *= owner.wantedBackBufferH;
                owner.peripherals.gdPushEvent(mode, arg1.getPointerId(ptrI), (int) x, (int) y);
            }
        });
		setContentView(surfaceview);
		myTIO = new TextboxImplObject(this);
		mySurface = surfaceview;
        AndroidPortGlobals.mainActivityLock.lock();
        AndroidPortGlobals.mainActivity = this;
        AndroidPortGlobals.mainActivityLock.unlock();
        GameThread.ensureStartedFromUIThread();
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
        surfaceLock.lock();
        if (AndroidPortGlobals.mainActivity == this)
            AndroidPortGlobals.mainActivity = null;
        surfaceLock.unlock();
        AndroidPortGlobals.mainActivityLock.unlock();
        // then call super
        super.onDestroy();
    }
}
