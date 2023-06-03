/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives.examples;

import java.util.Random;

/**
 * Created 3rd June 2023.
 */
public class SpriteMarkSimulator {
    public float[] x;
    public float[] y;
    public float[] xV;
    public float[] yV;

    public SpriteMarkSimulator(int capacity) {
        resize(capacity);
    }

    public void resize(int capacity) {
        x = new float[capacity];
        y = new float[capacity];
        xV = new float[capacity];
        yV = new float[capacity];
        Random r = new Random();
        for (int i = 0; i < capacity; i++) {
            x[i] = (r.nextFloat() - 0.5f) * 2;
            y[i] = (r.nextFloat() - 0.5f) * 2;
            xV[i] = (r.nextFloat() - 0.5f) * 0.003f;
            yV[i] = (r.nextFloat() - 0.5f) * 0.003f;
        }
    }

    public void iterate() {
        for (int i = 0; i < x.length; i++) {
            x[i] += xV[i];
            y[i] += yV[i];
            if (x[i] > 1) {
                x[i] = 1;
                xV[i] = -xV[i];
            } else if (x[i] < -1) {
                x[i] = -1;
                xV[i] = -xV[i];
            }
            if (y[i] > 1) {
                y[i] = 1;
                yV[i] = -yV[i];
            } else if (y[i] < -1) {
                y[i] = -1;
                yV[i] = -yV[i];
            }
        }
    }
}
