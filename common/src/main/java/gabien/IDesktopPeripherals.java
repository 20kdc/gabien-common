/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import java.util.HashSet;

/**
 * Used to represent desktop peripherals from an IGrInDriver.
 * May never have to be given.
 */
public interface IDesktopPeripherals extends IPeripherals {
    // Mouse management.
    int getMouseX();
    int getMouseY();
    // Gives -1, 0, or 1. (-1 is north.)
    int getMousewheelBuffer();

    boolean isKeyDown(int key);

    // Note that if this returns
    // true,it won't return true
    // again for any other call for
    // that key until another press
    // happens.
    boolean isKeyJustPressed(int key);

    // Creates a list of all the active keycodes (think for keybinders)
    HashSet<Integer> activeKeys();
}
