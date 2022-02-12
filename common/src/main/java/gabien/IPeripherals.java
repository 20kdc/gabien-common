/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.ui.IFunction;
import gabien.ui.IPointer;

import java.util.HashSet;

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

    // Must be called once every frame to maintain a textbox.
    // Only one can be maintained at a given time.
    // The Y position is the *centre* - the textbox will be as tall as it wants to be.
    // Note that the textbox is still hooked into key events, so make sure not to respond to anything that could ever be used in normal typing.
    // 'feedback' provides live feedback, and should be null under most circumstances.
    String maintain(int x, int y, int width, String text, IFunction<String, String> feedback);
    boolean isEnterJustPressed();
}
