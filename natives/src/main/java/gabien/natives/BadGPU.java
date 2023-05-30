/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Safe wrapper for BadGPU.
 * VERSION: 0.13.0
 * Created 30th May, 2023.
 */
public abstract class BadGPU extends BadGPUEnum {
    private BadGPU() {
    }

    public static @NonNull Instance newInstance(int newInstanceFlags) {
        long instance = BadGPUUnsafe.newInstance(newInstanceFlags, InstanceCreationException.class);
        if (instance == 0)
            throw new InstanceCreationException("Unknown error");
        BadGPUUnsafe.unbindInstance(instance);
        return new Instance(instance);
    }

    @SuppressWarnings("serial")
    public static class InstanceCreationException extends RuntimeException {
        public InstanceCreationException(String text) {
            super(text);
        }
    }

    private static final class InstanceBinder implements AutoCloseable {
        private final ReentrantLock rl = new ReentrantLock();
        private final long instance;
        private volatile boolean shutdown;
        private InstanceBinder(long l) {
            instance = l;
        }
        public InstanceBinder bind() {
            if (shutdown)
                return null;
            rl.lock();
            // System.out.println("LOCK");
            BadGPUUnsafe.bindInstance(instance);
            return this;
        }
        public void shutdown() {
            if (shutdown)
                return;
            shutdown = true;
            // System.out.println("UNLOCK");
            rl.unlock();
        }
        @Override
        public void close() {
            if (shutdown)
                return;
            BadGPUUnsafe.unbindInstance(instance);
            // System.out.println("UNLOCK");
            rl.unlock();
        }
    }

    /**
     * This is called BadGPUObject, but that was changed here to avoid confusion with Java Object.
     */
    public static class Ref {
        public final InstanceBinder syncObject;
        public final long pointer;
        protected volatile boolean valid = true;

        private Ref(InstanceBinder ib, long l) {
            syncObject = ib;
            pointer = l;
        }

        private Ref(Ref i, long l) {
            syncObject = i.syncObject;
            pointer = l;
        }

        public final boolean isValid() {
            return valid;
        }

        public final void dispose() {
            try (InstanceBinder ib = syncObject.bind()) {
                if (valid) {
                    valid = false;
                    BadGPUUnsafe.unref(pointer);
                    if (pointer == ib.instance)
                        syncObject.shutdown();
                }
            }
        }
    }
    public static final class Instance extends Ref {
        private Instance(long l) {
            super(new InstanceBinder(l), l);
        }

        public @Nullable String getMetaInfo(MetaInfoType type) {
            try (InstanceBinder ib = syncObject.bind()) {
                if (!valid)
                    throw new InvalidatedPointerException(this);
                return BadGPUUnsafe.getMetaInfo(pointer, type.value);
            }
        }

        public void flush() {
            try (InstanceBinder ib = syncObject.bind()) {
                if (!valid)
                    throw new InvalidatedPointerException(this);
                BadGPUUnsafe.flushInstance(pointer);
            }
        }

        public @Nullable Texture newTexture(int flags, int width, int height, ByteBuffer data, int offset) {
            if (width >= 32768 || height >= 32768 || width < 1 || height < 1)
                throw new IllegalArgumentException("Width/height not 0-32767.");
            if (data != null) {
                int size = width * height;
                size *= ((flags & TextureFlags.HasAlpha) != 0) ? 4 : 3;
                int dataCap = data.capacity();
                if (offset < 0 || offset > dataCap)
                    throw new IllegalArgumentException("Offset not within buffer.");
                if ((dataCap - offset) < size)
                    throw new IllegalArgumentException("Region not within buffer.");
            }
            long res;
            try (InstanceBinder ib = syncObject.bind()) {
                if (!valid)
                    throw new InvalidatedPointerException(this);
                res = BadGPUUnsafe.newTexture(pointer, flags, width, height, data, offset);
            }
            return res == 0 ? null : new Texture(this, res);
        }
        public @Nullable DSBuffer newDSBuffer(long instance, int width, int height) {
            long res;
            try (InstanceBinder ib = syncObject.bind()) {
                if (!valid)
                    throw new InvalidatedPointerException(this);
                res = BadGPUUnsafe.newDSBuffer(pointer, width, height);
            }
            return res == 0 ? null : new DSBuffer(this, res);
        }
    }
    public static final class Texture extends Ref {
        private Texture(Ref i, long l) {
            super(i, l);
        }
        public boolean generateMipmap() {
            try (InstanceBinder ib = syncObject.bind()) {
                if (!valid)
                    throw new InvalidatedPointerException(this);
                return BadGPUUnsafe.generateMipmap(pointer);
            }
        }
        public boolean readPixels(int x, int y, int width, int height, ByteBuffer data, long offset) {
            if (width == 0 || height == 0)
                return true;
            if (width >= 32768 || height >= 32768 || width < 1 || height < 1)
                throw new IllegalArgumentException("Width/height not 0-32767.");
            if (data == null)
                throw new IllegalArgumentException("data must not be null.");
            int size = width * height * 4;
            int dataCap = data.capacity();
            if (offset < 0 || offset > dataCap)
                throw new IllegalArgumentException("Offset not within buffer.");
            if ((dataCap - offset) < size)
                throw new IllegalArgumentException("Region not within buffer.");
            try (InstanceBinder ib = syncObject.bind()) {
                if (!valid)
                    throw new InvalidatedPointerException(this);
                return BadGPUUnsafe.readPixels(pointer, x, y, width, height, data, offset);
            }
        }
    }
    public static final class DSBuffer extends Ref {
        private DSBuffer(Ref i, long l) {
            super(i, l);
        }
    }
    // inter-object operations
    public static boolean drawClear(
        @Nullable Texture sTexture, @Nullable DSBuffer sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        float cR, float cG, float cB, float cA, float depth, int stencil
    ) {
        if (sTexture == null && sDSBuffer == null)
            return false;
        if (sTexture != null && sDSBuffer != null)
            assert sTexture.syncObject == sDSBuffer.syncObject;
        @SuppressWarnings("resource")
        InstanceBinder syncObj = sTexture != null ? sTexture.syncObject : sDSBuffer.syncObject;
        try (InstanceBinder ib = syncObj.bind()) {
            if (sTexture != null && !sTexture.valid)
                throw new InvalidatedPointerException(sTexture);
            if (sDSBuffer != null && !sDSBuffer.valid)
                throw new InvalidatedPointerException(sDSBuffer);
            return BadGPUUnsafe.drawClear(
                    sTexture != null ? sTexture.pointer : 0, sDSBuffer != null ? sDSBuffer.pointer : 0, sFlags, sScX, sScY, sScWidth, sScHeight,
                    cR, cG, cB, cA, depth, stencil);
        }
    }
    private static int getVertexCount(int iStart, int iCount, short[] indices, int indicesOfs) {
        // this is a quirk of the approach to JNI used here, it's kinda funny and also sad
        iStart += indicesOfs;
        int vCount = 0;
        // Since this iterates through all indices, it also implicitly validates the bounds of the array.
        for (int i = 0; i < iCount; i++) {
            int efCount = (indices[i] & 0xFFFF) + 1;
            if (efCount > vCount)
                vCount = efCount;
            iStart++;
        }
        return vCount;
    }
    private static void checkVL(int flags, float[] vPos, int vPosOfs, float[] vCol, int vColOfs, float[] vTC, int vTCOfs,
            int iStart, int iCount, short[] indices, int indicesOfs,
            float[] matrixA, int matrixAOfs, float[] matrixB, int matrixBOfs, float[] matrixT, int matrixTOfs) {
        // Collate vertex counts
        int vCount = getVertexCount(iStart, iCount, indices, indicesOfs);
        int cCount = ((flags & DrawFlags.FreezeColour) != 0) ? 1 : vCount;
        int tCount = ((flags & DrawFlags.FreezeTC) != 0) ? 1 : vCount;
        // Check them
        if (vPosOfs < 0 || (vPosOfs + vCount) > vPos.length)
            throw new IllegalArgumentException("vPos out of bounds");
        if (vColOfs < 0 || (vColOfs + cCount) > vCol.length)
            throw new IllegalArgumentException("vCol out of bounds");
        if (vTCOfs < 0 || (vTCOfs + tCount) > vTC.length)
            throw new IllegalArgumentException("vTC out of bounds");
        // Matrices
        if (matrixA != null)
            if (matrixAOfs < 0 || (matrixAOfs + 16) > matrixA.length)
                throw new IllegalArgumentException("matrixA out of bounds");
        if (matrixB != null)
            if (matrixBOfs < 0 || (matrixBOfs + 16) > matrixB.length)
                throw new IllegalArgumentException("matrixB out of bounds");
        if (matrixT != null)
            if (matrixTOfs < 0 || (matrixTOfs + 16) > matrixT.length)
                throw new IllegalArgumentException("matrixT out of bounds");
    }
    public static boolean drawGeom(
        @Nullable Texture sTexture, @Nullable DSBuffer sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        int flags,
        float[] vPos, int vPosOfs, float[] vCol, int vColOfs, float[] vTC, int vTCOfs,
        PrimitiveType pType, float plSize,
        int iStart, int iCount, short[] indices, int indicesOfs,
        float[] matrixA, int matrixAOfs, float[] matrixB, int matrixBOfs,
        float depthN, float depthF,
        int vX, int vY, int vW, int vH,
        @Nullable Texture texture, float[] matrixT, int matrixTOfs,
        float poFactor, float poUnits,
        float alphaTestMin,
        Compare stFunc, int stRef, int stMask,
        StencilOp stSF, StencilOp stDF, StencilOp stDP,
        Compare dtFunc,
        BlendWeight bwRGBS, BlendWeight bwRGBD, BlendEquation beRGB,
        BlendWeight bwAS, BlendWeight bwAD, BlendEquation beA
    ) {
        if (sTexture == null && sDSBuffer == null)
            return false;
        if (sTexture != null && sDSBuffer != null)
            assert sTexture.syncObject == sDSBuffer.syncObject;
        @SuppressWarnings("resource")
        InstanceBinder syncObj = sTexture != null ? sTexture.syncObject : sDSBuffer.syncObject;
        if (texture != null)
            assert syncObj == texture.syncObject;
        // actual parameter checking
        checkVL(flags, vPos, vPosOfs, vCol, vColOfs, vTC, vTCOfs,
                iStart, iCount, indices, indicesOfs,
                matrixA, matrixAOfs, matrixB, matrixBOfs, matrixT, matrixTOfs);
        // continue
        try (InstanceBinder ib = syncObj.bind()) {
            if (sTexture != null && !sTexture.valid)
                throw new InvalidatedPointerException(sTexture);
            if (sDSBuffer != null && !sDSBuffer.valid)
                throw new InvalidatedPointerException(sDSBuffer);
            if (texture != null && !texture.valid)
                throw new InvalidatedPointerException(texture);
            return BadGPUUnsafe.drawGeom(
                    sTexture != null ? sTexture.pointer : 0, sDSBuffer != null ? sDSBuffer.pointer : 0, sFlags, sScX, sScY, sScWidth, sScHeight,
                    flags,
                    vPos, vPosOfs, vCol, vColOfs, vTC, vTCOfs,
                    pType.value, plSize,
                    iStart, iCount, indices, indicesOfs,
                    matrixA, matrixAOfs, matrixB, matrixBOfs,
                    depthN, depthF,
                    vX, vY, vW, vH,
                    texture != null ? texture.pointer : 0, matrixT, matrixTOfs,
                    poFactor, poUnits,
                    alphaTestMin,
                    stFunc.value, stRef, stMask,
                    stSF.value, stDF.value, stDP.value,
                    dtFunc.value,
                    bwRGBS.value, bwRGBD.value, beRGB.value,
                    bwAS.value, bwAD.value, beA.value);
        }
    }
    public static boolean drawGeomNoDS(
        @Nullable Texture sTexture, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        int flags,
        float[] vPos, int vPosOfs, float[] vCol, int vColOfs, float[] vTC, int vTCOfs,
        PrimitiveType pType, float plSize,
        int iStart, int iCount, short[] indices, int indicesOfs,
        float[] matrixA, int matrixAOfs, float[] matrixB, int matrixBOfs,
        int vX, int vY, int vW, int vH,
        @Nullable Texture texture, float[] matrixT, int matrixTOfs,
        float alphaTestMin,
        BlendWeight bwRGBS, BlendWeight bwRGBD, BlendEquation beRGB,
        BlendWeight bwAS, BlendWeight bwAD, BlendEquation beA
    ) {
        if (sTexture == null)
            return false;
        if (texture != null)
            assert sTexture.syncObject == texture.syncObject;
        // actual parameter checking
        checkVL(flags, vPos, vPosOfs, vCol, vColOfs, vTC, vTCOfs,
                iStart, iCount, indices, indicesOfs,
                matrixA, matrixAOfs, matrixB, matrixBOfs, matrixT, matrixTOfs);
        // continue
        try (InstanceBinder ib = sTexture.syncObject.bind()) {
            if (!sTexture.valid)
                throw new InvalidatedPointerException(sTexture);
            if (texture != null && !texture.valid)
                throw new InvalidatedPointerException(texture);
            return BadGPUUnsafe.drawGeomNoDS(
                    sTexture != null ? sTexture.pointer : 0, sFlags, sScX, sScY, sScWidth, sScHeight,
                    flags,
                    vPos, vPosOfs, vCol, vColOfs, vTC, vTCOfs,
                    pType.value, plSize,
                    iStart, iCount, indices, indicesOfs,
                    matrixA, matrixAOfs, matrixB, matrixBOfs,
                    vX, vY, vW, vH,
                    texture != null ? texture.pointer : 0, matrixT, matrixTOfs,
                    alphaTestMin,
                    bwRGBS.value, bwRGBD.value, beRGB.value,
                    bwAS.value, bwAD.value, beA.value);
        }
    }
}
