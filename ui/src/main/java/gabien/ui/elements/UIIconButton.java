/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.elements;

import gabien.render.IGrDriver;
import gabien.ui.theming.IIcon;
import gabien.uslx.append.Rect;

/**
 * A button with an icon on it.
 * Created on January 28th, 2018 as part of an R48 plan to reduce translator work.
 * Later migrated to GaBIEn, 1st December 2023, as part of IIcon stuff. 
 */
public class UIIconButton extends UIButton<UIIconButton>  {
    public IIcon symbol;

    public UIIconButton(IIcon symbolIndex, int fontSize, Runnable runnable) {
        super(getRecommendedBorderWidth(fontSize));
        int margin = fontSize / 8;
        symbol = symbolIndex;
        onClick = runnable;
        // See rationale in gabien-core UILabel. Note, though, that the width is smaller.
        Rect sz = new Rect(0, 0, fontSize + (margin * 2), fontSize + margin);
        setWantedSize(sz);
        setForcedBounds(null, new Rect(sz));
    }

    @Override
    public void renderContents(boolean textBlack, IGrDriver igd) {
        int bw = getBorderWidth();
        int sw = getSize().width;
        int sh = getSize().height;
        int efs = Math.min(sw, sh);
        int x = (sw - efs) / 2;
        int y = (sh - efs) / 2;
        if (textBlack)
            igd.clearRect(0, 0, 0, bw, bw, sw - (bw * 2), sh - (bw * 2));
        symbol.draw(igd, bw + x, bw + y, efs - (bw * 2));
    }
}
