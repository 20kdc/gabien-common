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

    LAFChain(LAFChain.Node lafParent) {
        // to prevent running a theme update on an incompletely initialized element:
        // calculate the theme locally!
        this.lafParent = lafParent;
        if (lafParent != null)
            lafParentHolder = lafParent.children.addWeak(this);
        theme = calculateTheme();
    }

    private @Nullable LAFChain.Node lafParent;
    // This is what holds the LAFChain to the parent.
    // If the LAFChain would be GC'd, the holder gets finalized.
    // This disconnects things upstream.
    private RefSyncSet<LAFChain>.Holder lafParentHolder;

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
     */
    public final @Nullable LAFChain getLAFParent() {
        return lafParent;
    }

    /**
     * Sets the theming parent and updates themes.
     */
    public void setLAFParent(@Nullable LAFChain.Node parent) {
        if (lafParentHolder != null) {
           lafParent.children.remove(lafParentHolder);
           lafParentHolder = null;
        }
        lafParent = parent;
        if (lafParent != null)
            lafParentHolder = lafParent.children.addWeak(this);
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

    private final @NonNull Theme calculateTheme() {
        Theme override = themeOverride;
        LAFChain parent = lafParent;
        if (override != null) {
            return override;
        } else if (parent != null) {
            return parent.theme;
        } else {
            return Theme.ROOT;
        }
    }

    /**
     * Called when something occurs that might change the theme.
     * Package-private; this method is only called here.
     * This has to be called by the element parents (UIPanel, UIProxy).
     * This is so their children magically update.
     */
    private final void themeUpdate() {
        @NonNull Theme newTheme = calculateTheme();
        if (newTheme != theme) {
            // System.out.println("theme change " + theme + " -> " + newTheme + " @ " + this);
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

        @Override
        void themeUpdateChildren() {
            for (LAFChain chain : children)
                chain.themeUpdate();
        }
    }
}
