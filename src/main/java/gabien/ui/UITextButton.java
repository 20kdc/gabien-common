/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrDriver;
import gabien.IPeripherals;

/**
 * Changed on 16th February 2018 for the usual fun w/ the redesign.
 */
public class UITextButton extends UIButton {
    public String text;
    private final UILabel.Contents contents;
    
    public UITextButton(String tex, int h, Runnable click) {
        super(UIBorderedElement.getRecommendedBorderWidth(h));
        contents = new UILabel.Contents(h);
        text = tex;
        onClick = click;

        setWantedSize(getRecommendedTextSize("", h));
        runLayout();
        setForcedBounds(null, new Rect(getWantedSize()));
    }

    @Override
    public UITextButton togglable(boolean st) {
        super.togglable(st);
        return this;
    }

    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);
        // See UILabel for the reasoning here.
        runLayout();
    }

    @Override
    public void runLayout() {
        super.runLayout();
        Size p = contents.update(getSize(), getBorderWidth(), text);
        if (p != null)
            setWantedSize(p);
    }

    @Override
    public void renderContents(boolean selected, boolean textBlack, IPeripherals peripherals, IGrDriver igd) {
        contents.render(textBlack, 0, 0, igd);
    }
}
