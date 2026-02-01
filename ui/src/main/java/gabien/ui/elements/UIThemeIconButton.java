/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.elements;

import org.eclipse.jdt.annotation.NonNull;

import gabien.render.IDrawable;
import gabien.ui.theming.Theme;

/**
 * UIBaseIconButton for theme-based icons.
 * Created 2nd December 2023. 
 */
public class UIThemeIconButton extends UIBaseIconButton<UIThemeIconButton> {
    public @NonNull Theme.Attr<IDrawable> iconAttr;
    public UIThemeIconButton(@NonNull Theme.Attr<IDrawable> symbol, int fontSize, Runnable runnable) {
        super(fontSize, runnable);
        iconAttr = symbol;
    }

    @Override
    protected IDrawable getCurrentIcon(boolean textBlack) {
        return iconAttr.get(this);
    }
}
