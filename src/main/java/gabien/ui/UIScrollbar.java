/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package gabien.ui;

import gabien.IGrInDriver;

/**
 * Replacement for UIVScrollbar in gabien-app-r48.
 * Created on 08/06/17.
 */
public class UIScrollbar extends UIElement {
    public double scrollPoint = 0.0;
    public final boolean vertical;

    public UIScrollbar(boolean vert) {
        vertical = vert;
        setBounds(new Rect(0, 0, 32, 32));
    }

    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd) {
        Rect bounds = getBounds();
        int margin = (vertical ? bounds.width : bounds.height) / 8;
        int nub = (vertical ? bounds.width : bounds.height) - (margin * 2);

        double scalingFactor = 1.0 / ((vertical ? bounds.height : bounds.width) - ((margin * 2) + nub));

        igd.clearRect(64, 64, 64, ox, oy, bounds.width, bounds.height);
        igd.clearRect(32, 32, 32, ox + margin, oy + margin, bounds.width - (margin * 2), bounds.height - (margin * 2));
        if (vertical) {
            igd.clearRect(255, 255, 255, ox + margin, oy + margin + ((int) (scrollPoint / scalingFactor)), nub, nub);
        } else {
            igd.clearRect(255, 255, 255, ox + margin + ((int) (scrollPoint / scalingFactor)), oy + margin, nub, nub);
        }
    }

    @Override
    public void handleClick(int x, int y, int button) {
        // Do nothing.
    }

    @Override
    public void handleDrag(int x, int y) {
        Rect bounds = getBounds();
        int margin = (vertical ? bounds.width : bounds.height) / 8;
        int nub = (vertical ? bounds.width : bounds.height) - (margin * 2);

        double scalingFactor = 1.0 / ((vertical ? bounds.height : bounds.width) - ((margin * 2) + nub));
        if (vertical) {
            scrollPoint = (y - (margin + (nub / 2))) * scalingFactor;
        } else {
            scrollPoint = (x - (margin + (nub / 2))) * scalingFactor;
        }
        if (scrollPoint < 0)
            scrollPoint = 0;
        if (scrollPoint > 1)
            scrollPoint = 1;
    }
}
