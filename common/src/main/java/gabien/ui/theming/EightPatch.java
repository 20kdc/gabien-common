/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import gabien.IGrDriver;
import gabien.IImage;
import gabien.ui.Rect;

/**
 * This is a "pure" eight-patch border.
 * The interior isn't handled because that requires dealing the specifics of tiling.
 * Created February 17th, 2023
 */
public final class EightPatch {
    /**
     * Graphical contents.
     */
    public final IImage image;

    /**
     * Outer region. All pixels of the patch fit in this region.
     */
    public final Rect outerRegion;

    /**
     * Inner region. This is relative to the outer region.
     */
    public final Rect innerRegion;

    /**
     * X/Y coordinates at various points.
     */
    private final int x0, x1, x2, y0, y1, y2;

    /**
     * widths/heights at various points
     */
    private final int w0, w1, w2, h0, h1, h2;

    /**
     * @param image Image
     * @param or Outer region
     * @param ir Inner region (relative to outer)
     */
    public EightPatch(IImage image, Rect or, Rect ir) {
        this.image = image;
        outerRegion = or;
        innerRegion = ir;
        // Beware: A cheap trick to preserve from the bad ol' days is that of "fixing an empty middle".
        // Basically, the 1-segments IN PARTICULAR (these need to NOT alter the 0 or 2 segments)...
        //  ... are made to overlap the seams between the corner rects (1 pixel on either side).
        // This sounds bad, until you put it in practice - then it's just a reduction in the theme image size.
        boolean emptyW = ir.width == 0; 
        boolean emptyH = ir.height == 0;
        int nudgeX1 = emptyW ? -1 : 0;
        int nudgeY1 = emptyH ? -1 : 0;
        // 0-1-2-3
        x0 = outerRegion.x;
        x1 = x0 + innerRegion.x + nudgeX1;
        x2 = x1 + innerRegion.width - nudgeX1;
        // Y
        y0 = outerRegion.y;
        y1 = y0 + innerRegion.y + nudgeY1;
        y2 = y1 + innerRegion.height - nudgeY1;
        // 0-1-2-3
        //  0 1 2
        // W
        w0 = innerRegion.x;
        w1 = emptyW ? 2 : innerRegion.width;
        w2 = outerRegion.width - (innerRegion.x + innerRegion.width);
        // H
        h0 = innerRegion.y;
        h1 = emptyH ? 2 : innerRegion.height;
        h2 = outerRegion.height - (innerRegion.y + innerRegion.height);
    }

    /**
     * Draw this eight-patch at the given integer scale.
     */
    public final void draw(IGrDriver igd, int bs, int tx0, int ty0, int w, int h) {
        int tw0 = w0 * bs;
        int tw2 = w2 * bs;
        int th0 = h0 * bs;
        int th2 = h2 * bs;
        draw(igd, tw0, tw2, th0, th2, tx0, ty0, w, h);
    }

    /**
     * Draw this eight-patch with the given widths and heights for each edge. 
     */
    public final void draw(IGrDriver igd, int tw0, int tw2, int th0, int th2, int tx0, int ty0, int w, int h) {
        // target widths/heights
        int tw1 = w - (tw0 + tw2);
        int th1 = h - (th0 + th2);

        // target points
        int tx1 = tx0 + tw0;
        int tx2 = (tx0 + w) - tw2;

        int ty1 = ty0 + th0;
        int ty2 = (ty0 + h) - th2;

        // now draw all 8 patches
        // 012
        igd.blitScaledImage(x0, y0, w0, h0, tx0, ty0, tw0, th0, image);
        igd.blitScaledImage(x1, y0, w1, h0, tx1, ty0, tw1, th0, image);
        igd.blitScaledImage(x2, y0, w2, h0, tx2, ty0, tw2, th0, image);
        // 3.4
        igd.blitScaledImage(x0, y1, w0, h1, tx0, ty1, tw0, th1, image);
        igd.blitScaledImage(x2, y1, w2, h1, tx2, ty1, tw2, th1, image);
        // 567
        igd.blitScaledImage(x0, y2, w0, h2, tx0, ty2, tw0, th2, image);
        igd.blitScaledImage(x1, y2, w1, h2, tx1, ty2, tw1, th2, image);
        igd.blitScaledImage(x2, y2, w2, h2, tx2, ty2, tw2, th2, image);
    }
}
