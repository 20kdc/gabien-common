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
    private boolean shortTabs = false;

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
        int tl = 0;
        for (int i = 0; i < tabElems.length; i++) {
            UIElement tab = tabElems[i];
            tab.setBounds(new Rect(0, tabTextHeight + 2, r.width, r.height - (tabTextHeight + 2)));
            tl += UILabel.getTextLength(tabNames[i], tabTextHeight) + 8;
        }
        shortTabs = tl > r.width;
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
        igd.clearRect(16, 16, 16, ox, oy, bounds.width, tabTextHeight + 2);
        igd.clearRect(32, 32, 32, ox, oy + (tabTextHeight / 2), bounds.width, 2);
        int pos = 0;
        for (int i = 0; i < tabElems.length; i++) {
            int tabW = UILabel.getTextLength(getVisibleTabName(i), tabTextHeight) + 8;
            int base = ((i & 1) != 0) ? 64 : 32;
            if (i == tab)
                base = 128;
            igd.clearRect(base, base, base, ox + pos, oy, tabW, tabTextHeight + 2);
            // use a margin to try and still provide a high-contrast display despite the usability 'improvements' making the tabs brighter supposedly provides
            igd.clearRect(base / 2, base / 2, base / 2, ox + pos + 1, oy + 1, tabW - 2, tabTextHeight);

            UILabel.drawString(igd, ox + pos + 4, oy + (tabTextHeight / 8), getVisibleTabName(i), true, tabTextHeight);
            pos += tabW;
        }
    }

    private String getVisibleTabName(int i) {
        if (shortTabs)
            if (tabNames[i].length() > 0)
                return tabNames[i].substring(0, 1);
        return tabNames[i];
    }

    @Override
    public void handleClick(int x, int y, int button) {
        if (y < (tabTextHeight + 2)) {
            int pos = 0;
            for (int i = 0; i < tabElems.length; i++) {
                pos += UILabel.getTextLength(getVisibleTabName(i), tabTextHeight) + 8;
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
