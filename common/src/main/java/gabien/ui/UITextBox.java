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

// This serves a dual purpose:
// 1. text is *always* the current text in the box.
//    It is only reverted a frame after deselection.
//    (This makes it useful for dialogue boxes where the selection behavior is non-obvious.)
// 2. onEdit is called when enter is pressed, and otherwise the text will revert.
//    (This makes it useful for property-editor interfaces which need that kind of confirmation.)
public class UITextBox extends UILabel {
    private String textLastSeen = "";
    private String textCStr = "";
    public Runnable onEdit = EmptyLambdas.emptyRunnable;
    public IFunction<String, String> feedback;
    public boolean multiLine;

    private boolean tempDisableSelection = false;
    private ITextEditingSession editingSession;

    public UITextBox(String text, int h) {
        super(text, h);
        setBorderType(Theme.B_TEXTBOX);
    }

    public UITextBox setMultiLine() {
        multiLine = true;
        return this;
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        selected &= !tempDisableSelection;

        if (!textLastSeen.equals(text)) {
            textCStr = text;
            textLastSeen = text;
        }
        if (selected) {
            // ensure we have an editing session
            if (editingSession == null)
                editingSession = peripherals.openTextEditingSession(text, multiLine, contents.textHeight, feedback);
            Rect crib = getContentsRelativeInputBounds();
            String ss = editingSession.maintain(crib.x, crib.y, crib.width, crib.height, text);
            // Update storage.
            text = ss;
            textLastSeen = ss;
            if (!multiLine) {
                // Enter confirmation.
                if (editingSession.isEnterJustPressed()) {
                    textCStr = text;
                    // warning: can bamboozle and cause editingSession loss!
                    onEdit.run();
                    peripherals.clearKeys();
                    tempDisableSelection = true;
                }
            }
            if ((editingSession != null) && editingSession.isSessionDead()) {
                // clean up if the session died
                tempDisableSelection = true;
                editingSession = null;
            }
        } else {
            // close off any editing session going on
            if (editingSession != null) {
                if (multiLine) {
                    textCStr = text;
                    onEdit.run();
                }
                if (editingSession != null) {
                    editingSession.endSession();
                    editingSession = null;
                }
            }
            // restore text from backup
            text = textCStr;
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
