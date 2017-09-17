/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.IGrInDriver;

/**
 * Created on 12/27/16.
 */
public class UIPopupMenu extends UIElement {
    public String[] menuItems;
    public Runnable[] menuExecs;
    public boolean canResize = false;
    public int textHeight;

    public UIPopupMenu(String[] strings, Runnable[] tilesets, int h, boolean rsz) {
        menuItems = strings;
        menuExecs = tilesets;
        textHeight = h;
        canResize = true;
        setBounds(new Rect(0, 0, 320, 10));
        canResize = rsz;
    }

    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd) {
        Rect b = getBounds();
        if (!canResize)
            igd.clearRect(0, 0, 0, ox, oy, b.width, b.height);
        int sz = UILabel.getRecommendedSize("", textHeight).height;
        int i = 0;
        int mx = igd.getMouseX();
        int my = igd.getMouseY();
        int selectedIdx = -1;
        if (mx >= ox)
            if (mx < (ox + b.width))
                selectedIdx = UIElement.sensibleCellDiv(my - oy, sz);
        for (String s : menuItems) {
            UILabel.drawLabel(igd, b.width, ox, oy, s, ((i++) == selectedIdx) ? 2 : 1, textHeight);
            oy += sz;
        }
    }

    @Override
    public void setBounds(Rect b) {
        if (canResize) {
            int sz = UILabel.getRecommendedSize("", textHeight).height;
            if (b.height != (menuItems.length * sz)) {
                super.setBounds(new Rect(b.x, b.y, b.width, menuItems.length * sz));
            } else {
                super.setBounds(b);
            }
        } else {
            super.setBounds(b);
        }
    }

    @Override
    public void handleClick(int x, int y, int button) {
        int sz = UILabel.getRecommendedSize("", textHeight).height;
        int b = UIElement.sensibleCellDiv(y, sz);
        if (b < 0)
            return;
        if (b >= menuItems.length)
            return;
        optionExecute(b);
    }

    // Used for special behavior when an option is run (closing the menu, wasting time, closing the menu, counting kittens, closing the menu...)
    public void optionExecute(int b) {
        menuExecs[b].run();
    }
}
