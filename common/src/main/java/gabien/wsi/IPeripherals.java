/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.wsi;

import java.util.HashSet;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
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
    ITextEditingSession openTextEditingSession(@NonNull String text, boolean multiLine, int textHeight, @Nullable Function<String, String> feedback);
}
