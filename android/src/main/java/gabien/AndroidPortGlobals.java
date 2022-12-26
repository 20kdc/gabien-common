/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;

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
    public static final GrInDriver theMainWindow = new GrInDriver(800, 600);
    /**
     * The activity can't be *completely* destroyed while a lock is held.
     * This is important as it ensures we don't keep any undead Activities around.
     */
    public static final ReentrantLock mainActivityLock = new ReentrantLock();
    /**
     * This is the activity. Careful with this, use mainActivityLock.
     */
    public static MainActivity mainActivity;
}
