/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.natives.BadGPU;
import gabien.natives.BadGPUEnum.BlendOp;
import gabien.natives.BadGPUEnum.BlendWeight;
import gabien.render.ITexRegion;

/**
 * Represents a buffer that can be drawn to.
 * Created on 04/06/17.
 */
public interface IGrDriver extends IImage {
    public static final int BLEND_NONE = BadGPU.blendProgram(
        BlendWeight.One, BlendWeight.Zero, BlendOp.Add,
        BlendWeight.One, BlendWeight.Zero, BlendOp.Add
    );
    public static final int BLEND_NORMAL = BadGPU.blendProgram(
        BlendWeight.SrcA, BlendWeight.InvertSrcA, BlendOp.Add,
        BlendWeight.SrcA, BlendWeight.InvertSrcA, BlendOp.Add
    );
    public static final int BLEND_ADD = BadGPU.blendProgram(
        BlendWeight.One, BlendWeight.One, BlendOp.Add,
        BlendWeight.Zero, BlendWeight.One, BlendOp.Add
    );
    public static final int BLEND_SUB = BadGPU.blendProgram(
        BlendWeight.One, BlendWeight.One, BlendOp.ReverseSub,
        BlendWeight.Zero, BlendWeight.One, BlendOp.Add
    );

    // Basic blit operations.
    void blitImage(float srcx, float srcy, float srcw, float srch, float x, float y, ITexRegion i);
    void blitTiledImage(float x, float y, float w, float h, IImage cachedTile);
    void blitScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, ITexRegion i);
    default void blitScaledImage(float x, float y, float acw, float ach, ITexRegion i) {
        blitScaledImage(0, 0, i.getRegionWidth(), i.getRegionHeight(), x, y, acw, ach, i);
    }

    /**
     * Legacy interface. This is just inherently awkward.
     */
    default void blendRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, ITexRegion i, boolean blendSub) {
        blitRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, blendSub ? BLEND_SUB : BLEND_ADD);
    }

    default void blitRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, ITexRegion i) {
        blitRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, BLEND_NORMAL);
    }

    /**
     * This operation can be considered the logical basis of all the blit-series operations in IGrDriver.
     * (This isn't how it actually works ever since Vopeks, but it's close. Mainly, tiled acts differently.)
     * Importantly, here's how rotation is applied:
     * Firstly, the image is placed as if no rotation were involved.
     * Then the destination is rotated anticlockwise by angle degrees.
     */
    void blitRotatedScaledImage(float srcx, float srcy, float srcw, float srch, float x, float y, float acw, float ach, float angle, ITexRegion i, int blendSub);

    void clearAll(int i, int i0, int i1);

    default void clearRect(int r, int g, int b, float x, float y, float width, float height) {
        clearRectAlpha(r, g, b, 255, x, y, width, height);
    }

    void clearRectAlpha(int r, int g, int b, int a, float x, float y, float width, float height);

    /**
     * Stop all drawing operations. Makes an OsbDriver unusable.
     */
    void shutdown();

    // Gets the local ST buffer, in the order: translateX, translateY, cropL, cropU, cropR, cropD
    // The crop coordinates are independent of the translation coordinates.
    int[] getLocalST();
    // Propagates changes to the local ST buffer (changes only take effect from this point on)
    void updateST();

    /**
     * Ok, so this is a particularly evil little call.
     * This is like getTextureFromTask, but it releases custody of the texture.
     * This irrevocably alters the image (to being non-existent).
     * As such, you can only do this on an IGrDriver.
     */
    @Nullable BadGPU.Texture releaseTextureCustodyFromTask();

    /**
     * This converts an IGrDriver into an immutable image.
     * Data-wise, this is an in-place operation and shuts down the IGrDriver.
     */
    @NonNull IImage convertToImmutable(@Nullable String debugId);
}
