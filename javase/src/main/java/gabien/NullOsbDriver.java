/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import java.awt.image.BufferedImage;

/**
 * Created on 6/20/17.
 */
public class NullOsbDriver implements IWindowGrBackend {
    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int[] getPixels() {
        return new int[0];
    }

    @Override
    public byte[] createPNG() {
        return new byte[0];
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {

    }

    @Override
    public void blitTiledImage(int x, int y, int w, int h, IImage cachedTile) {

    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {

    }

    @Override
    public void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i) {

    }

    @Override
    public void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean blendSub) {

    }

    @Override
    public void drawText(int x, int y, int r, int g, int b, int i, String text) {

    }

    @Override
    public void clearAll(int i, int i0, int i1) {

    }

    @Override
    public void clearRect(int r, int g, int b, int x, int y, int width, int height) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public int[] getLocalST() {
        return new int[6];
    }

    @Override
    public void updateST() {

    }

    @Override
    public void resize(int wantedRW, int wantedRH) {

    }

    @Override
    public Runnable[] getLockingSequenceN() {
        return null;
    }

    @Override
    public Object getNative() {
        // closest thing to a "no-op" image
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    }
}
