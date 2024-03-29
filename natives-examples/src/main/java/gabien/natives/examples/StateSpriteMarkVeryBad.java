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
public class StateSpriteMarkVeryBad extends StateSpriteMarkBase {
    public static final float[] spriteVertexData = {
            -0.01f, -0.01f, 0, 1,
             0.01f, -0.01f, 0, 1,
             0.01f,  0.01f, 0, 1,
            -0.01f,  0.01f, 0, 1
    };
    public static final float[] spriteTCData = {
            0, 0, 0, 1,
            1, 0, 0, 1,
            1, 1, 0, 1,
            0, 1, 0, 1
    };
    public static final short[] spriteIndices = {0, 1, 2, 0, 3, 2 };

    private float[] tmpMatrix = new float[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    public StateSpriteMarkVeryBad(IMain m) {
        super(m);
    }

    @Override
    public void actualImplementation(Texture screen, int w, int h) {
        for (int i = 0; i < sms.x.length; i++) {
            tmpMatrix[12] = sms.x[i];
            tmpMatrix[13] = sms.y[i];
            BadGPU.drawGeomNoDS(screen, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0,
                    0,
                    4, spriteVertexData, 0, null, 0, 4, spriteTCData, 0,
                    BadGPU.PrimitiveType.Triangles, 0,
                    0, 6, spriteIndices, 0,
                    tmpMatrix, 0,
                    0, 0, w, h,
                    cached, null, 0,
                    null, 0, BadGPU.Compare.Always, 0,
                    0);
        }
    }

}
