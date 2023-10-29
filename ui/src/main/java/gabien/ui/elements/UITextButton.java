/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.elements;

import org.eclipse.jdt.annotation.Nullable;

import gabien.render.IGrDriver;
import gabien.uslx.append.Size;

/**
 * Changed on 16th February 2018 for the usual fun w/ the redesign.
 */
public class UITextButton extends UIButton<UITextButton> {
    private String text;
    private final UILabel.Contents contents;
    public boolean centred;
    
    public UITextButton(String tex, int h, Runnable click) {
        super(UIBorderedElement.getRecommendedBorderWidth(h));
        text = tex;
        onClick = click;

        contents = new UILabel.Contents(h, "", tex, getBorderWidth(), getTheme());

        forceToRecommended();
    }

    public void setText(String text) {
        if (this.text.equals(text))
            return;
        this.text = text;
        contents.setText(text);
        layoutRecalculateMetrics();
    }

    public String getText() {
        return text;
    }

    @Override
    public void onThemeChanged() {
        super.onThemeChanged();
        contents.setTheme(getTheme());
        layoutRecalculateMetrics();
    }

    @Override
    public int layoutGetHForW(int width) {
        return contents.getHForW(width);
    }

    @Override
    protected @Nullable Size layoutRecalculateMetricsImpl() {
        return contents.getWantedSize();
    }

    @Override
    public void renderContents(boolean textBlack, IGrDriver igd) {
        contents.update(getSize());
        contents.render(textBlack, 0, 0, igd, centred);
    }

    public UITextButton centred() {
        centred = true;
        return this;
    }
}
