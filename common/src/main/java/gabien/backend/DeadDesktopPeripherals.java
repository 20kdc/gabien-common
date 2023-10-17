/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

import java.util.HashSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.append.*;
import gabien.wsi.IDesktopPeripherals;
import gabien.wsi.IPointer;
import gabien.wsi.ITextEditingSession;

/**
 * Created on 05/03/2020.
 */
public final class DeadDesktopPeripherals implements IDesktopPeripherals {
    public static final DeadDesktopPeripherals INSTANCE = new DeadDesktopPeripherals(); 

    @Override
    public void performOffset(int x, int y) {
    }

    @Override
    public void clearOffset() {
    }

    @Override
    public HashSet<IPointer> getActivePointers() {
        return new HashSet<IPointer>();
    }

    @Override
    public void clearKeys() {
        
    }

    @Override
    public ITextEditingSession openTextEditingSession(@NonNull String text, boolean multiLine, int textHeight, @Nullable Function<String, String> feedback) {
        return new DeadTextEditingSession();
    }

    @Override
    public int getMouseX() {
        return 0;
    }

    @Override
    public int getMouseY() {
        return 0;
    }

    @Override
    public int getMousewheelBuffer() {
        return 0;
    }

    @Override
    public boolean isKeyDown(int key) {
        return false;
    }

    @Override
    public boolean isKeyJustPressed(int key) {
        return false;
    }

    @Override
    public HashSet<Integer> activeKeys() {
        return new HashSet<Integer>();
    }

}
