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
 * Created 30th May, 2023
 */
public class StateMenu implements IState {
    float tx = 0;
    @Override
    public void frame(Main m, Texture screen, int w, int h) {
        tx += 0.01f;
        BadGPU.drawClear(screen, null, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
        Main.triImm(screen, w, h,
                0, -1,
                1, 0, 0,
                1, 1,
                0, 1, 0,
                -1, 1,
                0, 0, 1);
        Main.triImm(screen, w, h,
                0, -1,
                1, 0, 0,
                (float) Math.sin(tx), 0,
                1, 1, 0,
                -1, 1,
                0, 0, 1);
    }
}
