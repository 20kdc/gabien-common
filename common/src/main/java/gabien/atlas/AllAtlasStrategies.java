/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.atlas;

/**
 * Main list of atlas strategies.
 * Created 18th June, 2023.
 */
public final class AllAtlasStrategies {
    public static final IAtlasStrategy[] strategies = {
        BinaryTreeAtlasStrategy.INSTANCE,
        ExtremelyBadAtlasStrategy.INSTANCE
    };

    private AllAtlasStrategies() {
    }
}
