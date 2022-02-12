/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

/**
 * Created on 13/08/17.
 */
public class UIAutoclosingPopupMenu extends UIPopupMenu {

    private boolean wantsSelfClose = false;

    public UIAutoclosingPopupMenu(String[] strings, Runnable[] tilesets, int h, int sh, boolean rsz) {
        super(strings, tilesets, h, sh, rsz);
    }

    @Override
    public boolean requestsUnparenting() {
        return wantsSelfClose;
    }

    @Override
    public void optionExecute(int b) {
        super.optionExecute(b);
        wantsSelfClose = true;
    }
}
