/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import gabien.render.IGrDriver;
import gabien.render.IImage;
import gabien.render.ITexRegion;
import gabien.ui.Rect;

/**
 * Regular border
 * Split from RegularBorder 15th June, 2023.
 */
public class TiledBorder implements IBorder {
    private final EightPatch w3;
    private final IImage tile;
    private final int flags;

    public TiledBorder(int flags, ITexRegion themesImg) {
        this.flags = flags;
        Rect outerRegion, innerRegion;

        outerRegion = new Rect(6, 0, 6, 6);
        innerRegion = new Rect(3, 3, 0, 0);
        w3 = new EightPatch(themesImg, outerRegion, innerRegion);

        // Extract tile
        tile = themesImg.copy(0, 6, 12, 12);
    }

    @Override
    public void draw(IGrDriver igd, int borderWidth, int x, int y, int w, int h) {
        // Bite the bullet - user *wants* tiling
        igd.blitTiledImage(x, y, w, h, tile);
        // Entire highres border space is reserved for tiling pattern.
        // Try to make the most of lowres? :(
        if (borderWidth != 0)
            borderWidth = RegularBorder.ensureBWV(Math.max(borderWidth, 3), 3);

        if (borderWidth <= 0)
            return;

        w3.draw(igd, borderWidth, borderWidth, borderWidth, borderWidth, x, y, w, h);
    }

    @Override
    public boolean getFlag(int flag) {
        return (flags & flag) != 0;
    }
}
