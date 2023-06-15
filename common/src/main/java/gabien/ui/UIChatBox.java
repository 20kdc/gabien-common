/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.GaBIEn;
import gabien.IDesktopPeripherals;
import gabien.IGrInDriver;
import gabien.IPeripherals;
import gabien.ITextEditingSession;
import gabien.uslx.append.*;

/**
 * For consoles and the like.
 * Created 28th February 2023.
 */
public class UIChatBox extends UILabel {
    private String history = "";
    public IConsumer<String> onSubmit = (text) -> {};

    private boolean tempDisableSelection = false;
    private ITextEditingSession editingSession;

    public UIChatBox(String text, int h) {
        super(text, h);
        borderType = 3;
    }

    @Override
    public void updateContents(double deltaTime, boolean selected, IPeripherals peripherals) {
        selected &= !tempDisableSelection;

        if (selected) {
            // ensure we have an editing session
            if (editingSession == null)
                editingSession = peripherals.openTextEditingSession(text, false, contents.textHeight, null);
            Rect crib = getContentsRelativeInputBounds();
            if (peripherals instanceof IDesktopPeripherals)
                if (((IDesktopPeripherals) peripherals).isKeyJustPressed(IGrInDriver.VK_UP))
                    text = history;
            text = editingSession.maintain(crib.x, crib.y, crib.width, crib.height, text);
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
        borderType = selected ? 4 : 3;
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
