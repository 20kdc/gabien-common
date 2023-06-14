/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import gabien.IGrDriver;
import gabien.IImage;
import gabien.render.ITexRegion;

/**
 * Regular border
 * Created 14th June, 2023.
 */
class RegularBorder implements IBorder {
    public EightPatch w1;
    public EightPatch w2;
    public EightPatch w3;
    public EightPatch w4;
    public ITexRegion stretchC1;
    public ITexRegion stretchC2;
    public ITexRegion stretchC4;
    public IImage tile;
    public int flags;

    @Override
    public void draw(IGrDriver igd, int borderWidth, int x, int y, int w, int h) {
        // This variable dates back to when this class did its own rendering.
        // This has been preserved.
        int chunkSize;
        if ((flags & ThemingCentral.BF_TILED) != 0) {
            // Bite the bullet - user *wants* tiling
            igd.blitTiledImage(x, y, w, h, tile);
            // Entire highres border space is reserved for tiling pattern.
            // Try to make the most of lowres? :(
            chunkSize = 3;
            if (borderWidth != 0)
                borderWidth = ensureBWV(Math.max(borderWidth, 3), chunkSize);
        } else {
            int eBorderWidth = borderWidth;
            if (borderWidth == 0)
                eBorderWidth = Math.min(w, h);
            chunkSize = 1;
            ITexRegion stretchRegion = stretchC1;
            if (eBorderWidth >= 2) {
                chunkSize = 2;
                stretchRegion = stretchC2;
            }
            if (eBorderWidth >= 4) {
                chunkSize = 4;
                stretchRegion = stretchC4;
            }

            borderWidth = ensureBWV(borderWidth, chunkSize);

            if ((flags & ThemingCentral.BF_CLEAR) != 0) {
                igd.clearRect(0, 0, 0, x + borderWidth, y + borderWidth, w - (borderWidth * 2), h - (borderWidth * 2));
            } else {
                igd.blitScaledImage(x + borderWidth, y + borderWidth, w - (borderWidth * 2), h - (borderWidth * 2), stretchRegion);
            }
        }

        if (borderWidth <= 0)
            return;

        EightPatch borderAsset;
        if (chunkSize == 1) {
            borderAsset = w1;
        } else if (chunkSize == 2) {
            borderAsset = w2;
        } else if (chunkSize == 3) {
            borderAsset = w3;
        } else {
            borderAsset = w4;
        }
        borderAsset.draw(igd, borderWidth, borderWidth, borderWidth, borderWidth, x, y, w, h);
    }

    @Override
    public boolean getFlag(int flag) {
        return (flags & flag) != 0;
    }

    public static int ensureBWV(int borderWidth, int chunk) {
        if (borderWidth > chunk)
            return ((borderWidth + 2) / chunk) * chunk;
        return borderWidth;
    }
}
