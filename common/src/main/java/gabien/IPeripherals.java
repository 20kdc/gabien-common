/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.uslx.append.*;
import gabien.ui.IPointer;

import java.util.HashSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The minimum input peripheral interfaces needed for GaBIEn to operate properly.
 */
public interface IPeripherals {
    void performOffset(int x, int y);
    void clearOffset();

    // NOTE REGARDING THIS:
    // When a pointer ought to be created, it MUST STAY AROUND for at least until one of these calls occurs.
    HashSet<IPointer> getActivePointers();

    // This will clear all key-related buffers.
    // This includes maintained textboxes and the mouse.
    void clearKeys();

    /**
     * Starts a new text editing session (any previous session will automatically be ended).
     * feedback provides live editing feedback.
     */
    ITextEditingSession openTextEditingSession(@NonNull String text, boolean multiLine, int textHeight, @Nullable IFunction<String, String> feedback);
}
