/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.atlas;

import java.util.Comparator;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;

/**
 * Abstracts the details of how allocation within an atlas page works.
 * Created 17th June, 2023.
 */
public interface IAtlasStrategy {
    public static final Comparator<Size> SORT_HEIGHT_THEN_WIDTH = (sa, sb) -> {
        // order based on height
        if (sa.height > sb.height)
            return -1;
        if (sa.height < sb.height)
            return 1;
        // order based on width
        if (sa.width > sb.width)
            return -1;
        if (sa.width < sb.width)
            return 1;
        return 0;
    };

    /**
     * Creates a new instance.
     * @param atlasSize The atlas size.
     * @return A newly prepared instance.
     */
    @NonNull Instance instance(@NonNull Size atlasSize);

    /**
     * Gets a recommended mechanism for sorting placements, if any.
     */
    @Nullable Comparator<Size> getSortingAlgorithm();

    interface Instance {
        /**
         * Adds a placement to the atlas being built.
         * Returns null if the placement couldn't be added.
         * @return Placement or null on failure.
         */
        @Nullable Rect add(@NonNull Size size);
    }
}
