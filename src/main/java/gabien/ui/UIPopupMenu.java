/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;

/**
 * Created on 12/27/16, has been modified quite a bit since then (now it just gives up and uses panels)
 */
public class UIPopupMenu extends UIPanel {
    private UIScrollLayout usl;
    public boolean canResize = false;

    public UIPopupMenu(String[] strings, final Runnable[] tilesets, int h, int sh, boolean rsz) {
        usl = new UIScrollLayout(true, sh);
        canResize = true;
        int szw = 1;
        for (int i = 0; i < strings.length; i++) {
            final int fi = i;
            UITextButton utb = new UITextButton(h, strings[i], new Runnable() {
                @Override
                public void run() {
                    optionExecute(fi);
                    tilesets[fi].run();
                }
            });
            szw = Math.max(szw, utb.getBounds().width);
            usl.panels.add(utb);
        }
        int sz = UITextButton.getRecommendedSize("", h).height;
        allElements.add(usl);
        setBounds(new Rect(0, 0, szw, sz * strings.length));
        canResize = rsz;
    }

    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd) {
        Rect b = getBounds();
        if (!canResize)
            igd.clearRect(0, 0, 0, ox, oy, b.width, b.height);
        super.updateAndRender(ox, oy, deltaTime, selected, igd);
    }

    @Override
    public void setBounds(Rect b) {
        if (canResize) {
            if (b.height != getBounds().height) {
                super.setBounds(new Rect(b.x, b.y, b.width, getBounds().height));
                usl.setBounds(new Rect(0, 0, b.width, getBounds().height));
                return;
            }
        }
        super.setBounds(b);
        usl.setBounds(new Rect(0, 0, b.width, b.height));
    }

    // Used for special behavior before an option is run (closing the menu, wasting time, closing the menu, counting kittens, closing the menu...)
    public void optionExecute(int b) {
    }
}
