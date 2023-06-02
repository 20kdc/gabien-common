/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

import java.nio.Buffer;

/**
 * Finally, what this project needed.
 * VERSION: 0.15.0
 * Created 29th May, 2023.
 */
public abstract class BadGPUUnsafe extends BadGPUEnum {
    private BadGPUUnsafe() {
    }
    // OM
    public static native long ref(long obj);
    public static native boolean unref(long obj);
    // IM
    // The passed-in class is used for the instance creation failure exception.
    public static native long newInstance(int flags, Class<?> ex);
    public static native String getMetaInfo(long instance, int type);
    public static native boolean bindInstance(long instance);
    public static native void unbindInstance(long instance);
    public static native void flushInstance(long instance);
    // TCE
    /**
     * Gets the size in bytes for a given size of image.
     * This is not wrapped natively for various reasons, mainly the overflows.
     */
    public static long pixelsSize(int fmt, int width, int height) {
        if (width <= 0 || height <= 0 || width > 32767 || height > 32767)
            return 0;
        if (fmt == 0) // rgba8888
            return width * height * 4L;
        if (fmt == 1) // rgba888
            return width * height * 3L;
        if (fmt == 2) // argbi32
            return width * height * 4L;
        return 0;
    }
    public static native void pixelsConvert(int fF, int tF, int width, int height, Buffer fD, long fDOfs, Buffer tD, long tDOfs);
    // TM
    public static native long newTexture(long instance, int flags, int width, int height, int fmt, Buffer data, long offset);
    //public static native long newTextureARGBI32(long instance, int flags, int width, int height, int fmt, int[] argb, int argbOfs);
    public static native long newDSBuffer(long instance, int width, int height);
    public static native boolean generateMipmap(long texture);
    public static native boolean readPixels(long texture, int x, int y, int width, int height, int fmt, Buffer data, long offset);
    //public static native boolean readPixelsARGBI32(long texture, int x, int y, int width, int height, int[] argb, int argbOfs);
    // DC
    public static native boolean drawClear(
        long sTexture, long sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        float cR, float cG, float cB, float cA, float depth, int stencil
    );
    public static native boolean drawGeom(
        long sTexture, long sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        int flags,
        float[] vPos, int vPosOfs, float[] vCol, int vColOfs, float[] vTC, int vTCOfs,
        int pType, float plSize,
        int iStart, int iCount, short[] indices, int indicesOfs,
        float[] matrixA, int matrixAOfs, float[] matrixB, int matrixBOfs,
        float depthN, float depthF,
        int vX, int vY, int vW, int vH,
        long texture, float[] matrixT, int matrixTOfs,
        float poFactor, float poUnits,
        float alphaTestMin,
        int stFunc, int stRef, int stMask,
        int stSF, int stDF, int stDP,
        int dtFunc,
        int bwRGBS, int bwRGBD, int beRGB,
        int bwAS, int bwAD, int beA
    );
    public static native boolean drawGeomNoDS(
        long sTexture, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        int flags,
        float[] vPos, int vPosOfs, float[] vCol, int vColOfs, float[] vTC, int vTCOfs,
        int pType, float plSize,
        int iStart, int iCount, short[] indices, int indicesOfs,
        float[] matrixA, int matrixAOfs, float[] matrixB, int matrixBOfs,
        int vX, int vY, int vW, int vH,
        long texture, float[] matrixT, int matrixTOfs,
        float alphaTestMin,
        int bwRGBS, int bwRGBD, int beRGB,
        int bwAS, int bwAD, int beA
    );
}

