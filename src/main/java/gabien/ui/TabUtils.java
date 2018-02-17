/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.FontManager;
import gabien.IGrDriver;

/**
 * Because frankly, the previous tab/window code sucked.
 *
 * Created on February 13th 2018.
 */
public class TabUtils {
    public static int getTabWidth(UIWindowView.WVWindow window, int shortTab, int h) {
        int margin = h / 8;
        int tabExMargin = margin + (margin / 2);
        int textHeight = h - (margin * 2);
        if (shortTab == 0)
            tabExMargin = 0;
        return FontManager.getLineLength(getVisibleTabName(window, shortTab), textHeight) + (tabExMargin * 2) + (h * window.icons.length);
    }

    public static String getVisibleTabName(UIWindowView.WVWindow w, int shortTab) {
        String name = w.contents.toString();
        if (shortTab != -1)
            return name.substring(0, Math.min(name.length(), shortTab));
        return name;
    }

    /*
     * > function getMargin(i) return math.floor(i / 8) end
     * > function getHeight(i) return i + (math.floor(i / 8) * 2) end
     * > function getRoundtrip(i) return getHeight(i) - (getMargin(i) * 2) end
     * > for i = 1, 128 do print(i, getRoundtrip(i)) end
     */
    public static int getHeight(int h) {
        return h + ((h / 8) * 2);
    }

    public static void drawTab(int base, int inner, int x, int y, int w, int h, IGrDriver igd, String text, UIWindowView.IWVWindowIcon[] icons) {
        int margin = h / 8;
        int textHeight = h - (margin * 2);
        int tabExMargin = margin + (margin / 2);
        int tabIcoMargin = h / 4;

        igd.clearRect(base, base, base, x, y, w, h);
        // use a margin to try and still provide a high-contrast display despite the usability 'improvements' making the tabs brighter supposedly provides
        igd.clearRect(inner, inner, inner, x + margin, y + margin, w - (margin * 2), h - (margin * 2));

        FontManager.drawString(igd, x + tabExMargin, y + tabExMargin, text, true, textHeight);

        int icoBack = h;
        for (UIWindowView.IWVWindowIcon i : icons) {
            // sometimes too bright, deal with that
            int size = h - (tabIcoMargin * 2);
            int subMargin = tabIcoMargin / 2;
            igd.clearRect(0, 0, 0, x + w - ((icoBack - tabIcoMargin) + subMargin), y + tabIcoMargin - subMargin, size + (subMargin * 2), size + (subMargin * 2));
            i.draw(igd, x + w - (icoBack - tabIcoMargin), y + tabIcoMargin, size);
            icoBack += h;
        }
    }

    public static boolean clickInTab(UIWindowView.WVWindow wn, int x, int y, int w, int h) {
        int tabIcoMargin = h / 4;

        int icoBack = h;
        for (UIWindowView.IWVWindowIcon i : wn.icons) {
            // sometimes too bright, deal with that
            int size = h - (tabIcoMargin * 2);
            Rect rc = new Rect(w - (icoBack - tabIcoMargin), tabIcoMargin, size, size);
            if (rc.contains(x, y)) {
                i.click();
                return true;
            }
            icoBack += h;
        }
        return false;
    }
}
