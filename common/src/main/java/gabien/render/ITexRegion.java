/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.render;

import org.eclipse.jdt.annotation.NonNull;

import gabien.GaBIEn;
import gabien.IGrDriver;
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

    /**
     * Creates a subregion.
     */
    default @NonNull ITexRegion subRegion(float x, float y, float w, float h) {
        return new TexRegion(this, x, y, w, h);
    }

    /**
     * Creates an IImage copy. Useful for tiling.
     */
    default @NonNull IImage copy(float x, float y, int w, int h) {
        // We don't want any unnecessary OSBs because they carry a lot of baggage.
        // So we make one temporarily just for the blitting code, then steal it.
        IGrDriver osb = GaBIEn.makeOffscreenBuffer(w, h, "ITexRegion.copy (OSB)");
        osb.blitImage(x, y, w, h, 0, 0, this);
        return osb.convertToImmutable("ITexRegion.copy (Final)");
    }

    /**
     * Creates an IImage copy. Useful for tiling.
     */
    default @NonNull IImage copy() {
        return copy(0, 0, (int) getRegionWidth(), (int) getRegionHeight());
    }
}
