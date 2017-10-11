/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

/**
 * Created on 13/04/16.
 */
public class UIAdjuster extends UIPanel implements IConsumer<String> {
    public UITextButton incButton, decButton;
    public UILabel numberDisplay;

    public UIAdjuster(int h, final ISupplier<String> inc, final ISupplier<String> dec) {
        incButton = new UITextButton(h, "+", new Runnable() {
            @Override
            public void run() {
                accept(inc.get());
            }
        });
        allElements.add(incButton);
        decButton = new UITextButton(h, "-", new Runnable() {
            @Override
            public void run() {
                accept(dec.get());
            }
        });
        allElements.add(decButton);
        numberDisplay = new UILabel("ERR", h);
        allElements.add(numberDisplay);
        int ibh = incButton.getBounds().height;
        int nbh = numberDisplay.getBounds().height;
        int dbh = decButton.getBounds().height;

        int rbh = ibh;
        if (nbh > rbh)
            rbh = nbh;
        if (dbh > rbh)
            rbh = dbh;
        setBounds(new Rect(0, 0, 128, rbh));
    }

    public void setBounds(Rect r) {
        super.setBounds(r);
        int ibh = incButton.getBounds().height;
        int nbh = numberDisplay.getBounds().height;
        int dbh = decButton.getBounds().height;

        int ibw = incButton.getBounds().width;
        int dbw = decButton.getBounds().width;

        incButton.setBounds(new Rect(0, 0, ibw, ibh));
        numberDisplay.setBounds(new Rect(ibw, 0, r.width - (ibw + dbw), nbh));
        decButton.setBounds(new Rect(r.width - dbw, 0, dbw, dbh));

    }

    @Override
    public void accept(String a) {
        numberDisplay.Text = a;
    }
}
