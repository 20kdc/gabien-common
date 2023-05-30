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
 * VERSION: 0.12.0
 * Created 29th May, 2023.
 */
public abstract class BadGPUUnsafe {
    private BadGPUUnsafe() {
    }
    // OM
    public static native long ref(long obj);
    public static native boolean unref(long obj);
    // IM
    public static final int BADGPUNewInstanceFlags_CanPrintf = 1;
    public static final int BADGPUNewInstanceFlags_BackendCheck = 2;
    public static final int BADGPUNewInstanceFlags_BackendCheckAggressive = 4;
    // The passed-in class is used for the instance creation failure exception.
    public static native long newInstance(int flags, Class<?> ex);
    public static final int BADGPUMetaInfoType_Vendor = 0x1F00;
    public static final int BADGPUMetaInfoType_Renderer = 0x1F01;
    public static final int BADGPUMetaInfoType_Version = 0x1F02;
    public static native String getMetaInfo(long instance, int type);
    // TM
    public static final int BADGPUTextureFlags_HasAlpha = 1;
    public static native long newTexture(long instance, int flags, int width, int height, Buffer data, long offset);
    public static native long newDSBuffer(long instance, int width, int height);
    public static native boolean generateMipmap(long texture);
    public static native boolean readPixels(long texture, int x, int y, int width, int height, Buffer data, long offset);
    // DC
    public static final int BADGPUSessionFlags_StencilAll = 0x00FF;
    public static final int BADGPUSessionFlags_Stencil0 = 0x0001;
    public static final int BADGPUSessionFlags_Stencil1 = 0x0002;
    public static final int BADGPUSessionFlags_Stencil2 = 0x0004;
    public static final int BADGPUSessionFlags_Stencil3 = 0x0008;
    public static final int BADGPUSessionFlags_Stencil4 = 0x0010;
    public static final int BADGPUSessionFlags_Stencil5 = 0x0020;
    public static final int BADGPUSessionFlags_Stencil6 = 0x0040;
    public static final int BADGPUSessionFlags_Stencil7 = 0x0080;
    public static final int BADGPUSessionFlags_MaskR = 0x0100;
    public static final int BADGPUSessionFlags_MaskG = 0x0200;
    public static final int BADGPUSessionFlags_MaskB = 0x0400;
    public static final int BADGPUSessionFlags_MaskA = 0x0800;
    public static final int BADGPUSessionFlags_MaskDepth = 0x1000;
    public static final int BADGPUSessionFlags_MaskRGBA = 0x0F00;
    public static final int BADGPUSessionFlags_MaskRGBAD = 0x1F00;
    public static final int BADGPUSessionFlags_MaskAll = 0x00FF;
    public static final int BADGPUSessionFlags_Scissor = 0x2000;
    public static native boolean drawClear(
        long sTexture, long sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        float cR, float cG, float cB, float cA, float depth, int stencil
    );
    public static final int BADGPUPrimitiveType_Points = 0x0000;
    public static final int BADGPUPrimitiveType_Lines = 0x0001;
    public static final int BADGPUPrimitiveType_Triangles = 0x0004;
    public static final int BADGPUCompare_Never = 0x0200;
    public static final int BADGPUCompare_Always = 0x0207;
    public static final int BADGPUCompare_Less = 0x0201;
    public static final int BADGPUCompare_LEqual = 0x0203;
    public static final int BADGPUCompare_Equal = 0x0202;
    public static final int BADGPUCompare_Greater = 0x0204;
    public static final int BADGPUCompare_GEqual = 0x0206;
    public static final int BADGPUCompare_NotEqual = 0x0205;
    public static final int BADGPUStencilOp_Keep = 0x1E00;
    public static final int BADGPUStencilOp_Zero = 0;
    public static final int BADGPUStencilOp_Replace = 0x1E01;
    public static final int BADGPUStencilOp_Inc = 0x1E02;
    public static final int BADGPUStencilOp_Dec = 0x1E03;
    public static final int BADGPUStencilOp_Invert = 0x150A;
    public static final int BADGPUBlendEquation_Add = 0x8006;
    public static final int BADGPUBlendEquation_Sub = 0x800A;
    public static final int BADGPUBlendEquation_ReverseSub = 0x800B;
    public static final int BADGPUBlendWeight_Zero = 0;
    public static final int BADGPUBlendWeight_One = 1;
    public static final int BADGPUBlendWeight_Src = 0x300;
    public static final int BADGPUBlendWeight_InvertSrc = 0x301;
    public static final int BADGPUBlendWeight_Dst = 0x306;
    public static final int BADGPUBlendWeight_InvertDst = 0x307;
    public static final int BADGPUBlendWeight_SrcA = 0x0302;
    public static final int BADGPUBlendWeight_InvertSrcA = 0x0303;
    public static final int BADGPUBlendWeight_DstA = 0x0304;
    public static final int BADGPUBlendWeight_InvertDstA = 0x0305;
    public static final int BADGPUBlendWeight_SrcAlphaSaturate = 0x0308;
    public static native boolean drawGeom(
        long sTexture, long sDSBuffer, int sFlags, int sScX, int sScY, int sScWidth, int sScHeight,
        int flags,
        float[] vPos, int vPosOfs, float[] vCol, int vColOfs, float[] vTC, int vTCOfs,
        int pType, float plSize,
        int iStart, int iCount, int[] indices, int indicesOfs,
        float[] matrixA, float[] matrixB,
        float depthN, float depthF,
        int vX, int vY, int vW, int vH,
        long texture, float[] matrixT,
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
        int iStart, int iCount, int[] indices,
        float[] matrixA, float[] matrixB,
        int vX, int vY, int vW, int vH,
        long texture, float[] matrixT,
        float alphaTestMin,
        int bwRGBS, int bwRGBD, int beRGB,
        int bwAS, int bwAD, int beA
    );
}

