/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;

/**
 * NOTE: The layout of this keeps in mind that 1/8th of the text height is spacing (rounding up)
 * Created on 12/28/16.
 */
public class UITabPane extends UIPanel {
    public String[] tabNames;
    public UIElement[] tabElems;
    // Don't write to this. Might change to getter in future
    public int tab = 0;
    private boolean tabUpdated = false;
    public final int tabTextHeight;
    private boolean shortTabs = false;
    // This is used as the basis for actual calculations.
    public int tabBarHeight = 0;
    public int padding = 0;

    public UITabPane(String[] strings, UIElement[] tabs, int h) {
        tabTextHeight = h;
        tabNames = strings;
        tabElems = tabs;
        allElements.add(tabs[0]);
        selectedElement = tabs[0];
        useScissoring = true;
        padding = ((h + 7) / 8);
        tabBarHeight = tabTextHeight + padding + 2; // +2 for additional border
    }

    @Override
    public void setBounds(Rect r) {
        int tl = 0;
        for (int i = 0; i < tabElems.length; i++) {
            UIElement tab = tabElems[i];
            tab.setBounds(new Rect(0, tabBarHeight, r.width, r.height - tabBarHeight));
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
        igd.clearRect(16, 16, 16, ox, oy, bounds.width, tabBarHeight);
        // There would be "oy + 1" instead of just "oy", but ofc it has to be centred, so it would just be -1'd again...
        igd.clearRect(32, 32, 32, ox, oy + ((tabTextHeight + padding) / 2), bounds.width, 2);
        int pos = 0;
        for (int i = 0; i < tabElems.length; i++) {
            int tabW = UILabel.getTextLength(getVisibleTabName(i), tabTextHeight) + 8;
            int base = ((i & 1) != 0) ? 64 : 32;
            if (i == tab)
                base = 128;
            int margin = tabTextHeight / 6;
            igd.clearRect(base, base, base, ox + pos, oy, tabW, tabBarHeight);
            // use a margin to try and still provide a high-contrast display despite the usability 'improvements' making the tabs brighter supposedly provides
            igd.clearRect(base / 2, base / 2, base / 2, ox + pos + margin, oy + margin, tabW - (margin * 2), (tabTextHeight + padding + 2) - (margin * 2));

            UILabel.drawString(igd, ox + pos + 4, oy + 1 + padding, getVisibleTabName(i), true, tabTextHeight);
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
        if (y < tabBarHeight) {
            int pos = 0;
            for (int i = 0; i < tabElems.length; i++) {
                pos += UILabel.getTextLength(getVisibleTabName(i), tabTextHeight) + 8;
                if (x < pos) {
                    selectTab(i);
                    break;
                }
            }
        }
        super.handleClick(x, y, button);
    }

    public void selectTab(int i) {
        allElements.clear();
        allElements.add(tabElems[i]);
        if (tab != i)
            tabUpdated = true;
        tab = i;
    }
}
