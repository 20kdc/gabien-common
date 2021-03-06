/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IImage;
import gabien.IPeripherals;

/**
 * Subclass of UIPanel for use in inner classes and such
 * Created on 15th December 2017
 */
public class UIPublicPanel extends UIElement.UIPanel {
    public IImage baseImage; // I forgot this existed. Whoops.
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
    public void render(IGrDriver igd) {
        if (baseImage != null) {
            Size bounds = getSize();
            if (!imageScale) {
                igd.blitImage(imageX, imageY, bounds.width, bounds.height, 0, 0, baseImage);
            } else {
                igd.blitScaledImage(imageX, imageY, imageSW, imageSH, 0, 0, bounds.width, bounds.height, baseImage);
            }
        }
        super.render(igd);
    }
}
