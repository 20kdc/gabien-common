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
 * Created 3rd June 2023, refactored to base class 11th June 2023
 */
public abstract class StateSpriteMarkBase extends State {
    public SpriteMarkSimulator sms = new SpriteMarkSimulator(100);
    final BadGPU.Texture cached;
    BadGPU.Texture cachedHeader, cachedHeader2;

    public StateSpriteMarkBase(IMain m) {
        super(m);
        cached = U.loadTex(m.getInstance(), "img.png");
        cachedHeader = U.drawText(main.getInstance(), getClass().getSimpleName());
        changeCapacity(100);
    }
    private void changeCapacity(int newCap) {
        if (newCap < 0)
            newCap = 0;
        sms.resize(newCap);
        cachedHeader2 = U.drawText(main.getInstance(), "W/S to set sprite count: " + newCap + ", Z exits");
    }

    @Override
    public final void frame(Texture screen, int w, int h) {
        if (main.getKeyEvent(Main.KEY_Z)) {
            main.setState(new StateMenu(main));
            return;
        }
        if (main.getKeyEvent(Main.KEY_W))
            changeCapacity(sms.x.length + 100);
        if (main.getKeyEvent(Main.KEY_S))
            changeCapacity(sms.x.length - 100);
        sms.iterate();
        BadGPU.drawClear(screen, null, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0);
        actualImplementation(screen, w, h);
        U.texRctImm(screen, 0, 0, U.TEXTBUF_W, U.TEXTBUF_H, cachedHeader);
        U.texRctImm(screen, 0, U.TEXTBUF_H, U.TEXTBUF_W, U.TEXTBUF_H, cachedHeader2);
    }

    public abstract void actualImplementation(Texture screen, int w, int h);
}
