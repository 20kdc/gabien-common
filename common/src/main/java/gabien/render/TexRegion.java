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
 * Translating ITexRegion implementation.
 * Created 14th June, 2023.
 */
public class TexRegion implements ITexRegion {
    public final @NonNull ITexRegion base;
    public final float x, y, w, h;
    public TexRegion(ITexRegion base, float x, float y, float w, float h) {
        this.base = base;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
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
        return base.getS(this.x + x, this.y + y);
    }

    @Override
    public float getT(float x, float y) {
        return base.getT(this.x + x, this.y + y);
    }

    @Override
    @NonNull
    public IImage getSurface() {
        return base.getSurface();
    }

    @Override
    @NonNull
    public TexRegion subRegion(float x, float y, float w, float h) {
        return new TexRegion(base, this.x + x, this.y + y, w, h);
    }
}
