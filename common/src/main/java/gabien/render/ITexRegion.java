/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.render;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;

/**
 * Represents something pixels can be sampled out of.
 * This version can present different "actual underlying" IImgRegions based on the state of the batcher.
 * This is much more friendly to performance when mixing a batcher and atlas textures.
 * This is because the atlas texture can host multiple copies of common textures, stopping ABAC cycles.
 * Created 23rd June, 2023.
 */
public interface ITexRegion extends IDrawable {
    /**
     * Tries to pick an optimal source based on the current batch surface.
     */
    IImgRegion pickImgRegion(@Nullable IImage lastSurface);

    /**
     * Creates a subregion.
     */
    @NonNull ITexRegion subRegion(float x, float y, float w, float h);

    /**
     * Creates an IImage copy. Useful for tiling.
     */
    default @NonNull IImage copy(float x, float y, int w, int h) {
        // We don't want any unnecessary OSBs because they carry a lot of baggage.
        // So we make one temporarily just for the blitting code, then steal it.
        IGrDriver osb = GaBIEn.makeOffscreenBuffer(w, h, "ITexRegion.copy (OSB)");
        osb.blitScaledImage(x, y, w, h, 0, 0, w, h, this, IGrDriver.BLEND_NONE, 0);
        return osb.convertToImmutable("ITexRegion.copy (Final)");
    }

    /**
     * Creates an IImage copy. Useful for tiling.
     */
    default @NonNull IImage copy() {
        return copy(0, 0, (int) getRegionWidth(), (int) getRegionHeight());
    }

    /**
     * Performs a standard draw.
     */
    @Override
    default void draw(float x, float y, float w, float h, IGrDriver igd) {
        igd.blitScaledImage(x, y, w, h, this);
    }
}
