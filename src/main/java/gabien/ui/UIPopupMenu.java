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
public class UIPopupMenu extends UIElement.UIProxy {
    // Doesn't do anything right now.
    private boolean requestResize;

    public UIPopupMenu(String[] strings, final Runnable[] tilesets, int h, int sh, boolean rsz) {
        UIScrollLayout usl = new UIScrollLayout(true, sh);
        for (int i = 0; i < strings.length; i++) {
            final int fi = i;
            UITextButton utb = new UITextButton(strings[i], h, new Runnable() {
                @Override
                public void run() {
                    optionExecute(fi);
                    tilesets[fi].run();
                }
            });
            usl.panelsAdd(utb);
        }
        proxySetElement(usl, true);
        requestResize = rsz;
    }


    // Used for special behavior before an option is run (closing the menu, wasting time, closing the menu, counting kittens, closing the menu...)
    public void optionExecute(int b) {
    }
}
