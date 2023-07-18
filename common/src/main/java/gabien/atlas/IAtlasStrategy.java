/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.atlas;

import gabien.ui.Rect;
import gabien.ui.Size;

/**
 * Abstracts the details of how allocation within an atlas page works.
 * Created 17th June, 2023.
 */
public interface IAtlasStrategy {
    /**
     * Returns the placements.
     * Note that each element of the array can be null on failure-to-place.
     * @param atlasSize Atlas size.
     * @param placements Input placements.
     */
    Rect[] calculate(Size atlasSize, Size[] placements);
}
