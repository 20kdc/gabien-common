/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.render;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Drawable region.
 * Created 26th October, 2023.
 */
public class DrawableRegion implements IDrawable {
    public final IDrawable parent;
    public final float regX, regY, regWidth, regHeight;
    private final float ofsX, ofsY;
    private final float widthMul, heightMul;
    private final float parentWidth, parentHeight;

    /**
     * Creates a DrawableRegion.
     * Not optimized, use IDrawable.subRegion instead unless you're sure you want this behaviour.
     */
    public DrawableRegion(IDrawable parent, float x, float y, float width, float height) {
        this.parent = parent;
        this.regX = x;
        this.regY = y;
        this.regWidth = width;
        this.regHeight = height;
        this.parentWidth = parent.getRegionWidth();
        this.parentHeight = parent.getRegionHeight();
        // If we're half the size of the parent, we need to draw the parent at double size
        this.widthMul = parentWidth / width;
        this.heightMul = parentHeight / height;
        // If we're taking from a region 1px right and down of parent, we need to move that many pixels up and left
        // We need to do this in parent space, so it'll be handled by draw after w/h have been multiplied
        this.ofsX = -x / parentWidth;
        this.ofsY = -y / parentHeight;
    }

    @Override
    public float getRegionWidth() {
        return regWidth;
    }

    @Override
    public float getRegionHeight() {
        return regHeight;
    }

    @Override
    public void drawTo(float x, float y, float w, float h, IGrDriver target) {
        w *= widthMul;
        h *= heightMul;
        x += ofsX * w;
        y += ofsY * h;
        parent.drawTo(x, y, w, h, target);
    }

    @Override
    @NonNull
    public IDrawable subRegion(float x, float y, float w, float h) {
        return new DrawableRegion(parent, regX + x, regY + y, w, h);
    }
}
