/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backendhelp;

import org.eclipse.jdt.annotation.NonNull;

import gabien.IGrDriver;
import gabien.IImage;
import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Texture;

/**
 * Created on 6/20/17 as NullOsbDriver. Migrated to gabien.backendhelp.NullGrDriver 7th June, 2023.
 */
public class NullGrDriver implements IGrDriver {
    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull int[] buffer, int bufferOfs, @NonNull Runnable onDone) {
        onDone.run();
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull byte[] buffer, int bufferOfs, @NonNull Runnable onDone) {
        onDone.run();
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
    public void clearAll(int i, int i0, int i1) {

    }

    @Override
    public void clearRectAlpha(int r, int g, int b, int a, int x, int y, int width, int height) {

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
    public void batchFlush() {
    }

    @Override
    public void batchReference(IImage caller) {
    }

    @Override
    public void batchUnreference(IImage caller) {
    }

    @Override
    public Texture getTextureFromTask() {
        return null;
    }

    @Override
    public void otrLock() {
    }

    @Override
    public void otrUnlock() {
    }
}
