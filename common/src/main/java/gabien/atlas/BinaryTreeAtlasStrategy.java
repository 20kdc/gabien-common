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
 * Atlasing strategy based on binary trees.
 * Created 18th June, 2023.
 */
public final class BinaryTreeAtlasStrategy implements IAtlasStrategy {
    public final static BinaryTreeAtlasStrategy INSTANCE = new BinaryTreeAtlasStrategy();

    @Override
    @NonNull
    public Instance instance(@NonNull Size atlasSize) {
        TreeNode root = new TreeNode(new Rect(atlasSize), null);
        return new Instance() {
            @Override
            @Nullable
            public Rect add(@NonNull Size size) {
                return root.place(size);
            }
        };
    }

    @Override
    @Nullable
    public Comparator<Size> getSortingAlgorithm() {
        return SORT_HEIGHT_THEN_WIDTH;
    }

    private static class TreeNode {
        final Rect location;
        Occupancy mode = Occupancy.NONE;
        @Nullable TreeNode a, b, p;
        int weight;
        TreeNode(Rect l, @Nullable TreeNode p) {
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
                TreeNode a = this.a;
                TreeNode b = this.b;
                assert a != null;
                assert b != null;
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
                        this.a = null;
                        this.b = null;
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
