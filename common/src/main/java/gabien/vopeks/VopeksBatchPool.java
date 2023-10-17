/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import org.eclipse.jdt.annotation.NonNull;

import gabien.render.IGrDriver;
import gabien.render.IImage;
import gabien.uslx.append.ObjectPool;

/**
 * Vopeks batch pool.
 * Pulled out of VopeksBatchingSurface 17th October 2023.
 */
class VopeksBatchPool extends ObjectPool<VopeksBatch> {
    private final Vopeks vopeks;

    private final IImage parent;

    public VopeksBatchPool(Vopeks vopeks, IImage parent, int expandChunkSize) {
        super(expandChunkSize);
        this.vopeks = vopeks;
        this.parent = parent;
    }

    @Override
    protected @NonNull VopeksBatch gen() {
        return new VopeksBatch(vopeks, parent, this);
    }
    @Override
    public void reset(@NonNull VopeksBatch element) {
        element.cropL = 0;
        element.cropU = 0;
        element.cropR = 0;
        element.cropD = 0;
        element.cropEssential = false;
        element.vertexCount = 0;
        element.blendMode = IGrDriver.BLEND_NONE;
        element.drawFlagsEx = 0;
        element.tex = null;
        element.megabuffer = null;
        element.verticesOfs = 0;
        element.coloursOfs = 0;
        element.texCoordsOfs = 0;
        element.hasColours = false;
    }
}