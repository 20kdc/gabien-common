/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;

/**
 * It's the usual business - you need a dynamic "replace me!" element sometimes.
 * Created on 24th May, 2022.
 */
public class UIDynamicProxy extends UIElement.UIPanel {
    private @Nullable UIElement content = null;

    public @Nullable UIElement dynProxyGet() {
        return content;
    }

    public void dynProxySet(@Nullable UIElement uie) {
        if (content != null) {
            layoutRemoveElement(content);
            content = null;
            setWantedSize(new Size(0, 0));
        }
        content = uie;
        if (content != null) {
            layoutAddElement(content);
            setWantedSize(content.getWantedSize());
        }
        runLayoutLoop();
    }

    @Override
    public void runLayout() {
        if (content != null) {
            content.setForcedBounds(this, new Rect(getSize()));
            setWantedSize(content.getWantedSize());
        }
    }
}
