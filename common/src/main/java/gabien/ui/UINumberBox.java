/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.IPeripherals;
import gabien.ITextEditingSession;
import gabien.ui.theming.Theme;
import gabien.uslx.append.*;

/**
 * Forgot creation date, but it was back at the time of IkachanMapEdit.
 * The Change done on February 17th, 2018.
 */
public class UINumberBox extends UILabel {

    public UINumberBox(long number, int h) {
        this(number, h, "0000");
    }

    public UINumberBox(long number, int h, String spacer) {
        super(Long.toString(number), h, spacer);
        this.number = number;
        setBorderType(Theme.B_TEXTBOX);
        alignX = 2;
    }

    // The caching exists so that edits have to be confirmed for onEdit usage.
    private long editingCNumber = 0;

    private long editingNLast = 0;
    public long number = 0;

    public boolean readOnly = false;
    public Runnable onEdit = EmptyLambdas.emptyRunnable;

    private boolean tempDisableSelection = false;
    private ITextEditingSession editingSession;

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
        if (selected && (!readOnly)) {
            // ensure we have an editing session
            if (editingSession == null) {
                editingSession = peripherals.openTextEditingSession(String.valueOf(number), false, contents.textHeight, null);
            }
            Rect crib = getContentsRelativeInputBounds();
            String ss = editingSession.maintain(crib.x, crib.y, crib.width, crib.height, String.valueOf(number));
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
            if (editingSession.isEnterJustPressed()) {
                editingCNumber = number;
                onEdit.run();
                peripherals.clearKeys();
                tempDisableSelection = true;
            } else if (editingSession.isSessionDead()) {
                tempDisableSelection = true;
            }
        } else {
            if (editingSession != null) {
                editingSession.endSession();
                editingSession = null;
            }
        }
        setBorderType(selected ? Theme.B_TEXTBOXF : Theme.B_TEXTBOX);
        super.updateContents(deltaTime, selected, peripherals);
    }

    @Override
    public void setAttachedToRoot(boolean attached) {
        super.setAttachedToRoot(attached);
        if (editingSession != null && !attached) {
            editingSession.endSession();
            editingSession = null;
        }
    }

    @Override
    public IPointerReceiver handleNewPointer(IPointer state) {
        tempDisableSelection = false;
        return super.handleNewPointer(state);
    }
}
