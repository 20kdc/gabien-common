/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;

public class MainActivity extends Activity {
	public TextboxImplObject myTIO;
    public SurfaceView mySurface;

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
                int acto = arg1.getAction();
                int act = (acto & MotionEvent.ACTION_MASK);
                // ACTION_POINTER_INDEX_MASK
                int ptrI = (acto >> 8) & 0xFF;
                switch (act) {
                    case MotionEvent.ACTION_DOWN:
                        mapToArea(AndroidPortGlobals.theMainWindow, true, arg1, 0);
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mapToArea(AndroidPortGlobals.theMainWindow, true, arg1, ptrI);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        for (int i = 0; i < arg1.getPointerCount(); i++)
                            mapToArea(AndroidPortGlobals.theMainWindow, true, arg1, i);
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        mapToArea(AndroidPortGlobals.theMainWindow, false, arg1, ptrI);
                        break;
                    // Sent "when the last pointer leaves the screen".
                    // I hope you aren't lying.
                    case MotionEvent.ACTION_UP:
                        mapToArea(AndroidPortGlobals.theMainWindow, false, arg1, 0);
                        AndroidPortGlobals.theMainWindow.peripherals.gdResetPointers();
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
                x *= owner.w;
                y *= owner.h;
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
        if (AndroidPortGlobals.mainActivity == this)
            AndroidPortGlobals.mainActivity = null;
        AndroidPortGlobals.mainActivityLock.unlock();
        // then call super
        super.onDestroy();
    }
}
