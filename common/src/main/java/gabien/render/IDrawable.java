/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.render;

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
     * Attempts to draw this object to the given coordinates with the given scale.
     */
    void draw(float x, float y, float w, float h, IGrDriver target);
}
