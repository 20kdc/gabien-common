/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

/**
 * Enums and stuff, because they're weird.
 * This is extensible so that it can be pulled into BadGPUUnsafe and BadGPU.
 * Bit of a cheat, but who's counting?
 * VERSION: 0.14.0
 * Created 30th May, 2023.
 */
public class BadGPUEnum {
    public abstract class NewInstanceFlags {
        public static final int CanPrintf = 1;
        public static final int BackendCheck = 2;
        public static final int BackendCheckAggressive = 4;
        private NewInstanceFlags() {
        }
    }
    public enum MetaInfoType {
        Vendor(0x1F00),
        Renderer(0x1F01),
        Version(0x1F02);
        public final int value;
        private MetaInfoType(int ev) {
            value = ev;
        }
    }
    public abstract class TextureFlags {
        public static final int HasAlpha = 1;
        private TextureFlags() {
        }
    }
    public abstract class SessionFlags {
        public static final int StencilAll = 0x00FF;
        public static final int Stencil0 = 0x0001;
        public static final int Stencil1 = 0x0002;
        public static final int Stencil2 = 0x0004;
        public static final int Stencil3 = 0x0008;
        public static final int Stencil4 = 0x0010;
        public static final int Stencil5 = 0x0020;
        public static final int Stencil6 = 0x0040;
        public static final int Stencil7 = 0x0080;
        public static final int MaskR = 0x0100;
        public static final int MaskG = 0x0200;
        public static final int MaskB = 0x0400;
        public static final int MaskA = 0x0800;
        public static final int MaskDepth = 0x1000;
        public static final int MaskRGBA = 0x0F00;
        public static final int MaskRGBAD = 0x1F00;
        public static final int MaskAll = 0x1FFF;
        public static final int Scissor = 0x2000;
        private SessionFlags() {
        }
    }
    public abstract class DrawFlags {
        public static final int FrontFaceCW = 1;
        public static final int CullFace = 2;
        public static final int CullFaceFront = 4;
        public static final int StencilTest = 8;
        public static final int DepthTest = 16;
        public static final int Blend = 32;
        public static final int AlphaTestInvert = 64;
        public static final int FreezeColor = 128;
        public static final int FreezeColour = 128;
        public static final int FreezeTC = 256;
        public static final int MinLinear = 512;
        public static final int MagLinear = 1024;
        public static final int Mipmap = 2048;
        public static final int WrapS = 4096;
        public static final int WrapT = 8192;
        private DrawFlags() {
        }
    }
    public enum PrimitiveType {
        Points(0x0000),
        Lines(0x0001),
        Triangles(0x0004);
        public final int value;
        private PrimitiveType(int ev) {
            value = ev;
        }
    }
    public enum Compare {
        Never(0x0200),
        Always(0x0207),
        Less(0x0201),
        LEqual(0x0203),
        Equal(0x0202),
        Greater(0x0204),
        GEqual(0x0206),
        NotEqual(0x0205);
        public final int value;
        private Compare(int ev) {
            value = ev;
        }
    }
    public enum StencilOp {
        Keep(0x1E00),
        Zero(0),
        Replace(0x1E01),
        Inc(0x1E02),
        Dec(0x1E03),
        Invert(0x150A);
        public final int value;
        private StencilOp(int ev) {
            value = ev;
        }
    }
    public enum BlendEquation {
        Add(0x8006),
        Sub(0x800A),
        ReverseSub(0x800B);
        public final int value;
        private BlendEquation(int ev) {
            value = ev;
        }
    }
    public enum BlendWeight {
        Zero(0),
        One(1),
        Src(0x0300),
        InvertSrc(0x0301),
        Dst(0x0306),
        InvertDst(0x0307),
        SrcA(0x0302),
        InvertSrcA(0x0303),
        DstA(0x0304),
        InvertDstA(0x0305),
        SrcAlphaSaturate(0x0308);
        public final int value;
        private BlendWeight(int ev) {
            value = ev;
        }
    }
}
