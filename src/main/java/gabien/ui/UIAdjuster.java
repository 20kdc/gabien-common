/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

/**
 * Created on 13/04/16, Change @ feb 17, 2018
 */
public class UIAdjuster extends UIElement.UIPanel implements IConsumer<String> {
    public final UITextButton incButton, decButton;
    public final UILabel numberDisplay;

    public UIAdjuster(int h, final ISupplier<String> inc, final ISupplier<String> dec) {
        incButton = new UITextButton("+", h, new Runnable() {
            @Override
            public void run() {
                accept(inc.get());
            }
        });
        layoutAddElement(incButton);
        decButton = new UITextButton("-", h, new Runnable() {
            @Override
            public void run() {
                accept(dec.get());
            }
        });
        layoutAddElement(decButton);
        numberDisplay = new UILabel("ERROR", h);
        layoutAddElement(numberDisplay);

        Rect nBounds = calcWantedSize();
        setForcedBounds(null, nBounds);
        setWantedSize(nBounds);
    }

    private Rect calcWantedSize() {
        Size incButtonSize = incButton.getWantedSize();
        Size numberDisplaySize = numberDisplay.getWantedSize();
        Size decButtonSize = decButton.getWantedSize();
        return new Rect(0, 0, incButtonSize.width + numberDisplaySize.width + decButtonSize.width, Math.max(incButtonSize.height, Math.max(numberDisplaySize.height, decButtonSize.height)));
    }

    @Override
    public void runLayout() {
        Size incButtonSize = incButton.getWantedSize();
        Size numberDisplaySize = numberDisplay.getWantedSize();
        Size decButtonSize = decButton.getWantedSize();

        int ibw = incButtonSize.width;
        int dbw = decButtonSize.width;

        Size m = getSize();

        incButton.setForcedBounds(this, new Rect(0, 0, ibw, incButtonSize.height));
        numberDisplay.setForcedBounds(this, new Rect(ibw, 0, m.width - (ibw + dbw), numberDisplaySize.height));
        decButton.setForcedBounds(this, new Rect(m.width - dbw, 0, dbw, decButtonSize.height));

        setWantedSize(calcWantedSize());
    }

    @Override
    public void accept(String a) {
        numberDisplay.text = a;
    }
}
