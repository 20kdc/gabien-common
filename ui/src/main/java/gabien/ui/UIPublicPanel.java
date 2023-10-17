/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.render.IGrDriver;
import gabien.render.ITexRegion;
import gabien.uslx.append.Size;

/**
 * Subclass of UIPanel for use in inner classes and such
 * Created on 15th December 2017
 */
public class UIPublicPanel extends UIElement.UIPanel {
    public ITexRegion baseImage; // I forgot this existed. Whoops.
    public int imageX, imageY;
    public boolean imageScale;
    public int imageSW, imageSH;

    public UIPublicPanel(int w, int h) {
        super(w, h);
        initialize();
    }

    // Exists to be overridden.
    public void initialize() {

    }

    @Override
    public void runLayout() {

    }

    @Override
    public void renderLayer(IGrDriver igd, UILayer layer) {
        // Luckily, it doesn't need to be on any layer lower than Legacy.
        if (layer == UILayer.Content) {
            if (baseImage != null) {
                Size bounds = getSize();
                if (!imageScale) {
                    igd.blitImage(imageX, imageY, bounds.width, bounds.height, 0, 0, baseImage);
                } else {
                    igd.blitScaledImage(imageX, imageY, imageSW, imageSH, 0, 0, bounds.width, bounds.height, baseImage);
                }
            }
        }
        super.renderLayer(igd, layer);
    }
}
