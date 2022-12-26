/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

/**
 * Advanced HFS (Hidden Fun Stuff) Protection
 * Created on 27th July 2022
 */
public class GameThread extends Thread {
    // UI thread accesses this
    private static GameThread gameThread = null;

    public static void ensureStartedFromUIThread() {
        if (gameThread == null) {
            gameThread = new GameThread();
            gameThread.start();
        }
    }

    private GameThread() {
        super("Game Thread");
    }

    @Override
    public void run() {
        try {
            GaBIenImpl.main();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

