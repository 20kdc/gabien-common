/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import java.util.function.Consumer;
import java.util.function.Function;

import gabien.uslx.append.*;

/**
 * Created on 13/04/16, Change @ feb 17, 2018
 */
public class UIAdjuster extends UIElement.UIPanel implements Consumer<String> {
    public final UITextButton incButton, decButton;
    public final UINumberBox numberDisplay;

    public UIAdjuster(int h, long initial, final Function<Long, Long> write) {
        // Not entirely correct, but reduces time wasted on word-wrapping
        super(h * 16, h);
        incButton = new UITextButton("+", h, new Runnable() {
            @Override
            public void run() {
                numberDisplay.number = write.apply(numberDisplay.number + 1);
            }
        });
        layoutAddElement(incButton);
        decButton = new UITextButton("-", h, new Runnable() {
            @Override
            public void run() {
                numberDisplay.number = write.apply(numberDisplay.number - 1);
            }
        });
        layoutAddElement(decButton);
        numberDisplay = new UINumberBox(initial, h);
        numberDisplay.onEdit = new Runnable() {
            @Override
            public void run() {
                numberDisplay.number = write.apply(numberDisplay.number);
            }
        };
        layoutAddElement(numberDisplay);

        forceToRecommended();
    }

    @Override
    public void runLayout() {
        Size decButtonSize = decButton.getWantedSize();
        Size numberDisplaySize = numberDisplay.getWantedSize();
        Size incButtonSize = incButton.getWantedSize();

        int dbw = decButtonSize.width;
        int ibw = incButtonSize.width;

        Size m = getSize();

        decButton.setForcedBounds(this, new Rect(0, 0, dbw, decButtonSize.height));
        numberDisplay.setForcedBounds(this, new Rect(dbw, 0, m.width - (dbw + ibw), numberDisplaySize.height));
        incButton.setForcedBounds(this, new Rect(m.width - ibw, 0, ibw, incButtonSize.height));

        setWantedSize(new Rect(0, 0, incButtonSize.width + numberDisplaySize.width + decButtonSize.width, Math.max(incButtonSize.height, Math.max(numberDisplaySize.height, decButtonSize.height))));
    }

    @Override
    public void accept(String a) {
        numberDisplay.text = a;
    }

    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        if (north) {
            incButton.onClick.run();
        } else {
            decButton.onClick.run();
        }
    }
}
