/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.render;

import org.eclipse.jdt.annotation.NonNull;

import gabien.uslx.append.Block;

/**
 * Something that can be drawn to the screen in a "standard" mode.
 * Note that this is not very customizable - it's intended for flexibility of implementation.
 * A Drawable may be vector graphics or bitmaps or multiple overlaid bitmaps or all sorts of things.
 * Created 26th October, 2023.
 */
public interface IDrawable {
    /**
     * Gets the theoretical width of this region.
     * This is metadata only and not necessarily accurate.
     * In theory, the region encompasses 0 to width.
     */
    float getRegionWidth();

    /**
     * Gets the theoretical width of this region.
     * This is metadata only and not necessarily accurate.
     * In theory, the region encompasses 0 to height.
     */
    float getRegionHeight();

    /**
     * Attempts to create a subregion.
     * Notably, subregions of IDrawable do not necessarily bound their contents properly.
     * Their main use is when the content is known to be bounded, but has the wrong aspect ratio.
     * In this event a "super-region" may be created to letterbox the Drawable.
     * Or a "sub-region" may be created to slice off unused areas (pan-and-scan kinda deal).
     * Note that this is specifically referring to how this all works for IDrawable.
     * It doesn't so much apply to, say, ITexRegion, which is usually cut up more due to atlasing/spritesheets.
     */
    default @NonNull IDrawable subRegion(float x, float y, float w, float h) {
        return new DrawableRegion(this, x, y, w, h);
    }

    /**
     * Attempts to draw this object to the given coordinates with the given size.
     * This should ideally be treated as a simple scale.
     * Algorithms like fitting should be built on top of Drawable.
     */
    void drawTo(float x, float y, float w, float h, IGrDriver igd);

    /**
     * Draws this Drawable scissored.
     * Drawables that always keep their contents within their bounds can override this to alias to draw.
     * Importantly, drawScissored is not overridden by subregions.
     * This ensures that the bounds you specify are applied (they aren't transformed for subregions).
     */
    default void drawScissoredTo(float xf, float yf, float wf, float hf, IGrDriver igd) {
        try (Block sc = igd.openScissor(xf, yf, wf, hf)) {
            drawTo(xf, yf, wf, hf, igd);
        }
    }
}
