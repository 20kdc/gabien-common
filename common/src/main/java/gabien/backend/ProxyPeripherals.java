/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

import java.util.HashSet;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.wsi.IPeripherals;
import gabien.wsi.IPointer;
import gabien.wsi.ITextEditingSession;

/**
 * Created on 05/03/2020.
 */
public class ProxyPeripherals<T extends IPeripherals> implements IPeripherals {
    @SuppressWarnings("null")
    public T target;

    @Override
    public void performOffset(int x, int y) {
        target.performOffset(x, y);
    }

    @Override
    public void clearOffset() {
        target.clearOffset();
    }

    @Override
    public HashSet<IPointer> getActivePointers() {
        return target.getActivePointers();
    }

    @Override
    public void clearKeys() {
        target.clearKeys();
    }

    @Override
    public ITextEditingSession openTextEditingSession(@NonNull String text, boolean multiLine, int textHeight, @Nullable Function<String, String> feedback) {
        return target.openTextEditingSession(text, multiLine, textHeight, feedback);
    }
}
