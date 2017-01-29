/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

/**
 * Created on 13/04/16.
 */
public class UIAdjuster extends UIPanel implements IConsumer<String> {

    public final boolean x2;
    public UITextButton incButton, decButton;
    public UILabel numberDisplay;

    public UIAdjuster(boolean ix2, final ISupplier<String> inc, final ISupplier<String> dec) {
        x2 = ix2;
        incButton = new UITextButton(x2, "+", new Runnable() {
            @Override
            public void run() {
                accept(inc.get());
            }
        });
        allElements.add(incButton);
        decButton = new UITextButton(x2, "-", new Runnable() {
            @Override
            public void run() {
                accept(dec.get());
            }
        });
        allElements.add(decButton);
        numberDisplay = new UILabel("ERR", x2 ? 16 : 8);
        allElements.add(numberDisplay);
    }

    public void setBounds(Rect r) {
        super.setBounds(r);
        incButton.setBounds(new Rect(0, 0, 18, r.height));
        numberDisplay.setBounds(new Rect(18, 0, r.width - 36, r.height));
        decButton.setBounds(new Rect(r.width - 18, 0, 18, r.height));
    }

    @Override
    public void accept(String a) {
        numberDisplay.Text = a;
    }
}
