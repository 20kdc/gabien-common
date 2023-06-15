/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.ui;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.ui.theming.Theme;
import gabien.uslx.append.RefSyncSet;

/**
 * This exists specifically so that you can create a "master root theme" object.
 * Created 15th June, 2023. 
 */
public abstract class LAFChain {
    LAFChain() {
        // Nothing here
    }

    private @Nullable LAFChain.Node lafParentOverride;

    /**
     * Actual calculated theme.
     * Package-private, get via getTheme.
     */
    private @NonNull Theme theme = Theme.ROOT;

    /**
     * Theme override.
     */
    private @Nullable Theme themeOverride = null;

    /**
     * Gets the theming parent.
     * This mustn't be publicly accessible because that might allow "weird" overrides that don't call themeUpdate.
     */
    @Nullable LAFChain getLAFParentInternal() {
        return lafParentOverride;
    }

    public final @Nullable LAFChain getLAFParent() {
        return getLAFParentInternal();
    }

    /**
     * Sets the theming parent override.
     */
    public void setLAFParentOverride(@Nullable LAFChain.Node parent) {
        if (lafParentOverride != null)
           lafParentOverride.removeLAFChild(this);
        lafParentOverride = parent;
        if (lafParentOverride != null)
            lafParentOverride.addLAFChild(this);
        themeUpdate();
    }

    /**
     * Gets the theme. This will never be null, as there is always a theme of last resort.
     */
    public final @NonNull Theme getTheme() {
        return theme;
    }

    /**
     * Gets the theme override.
     */
    public final @Nullable Theme getThemeOverride() {
        return themeOverride;
    }

    /**
     * Sets the theme override.
     * This is how you inject a new theme into the tree.
     */
    public final void setThemeOverride(@Nullable Theme themeOverride) {
        this.themeOverride = themeOverride;
        themeUpdate();
    }

    /**
     * Called when something occurs that might change the theme.
     * Package-private; this method is only called here and in UIElement.java
     * This has to be called by the element parents (UIPanel, UIProxy).
     * This is so their children magically update.
     */
    final void themeUpdate() {
        @NonNull Theme newTheme;
        Theme override = themeOverride;
        LAFChain parent = getLAFParentInternal();
        if (override != null) {
            newTheme = override;
        } else if (parent != null) {
            newTheme = parent.theme;
        } else {
            newTheme = Theme.ROOT;
        }
        if (newTheme != theme) {
            theme = newTheme;
            onThemeChanged();
            themeUpdateChildren();
        }
    }

    /**
     * Called to indicate the theme changed.
     */
    void themeUpdateChildren() {
        
    }

    /**
     * Called to indicate the theme changed.
     */
    public void onThemeChanged() {
        
    }

    /**
     * Represents an abstract theming node.
     * UIElement theming uses the UIElement tree (hence all the package-private stuff).
     * However, this is a separate tree that you can connect into via getLAFParent and friends.
     * Importantly, this tree is (intentionally) weakly referenced.
     */
    public static final class Node extends LAFChain {
        private RefSyncSet<LAFChain> children = new RefSyncSet<>();

        void addLAFChild(LAFChain child) {
            children.addWeak(child);
        }

        void removeLAFChild(LAFChain child) {
            children.removeValue(child);
        }

        @Override
        void themeUpdateChildren() {
            for (LAFChain chain : children)
                chain.themeUpdate();
        }
    }
}
