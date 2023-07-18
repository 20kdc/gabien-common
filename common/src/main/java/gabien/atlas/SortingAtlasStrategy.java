/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.atlas;

import java.util.Arrays;
import java.util.Comparator;

import gabien.ui.Rect;
import gabien.ui.Size;

/**
 * Some atlasing strategies pre-sort their placements.
 * Created 18th June, 2023.
 */
public abstract class SortingAtlasStrategy implements IAtlasStrategy {
    public final Comparator<Size> sortingFn;
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

    public SortingAtlasStrategy(Comparator<Size> sf) {
        sortingFn = sf;
    }

    @Override
    public final Rect[] calculate(Size atlasSize, Size[] placements) {
        Integer[] sortedPlacementOrder = new Integer[placements.length];
        for (int i = 0; i < placements.length; i++)
            sortedPlacementOrder[i] = i;
        Arrays.sort(sortedPlacementOrder, (a, b) -> {
            Size sa = placements[a];
            Size sb = placements[b];
            return sortingFn.compare(sa, sb);
        });
        Rect[] res = new Rect[placements.length];
        Size[] placementsSorted = new Size[placements.length];
        for (int i = 0; i < placements.length; i++)
            placementsSorted[i] = placements[sortedPlacementOrder[i]];
        Rect[] ret = calculateSorted(atlasSize, placementsSorted);
        for (int i = 0; i < placements.length; i++)
            res[sortedPlacementOrder[i]] = ret[i];
        return res;
    }

    public abstract Rect[] calculateSorted(Size atlasSize, Size[] placements);
}
