/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.natives.BadGPU;
import gabien.render.IGrDriver;
import gabien.render.IReplicatedTexRegion;

/**
 * Created on 6/20/17 as NullOsbDriver. Migrated to gabien.backendhelp.NullGrDriver 7th June, 2023.
 */
public class NullGrDriver extends IGrDriver {
    public NullGrDriver() {
        super(null, 0, 0);
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull int[] buffer, int bufferOfs, @NonNull Runnable onDone) {
        onDone.run();
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull byte[] buffer, int bufferOfs, @NonNull Runnable onDone) {
        onDone.run();
    }

    @Override
    public void rawBatchXYST(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2) {
    }

    @Override
    public void rawBatchXYST(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float x1, float y1, float s1, float t1, float x2, float y2, float s2, float t2, float x3, float y3, float s3, float t3) {
    }

    @Override
    public void rawBatchXYSTRGBA(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float r0, float g0, float b0, float a0, float x1, float y1, float s1, float t1, float r1, float g1, float b1, float a1, float x2, float y2, float s2, float t2, float r2, float g2, float b2, float a2) {
    }

    @Override
    public void rawBatchXYSTRGBA(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, TilingMode tilingMode, @Nullable IReplicatedTexRegion iU, float x0, float y0, float s0, float t0, float r0, float g0, float b0, float a0, float x1, float y1, float s1, float t1, float r1, float g1, float b1, float a1, float x2, float y2, float s2, float t2, float r2, float g2, float b2, float a2, float x3, float y3, float s3, float t3, float r3, float g3, float b3, float a3) {
    }

    @Override
    public void clearAll(int i, int i0, int i1) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void batchFlush() {
    }
}
