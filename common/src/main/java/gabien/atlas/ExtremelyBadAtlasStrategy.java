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

import gabien.ui.Rect;
import gabien.ui.Size;

/**
 * This is not how you atlas.
 * Rather, this is how you absolutely don't atlas.
 * The useful thing about this strategy is that it's so awful it should break badly written code.
 * Created 17th June, 2023.
 */
public final class ExtremelyBadAtlasStrategy implements IAtlasStrategy {
    public final static ExtremelyBadAtlasStrategy INSTANCE = new ExtremelyBadAtlasStrategy();

    private ExtremelyBadAtlasStrategy() {
    }

    @Override
    @NonNull
    public Instance instance(@NonNull Size atlasSize) {
        return new Instance() {
            boolean hasPlacement = false;
            @Override
            @Nullable
            public Rect add(@NonNull Size size) {
                if (hasPlacement)
                    return null;
                if (size.width > atlasSize.width || size.height > atlasSize.height)
                    return null;
                hasPlacement = true;
                return new Rect(size);
            }
        };
    }

    @Override
    public Comparator<Size> getSortingAlgorithm() {
        return null;
    }
}
