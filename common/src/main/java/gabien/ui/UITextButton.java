/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.GaBIEn;
import gabien.render.IGrDriver;
import gabien.wsi.IPeripherals;

/**
 * Changed on 16th February 2018 for the usual fun w/ the redesign.
 */
public class UITextButton extends UIButton<UITextButton> {
    public String text;
    private final UILabel.Contents contents;
    public boolean centred;
    
    public UITextButton(String tex, int h, Runnable click) {
        super(UIBorderedElement.getRecommendedBorderWidth(h));
        contents = new UILabel.Contents(h);
        text = tex;
        onClick = click;

        // Same reasoning as in UILabel
        setForcedBounds(null, new Rect(getRecommendedTextSize(GaBIEn.sysThemeRoot.getTheme(), tex, h, getBorderWidth())));
        // This overrides the previous wanted size!
        forceToRecommended();
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        super.updateContents(deltaTime, selected, peripherals);
        // See UILabel for the reasoning here.
        runLayout();
    }

    @Override
    public void onThemeChanged() {
        super.onThemeChanged();
        // Same as with updateContents.
        runLayout();
    }

    @Override
    public void runLayout() {
        super.runLayout();
        Size p = contents.update(getTheme(), getSize(), getBorderWidth(), text);
        if (p != null)
            setWantedSize(p);
    }

    @Override
    public void renderContents(boolean textBlack, IGrDriver igd) {
        contents.render(textBlack, 0, 0, igd, centred);
    }

    public UITextButton centred() {
        centred = true;
        return this;
    }
}
