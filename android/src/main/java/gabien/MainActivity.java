/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends Activity implements Runnable {
	public static Thread gameThread = null;
	public static GrInDriver theMainWindow = new GrInDriver(800,  600);
	public static MainActivity last; // W:UITHREAD R:GTHREAD

	public TextboxImplObject myTIO;
    public SurfaceView mySurface;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
                x *= owner.w;
                y *= owner.h;
                owner.peripherals.gdPushEvent(mode, arg1.getPointerId(ptrI), (int) x, (int) y);
            }
        });
		setContentView(surfaceview);
		myTIO = new TextboxImplObject(this);
		mySurface = surfaceview;
		if (gameThread == null) {
			gameThread = new Thread(this);
			gameThread.start();
		}
		last = this;
	}

    @Override
	public void run() {
		try {
			GaBIenImpl.main();
		} catch (Exception e) {
			e.printStackTrace();
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
}
