/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import gabien.natives.BadGPU;
import gabien.natives.BadGPUUnsafe;
import gabien.natives.BadGPU.Instance;
import gabien.render.IGrDriver;
import gabien.render.IImage;
import gabien.uslx.append.ObjectPool;
import gabien.vopeks.Vopeks.ITask;

/**
 * Vopeks batch. This does "call optimizations" like dropping texcoords if not used / etc.
 * Pulled out of VopeksBatchingSurface 17th October 2023.
 */
class VopeksBatch implements ITask {
    private final Vopeks vopeks;

    private final IImage parent;

    private final ObjectPool<VopeksBatch> srcPool;

    /**
     * @param vopeksBatchingSurface
     */
    VopeksBatch(Vopeks vopeks, IImage parent, ObjectPool<VopeksBatch> pool) {
        this.vopeks = vopeks;
        this.parent = parent;
        this.srcPool = pool;
    }

    int cropL, cropU, cropR, cropD;
    int vertexCount;
    int blendMode = IGrDriver.BLEND_NONE;
    int drawFlagsEx = 0;
    IImage tex;
    float[] megabuffer; int verticesOfs, coloursOfs, texCoordsOfs;
    boolean hasColours;
    boolean cropEssential;

    @Override
    public void run(Instance instance) {
        BadGPU.Texture screen = parent.getTextureFromTask();
        if (screen == null) {
            System.err.println("VopeksBatchingSurface: Texture disappeared from " + parent + ". Someone try something silly?");
            return;
        }

        BadGPU.Texture tx = tex != null ? tex.getTextureFromTask() : null;
        long tx2 = tx != null ? tx.pointer : 0;
        int alphaComp = BadGPU.Compare.Always.value;
        int drawFlags = 0;
        if (blendMode != IGrDriver.BLEND_NONE)
            drawFlags |= BadGPU.DrawFlags.Blend;
        // In the normal blend mode, an alpha of 0 leads to a NOP, so discard those pixels.
        if (blendMode == IGrDriver.BLEND_NORMAL)
            alphaComp = BadGPU.Compare.Greater.value;

        drawFlags |= drawFlagsEx;

        BadGPUUnsafe.drawGeomNoDS(screen.pointer, BadGPU.SessionFlags.MaskRGBA | BadGPU.SessionFlags.Scissor,
                cropL, cropU, cropR - cropL, cropD - cropU,
                drawFlags,
                2, megabuffer, verticesOfs, hasColours ? megabuffer : null, coloursOfs, 2, tx == null ? null : megabuffer, texCoordsOfs,
                BadGPU.PrimitiveType.Triangles.value, 1,
                0, vertexCount, null, 0,
                null, 0,
                0, 0, parent.width, parent.height,
                tx2, null, 0,
                null, 0, alphaComp, 0,
                blendMode);
        vopeks.floatPool.finish(megabuffer);
        srcPool.finish(this);
    }

    public boolean matchesState(boolean cropEssential, int cropL, int cropU, int cropR, int cropD, int blendMode, int drawFlagsEx, IImage tex) {
        if (cropEssential) {
            if (cropL != this.cropL || cropU != this.cropU || cropR != this.cropR || cropD != this.cropD) {
                // System.out.println("break batch: SCO " + cropL + "," + cropU + "," + cropR + "," + cropD + " -> " + this.cropL + "," + this.cropU + "," + this.cropR + "," + this.cropD);
                return false;
            }
        } else if (this.cropEssential) {
            if (cropL > this.cropL || cropU > this.cropU || cropR < this.cropR || cropD < this.cropD) {
                // System.out.println("break batch: SCO on a non-essential crop");
                return false;
            }
        }
        if (tex != this.tex) {
            // System.out.println("break batch: tex: " + tex + " -> " + this.tex);
            return false;
        }
        if (blendMode != this.blendMode) {
            // System.out.println("break batch: blendMode: " + blendMode + " -> " + this.blendMode);
            return false;
        }
        if (drawFlagsEx != this.drawFlagsEx) {
            // System.out.println("break batch: tilingMode: " + tilingMode + " -> " + this.tilingMode);
            return false;
        }
        return true;
    }
}