/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.elements;

import java.util.function.Consumer;

import gabien.GaBIEn;
import gabien.ui.IPointerReceiver;
import gabien.ui.theming.Theme;
import gabien.uslx.append.*;
import gabien.wsi.IDesktopPeripherals;
import gabien.wsi.IGrInDriver;
import gabien.wsi.IPeripherals;
import gabien.wsi.IPointer;
import gabien.wsi.ITextEditingSession;

/**
 * For consoles and the like.
 * Created 28th February 2023.
 */
public class UIChatBox extends UILabel {
    private String history = "";
    public Consumer<String> onSubmit = (text) -> {};

    private boolean tempDisableSelection = false;
    private ITextEditingSession editingSession;

    public UIChatBox(String text, int h) {
        super(text, h);
        setBorderType(Theme.B_TEXTBOX);
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        selected &= !tempDisableSelection;

        if (selected) {
            String text = getText();
            // ensure we have an editing session
            if (editingSession == null)
                editingSession = peripherals.openTextEditingSession(text, false, contents.textHeight, null);
            Rect crib = getContentsRelativeInputBounds();
            if (peripherals instanceof IDesktopPeripherals)
                if (((IDesktopPeripherals) peripherals).isKeyJustPressed(IGrInDriver.VK_UP))
                    editingSession.setText(history);
            text = editingSession.maintain(crib.x, crib.y, crib.width, crib.height);
            // Enter confirmation.
            if (editingSession.isEnterJustPressed()) {
                history = text;
                text = "";
                onSubmit.accept(history);
                // shut down the editing session, just to restart it again
                // this is to clear the isEnterJustPressed flag
                editingSession.endSession();
                editingSession = null;
                // this is a workaround to stop Android seizing up
                if (GaBIEn.singleWindowApp())
                    tempDisableSelection = true;
            }
            setText(text);
            if ((editingSession != null) && editingSession.isSessionDead()) {
                // clean up if the session died
                tempDisableSelection = true;
                editingSession = null;
            }
        } else {
            // close off any editing session going on
            if (editingSession != null) {
                if (editingSession != null) {
                    editingSession.endSession();
                    editingSession = null;
                }
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
