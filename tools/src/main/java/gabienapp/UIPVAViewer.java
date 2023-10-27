/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import gabien.GaBIEn;
import gabien.pva.PVAFile;
import gabien.pva.PVARenderer;
import gabien.render.IGrDriver;
import gabien.ui.UIElement;
import gabien.ui.UILayer;
import gabien.uslx.append.Size;
import gabien.wsi.IPeripherals;

/**
 * Created 2nd October, 2023.
 */
public class UIPVAViewer extends UIElement {
    public final PVAFile pva;
    public final PVARenderer ren;

    public UIPVAViewer(PVAFile pf) {
        super(pf.header.width, pf.header.height);
        pva = pf;
        ren = new PVARenderer(pva);
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
    }

    @Override
    public void renderLayer(IGrDriver igd, UILayer layer) {
        if (layer != UILayer.Content)
            return;
        Size sz = getSize();
        int w = sz.width;
        int h = sz.height;
        double time = GaBIEn.getTime() * 1000;
        int whichFrameToRender = pva.frameOfLooped(time);
        if (whichFrameToRender != -1)
            ren.renderInline(pva.frames[whichFrameToRender], igd, 0, 0, w, h);
    }
}
