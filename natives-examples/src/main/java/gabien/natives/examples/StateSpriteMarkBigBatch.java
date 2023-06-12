/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives.examples;

import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Texture;

/**
 * Created 3rd June 2023.
 */
public class StateSpriteMarkBigBatch extends StateSpriteMarkBase {
    private float[] vtxBuf = new float[65536];
    private float[] tcBuf = new float[65536];

    public StateSpriteMarkBigBatch(IMain m) {
        super(m);
    }

    private int putVertex(int vtxPtr, float x, float y, float u, float v) {
        vtxBuf[vtxPtr + 0] = x;
        vtxBuf[vtxPtr + 1] = y;
        vtxBuf[vtxPtr + 2] = 0;
        vtxBuf[vtxPtr + 3] = 1;
        tcBuf[vtxPtr + 0] = u;
        tcBuf[vtxPtr + 1] = v;
        tcBuf[vtxPtr + 2] = 0;
        tcBuf[vtxPtr + 3] = 1;
        return vtxPtr + 4;
    }

    public void drawSpriteSection(int start, int end, Texture screen, int w, int h) {
        int vtxPtr = 0;
        for (int i = start; i < end; i++) {
            float x0 = sms.x[i] - 0.01f;
            float x1 = sms.x[i] + 0.01f;
            float y0 = sms.y[i] - 0.01f;
            float y1 = sms.y[i] + 0.01f;
            vtxPtr = putVertex(vtxPtr, x0, y0, 0, 0);
            vtxPtr = putVertex(vtxPtr, x1, y0, 1, 0);
            vtxPtr = putVertex(vtxPtr, x1, y1, 1, 1);
            vtxPtr = putVertex(vtxPtr, x0, y0, 0, 0);
            vtxPtr = putVertex(vtxPtr, x0, y1, 0, 1);
            vtxPtr = putVertex(vtxPtr, x1, y1, 1, 1);
        }
        BadGPU.drawGeomNoDS(screen, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0,
                0,
                4, vtxBuf, 0, null, 0, 4, tcBuf, 0,
                BadGPU.PrimitiveType.Triangles, 0,
                0, vtxPtr / 4, null, 0,
                null, 0,
                0, 0, w, h,
                cached, null, 0,
                null, 0, BadGPU.Compare.Always, 0,
                0);
    }

    @Override
    public void actualImplementation(Texture screen, int w, int h) {
        int spriteStart = 0;
        int sprites = sms.x.length;
        while (sprites >= 2000) {
            drawSpriteSection(spriteStart, spriteStart + 2000, screen, w, h);
            sprites -= 2000;
            spriteStart += 2000;
        }
        if (sprites > 0)
            drawSpriteSection(spriteStart, spriteStart + sprites, screen, w, h);
    }

}
