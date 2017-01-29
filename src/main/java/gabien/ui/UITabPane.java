/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.IGrInDriver;

/**
 * Created on 12/28/16.
 */
public class UITabPane extends UIPanel {
    public String[] tabNames;
    public UIElement[] tabElems;
    public int tab = 0;
    public int tabTextHeight = 8;
    public boolean tabUpdated = false;
    public UITabPane(String[] strings, UIElement[] tabs, int h) {
        tabTextHeight = h;
        tabNames = strings;
        tabElems = tabs;
        allElements.add(tabs[0]);
        selectedElement = tabs[0];
        useScissoring = true;
    }

    @Override
    public void setBounds(Rect r) {
        for (UIElement tab : tabElems)
            tab.setBounds(new Rect(0, tabTextHeight + 2, r.width, r.height - (tabTextHeight + 2)));
        super.setBounds(r);
    }

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean select, IGrInDriver igd) {
        if (tabUpdated) {
            tabUpdated = false;
            igd.clearKeys();
        }
        super.updateAndRender(ox, oy, DeltaTime, select, igd);
        Rect bounds = getBounds();
        igd.clearRect(0, 0, 0, ox, oy, bounds.width, tabTextHeight + 2);
        int pos = 0;
        for (int i = 0; i < tabElems.length; i++) {
            int tabW = UILabel.getTextLength(tabNames[i], tabTextHeight) + 8;
            igd.clearRect(0, (i == tab) ? 32 : 0, ((i & 1) != 0) ? 32 : 0, ox + pos, oy, tabW, tabTextHeight + 2);
            UILabel.drawString(igd, ox + pos + 4, oy + 1, tabNames[i], true, tabTextHeight);
            pos += tabW;
        }
    }

    @Override
    public void handleClick(int x, int y, int button) {
        if (y < (tabTextHeight + 2)) {
            int pos = 0;
            for (int i = 0; i < tabElems.length; i++) {
                pos += UILabel.getTextLength(tabNames[i], tabTextHeight) + 8;
                if (x < pos) {
                    allElements.clear();
                    allElements.add(tabElems[i]);
                    if (tab != i)
                        tabUpdated = true;
                    tab = i;
                    return;
                }
            }
            return;
        }
        super.handleClick(x, y, button);
    }
}
