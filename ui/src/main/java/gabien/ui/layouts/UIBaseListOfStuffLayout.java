/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.layouts;

import gabien.ui.UIElement;

/**
 * Split out of UIScrollLayout 10th May 2024.
 */
public abstract class UIBaseListOfStuffLayout extends UIElement.UIPanel {
    private final boolean defaultVisState;

    public UIBaseListOfStuffLayout(boolean vis) {
        defaultVisState = vis;
    }

    public void panelsSet() {
        for (UIElement uie : layoutGetElements())
            layoutRemoveElement(uie);
        layoutRecalculateMetrics();
    }

    public void panelsSet(UIElement... contents) {
        for (UIElement uie : layoutGetElements())
            layoutRemoveElement(uie);
        for (UIElement uie : contents) {
            layoutAddElement(uie);
            layoutSetElementVis(uie, defaultVisState);
        }
        layoutRecalculateMetrics();
    }

    public <T extends UIElement> void panelsSet(Iterable<T> contents) {
        for (UIElement uie : layoutGetElements())
            layoutRemoveElement(uie);
        for (UIElement uie : contents) {
            layoutAddElement(uie);
            layoutSetElementVis(uie, defaultVisState);
        }
        layoutRecalculateMetrics();
    }
}
