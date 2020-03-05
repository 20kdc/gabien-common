/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.backendhelp;

import java.util.HashSet;

import gabien.IDesktopPeripherals;

/**
 * Created on 05/03/2020.
 */
public class ProxyDesktopPeripherals<T extends IDesktopPeripherals> extends ProxyPeripherals<T> implements IDesktopPeripherals {

    @Override
    public int getMouseX() {
        return target.getMouseX();
    }

    @Override
    public int getMouseY() {
        return target.getMouseY();
    }

    @Override
    public int getMousewheelBuffer() {
        return target.getMousewheelBuffer();
    }

    @Override
    public boolean isKeyDown(int key) {
        return target.isKeyDown(key);
    }

    @Override
    public boolean isKeyJustPressed(int key) {
        return target.isKeyJustPressed(key);
    }

    @Override
    public HashSet<Integer> activeKeys() {
        return target.activeKeys();
    }

}
