/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

/**
 * Created on 13/08/17.
 */
public class UIAutoclosingPopupMenu extends UIPopupMenu implements IWindowElement {

    private boolean wantsSelfClose = false;

    public UIAutoclosingPopupMenu(String[] strings, Runnable[] tilesets, int h, boolean rsz) {
        super(strings, tilesets, h, rsz);
    }

    @Override
    public boolean wantsSelfClose() {
        return wantsSelfClose;
    }

    @Override
    public void windowClosed() {

    }

    @Override
    public void optionExecute(int b) {
        super.optionExecute(b);
        wantsSelfClose = true;
    }
}
