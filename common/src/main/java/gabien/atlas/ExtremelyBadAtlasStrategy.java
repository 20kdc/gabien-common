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
 * This is not how you atlas.
 * Rather, this is how you absolutely don't atlas.
 * The useful thing about this strategy is that it's so awful it should break badly written code.
 * Created 17th June, 2023.
 */
public final class ExtremelyBadAtlasStrategy implements IAtlasStrategy {
    public final ExtremelyBadAtlasStrategy INSTANCE = new ExtremelyBadAtlasStrategy();

    private ExtremelyBadAtlasStrategy() {
    }

    @Override
    public @NonNull Instance withParameters(int w, int h, boolean border) {
        return new Instance() {
            boolean hasMadeOneAllocation;
            @Override
            public @Nullable Rect allocate(int pw, int ph) {
                if (hasMadeOneAllocation)
                    return null;
                if (pw > w || ph > h)
                    return null;
                hasMadeOneAllocation = true;
                return new Rect(0, 0, pw, ph);
            }
        };
    }
}
