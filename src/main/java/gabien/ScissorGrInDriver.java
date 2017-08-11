/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package gabien;

import gabien.backendhelp.Blender;
import gabien.ui.Rect;

import java.util.HashSet;

/**
 * Used for cropping in another driver.
 * Created on 1/29/17.
 */
public class ScissorGrInDriver implements IGrInDriver {
    public int workLeft, workTop;
    public int workRight, workBottom;
    public IGrInDriver inner;

    @Override
    public boolean stillRunning() {
        return inner.stillRunning();
    }

    @Override
    public int getWidth() {
        return inner.getWidth();
    }

    @Override
    public int getHeight() {
        return inner.getHeight();
    }

    @Override
    public int[] getPixels() {
        return inner.getPixels();
    }

    @Override
    public byte[] createPNG() {
        return inner.createPNG();
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {
        commonBlitImage(srcx, srcy, srcw, srch, x, y, srcw, srch, i);
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        commonBlitImage(srcx, srcy, srcw, srch, x, y, acw, ach, i);
    }

    @Override
    public void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i) {
        // Nothing can be done here.
        inner.blitRotatedScaledImage(srcx, srcy, srcw, srch, x, y, acw, ach, angle, i);
    }

    @Override
    public void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean blendSub) {
        Blender.blendRotatedScaledImage(this, srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, blendSub);
    }

    @Override
    public void drawText(int x, int y, int r, int g, int b, int i, String text) {
        if (x < workLeft)
            return;
        if (y < workTop)
            return;
        if (x >= workRight)
            return;
        if (y >= workBottom)
            return;
        inner.drawText(x, y, r, g, b, i, text);
    }

    private void commonBlitImage(int srcx, int srcy, int srcw, int srch, int x, int y, int scrw, int scrh, IImage i) {
        Rect spriteSpace = new Rect(srcx, srcy, srcw, srch);
        Rect screenSpace = new Rect(x, y, scrw, scrh);

        Rect alteredScreenspace = screenSpace.getIntersection(new Rect(workLeft, workTop, workRight - workLeft, workBottom - workTop));
        if (alteredScreenspace == null)
            return;
        spriteSpace = spriteSpace.transformed(screenSpace, alteredScreenspace);
        screenSpace = alteredScreenspace;

        Rect alteredSpritespace = spriteSpace.getIntersection(new Rect(0, 0, i.getWidth(), i.getHeight()));
        if (alteredSpritespace == null)
            return;
        screenSpace = screenSpace.transformed(spriteSpace, alteredSpritespace);
        spriteSpace = alteredSpritespace;

        if ((scrw != srcw) || (scrh != srch)) {
            inner.blitScaledImage(spriteSpace.x, spriteSpace.y, spriteSpace.width, spriteSpace.height, screenSpace.x, screenSpace.y, screenSpace.width, screenSpace.height, i);
        } else {
            inner.blitImage(spriteSpace.x, spriteSpace.y, spriteSpace.width, spriteSpace.height, screenSpace.x, screenSpace.y, i);
        }
    }

    @Override
    public void clearAll(int i, int i0, int i1) {
        inner.clearRect(i, i0, i1, workLeft, workTop, workRight - workLeft, workBottom - workTop);
    }

    @Override
    public void clearRect(int r, int g, int b, int x, int y, int width, int height) {
        Rect rct = new Rect(x, y, width, height);
        rct = new Rect(workLeft, workTop, workRight - workLeft, workBottom - workTop).getIntersection(rct);
        if (rct != null)
            inner.clearRect(r, g, b, rct.x, rct.y, rct.width, rct.height);
    }

    @Override
    public boolean flush() {
        return inner.flush();
    }

    @Override
    public boolean isKeyDown(int KEYID) {
        return inner.isKeyDown(KEYID);
    }

    @Override
    public boolean isKeyJustPressed(int KEYID) {
        return inner.isKeyJustPressed(KEYID);
    }

    @Override
    public void clearKeys() {
        inner.clearKeys();
    }

    @Override
    public int getMouseX() {
        return inner.getMouseX();
    }

    @Override
    public int getMouseY() {
        return inner.getMouseY();
    }

    @Override
    public HashSet<Integer> getMouseDown() {
        return inner.getMouseDown();
    }

    @Override
    public HashSet<Integer> getMouseJustDown() {
        return inner.getMouseJustDown();
    }

    @Override
    public HashSet<Integer> getMouseJustUp() {
        return inner.getMouseJustUp();
    }

    @Override
    public boolean getMousewheelJustDown() {
        return inner.getMousewheelJustDown();
    }

    @Override
    public boolean getMousewheelDir() {
        return inner.getMousewheelDir();
    }

    @Override
    public String maintain(int x, int y, int width, String text) {
        return inner.maintain(x, y, width, text);
    }

    @Override
    public void shutdown() {
        inner.shutdown();
    }
}
