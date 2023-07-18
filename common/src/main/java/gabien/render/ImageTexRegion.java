/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.render;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Translating ITexRegion implementation.
 * Notably, this class intends to be optimized, so it only works on IImages.
 * Also notably, this class can scale as a side-effect, but intentionally never directly.
 * Created 14th June, 2023.
 */
public final class ImageTexRegion implements IImgRegion {
    public final @NonNull IImage base;
    public final float x, y, w, h, sW, sH;

    /**
     * Creates the ImageTexRegion.
     * @param base Base surface.
     * @param x X of region (relative to sW/sH)
     * @param y Y of region (relative to sW/sH)
     * @param w Width metadata
     * @param h Height metadata
     * @param sW Virtual width of surface. Can be used to implement scaling.
     * @param sH Virtual height of surface. Can be used to implement scaling.
     */
    public ImageTexRegion(IImage base, float x, float y, float w, float h, float sW, float sH) {
        this.base = base;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.sW = sW;
        this.sH = sH;
    }

    @Override
    public float getRegionWidth() {
        return w;
    }

    @Override
    public float getRegionHeight() {
        return h;
    }

    @Override
    public float getS(float x, float y) {
        return (this.x + x) / sW;
    }

    @Override
    public float getT(float x, float y) {
        return (this.y + y) / sH;
    }

    @Override
    @NonNull
    public IImage getSurface() {
        return base.getSurface();
    }

    @Override
    @NonNull
    public ImageTexRegion subRegion(float x, float y, float w, float h) {
        return new ImageTexRegion(base, this.x + x, this.y + y, w, h, this.sW, this.sH);
    }
}
