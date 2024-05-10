/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.elements;

import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

import gabien.ui.UIElement;
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
        numberDisplay = new UINumberBox(initial, h);
        incButton = new UITextButton("+", h, () -> {
            numberDisplay.setNumber(write.apply(numberDisplay.getNumber() + 1));
        });
        layoutAddElement(incButton);
        decButton = new UITextButton("-", h, () -> {
            numberDisplay.setNumber(write.apply(numberDisplay.getNumber() - 1));
        });
        layoutAddElement(decButton);
        numberDisplay.onEdit = () -> {
            numberDisplay.setNumber(write.apply(numberDisplay.getNumber()));
        };
        layoutAddElement(numberDisplay);

        forceToRecommended();
    }

    @Override
    protected void layoutRunImpl() {
        Size decButtonSize = decButton.getWantedSize();
        Size numberDisplaySize = numberDisplay.getWantedSize();
        Size incButtonSize = incButton.getWantedSize();

        int dbw = decButtonSize.width;
        int ibw = incButtonSize.width;

        Size m = getSize();

        decButton.setForcedBounds(this, new Rect(0, 0, dbw, decButtonSize.height));
        numberDisplay.setForcedBounds(this, new Rect(dbw, 0, m.width - (dbw + ibw), numberDisplaySize.height));
        incButton.setForcedBounds(this, new Rect(m.width - ibw, 0, ibw, incButtonSize.height));
    }

    @Override
    protected @Nullable Size layoutRecalculateMetricsImpl() {
        Size decButtonSize = decButton.getWantedSize();
        Size numberDisplaySize = numberDisplay.getWantedSize();
        Size incButtonSize = incButton.getWantedSize();

        return new Size(incButtonSize.width + numberDisplaySize.width + decButtonSize.width, Math.max(incButtonSize.height, Math.max(numberDisplaySize.height, decButtonSize.height)));
    }

    @Override
    public void accept(String a) {
        numberDisplay.setText(a);
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
