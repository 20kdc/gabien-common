/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.elements;

import java.util.function.Function;

import gabien.ui.IPointerReceiver;
import gabien.ui.theming.Theme;
import gabien.uslx.append.*;
import gabien.wsi.IPeripherals;
import gabien.wsi.IPointer;
import gabien.wsi.ITextEditingSession;

// This serves a dual purpose:
// 1. text is *always* the current text in the box.
//    It is only reverted a frame after deselection.
//    (This makes it useful for dialogue boxes where the selection behavior is non-obvious.)
// 2. onEdit is called when enter is pressed, and otherwise the text will revert.
//    (This makes it useful for property-editor interfaces which need that kind of confirmation.)
public class UITextBox extends UILabel {
    private String textAsSeenByProgram;
    public Runnable onEdit = EmptyLambdas.emptyRunnable;
    public Function<String, String> feedback;
    public boolean multiLine;

    private boolean tempDisableSelection = false;
    private ITextEditingSession editingSession;

    public UITextBox(String text, int h) {
        super(text, h);
        textAsSeenByProgram = text;
        setBorderType(Theme.B_TEXTBOX);
    }

    public UITextBox setMultiLine() {
        multiLine = true;
        return this;
    }

    @Override
    public void setText(String text) {
        textAsSeenByProgram = text;
        if (editingSession != null)
            editingSession.setText(text);
        super.setText(text);
    }

    @Override
    public String getText() {
        return textAsSeenByProgram;
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        selected &= !tempDisableSelection;

        String textBeingEdited = super.getText();
        if (selected) {
            // ensure we have an editing session
            if (editingSession == null)
                editingSession = peripherals.openTextEditingSession(textBeingEdited, multiLine, contents.textHeight, feedback);
            Rect crib = getContentsRelativeInputBounds();
            String ss = editingSession.maintain(crib.x, crib.y, crib.width, crib.height);
            if (!multiLine) {
                // Enter confirmation.
                if (editingSession.isEnterJustPressed()) {
                    // do this early since we're about to setText
                    editingSession.endSession();
                    // System.out.println("UITextBox: Enter just pressed, text: " + ss);
                    // update text *as seen by program*
                    setText(ss);
                    // warning: can bamboozle and cause editingSession loss!
                    onEdit.run();
                    peripherals.clearKeys();
                    tempDisableSelection = true;
                }
            }
            if ((editingSession != null) && editingSession.isSessionDead()) {
                // clean up if the session died
                tempDisableSelection = true;
                closeOffSession();
            }
        } else {
            // close off any editing session going on
            // this is the only place where this can happen so this is where the closeoff logic lives
            if (editingSession != null) {
                if (multiLine) {
                    // do this early since we're about to setText
                    editingSession.endSession();
                    // update text *as seen by program*
                    setText(textBeingEdited);
                    onEdit.run();
                }
                closeOffSession();
            }
        }
        setBorderType(selected ? Theme.B_TEXTBOXF : Theme.B_TEXTBOX);
        super.updateContents(deltaTime, selected, peripherals);
    }

    @Override
    public void setAttachedToRoot(boolean attached) {
        super.setAttachedToRoot(attached);
        if (!attached)
            closeOffSession();
    }

    private void closeOffSession() {
        if (editingSession != null) {
            editingSession.endSession();
            editingSession = null;
            // restore text from backup (only place where a session can be closed off!)
            // if the session ended well then it will have been confirmed before we hit this
            super.setText(textAsSeenByProgram);
        }
    }

    @Override
    public IPointerReceiver handleNewPointer(IPointer state) {
        tempDisableSelection = false;
        return super.handleNewPointer(state);
    }
}
