/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives.examples;

import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Texture;
import gabien.natives.examples.stencilshadows.StateStencilShadows;

/**
 * Created 30th May, 2023
 */
public class StateMenu extends State {
    final BadGPU.Texture cachedHeaderText;
    BadGPU.Texture cachedCurrentText;
    final Class<?>[] options = {
            StateSpriteMarkVeryBad.class,
            StateSpriteMarkBigBatch.class,
            StateStencilShadows.class
    };
    int optionId;

    public StateMenu(IMain m) {
        super(m);
        cachedHeaderText = U.drawText(main.getInstance(), "A/D to select, Z to confirm");
        updateOpt();
    }

    private void updateOpt() {
        cachedCurrentText = U.drawText(main.getInstance(), options[optionId].getSimpleName());
    }

    @Override
    public void frame(Texture screen, int w, int h) {
        if (main.getKeyEvent(IMain.KEY_Z)) {
            try {
                main.setState((State) options[optionId].getConstructor(IMain.class).newInstance(main));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }
        if (main.getKeyEvent(IMain.KEY_A)) {
            optionId--;
            if (optionId < 0)
                optionId = options.length - 1;
            updateOpt();
            return;
        }
        if (main.getKeyEvent(IMain.KEY_D)) {
            optionId++;
            if (optionId >= options.length)
                optionId = 0;
            updateOpt();
            return;
        }
        BadGPU.drawClear(screen, null, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
        U.texRctImm(screen, 0, 0, U.TEXTBUF_W, U.TEXTBUF_H, cachedHeaderText);
        U.texRctImm(screen, 0, U.TEXTBUF_H, U.TEXTBUF_W, U.TEXTBUF_H, cachedCurrentText);
    }
}
