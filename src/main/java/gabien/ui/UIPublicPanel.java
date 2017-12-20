/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;
import gabien.IImage;

/**
 * Subclass of UIPanel for use in inner classes and such
 * Created on 15th December 2017
 */
public class UIPublicPanel extends UIPanel {
    public IImage baseImage; // I forgot this existed. Whoops.
    public int imageX, imageY;
    public boolean imageScale;
    public int imageSW, imageSH;

    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean select, IGrInDriver igd) {
        if (baseImage != null) {
            Rect bounds = getBounds();
            if (!imageScale) {
                igd.blitImage(imageX, imageY, bounds.width, bounds.height, ox, oy, baseImage);
            } else {
                igd.blitScaledImage(imageX, imageY, imageSW, imageSH, ox, oy, bounds.width, bounds.height, baseImage);
            }
        }
        super.updateAndRender(ox, oy, deltaTime, select, igd);
    }

    public void clearElements() {
        allElements.clear();
        selectedElement = null;
    }

    public void addElement(UIElement uie) {
        if (!allElements.contains(uie))
            allElements.add(uie);
    }
}
