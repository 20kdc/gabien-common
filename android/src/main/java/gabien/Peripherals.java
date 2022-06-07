/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.ui.IPointer;
import gabien.ui.Rect;
import gabien.uslx.append.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Just your ordinary IPCRESS peripherals implementation.
 * Created on 18th February 2018.
 */
public class Peripherals implements IPeripherals {
    // The parent GrInDriver. (GTHREAD)
    public GrInDriver parent;

    // The current offset. (GTHREAD)
    public int offsetX, offsetY;

    public ReentrantLock pointersLock = new ReentrantLock();
    public HashSet<UPointer> pointersThatWePretendDoNotExist = new HashSet<UPointer>();
    public HashMap<Integer, UPointer> pointersMap = new HashMap<Integer, UPointer>();
    
    private boolean enterPressed, textboxMaintainedThisFrame, textboxWasReadyAtLastCheck;
    private String lastTextSentToTextbox;

    public Peripherals(GrInDriver gd) {
        parent = gd;
    }

    @Override
    public void performOffset(int x, int y) {
        offsetX += x;
        offsetY += y;
    }

    @Override
    public void clearOffset() {
        offsetX = 0;
        offsetY = 0;
        pointersLock.lock();
        for (UPointer mp : pointersMap.values())
            mp.flushOffset();
        pointersLock.unlock();
    }

    @Override
    public HashSet<IPointer> getActivePointers() {
        pointersLock.lock();
        HashSet<IPointer> r = new HashSet<IPointer>(pointersMap.values());
        r.removeAll(pointersThatWePretendDoNotExist);
        pointersLock.unlock();
        return r;
    }

    @Override
    public void clearKeys() {
        enterPressed = false;
        pointersLock.lock();
        pointersThatWePretendDoNotExist.addAll(pointersMap.values());
        pointersLock.unlock();
    }

    @Override
    public String maintain(Rect area, String text, int textHeight, IFunction<String, String> feedback) {
        ITextboxImplementation impl = TextboxImplObject.getInstance();
        if ((lastTextSentToTextbox == null) || (!lastTextSentToTextbox.equals(text))) {
            impl.setActive(text, feedback);
            lastTextSentToTextbox = text;
            textboxWasReadyAtLastCheck = true;
        }
        textboxMaintainedThisFrame = true;
        return impl.getLastKnownText();
    }

    @Override
    public boolean isEnterJustPressed() {
        boolean bv = enterPressed;
        enterPressed = false;
        return bv;
    }
    
    public void gdUpdateTextbox(boolean flushing) {
        if (flushing) {
            if (textboxMaintainedThisFrame) {
                textboxMaintainedThisFrame = false;
            } else {
                // textbox expired
                if (lastTextSentToTextbox != null) {
                    TextboxImplObject.getInstance().setInactive();
                    lastTextSentToTextbox = null;
                }
            }
        }
        // detect specifically the *event* of textbox closure, but do not actually
        //  mark the textbox as unmaintained because this would cause it to be re-opened
        if (!TextboxImplObject.getInstance().checkupUsage()) {
            if (textboxWasReadyAtLastCheck) {
                textboxWasReadyAtLastCheck = false;
                enterPressed = true;
            }
        }
    }

    public void gdResetPointers() {
        pointersLock.lock();
        pointersMap.clear();
        pointersLock.unlock();
    }

    public void gdPushEvent(boolean mode, int pointerId, int x, int y) {
        pointersLock.lock();
        if (!mode) {
            UPointer ptr = pointersMap.remove(pointerId);
            if (ptr != null)
                pointersThatWePretendDoNotExist.remove(ptr);
        } else {
            UPointer up = pointersMap.get(pointerId);
            if (up == null) {
                up = new UPointer();
                pointersMap.put(pointerId, up);
            }
            up.actualX = x;
            up.actualY = y;
        }
        pointersLock.unlock();
    }

    private static class UPointer implements IPointer {
        public int offsetX, offsetY, actualX, actualY;
        @Override
        public int getX() {
            return offsetX + actualX;
        }

        @Override
        public int getY() {
            return offsetY + actualY;
        }

        @Override
        public PointerType getType() {
            return PointerType.Generic;
        }

        @Override
        public void performOffset(int x, int y) {
            offsetX += x;
            offsetY += y;
        }

        private void flushOffset() {
            offsetX = 0;
            offsetY = 0;
        }
    }
}
