/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import gabien.IGrDriver;

/**
 * Interface for borders, to try and clean this all up...
 * Created 14th June, 2023.
 */
public interface IBorder {
    /**
     * Draws the border.
     */
    void draw(IGrDriver igd, int borderWidth, int x, int y, int w, int h);

    /**
     * Border flags
     */
    boolean getFlag(int flag);
}
