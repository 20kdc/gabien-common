/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui.theming;

import gabien.IGrDriver;
import gabien.render.ITexRegion;
import gabien.ui.Rect;

/**
 * This is a "pure" eight-patch border.
 * The interior isn't handled because that requires dealing the specifics of tiling.
 * Created February 17th, 2023
 */
public final class EightPatch {
    /**
     * Outer region. All pixels of the patch fit in this region.
     */
    public final Rect outerRegion;

    /**
     * Inner region. This is relative to the outer region.
     */
    public final Rect innerRegion;

    /**
     * Regions:
     * 012
     * 3.5
     * 678
     */
    public final ITexRegion region0, region1, region2, region3, region5, region6, region7, region8;

    /**
     * Edge widths/heights.
     */
    public final int w0, h0, w2, h2;

    /**
     * @param image Image
     * @param or Outer region
     * @param ir Inner region (relative to outer)
     */
    public EightPatch(ITexRegion image, Rect or, Rect ir) {
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
        int x0 = outerRegion.x;
        int x1 = x0 + innerRegion.x + nudgeX1;
        int x2 = x1 + innerRegion.width - nudgeX1;
        // Y
        int y0 = outerRegion.y;
        int y1 = y0 + innerRegion.y + nudgeY1;
        int y2 = y1 + innerRegion.height - nudgeY1;
        // 0-1-2-3
        //  0 1 2
        // W
        w0 = innerRegion.x;
        int w1 = emptyW ? 2 : innerRegion.width;
        w2 = outerRegion.width - (innerRegion.x + innerRegion.width);
        // H
        h0 = innerRegion.y;
        int h1 = emptyH ? 2 : innerRegion.height;
        h2 = outerRegion.height - (innerRegion.y + innerRegion.height);

        region0 = image.subRegion(x0, y0, w0, h0);
        region1 = image.subRegion(x1, y0, w1, h0);
        region2 = image.subRegion(x2, y0, w2, h0);

        region3 = image.subRegion(x0, y1, w0, h1);
        // 4 missing
        region5 = image.subRegion(x2, y1, w2, h1);

        region6 = image.subRegion(x0, y2, w0, h2);
        region7 = image.subRegion(x1, y2, w1, h2);
        region8 = image.subRegion(x2, y2, w2, h2);
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
        igd.blitScaledImage(tx0, ty0, tw0, th0, region0);
        igd.blitScaledImage(tx1, ty0, tw1, th0, region1);
        igd.blitScaledImage(tx2, ty0, tw2, th0, region2);
        // 3.5
        igd.blitScaledImage(tx0, ty1, tw0, th1, region3);
        igd.blitScaledImage(tx2, ty1, tw2, th1, region5);
        // 678
        igd.blitScaledImage(tx0, ty2, tw0, th2, region6);
        igd.blitScaledImage(tx1, ty2, tw1, th2, region7);
        igd.blitScaledImage(tx2, ty2, tw2, th2, region8);
    }
}
