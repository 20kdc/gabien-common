/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
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
        int szw = 1;
        for (String s : strings)
            szw = Math.max(szw, UILabel.getRecommendedSize(s, textHeight).width);
        int sz = UILabel.getRecommendedSize("", textHeight).height;
        setBounds(new Rect(0, 0, szw, sz * strings.length));
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
