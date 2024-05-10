/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.pva;

import gabien.pva.PVAFile.FrameElm;
import gabien.render.IDrawable;
import gabien.render.IGrDriver;
import gabien.ui.theming.IIcon;

/**
 * PVA Frame as a Drawable
 * Created October 26th, 2023
 */
public class PVAFrameDrawable implements IDrawable, IIcon {
    public final PVARenderer renderer;
    public final FrameElm[] frameData;

    public PVAFrameDrawable(PVARenderer renderer, int frameIdx) {
        this(renderer, renderer.pvaFile.frames[frameIdx]);
    }

    public PVAFrameDrawable(PVARenderer renderer, FrameElm[] frameData) {
        this.renderer = renderer;
        this.frameData = frameData;
    }

    @Override
    public float getRegionWidth() {
        return renderer.pvaFile.header.width;
    }

    @Override
    public float getRegionHeight() {
        return renderer.pvaFile.header.height;
    }

    @Override
    public void drawTo(float x, float y, float w, float h, IGrDriver target) {
        renderer.renderInline(frameData, target, x, y, w, h);
    }

    @Override
    public void draw(IGrDriver igd, int x, int y, int size) {
        renderer.renderInline(frameData, igd, x, y, size, size);
    }
}
