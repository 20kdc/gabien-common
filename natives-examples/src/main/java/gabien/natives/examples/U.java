package gabien.natives.examples;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Texture;

/**
 * Utilities
 * Created 2nd June, 2023.
 */
public class U {
    public static final int TEXTBUF_W = 1024;
    public static final int TEXTBUF_H = 32;
    public static final Font FONT = new Font("", Font.PLAIN, 32);
    public static final int BLEND_STANDARD = BadGPU.blendProgram(
            BadGPU.blendEquation(BadGPU.BlendWeight.SrcA, BadGPU.BlendWeight.InvertSrcA, BadGPU.BlendOp.Add),
            BadGPU.blendEquation(BadGPU.BlendWeight.SrcA, BadGPU.BlendWeight.InvertSrcA, BadGPU.BlendOp.Add)
    );
    public static final int BLEND_ADD = BadGPU.blendProgram(
            BadGPU.blendEquation(BadGPU.BlendWeight.One, BadGPU.BlendWeight.One, BadGPU.BlendOp.Add),
            BadGPU.blendEquation(BadGPU.BlendWeight.One, BadGPU.BlendWeight.One, BadGPU.BlendOp.Add)
    );
    private static final float[] rectV = new float[] {
            -1, -1,
            1, -1,
            1, 1,
            -1, 1
    };
    private static final float[] rectTC = new float[] {
            0, 0,
            1, 0,
            1, 1,
            0, 1
    };
    private static final float[] triImmDat1 = new float[32];
    private static final short[] rectIndices = {0, 1, 2, 0, 2, 3};
    public static BadGPU.Texture loadTex(BadGPU.Instance i, String name) {
        try {
            return loadTex(i, ImageIO.read(U.class.getResourceAsStream(name)));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public static BadGPU.Texture drawText(BadGPU.Instance i, String text) {
        BufferedImage bi = new BufferedImage(TEXTBUF_W, TEXTBUF_H, BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.getGraphics();
        g.setColor(new Color(0x00FFFFFF));
        g.setFont(FONT);
        g.drawString(text, 0, 28);
        return loadTex(i, bi);
    }
    public static BadGPU.Texture loadTex(BadGPU.Instance i, BufferedImage bi) {
        int w = bi.getWidth();
        int h = bi.getHeight();
        int[] buf = new int[w * h];
        bi.getRGB(0, 0, w, h, buf, 0, w);
        return i.newTexture(w, h, BadGPU.TextureLoadFormat.ARGBI32, buf, 0);
    }
    private static int putImmDat(int idx, float a, float b, float c, float d) {
        triImmDat1[idx++] = a;
        triImmDat1[idx++] = b;
        triImmDat1[idx++] = c;
        triImmDat1[idx++] = d;
        return idx;
    }
    public static void triImm(BadGPU.Texture scr, int w, int h,
            float xA, float yA,
            float rA, float gA, float bA,
            float xB, float yB,
            float rB, float gB, float bB,
            float xC, float yC,
            float rC, float gC, float bC
        ) {
        float[] data = triImmDat1;
        int idx = 0;
        idx = putImmDat(idx, xA, yA, 0, 1);
        idx = putImmDat(idx, xB, yB, 0, 1);
        idx = putImmDat(idx, xC, yC, 0, 1);
        idx = putImmDat(idx, rA, gA, bA, 1);
        idx = putImmDat(idx, rB, gB, bB, 1);
        idx = putImmDat(idx, rC, gC, bC, 1);
        BadGPU.drawGeomNoDS(scr, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0,
                0,
                4, data, 0, data, 12, 4, null, 0,
                BadGPU.PrimitiveType.Triangles, 0,
                0, 3, null, 0,
                null, 0, null, 0,
                0, 0, w, h,
                null, null, 0,
                0);
    }
    public static void texRctImm(Texture screen, int i, int j, int w, int h, Texture cached) {
        int df = BadGPU.DrawFlags.MinLinear | BadGPU.DrawFlags.MagLinear | BadGPU.DrawFlags.Blend;
        BadGPU.drawGeomNoDS(screen, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0,
                df,
                2, rectV, 0, null, 0, 2, rectTC, 0,
                BadGPU.PrimitiveType.Triangles, 0,
                0, 6, rectIndices, 0,
                null, 0, null, 0,
                i, j, w, h,
                cached, null, 0,
                BLEND_STANDARD);
    }
}
