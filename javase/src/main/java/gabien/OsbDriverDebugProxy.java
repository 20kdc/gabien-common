/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

/**
 * Created 14th October 2022 to debug rendering issues...
 */
public class OsbDriverDebugProxy extends ProxyOsbDriver implements IWindowGrBackend {
    public final IImage scary = GaBIEn.createImage(new int[] {0xFFFF0000, 0xFF00FF00, 0xFF0000FF}, 3, 1);
    public OsbDriverDebugProxy(IWindowGrBackend targ) {
        super(targ);
    }

    public int getScaryFrame() {
        int f = (int) GaBIEn.getTime();
        if (f < 0)
            return 0;
        return f % 3;
    }

    @Override
    public void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean blendSub) {
        if (!checkRect(srcx, srcy, srcw, srch, i)) {
            super.blitRotatedScaledImage(getScaryFrame(), 0, 1, 1, x, y, acw, ach, angle, scary);
        } else {
            super.blendRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, blendSub);
        }
    }

    @Override
    public void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i) {
        if (!checkRect(srcx, srcy, srcw, srch, i)) {
            super.blitRotatedScaledImage(getScaryFrame(), 0, 1, 1, x, y, acw, ach, angle, scary);
        } else {
            super.blitRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i);
        }
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        if (!checkRect(srcx, srcy, srcw, srch, i)) {
            super.blitScaledImage(getScaryFrame(), 0, 1, 1, x, y, acw, ach, scary);
        } else {
            super.blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i);
        }
    }

    private boolean checkRect(int srcx, int srcy, int srcw, int srch, IImage i) {
        if ((srcw <= 0) || (srch <= 0))
            return false;
        if ((srcx < 0) || (srcy < 0))
            return false;
        if ((srcx + srcw > i.getWidth()) || (srcy + srch > i.getHeight()))
            return false;
        return true;
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {
        // prevent false alarms on empty blits
        if (srcw == 0 || srch == 0)
            return;
        if (!checkRect(srcx, srcy, srcw, srch, i)) {
            if (srcw < 1)
                srcw = 32;
            if (srch < 1)
                srch = 32;
            super.blitScaledImage(getScaryFrame(), 0, 1, 1, x, y, srcw, srch, scary);
        } else {
            super.blitImage(srcx, srcy, srcw, srch, x, y, i);
        }
    }
}
