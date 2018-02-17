/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IGrInDriver;

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
        Size sz = getRecommendedTextSize(text, h);
        setWantedSize(sz);
        setForcedBounds(null, new Rect(0, 0, sz.width, sz.height));
    }

    @Override
    public UITextButton togglable(boolean st) {
        super.togglable(st);
        return this;
    }

    @Override
    public void render(boolean selected, IPointer mouse, IGrInDriver igd) {
        super.render(selected, mouse, igd);
        Size p = contents.render(getSize(), getBorderWidth(), text, igd);
        if (p != null)
            setWantedSize(p);
    }
}
