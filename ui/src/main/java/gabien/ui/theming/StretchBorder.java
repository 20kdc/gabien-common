/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import gabien.render.IGrDriver;
import gabien.render.ITexRegion;
import gabien.uslx.append.Rect;

/**
 * Regular border.
 * Created 14th June, 2023.
 */
public class StretchBorder implements IBorder {
    private final EightPatch w1;
    private final EightPatch w2;
    private final EightPatch w4;
    private final ITexRegion stretchC1;
    private final ITexRegion stretchC2;
    private final ITexRegion stretchC4;
    private final int flags;

    public StretchBorder(int flags, ITexRegion themesImg) {
        Rect outerRegion, innerRegion;

        outerRegion = new Rect(0, 0, 3, 3);
        innerRegion = new Rect(1, 1, 1, 1);
        w1 = new EightPatch(themesImg, outerRegion, innerRegion);

        outerRegion = new Rect(6, 0, 6, 6);
        innerRegion = new Rect(2, 2, 2, 2);
        w2 = new EightPatch(themesImg, outerRegion, innerRegion);

        outerRegion = new Rect(0, 6, 12, 12);
        innerRegion = new Rect(4, 4, 4, 4);
        w4 = new EightPatch(themesImg, outerRegion, innerRegion);
        // Setup stretch areas
        stretchC1 = themesImg.subRegion(1, 1, 1, 1);
        stretchC2 = themesImg.subRegion(8, 2, 2, 2);
        stretchC4 = themesImg.subRegion(4, 10, 4, 4);

        this.flags = flags;
    }

    @Override
    public void draw(IGrDriver igd, int borderWidth, int x, int y, int w, int h) {
        // This variable dates back to when this class did its own rendering.
        // This has been preserved.
        int chunkSize;
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

        igd.blitScaledImage(x + borderWidth, y + borderWidth, w - (borderWidth * 2), h - (borderWidth * 2), stretchRegion);

        if (borderWidth <= 0)
            return;

        EightPatch borderAsset;
        if (chunkSize == 1) {
            borderAsset = w1;
        } else if (chunkSize == 2) {
            borderAsset = w2;
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
