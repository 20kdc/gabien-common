/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

import gabien.IPeripherals;

/**
 * Forgot creation date, but it was back at the time of IkachanMapEdit.
 * The Change done on February 17th, 2018.
 */
public class UINumberBox extends UILabel {

    public UINumberBox(long number, int h) {
        super(Long.toString(number), h);
        this.number = number;
        borderType = 3;
    }

    // The caching exists so that edits have to be confirmed for onEdit usage.
    private long editingCNumber = 0;

    private long editingNLast = 0;
    public long number = 0;

    public boolean readOnly = false;
    public Runnable onEdit = new Runnable() {

        @Override
        public void run() {
            // do nothing-this is one of those boxes that just sits there.
        }
    };

    private boolean tempDisableSelection = false;

    @Override
    public void runLayout() {
        text = Long.toString(number);
        super.runLayout();
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        selected &= !tempDisableSelection;
        if (number != editingNLast) {
            editingCNumber = number;
            editingNLast = number;
        } else if (!selected) {
            number = editingCNumber;
        }
        Size bounds = getSize();
        if (selected && (!readOnly)) {
            String ss = peripherals.maintain(-getBorderWidth(), (bounds.height / 2) - getBorderWidth(), bounds.width, String.valueOf(number), null);
            // Update storage.
            int lastMinusIdx = ss.lastIndexOf("-");
            boolean doInvertLater = false;
            if (lastMinusIdx > 0) {
                String pre = ss.substring(0, lastMinusIdx);
                String post = ss.substring(lastMinusIdx + 1);
                ss = pre + post;
                doInvertLater = true;
            }
            long newNum = 0;
            try {
                newNum = Long.parseLong(ss);
                if (doInvertLater)
                    newNum = -newNum;
            } catch (Exception e) {
            }
            number = newNum;
            editingNLast = number;
            // Enter confirmation.
            // NOTE: This has to be after the update to the local number.
            // Not doing this lead to an interesting bug where number boxes
            //  wouldn't work because the 'enter' press would revert the number.
            if (peripherals.isEnterJustPressed()) {
                editingCNumber = number;
                onEdit.run();
                peripherals.clearKeys();
                tempDisableSelection = true;
            }
        }
        borderType = selected ? 4 : 3;
        super.updateContents(deltaTime, selected, peripherals);
    }

    @Override
    public IPointerReceiver handleNewPointer(IPointer state) {
        tempDisableSelection = false;
        return super.handleNewPointer(state);
    }
}
