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
    public boolean x2 = false;
    public UIPopupMenu(String[] strings, Runnable[] tilesets, boolean ex2, boolean rsz) {
        menuItems = strings;
        menuExecs = tilesets;
        x2 = ex2;
        canResize = true;
        setBounds(new Rect(0, 0, 320, 10));
        canResize = rsz;
    }

    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd) {
        Rect b = getBounds();
        if (!canResize)
            igd.clearRect(0, 0, 0, ox, oy, b.width, b.height);
        int sz = x2 ? 18 : 9;
        int i = 0;
        int mx = igd.getMouseX();
        int my = igd.getMouseY();
        int selectedIdx = -1;
        if (mx >= ox)
            if (mx < (ox + b.width))
                selectedIdx = (my - oy) / sz;
        for (String s : menuItems) {
            if (!x2) {
                UILabel.drawLabel(igd, b.width, ox, oy, s, (i++) == selectedIdx);
            } else {
                UILabel.drawLabelx2(igd, b.width, ox, oy, s, (i++) == selectedIdx);
            }
            oy += sz;
        }
    }

    @Override
    public void setBounds(Rect b) {
        if (canResize) {
            int sz = x2 ? 18 : 9;
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
        int sz = x2 ? 18 : 9;
        int b = y / sz;
        if (b < 0)
            return;
        if (b >= menuItems.length)
            return;
        menuExecs[b].run();
    }
}
