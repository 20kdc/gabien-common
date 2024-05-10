/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.layouts;

import gabien.ui.UIElement;
import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;

/**
 * Takes a sort of 'line-layout' approach.
 * Created 10th May 2024. Wait, wha- oh, right, time.
 */
public class UIFlowLayout extends UIBaseListOfStuffLayout {
    public UIFlowLayout() {
        super(true);
    }

    public UIFlowLayout(UIElement... contents) {
        super(true);
        panelsSet(contents);
        forceToRecommended();
    }

    public UIFlowLayout(Iterable<UIElement> contents) {
        super(true);
        panelsSet(contents);
        forceToRecommended();
    }

    @Override
    public int layoutGetWForH(int height) {
        int totalWidth = 0;
        for (UIElement uie : layoutGetElementsIterable())
            totalWidth += uie.layoutGetWForH(height);
        return totalWidth;
    }

    @Override
    public int layoutGetHForW(int width) {
        int x = 0;
        int y = 0;
        int h = 0;
        for (UIElement uie : layoutGetElementsIterable()) {
            Size wanted = uie.getWantedSize();
            if (wanted.width >= width) {
                // newline
                x = 0;
                y += h;
                h = 0;
                // add
                h = uie.layoutGetHForW(width);
                // newline
                x = 0;
                y += h;
                h = 0;
                continue;
            }
            if ((x + wanted.width) > width) {
                // newline
                x = 0;
                y += h;
                h = 0;
            }
            // add
            h = Math.max(h, wanted.height);
            // advance
            x += wanted.width;
        }
        // finish
        y += h;
        return y;
    }

    @Override
    protected Size layoutRecalculateMetricsImpl() {
        int x = 0;
        int h = 0;
        for (UIElement uie : layoutGetElementsIterable()) {
            Size wanted = uie.getWantedSize();
            if (h < wanted.height)
                h = wanted.height;
            x += wanted.width;
        }
        return new Size(x, h);
    }

    @Override
    protected void layoutRunImpl() {
        Size sz = getSize();
        int x = 0;
        int y = 0;
        int h = 0;
        UIElement[] tmpLineBuffer = new UIElement[layoutGetElementsCount()];
        int lineBufferCount = 0;
        for (UIElement uie : layoutGetElementsIterable()) {
            Size wanted = uie.getWantedSize();
            if (wanted.width >= sz.width) {
                applyLineBuffer(tmpLineBuffer, lineBufferCount, y, h);
                lineBufferCount = 0;
                // newline
                x = 0;
                y += h;
                h = 0;
                // add
                h = uie.layoutGetHForW(sz.width);
                uie.setForcedBounds(this, new Rect(x, y, sz.width, h));
                // newline
                x = 0;
                y += h;
                h = 0;
                continue;
            }
            if ((x + wanted.width) > sz.width) {
                applyLineBuffer(tmpLineBuffer, lineBufferCount, y, h);
                lineBufferCount = 0;
                // newline
                x = 0;
                y += h;
                h = 0;
            }
            // add
            tmpLineBuffer[lineBufferCount++] = uie;
            h = Math.max(h, wanted.height);
            // advance
            x += wanted.width;
        }
        // finish
        applyLineBuffer(tmpLineBuffer, lineBufferCount, y, h);
    }

    private void applyLineBuffer(UIElement[] buffer, int count, int y, int height) {
        int x = 0;
        for (int i = 0; i < count; i++) {
            Size wanted = buffer[i].getWantedSize();
            buffer[i].setForcedBounds(this, new Rect(x, y, wanted.width, height));
            x += wanted.width;
        }
    }
}
