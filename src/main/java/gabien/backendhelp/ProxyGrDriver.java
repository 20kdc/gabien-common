/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.backendhelp;

import gabien.IGrDriver;
import gabien.IImage;

/**
 * Used to allow MT/non-MT implementations of a GrDriver.
 * Turns out multithreading everything isn't 100% a great idea.
 * Also note that this is useful as a fallback finalization wrapper.
 * Created on 08/06/17.
 */
public class ProxyGrDriver<T extends IGrDriver> implements IGrDriver, INativeImageHolder {
    public final T target;

    public ProxyGrDriver(T targ) {
        target = targ;
    }

    @Override
    public int getWidth() {
        return target.getWidth();
    }

    @Override
    public int getHeight() {
        return target.getHeight();
    }

    @Override
    public int[] getPixels() {
        return target.getPixels();
    }

    @Override
    public byte[] createPNG() {
        return target.createPNG();
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {
        target.blitImage(srcx, srcy, srcw, srch, x, y, i);
    }

    @Override
    public void blitTiledImage(int x, int y, int w, int h, IImage cachedTile) {
        target.blitTiledImage(x, y, w, h, cachedTile);
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        target.blitScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, i);
    }

    @Override
    public void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i) {
        target.blitRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i);
    }

    @Override
    public void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean blendSub) {
        target.blendRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, blendSub);
    }

    @Override
    public void drawText(int x, int y, int r, int g, int b, int i, String text) {
        target.drawText(x, y, r, g, b, i, text);
    }

    @Override
    public void clearAll(int i, int i0, int i1) {
        target.clearAll(i, i0, i1);
    }

    @Override
    public void clearRect(int r, int g, int b, int x, int y, int width, int height) {
        target.clearRect(r, g, b, x, y, width, height);
    }

    @Override
    public void shutdown() {
        target.shutdown();
    }

    @Override
    public int[] getLocalST() {
        return target.getLocalST();
    }

    @Override
    public void updateST() {
        target.updateST();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        shutdown();
    }

    @Override
    public Runnable[] getLockingSequenceN() {
        INativeImageHolder t = (INativeImageHolder) target;
        return t.getLockingSequenceN();
    }

    @Override
    public Object getNative() {
        INativeImageHolder t = (INativeImageHolder) target;
        return t.getNative();
    }
}
