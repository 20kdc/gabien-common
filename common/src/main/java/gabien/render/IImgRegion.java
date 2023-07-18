/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.render;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a texture region backed by an IImage with some transformations.
 * You should basically never need to refer to this interface outside of very low-level code.
 * (The kind of code that cares about calling getSurface.)
 * Created 14th June, 2023.
 */
public interface IImgRegion extends ITexRegion {
    /**
     * Gets the underlying image of this texture region.
     */
    @NonNull IImage getSurface();

    /**
     * Returns the texture S value for a given input coordinate.
     */
    float getS(float x, float y);

    /**
     * Returns the texture T value for a given input coordinate.
     */
    float getT(float x, float y);

    /**
     * The picked texture region of a raw texture region is always itself.
     */
    @Override
    default IImgRegion pickImgRegion(@Nullable IImage lastSurface) {
        return this;
    }

    /**
     * Creates a subregion.
     */
    @Override
    @NonNull IImgRegion subRegion(float x, float y, float w, float h);
}
