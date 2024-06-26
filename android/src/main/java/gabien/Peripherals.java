/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.wsi.IPeripherals;
import gabien.wsi.IPointer;
import gabien.wsi.ITextEditingSession;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

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
    public HashSet<UPointer> pointersJustDown = new HashSet<UPointer>();
    public HashMap<Integer, UPointer> pointersMap = new HashMap<Integer, UPointer>();

    public TextEditingSession currentTextEditingSession;

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
        r.addAll(pointersJustDown);
        pointersJustDown.clear();
        pointersLock.unlock();
        return r;
    }

    @Override
    public void clearKeys() {
        pointersLock.lock();
        pointersThatWePretendDoNotExist.addAll(pointersMap.values());
        pointersJustDown.clear();
        pointersLock.unlock();
        if (currentTextEditingSession != null)
            currentTextEditingSession.endSession();
    }

    @Override
    public ITextEditingSession openTextEditingSession(@NonNull String text, boolean multiLine, int textHeight, @Nullable Function<String, String> feedback) {
        return new TextEditingSession(this, text, multiLine, textHeight, feedback);
    }

    public void gdUpdateTextboxHoldingMALock() {
        if (currentTextEditingSession != null)
            currentTextEditingSession.gdUpdateTextboxHoldingMALock();
    }

    public void gdResetPointers() {
        pointersLock.lock();
        pointersMap.clear();
        // we do *NOT* clear pointersJustDown; this is called when the last pointer goes up,
        //  but app has not necessarily ack'd it yet
        pointersLock.unlock();
    }

    public void gdPushPointerDownOrMove(int pointerId, int x, int y, @Nullable IPointer.PointerType down) {
        pointersLock.lock();
        UPointer up = pointersMap.get(pointerId);
        if (up == null && down != null) {
            up = new UPointer();
            up.type = down;
            pointersMap.put(pointerId, up);
            pointersJustDown.add(up);
        }
        if (up != null) {
            up.actualX = x;
            up.actualY = y;
        }
        pointersLock.unlock();
    }

    public void gdPushPointerUp(int pointerId, int x, int y) {
        pointersLock.lock();
        UPointer ptr = pointersMap.remove(pointerId);
        if (ptr != null) {
            pointersThatWePretendDoNotExist.remove(ptr);
            ptr.actualX = x;
            ptr.actualY = y;
        }
        pointersLock.unlock();
    }

    private static class UPointer implements IPointer {
        public int offsetX, offsetY, actualX, actualY;
        public PointerType type = PointerType.Generic;
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
            return type;
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
