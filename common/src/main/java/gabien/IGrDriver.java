/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.render.ITexRegion;

/**
 * Represents a buffer that can be drawn to.
 * Created on 04/06/17.
 */
public interface IGrDriver extends IImage {
    // These return the size of the drawing buffer.
    int getWidth();
    // These return the size of the drawing buffer.
    int getHeight();

    // Basic blit operations.
    void blitImage(float srcx, float srcy, float srcw, float srch, float x, float y, ITexRegion i);
    void blitTiledImage(float x, float y, float w, float h, IImage cachedTile);
    void blitScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, ITexRegion i);

    /**
     * These two operations can be considered the logical basis of all the blit-series operations in IGrDriver.
     * (This isn't how it actually works ever since Vopeks, but it's close.)
     * The 'blend' variant performs additive or subtractive blending, while blit uses regular blending.
     * Importantly, here's how rotation is applied:
     * Firstly, the image is placed as if no rotation were involved.
     * Then the destination is rotated anticlockwise by angle degrees.
     */
    void blitRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, ITexRegion i);
    void blendRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, ITexRegion i, boolean blendSub);

    void clearAll(int i, int i0, int i1);

    default void clearRect(int r, int g, int b, float x, float y, float width, float height) {
        clearRectAlpha(r, g, b, 255, x, y, width, height);
    }

    void clearRectAlpha(int r, int g, int b, int a, float x, float y, float width, float height);

    /**
     * Stop all drawing operations. Makes an OsbDriver unusable.
     */
    void shutdown();

    // Gets the local ST buffer, in the order: translateX, translateY, cropL, cropU, cropR, cropD
    // The crop coordinates are independent of the translation coordinates.
    int[] getLocalST();
    // Propagates changes to the local ST buffer (changes only take effect from this point on)
    void updateST();
}
