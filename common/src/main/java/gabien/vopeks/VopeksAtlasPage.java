/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import org.eclipse.jdt.annotation.NonNull;

import gabien.natives.BadGPU;
import gabien.natives.BadGPUEnum.Compare;
import gabien.natives.BadGPUEnum.PrimitiveType;
import gabien.natives.BadGPUEnum.TextureLoadFormat;
import gabien.render.AtlasPage;
import gabien.render.IImage;
import gabien.render.IImgRegion;
import gabien.render.ITexRegion;

/**
 * See parent class.
 * Created 17th July, 2023.
 */
public final class VopeksAtlasPage extends AtlasPage {
    /**
     * The parent instance.
     */
    public final Vopeks vopeks;

    private volatile boolean wasDisposed;
    private static final float[] RECT_VERTICES = {
        -1, -1,
         1, -1,
         1,  1,
        -1,  1
    };
    private static final short[] RECT_INDICES = {
        0, 1, 2, 0, 2, 3
    };

    public VopeksAtlasPage(Vopeks vopeks, String id, int w, int h) {
        super(id, w, h);
        this.vopeks = vopeks;
        vopeks.putTask((instance) -> {
            texture = instance.newTexture(w, h);
        });
    }

    @Override
    public void copyFrom(int srcx, int srcy, int srcw, int srch, int targetx, int targety, ITexRegion base) {
        IImgRegion iir = base.pickImgRegion(null);
        IImage srf = iir.getSurface();
        srf.batchReference(this);
        int srcr = srcx + srcw;
        int srcd = srcy + srch;
        float[] uvs = {
            iir.getS(srcx, srcy), iir.getT(srcx, srcy),
            iir.getS(srcr, srcy), iir.getT(srcr, srcy),
            iir.getS(srcr, srcd), iir.getT(srcr, srcd),
            iir.getS(srcx, srcd), iir.getT(srcx, srcd)
        };
        vopeks.putTask((instance) -> {
            BadGPU.drawGeomNoDS(texture, BadGPU.SessionFlags.MaskRGBA, 0, 0, 0, 0,
                0,
                2, RECT_VERTICES, 0, null, 0, 2, uvs, 0,
                PrimitiveType.Triangles, 0,
                0, 6, RECT_INDICES, 0,
                null, 0,
                targetx, targety, srcw, srch,
                srf.getTextureFromTask(), null, 0,
                null, 0, Compare.Always, 0,
                0
            );
        });
        srf.batchUnreference(this);
    }

    @Override
    public void shutdown() {
        if (!wasDisposed) {
            wasDisposed = true;
            // We're about to dispose, so clean up references
            batchReferenceBarrier();
            vopeks.putTask((instance) -> {
                if (texture != null)
                    texture.dispose();
            });
        }
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, TextureLoadFormat format, @NonNull int[] data, int dataOfs, @NonNull Runnable onDone) {
        VopeksImage.getPixelsAsync(vopeks, this, x, y, w, h, format, data, dataOfs, onDone);
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, TextureLoadFormat format, @NonNull byte[] data, int dataOfs, @NonNull Runnable onDone) {
        VopeksImage.getPixelsAsync(vopeks, this, x, y, w, h, format, data, dataOfs, onDone);
    }

    @Override
    public void batchFlush() {
    }
}
