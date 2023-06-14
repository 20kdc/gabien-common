/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.render;

import org.eclipse.jdt.annotation.NonNull;

import gabien.IImage;

/**
 * Represents something pixels can be sampled out of.
 * Created 14th June, 2023.
 */
public interface ITexRegion {
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
     * Gets the underlying image of this texture region.
     */
    @NonNull IImage getSurface();

    /**
     * Returns the texture S value for a given input coordinate.
     */
    float getS(float x, float y);

    /**
     * Returns the texture T value for a given input coordinate.
     */
    float getT(float x, float y);
}
