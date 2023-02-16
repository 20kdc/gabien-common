/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import org.eclipse.jdt.annotation.NonNull;

import gabien.IGrDriver;

/**
 * Native font base class.
 * Implement only in the gabien backend, as the backend renderer will expect this to be of some specific subclass.
 * Created 16th February 2023.
 */
public abstract class NativeFont implements IFixedSizeFont {
    @Override
    public void drawLine(IGrDriver igd, int x, int y, @NonNull char[] text, int index, int length, boolean textBlack) {
        int c = textBlack ? 0 : 255;
        igd.drawText(x, y, c, c, c, text, index, length, this);
    }
}
