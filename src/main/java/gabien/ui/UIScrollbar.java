/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IPeripherals;

/**
 * Replacement for UIVScrollbar in gabien-app-r48.
 * ... Wow, this didn't need much work to update on feb17.2018 for the Change.
 * Probably because it has *almost no state*.
 * Then again, theming is important, so it did get changed to use borders with a side of more borders.
 * Created on 08/06/17.
 */
public class UIScrollbar extends UIBorderedElement {
    public double scrollPoint = 0.0;
    public double wheelScale = 0.1;
    public final boolean vertical;

    public UIScrollbar(boolean vert, int sc) {
        super(6, Math.max(1, sc / 8), sc, sc);
        // UIBorderedElement tries to he helpful, but we don't like it
        setWantedSize(new Size(sc, sc));
        vertical = vert;
    }

    @Override
    public void renderContents(boolean blackText, IGrDriver igd) {
        Size bounds = getSize();
        int margin = (vertical ? bounds.width : bounds.height) / 8;

        if (UIBorderedElement.getMoveDownFlag(7))
            margin = 0;

        int nub = (vertical ? bounds.width : bounds.height) - (margin * 2);
        // within the nub & area, margin is repeated to add shading

        double scalingFactor = 1.0 / ((vertical ? bounds.height : bounds.width) - ((margin * 2) + nub));

        int nubX, nubY;
        if (vertical) {
            nubX = margin;
            nubY = margin + ((int) Math.ceil(scrollPoint / scalingFactor));
        } else {
            nubX = margin + ((int) Math.ceil(scrollPoint / scalingFactor));
            nubY = margin;
        }

        int n3 = nub / 3;
        // Valid values are 1, 3, 6...
        if (n3 < 3)
            n3 = 1;
        n3 = (n3 / 3) * 3;
        UIBorderedElement.drawBorder(igd, 7, n3, nubX, nubY, nub, nub);
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {

    }

    @Override
    public void handlePointerUpdate(IPointer pointer) {
        Size bounds = getSize();
        int margin = (vertical ? bounds.width : bounds.height) / 8;
        int nub = (vertical ? bounds.width : bounds.height) - (margin * 2);

        double scalingFactor = 1.0 / ((vertical ? bounds.height : bounds.width) - ((margin * 2) + nub));
        if (vertical) {
            scrollPoint = (pointer.getY() - (margin + (nub / 2))) * scalingFactor;
        } else {
            scrollPoint = (pointer.getX() - (margin + (nub / 2))) * scalingFactor;
        }
        if (scrollPoint < 0)
            scrollPoint = 0;
        if (scrollPoint > 1)
            scrollPoint = 1;
    }

    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        scrollPoint += north ? -wheelScale : wheelScale;
        if (scrollPoint < 0)
            scrollPoint = 0;
        if (scrollPoint > 1)
            scrollPoint = 1;
    }

    public void setSBSize(int sbSize) {
        setWantedSize(new Size(sbSize, sbSize));
    }
}
