/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.atlas;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.ui.Rect;

/**
 * Abstracts the details of how allocation within an atlas page works.
 * Created 17th June, 2023.
 */
public interface IAtlasStrategy {
    /**
     * Instances an atlasing strategy.
     * One strategy object per atlas, please.
     * Strategy instances do NOT have to be thread-safe.
     * w and h are the atlas size. border enables/disables a 1-pixel border.
     */
    @NonNull Instance withParameters(int w, int h, boolean border);

    interface Instance {
        /**
         * Allocates a rectangle of the given size from the atlas.
         */
        @Nullable Rect allocate(int w, int h);
    }
}
