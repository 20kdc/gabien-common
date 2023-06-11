/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

/**
 * Finally, what this project needed.
 * VERSION: 0.25.0
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
    public static native void finishInstance(long instance);
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
    public static native void pixelsConvertBB(int fF, int tF, int width, int height, byte[] fD, int fDOfs, byte[] tD, int tDOfs);
    public static native void pixelsConvertBI(int fF, int tF, int width, int height, byte[] fD, int fDOfs, int[] tD, int tDOfs);
    public static native void pixelsConvertIB(int fF, int tF, int width, int height, int[] fD, int fDOfs, byte[] tD, int tDOfs);
    public static native void pixelsConvertRGBA8888ToARGBI32InPlaceB(int width, int height, byte[] data, int dataOfs);
    public static native void pixelsConvertRGBA8888ToARGBI32InPlaceI(int width, int height, int[] data, int dataOfs);
    // TM
    public static native long newTextureB(long instance, int width, int height, int fmt, byte[] data, int dataOfs);
    public static native long newTextureI(long instance, int width, int height, int fmt, int[] data, int dataOfs);
    public static native long newDSBuffer(long instance, int width, int height);
    public static native boolean generateMipmap(long texture);
    public static native boolean readPixelsB(long texture, int x, int y, int width, int height, int fmt, byte[] data, int dataOfs);
    public static native boolean readPixelsI(long texture, int x, int y, int width, int height, int fmt, int[] data, int dataOfs);
    // DC
    public static native boolean drawClear(
        long sTexture, long sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        float cR, float cG, float cB, float cA, float depth, int stencil
    );
    public static native boolean drawGeom(
        long sTexture, long sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        int flags,
        int vPosD, float[] vPos, int vPosOfs, float[] vCol, int vColOfs, int vTCD, float[] vTC, int vTCOfs,
        int pType, float plSize,
        int iStart, int iCount, short[] indices, int indicesOfs,
        float[] matrixA, int matrixAOfs, float[] matrixB, int matrixBOfs,
        int vX, int vY, int vW, int vH,
        long texture, float[] matrixT, int matrixTOfs,
        int stFunc, int stRef, int stMask,
        int stSF, int stDF, int stDP,
        int dtFunc, float depthN, float depthF, float poFactor, float poUnits,
        int blendProgram
    );
    public static native boolean drawGeomNoDS(
        long sTexture, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        int flags,
        int vPosD, float[] vPos, int vPosOfs, float[] vCol, int vColOfs, int vTCD, float[] vTC, int vTCOfs,
        int pType, float plSize,
        int iStart, int iCount, short[] indices, int indicesOfs,
        float[] matrixA, int matrixAOfs, float[] matrixB, int matrixBOfs,
        int vX, int vY, int vW, int vH,
        long texture, float[] matrixT, int matrixTOfs,
        int blendProgram
    );

    // Android magic
    public static native long ANDcreateEGLSurface(long instance, Object window);
    public static native void ANDdestroyEGLSurface(long instance, long surface);
    public static native void ANDoverrideSurface(long instance, long surface);
    public static native void ANDblitToSurface(long instance, long texture, long surface, int width, int height, float s0, float t0, float s1, float t1);
}

