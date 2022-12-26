/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.backendhelp;

import gabien.ITextEditingSession;

/**
 * Continuation of DeadDesktopPeripherals
 * Created 28th July 2022
 */
public class DeadTextEditingSession implements ITextEditingSession {
    @Override
    public String maintain(int x, int y, int w, int h, String text) {
        return text;
    }

    @Override
    public boolean isEnterJustPressed() {
        return false;
    }

    @Override
    public void endSession() {
    }

    @Override
    public boolean isSessionDead() {
        return true;
    }
}
