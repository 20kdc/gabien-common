/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.backendhelp;

import java.util.HashSet;

import gabien.IPeripherals;
import gabien.ui.IFunction;
import gabien.ui.IPointer;

/**
 * Created on 05/03/2020.
 */
public class ProxyPeripherals<T extends IPeripherals> implements IPeripherals {
    public T target;

    @Override
    public void performOffset(int x, int y) {
        target.performOffset(x, y);
    }

    @Override
    public void clearOffset() {
        target.clearOffset();
    }

    @Override
    public HashSet<IPointer> getActivePointers() {
        return target.getActivePointers();
    }

    @Override
    public void clearKeys() {
        target.clearKeys();
    }

    @Override
    public String maintain(int x, int y, int width, String text, IFunction<String, String> feedback) {
        return target.maintain(x, y, width, text, feedback);
    }

    @Override
    public boolean isEnterJustPressed() {
        return target.isEnterJustPressed();
    }

}
