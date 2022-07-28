/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.backendhelp;

import gabien.ITextEditingSession;
import gabien.uslx.append.IFunction;

/**
 * Continuation of DeadDesktopPeripherals
 * Created 28th July 2022
 */
public class DeadTextEditingSession implements ITextEditingSession {
    @Override
    public String maintain(int x, int y, int w, int h, String text, int textHeight, IFunction<String, String> feedback) {
        return text;
    }

    @Override
    public boolean isEnterJustPressed() {
        return false;
    }

    @Override
    public void endSession() {
    }
}
