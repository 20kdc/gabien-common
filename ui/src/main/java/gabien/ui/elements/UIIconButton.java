/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.elements;

import java.util.function.Function;

import gabien.render.IDrawable;

/**
 * A button with an icon on it.
 * Created on January 28th, 2018 as part of an R48 plan to reduce translator work.
 * Later migrated to GaBIEn, 1st December 2023, as part of IIcon stuff.
 * Split between base/subclass 2nd December 2023. 
 */
public class UIIconButton extends UIBaseIconButton<UIIconButton> {
    public Function<Boolean, IDrawable> symbol;

    public UIIconButton(Function<Boolean, IDrawable> symbol, int fontSize, Runnable runnable) {
        super(fontSize, runnable);
        this.symbol = symbol;
    }

    @Override
    protected IDrawable getCurrentIcon(boolean textBlack) {
        return symbol.apply(textBlack);
    }
}
