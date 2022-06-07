/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.backendhelp;

import java.util.HashSet;

import gabien.IDesktopPeripherals;
import gabien.ui.IPointer;
import gabien.ui.Rect;
import gabien.uslx.append.*;

/**
 * Created on 05/03/2020.
 */
public class DeadDesktopPeripherals implements IDesktopPeripherals {
    public static final DeadDesktopPeripherals INSTANCE = new DeadDesktopPeripherals(); 

    @Override
    public void performOffset(int x, int y) {
    }

    @Override
    public void clearOffset() {
    }

    @Override
    public HashSet<IPointer> getActivePointers() {
        return new HashSet<IPointer>();
    }

    @Override
    public void clearKeys() {
        
    }

    @Override
    public String maintain(int x, int y, int w, int h, String text, int textHeight, IFunction<String, String> feedback) {
        return text;
    }

    @Override
    public boolean isEnterJustPressed() {
        return false;
    }

    @Override
    public int getMouseX() {
        return 0;
    }

    @Override
    public int getMouseY() {
        return 0;
    }

    @Override
    public int getMousewheelBuffer() {
        return 0;
    }

    @Override
    public boolean isKeyDown(int key) {
        return false;
    }

    @Override
    public boolean isKeyJustPressed(int key) {
        return false;
    }

    @Override
    public HashSet<Integer> activeKeys() {
        return new HashSet<Integer>();
    }

}
