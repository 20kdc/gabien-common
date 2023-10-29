/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.dialogs;

import gabien.ui.UIElement;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UIScrollLayout;
import gabien.uslx.append.ArrayIterable;

/**
 * Created on 12/27/16, has been modified quite a bit since then (now it just gives up and uses panels)
 */
public class UIPopupMenu extends UIElement.UIProxy {
    // Doesn't do anything right now.
    @SuppressWarnings("unused")
    private boolean requestResize;

    /**
     * Constructor given an array of entries.
     * This isn't likely to come up in practice as the String[] / Runnable[] constructor is likely to be used instead.
     */
    public UIPopupMenu(Entry[] entries, int h, int sh, boolean rsz) {
        this(new ArrayIterable<Entry>(entries), h, sh, rsz);
    }

    /**
     * Constructor given a list (or similar) of entries.
     */
    public UIPopupMenu(Iterable<Entry> entries, int h, int sh, boolean rsz) {
        UIScrollLayout usl = new UIScrollLayout(true, sh);
        int i = 0;
        for (final Entry ent : entries) {
            final int fi = i++;
            UITextButton utb = new UITextButton(ent.text, h, () -> {
                optionExecute(fi);
                ent.action.run();
            });
            usl.panelsAdd(utb);
        }
        usl.panelsFinished();
        proxySetElement(usl, true);
        requestResize = rsz;
    }

    /**
     * Constructor given a popup menu.
     */
    public UIPopupMenu(String[] strings, final Runnable[] tilesets, int h, int sh, boolean rsz) {
        UIScrollLayout usl = new UIScrollLayout(true, sh);
        for (int i = 0; i < strings.length; i++) {
            final int fi = i;
            UITextButton utb = new UITextButton(strings[i], h, () -> {
                optionExecute(fi);
                tilesets[fi].run();
            });
            usl.panelsAdd(utb);
        }
        usl.panelsFinished();
        proxySetElement(usl, true);
        requestResize = rsz;
    }


    // Used for special behavior before an option is run (closing the menu, wasting time, closing the menu, counting kittens, closing the menu...)
    public void optionExecute(int b) {
    }

    public static final class Entry {
        public final String text;
        public final Runnable action;
        public Entry(String txt, Runnable a) {
            text = txt;
            action = a;
        }
    }
}
