/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.wsi;

import java.util.HashSet;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

/**
 * The minimum input peripheral interfaces needed for GaBIEn to operate properly.
 */
public interface IPeripherals {
    void performOffset(int x, int y);
    void clearOffset();

    /**
     * NOTE REGARDING THIS:
     * When a pointer ought to be created, it MUST STAY AROUND for at least until one of these calls occurs.
     * This also implies this call is not pure; it mutates the list by removing pointers that have been lost.
     */
    HashSet<IPointer> getActivePointers();

    /**
     * This will clear all key-related buffers.
     * This includes maintained textboxes and the mouse.
     */
    void clearKeys();

    /**
     * Starts a new text editing session (any previous session will automatically be ended).
     * feedback provides live editing feedback.
     */
    ITextEditingSession openTextEditingSession(String text, boolean multiLine, int textHeight, @Nullable Function<String, String> feedback);

    /**
     * Does a mouse appear to be connected?
     * (If so, mouse X/Y variables respond accordingly.)
     * DO NOT USE THIS FOR UI TOGGLES -- USE IDesktopPeripherals CHECK.
     * ONLY use this to determine if mouse X/Y is valid.
     * Note that if this is false, X/Y is invalid.
     * (There is an exception to this, but it is not generally relevant.)
     */
    default boolean mouseIsConnected() {
        return false;
    }

    // Mouse management.

    default int getMouseX() {
        return 0;
    }
    default int getMouseY() {
        return 0;
    }

    /**
     * Gives -1, 0, or 1. (-1 is north.)
     * This is stateful, like isKeyJustPressed.
     * If this returns any value other than 0,
     * getMouseX/getMouseY must point to a valid mouse position at a point in time >= the time of the last mousewheel event.
     */
    default int getMousewheelBuffer() {
        return 0;
    }

    default boolean isKeyDown(int key) {
        return false;
    }

    /**
     * Note that if this returns true, it won't return again for any other call for that key until another press happens.
     */
    default boolean isKeyJustPressed(int key) {
        return false;
    }

    /**
     * Creates a set of all the active keycodes (think for keybinders)
     */
    default HashSet<Integer> activeKeys() {
        return new HashSet<>();
    }
}
