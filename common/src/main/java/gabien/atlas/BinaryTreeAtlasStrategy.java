/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.atlas;

import org.eclipse.jdt.annotation.Nullable;

import gabien.ui.Rect;
import gabien.ui.Size;

/**
 * Atlasing strategy based on binary trees.
 * Created 18th June, 2023.
 */
public final class BinaryTreeAtlasStrategy extends SortingAtlasStrategy {
    public final static BinaryTreeAtlasStrategy INSTANCE = new BinaryTreeAtlasStrategy();

    private BinaryTreeAtlasStrategy() {
        super(SORT_HEIGHT_THEN_WIDTH);
    }

    @Override
    public Rect[] calculateSorted(Size atlasSize, Size[] placements) {
        Rect[] res = new Rect[placements.length];
        TreeNode root = new TreeNode(new Rect(atlasSize), null);
        for (int i = 0; i < placements.length; i++)
            res[i] = root.place(placements[i]);
        return res;
    }

    private static class TreeNode {
        final Rect location;
        Occupancy mode = Occupancy.NONE;
        @Nullable TreeNode a, b, p;
        int weight;
        TreeNode(Rect l, TreeNode p) {
            this.p = p;
            location = l;
            modWeight(1);
        }
        void modWeight(int mod) {
            weight += mod;
            if (p != null)
                p.modWeight(mod);
        }
        @Nullable Rect place(Size size) {
            boolean justSplit = false;
            if (mode == Occupancy.NONE) {
                boolean canSplitH = (location.width > 1 && size.width < location.width);
                boolean canSplitV = (location.height > 1 && size.height < location.height);
                int splitChoice = 0;
                if (canSplitH && canSplitV) {
                    // Try to make splits better.
                    if (location.width < location.height) {
                        splitChoice = 1;
                    } else {
                        splitChoice = 2;
                    }
                } else if (canSplitH) {
                    splitChoice = 2;
                } else if (canSplitV) {
                    splitChoice = 1;
                }
                // Note that the cuts are performed using size, not halfSize.
                if (splitChoice == 1) {
                    // split: V - falls through to split case
                    mode = Occupancy.SPLIT;
                    a = new TreeNode(new Rect(location.x, location.y, location.width, size.height), this);
                    b = new TreeNode(new Rect(location.x, location.y + size.height, location.width, location.height - size.height), this);
                    justSplit = true;
                } else if (splitChoice == 2) {
                    // split: H - falls through to split case
                    mode = Occupancy.SPLIT;
                    a = new TreeNode(new Rect(location.x, location.y, size.width, location.height), this);
                    b = new TreeNode(new Rect(location.x + size.width, location.y, location.width - size.width, location.height), this);
                    justSplit = true;
                } else if (size.width <= location.width && size.height <= location.height) {
                    // unable to find a good split
                    mode = Occupancy.FULL;
                    return new Rect(location.x, location.y, size.width, size.height);
                }
            }
            if (mode == Occupancy.SPLIT) {
                // split node... prefer to use already highly allocated areas if necessary
                // note that this is written to prefer A-side in case of a tie
                // this is because A-side is "set up" by above code to match size
                Rect res = null;
                if (a.weight < b.weight) {
                    res = b.place(size);
                    if (res == null)
                        res = a.place(size);
                } else {
                    res = a.place(size);
                    if (res == null)
                        res = b.place(size);
                }
                if (justSplit && res == null) {
                    // If we just split, and that didn't work out, we probably should *un*split.
                    // But check occupancy to be sure, wouldn't want to upset anything.
                    if (a.mode == Occupancy.NONE && b.mode == Occupancy.NONE) {
                        mode = Occupancy.NONE;
                        // zero out weights...
                        a.modWeight(-a.weight);
                        b.modWeight(-b.weight);
                        // ...and remove
                        a = null;
                        b = null;
                    }
                }
                return res;
            }
            return null;
        }
    }
    private enum Occupancy {
        NONE, FULL, SPLIT
    }
}
