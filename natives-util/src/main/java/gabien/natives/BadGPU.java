/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.append.ThreadOwned;

/**
 * Safe wrapper for BadGPU.
 * VERSION: 0.26.0
 * Created 30th May, 2023.
 */
public abstract class BadGPU extends BadGPUEnum {
    private BadGPU() {
    }

    public static Instance newInstance(int newInstanceFlags) {
        long instance = BadGPUUnsafe.newInstance(newInstanceFlags, InstanceCreationException.class);
        if (instance == 0)
            throw new InstanceCreationException("Unknown error");
        return new Instance(instance);
    }

    @SuppressWarnings("serial")
    public static class InstanceCreationException extends RuntimeException {
        public InstanceCreationException(String text) {
            super(text);
        }
    }

    @SuppressWarnings("serial")
    public static class InstanceBindException extends RuntimeException {
        public InstanceBindException() {
            super("Instance bind failed.");
        }
    }

    /**
     * Manages transfer of the BadGPU instance between threads.
     */
    private static final class TransferImpl extends ThreadOwned {
        private final long instance;
        private volatile boolean shutdown;

        private TransferImpl(long l) {
            instance = l;
        }

        @Override
        protected void bindImpl() {
            if (shutdown)
                return;
            if (!BadGPUUnsafe.bindInstance(instance))
                throw new InstanceBindException();
        }

        @Override
        protected void unbindImpl() {
            if (shutdown)
                return;
            BadGPUUnsafe.unbindInstance(instance);
        }
    }

    /**
     * This is called BadGPUObject, but that was changed here to avoid confusion with Java Object.
     */
    public static class Ref {
        public final ThreadOwned syncObject;
        final TransferImpl syncObjectInternal;
        public final long pointer;
        protected volatile boolean valid = true;

        private Ref(TransferImpl ib, long l) {
            syncObject = ib;
            syncObjectInternal = ib;
            pointer = l;
        }

        private Ref(Ref i, long l) {
            syncObject = i.syncObject;
            syncObjectInternal = i.syncObjectInternal;
            pointer = l;
        }

        public final boolean isValid() {
            return valid;
        }

        public void dispose() {
            syncObject.assertBound();
            if (valid) {
                valid = false;
                BadGPUUnsafe.unref(pointer);
                if (pointer == syncObjectInternal.instance)
                    syncObjectInternal.shutdown = true;
            }
        }
    }
    public static final class Instance extends Ref {
        private Instance(long l) {
            super(new TransferImpl(l), l);
        }

        public @Nullable String getMetaInfo(MetaInfoType type) {
            syncObject.assertBound();
            if (!valid)
                throw new InvalidatedPointerException(this);
            return BadGPUUnsafe.getMetaInfo(pointer, type.value);
        }

        public void flush() {
            syncObject.assertBound();
            if (!valid)
                throw new InvalidatedPointerException(this);
            BadGPUUnsafe.flushInstance(pointer);
        }

        public void finish() {
            syncObject.assertBound();
            if (!valid)
                throw new InvalidatedPointerException(this);
            BadGPUUnsafe.finishInstance(pointer);
        }

        public @Nullable Texture newTexture(int width, int height) {
            if (width >= 32768 || height >= 32768 || width < 1 || height < 1)
                throw new IllegalArgumentException("Width/height not 0-32767.");
            long res;
            syncObject.assertBound();
            if (!valid)
                throw new InvalidatedPointerException(this);
            res = BadGPUUnsafe.newTextureI(pointer, width, height, 0, null, 0);
            return res == 0 ? null : new Texture(this, res);
        }

        public @Nullable Texture newTexture(int width, int height, TextureLoadFormat fmt, @Nullable byte[] data, int dataOfs) {
            newTextureChecks(width, height, fmt, dataOfs, (data != null) ? data.length : -1);
            long res;
            res = BadGPUUnsafe.newTextureB(pointer, width, height, fmt.value, data, dataOfs);
            return res == 0 ? null : new Texture(this, res);
        }
        public @Nullable Texture newTexture(int width, int height, TextureLoadFormat fmt, @Nullable int[] data, int dataOfs) {
            newTextureChecks(width, height, fmt, dataOfs, (data != null) ? (data.length * 4) : -1);
            long res;
            res = BadGPUUnsafe.newTextureI(pointer, width, height, fmt.value, data, dataOfs);
            return res == 0 ? null : new Texture(this, res);
        }
        private void newTextureChecks(int width, int height, TextureLoadFormat fmt, int dataOfs, int dataLen) {
            if (width >= 32768 || height >= 32768 || width < 1 || height < 1)
                throw new IllegalArgumentException("Width/height not 0-32767.");
            if (dataLen != -1) {
                int size = (int) BadGPUUnsafe.pixelsSize(fmt.value, width, height);
                if (size <= 0)
                    throw new IllegalArgumentException("Size overflow.");
                if (dataOfs < 0)
                    throw new IllegalArgumentException("Data offset before start.");
                if (dataOfs > dataLen)
                    throw new IllegalArgumentException("Data offset after end.");
                if ((dataOfs + size) > dataLen)
                    throw new IllegalArgumentException("Data region after end.");
            }
            syncObject.assertBound();
            if (!valid)
                throw new InvalidatedPointerException(this);
        }

        public @Nullable DSBuffer newDSBuffer(int width, int height) {
            long res;
            syncObject.assertBound();
            if (!valid)
                throw new InvalidatedPointerException(this);
            res = BadGPUUnsafe.newDSBuffer(pointer, width, height);
            return res == 0 ? null : new DSBuffer(this, res);
        }
    }
    public static final class Texture extends Ref {
        private Texture(Ref i, long l) {
            super(i, l);
        }
        public boolean generateMipmap() {
            syncObject.assertBound();
            if (!valid)
                throw new InvalidatedPointerException(this);
            return BadGPUUnsafe.generateMipmap(pointer);
        }
        private void readPixelsChecks(int x, int y, int width, int height, TextureLoadFormat fmt, int dataOfs, int dataLen) {
            readPixelsBoundsChecks(x, y, width, height, fmt, dataOfs, dataLen);
            syncObject.assertBound();
            if (!valid)
                throw new InvalidatedPointerException(this);
        }
        // Regular readPixels is normal.
        @SuppressWarnings("unused")
        public boolean readPixels(int x, int y, int width, int height, TextureLoadFormat fmt, byte[] data, int dataOfs) {
            if (width == 0 || height == 0)
                return true;
            if (data == null)
                throw new IllegalArgumentException("data must not be null.");
            readPixelsChecks(x, y, width, height, fmt, dataOfs, data.length);
            return BadGPUUnsafe.readPixelsB(pointer, x, y, width, height, fmt.value, data, dataOfs);
        }
        @SuppressWarnings("unused")
        public boolean readPixels(int x, int y, int width, int height, TextureLoadFormat fmt, int[] data, int dataOfs) {
            if (width == 0 || height == 0)
                return true;
            if (data == null)
                throw new IllegalArgumentException("data must not be null.");
            readPixelsChecks(x, y, width, height, fmt, dataOfs * 4, data.length * 4);
            return BadGPUUnsafe.readPixelsI(pointer, x, y, width, height, fmt.value, data, dataOfs);
        }
        // Common bounds checks etc. for readPixels
        private static void readPixelsBoundsChecks(int x, int y, int width, int height, TextureLoadFormat fmt, int dataOfs, int dataLen) {
            if (width >= 32768 || height >= 32768 || width < 1 || height < 1)
                throw new IllegalArgumentException("Width/height not 0-32767.");
            int size = (int) BadGPUUnsafe.pixelsSize(fmt.value, width, height);
            if (size < 0)
                throw new IllegalArgumentException("Size overflow.");
            if (dataOfs < 0 || dataOfs > dataLen)
                throw new IllegalArgumentException("Offset not within buffer.");
            if ((dataLen - dataOfs) < size)
                throw new IllegalArgumentException("Region not within buffer.");
        }
    }
    public static final class DSBuffer extends Ref {
        private DSBuffer(Ref i, long l) {
            super(i, l);
        }
    }
    // inter-object operations
    @SuppressWarnings("null")
    public static boolean drawClear(
        @Nullable Texture sTexture, @Nullable DSBuffer sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        float cR, float cG, float cB, float cA, float depth, int stencil
    ) {
        if (sTexture == null && sDSBuffer == null)
            return false;
        if (sTexture != null && sDSBuffer != null)
            assert sTexture.syncObject == sDSBuffer.syncObject;
        ThreadOwned syncObj = sTexture != null ? sTexture.syncObject : sDSBuffer.syncObject;
        syncObj.assertBound();
        if (sTexture != null && !sTexture.valid)
            throw new InvalidatedPointerException(sTexture);
        if (sDSBuffer != null && !sDSBuffer.valid)
            throw new InvalidatedPointerException(sDSBuffer);
        return BadGPUUnsafe.drawClear(
                sTexture != null ? sTexture.pointer : 0, sDSBuffer != null ? sDSBuffer.pointer : 0, sFlags, sScX, sScY, sScWidth, sScHeight,
                cR, cG, cB, cA, depth, stencil);
    }
    private static int getVertexCount(int iStart, int iCount, @Nullable short[] indices, int indicesOfs) {
        if (iStart < 0)
            throw new IllegalArgumentException("Not supposed to have iStart be < 0");
        if ((iCount < 0) || (iCount > 65536))
            throw new IllegalArgumentException("Not supposed to have iCount be < 0 or > 65536");
        if (indicesOfs < 0)
            throw new IllegalArgumentException("Not supposed to have indicesOfs be < 0");
        if (indices == null)
            return iStart + iCount;
        // this is a quirk of the approach to JNI used here, it's kinda funny and also sad
        iStart += indicesOfs;
        int vCount = 0;
        // Since this iterates through all indices, it also implicitly validates the bounds of the array.
        for (int i = 0; i < iCount; i++) {
            int efCount = (indices[iStart] & 0xFFFF) + 1;
            if (efCount > vCount)
                vCount = efCount;
            iStart++;
        }
        return vCount;
    }
    private static void checkVL(int flags, int vPosD, float[] vPos, int vPosOfs, @Nullable float[] vCol, int vColOfs, int vTCD, @Nullable float[] vTC, int vTCOfs,
            int iStart, int iCount, @Nullable short[] indices, int indicesOfs,
            @Nullable float[] matrixA, int matrixAOfs, @Nullable float[] clipPlane, int clipPlaneOfs, @Nullable float[] matrixT, int matrixTOfs) {
        if (vPosD < 2 || vPosD > 4)
            throw new IllegalArgumentException("vPosD out of range");
        if (vTCD < 2 || vTCD > 4)
            throw new IllegalArgumentException("vTCD out of range");
        // Collate vertex counts
        // (the multiplication by 4 represents the 4 components)
        int vCount = getVertexCount(iStart, iCount, indices, indicesOfs);
        int cCount = ((flags & DrawFlags.FreezeColour) != 0) ? 1 : vCount;
        int tCount = ((flags & DrawFlags.FreezeTC) != 0) ? 1 : vCount;
        // Multiply by dimensions
        vCount *= vPosD;
        cCount *= 4;
        tCount *= vTCD;
        // Check them
        if (vPosOfs < 0 || (vPosOfs + vCount) > vPos.length)
            throw new IllegalArgumentException("vPos out of bounds");
        if (vCol != null)
            if (vColOfs < 0 || (vColOfs + cCount) > vCol.length)
            throw new IllegalArgumentException("vCol out of bounds");
        if (vTC != null)
            if (vTCOfs < 0 || (vTCOfs + tCount) > vTC.length)
                throw new IllegalArgumentException("vTC out of bounds");
        // Matrices
        if (matrixA != null)
            if (matrixAOfs < 0 || (matrixAOfs + 16) > matrixA.length)
                throw new IllegalArgumentException("matrixA out of bounds");
        if (clipPlane != null)
            if (clipPlaneOfs < 0 || (clipPlaneOfs + 4) > clipPlane.length)
                throw new IllegalArgumentException("clipPlane out of bounds");
        if (matrixT != null)
            if (matrixTOfs < 0 || (matrixTOfs + 16) > matrixT.length)
                throw new IllegalArgumentException("matrixT out of bounds");
    }
    @SuppressWarnings("null")
    public static boolean drawGeom(
        @Nullable Texture sTexture, @Nullable DSBuffer sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        int flags,
        int vPosD, float[] vPos, int vPosOfs, @Nullable float[] vCol, int vColOfs, int vTCD, @Nullable float[] vTC, int vTCOfs,
        PrimitiveType pType, float plSize,
        int iStart, int iCount, @Nullable short[] indices, int indicesOfs,
        @Nullable float[] matrixA, int matrixAOfs,
        int vX, int vY, int vW, int vH,
        @Nullable Texture texture, @Nullable float[] matrixT, int matrixTOfs,
        @Nullable float[] clipPlane, int clipPlaneOfs, Compare atFunc, float atRef,
        Compare stFunc, int stRef, int stMask,
        StencilOp stSF, StencilOp stDF, StencilOp stDP,
        Compare dtFunc, float depthN, float depthF, float poFactor, float poUnits,
        int blendProgram
    ) {
        if (sTexture == null && sDSBuffer == null)
            return false;
        if (sTexture != null && sDSBuffer != null)
            assert sTexture.syncObject == sDSBuffer.syncObject;
        ThreadOwned syncObj = sTexture != null ? sTexture.syncObject : sDSBuffer.syncObject;
        if (texture != null)
            assert syncObj == texture.syncObject;
        // actual parameter checking
        checkVL(flags, vPosD, vPos, vPosOfs, vCol, vColOfs, vTCD, vTC, vTCOfs,
                iStart, iCount, indices, indicesOfs,
                matrixA, matrixAOfs, clipPlane, clipPlaneOfs, matrixT, matrixTOfs);
        // continue
        syncObj.assertBound();
        if (sTexture != null && !sTexture.valid)
            throw new InvalidatedPointerException(sTexture);
        if (sDSBuffer != null && !sDSBuffer.valid)
            throw new InvalidatedPointerException(sDSBuffer);
        if (texture != null && !texture.valid)
            throw new InvalidatedPointerException(texture);
        return BadGPUUnsafe.drawGeom(
                sTexture != null ? sTexture.pointer : 0, sDSBuffer != null ? sDSBuffer.pointer : 0, sFlags, sScX, sScY, sScWidth, sScHeight,
                flags,
                vPosD, vPos, vPosOfs, vCol, vColOfs, vTCD, vTC, vTCOfs,
                pType.value, plSize,
                iStart, iCount, indices, indicesOfs,
                matrixA, matrixAOfs,
                vX, vY, vW, vH,
                texture != null ? texture.pointer : 0, matrixT, matrixTOfs,
                clipPlane, clipPlaneOfs, atFunc.value, atRef,
                stFunc.value, stRef, stMask,
                stSF.value, stDF.value, stDP.value,
                dtFunc.value, depthN, depthF, poFactor, poUnits,
                blendProgram);
    }
    public static boolean drawGeomNoDS(
        @Nullable Texture sTexture, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        int flags,
        int vPosD, float[] vPos, int vPosOfs, @Nullable float[] vCol, int vColOfs, int vTCD, @Nullable float[] vTC, int vTCOfs,
        PrimitiveType pType, float plSize,
        int iStart, int iCount, @Nullable short[] indices, int indicesOfs,
        @Nullable float[] matrixA, int matrixAOfs,
        int vX, int vY, int vW, int vH,
        @Nullable Texture texture, @Nullable float[] matrixT, int matrixTOfs,
        @Nullable float[] clipPlane, int clipPlaneOfs, Compare atFunc, float atRef,
        int blendProgram
    ) {
        if (sTexture == null)
            return false;
        if (texture != null)
            assert sTexture.syncObject == texture.syncObject;
        // actual parameter checking
        checkVL(flags, vPosD, vPos, vPosOfs, vCol, vColOfs, vTCD, vTC, vTCOfs,
                iStart, iCount, indices, indicesOfs,
                matrixA, matrixAOfs, clipPlane, clipPlaneOfs, matrixT, matrixTOfs);
        // continue
        sTexture.syncObject.assertBound();
        if (!sTexture.valid)
            throw new InvalidatedPointerException(sTexture);
        if (texture != null && !texture.valid)
            throw new InvalidatedPointerException(texture);
        return BadGPUUnsafe.drawGeomNoDS(
                sTexture != null ? sTexture.pointer : 0, sFlags, sScX, sScY, sScWidth, sScHeight,
                flags,
                vPosD, vPos, vPosOfs, vCol, vColOfs, vTCD, vTC, vTCOfs,
                pType.value, plSize,
                iStart, iCount, indices, indicesOfs,
                matrixA, matrixAOfs,
                vX, vY, vW, vH,
                texture != null ? texture.pointer : 0, matrixT, matrixTOfs,
                clipPlane, clipPlaneOfs, atFunc.value, atRef,
                blendProgram);
    }
}
