/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.natives.examples.stencilshadows;

import gabien.natives.BadGPU;

/**
 * This is kind of bad.
 * Created 11th June, 2023.
 */
public class STHCameraSetup {
    public final BadGPU.Instance instance;
    public final float[] matrix = new float[16];
    public BadGPU.Texture backBuffer;
    public BadGPU.DSBuffer dsBuffer;
    public int bW, bH;
    public STHCameraSetup(BadGPU.Instance i) {
        instance = i;
    }
    public void doSetup(BadGPU.Texture backBuffer, int w, int h) {
        this.backBuffer = backBuffer;
        if (bW != w || bH != h || dsBuffer == null) {
            bW = w;
            bH = h;
            dsBuffer = instance.newDSBuffer(w, h);
        }
        float x = ((float) bH) / ((float) bW);
        matrix[0] =  x; matrix[1] =  0; matrix[2] =  0;  matrix[3] = 0;
        matrix[4] =  0; matrix[5] =  1; matrix[6] =  0;  matrix[7] = 0;
        matrix[8] =  0; matrix[9] =  0; matrix[10] = 1; matrix[11] = 1;
        matrix[12] = 0; matrix[13] = 0; matrix[14] = 0; matrix[15] = 1;
    }
}
