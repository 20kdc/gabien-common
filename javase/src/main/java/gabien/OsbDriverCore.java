/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.backendhelp.Blender;
import gabien.backendhelp.INativeImageHolder;
import gabien.text.NativeFont;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Created on 04/06/17.
 */
public class OsbDriverCore extends AWTImage implements IWindowGrBackend {
    public Graphics2D bufGraphics;
    private final boolean alpha;

    // keeps track of Graphics-side local translation
    private int translationX, translationY;

    private int[] localST = new int[6];

    public OsbDriverCore(int w, int h, boolean a) {
        alpha = a;
        resize(w, h);
    }

    public void resize(int w, int h) {
        buf = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        bufGraphics = buf.createGraphics();
        translationX = 0;
        translationY = 0;
        localST[0] = 0;
        localST[1] = 0;
        localST[2] = 0;
        localST[3] = 0;
        localST[4] = w;
        localST[5] = h;
    }

    @Override
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i) {
        INativeImageHolder nih = (INativeImageHolder) i;
        bufGraphics.drawImage((BufferedImage) nih.getNative(), x, y, (x + srcw), (y + srch), srcx, srcy, (srcx + srcw), (srcy + srch), null);
    }

    @Override
    public void blitTiledImage(int x, int y, int w, int h, IImage i) {
        INativeImageHolder nih = (INativeImageHolder) i;
        int iw = i.getWidth();
        int ih = i.getHeight();
        for (int j = 0; j < w; j += iw) {
            for (int k = 0; k < h; k += ih) {
                int pw = Math.min(iw, w - j);
                int ph = Math.min(ih, h - k);
                bufGraphics.drawImage((BufferedImage) nih.getNative(), x + j, y + k, x + j + pw, y + k + ph, 0, 0, pw, ph, null);
            }
        }
        /* ACTUALLY SLOWER ACCORDING TO USER REPORTS AND PERSONAL TESTING
        Paint p = bufGraphics.getPaint();
        bufGraphics.setPaint(new TexturePaint((BufferedImage) nih.getNative(), new Rectangle2D.Float(0, 0, i.getWidth(), i.getHeight())));
        bufGraphics.translate(x, y);
        bufGraphics.fillRect(0, 0, w, h);
        bufGraphics.translate(-x, -y);
        bufGraphics.setPaint(p);*/
    }

    @Override
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i) {
        INativeImageHolder nih = (INativeImageHolder) i;
        bufGraphics.drawImage((BufferedImage) nih.getNative(), x, y, (x + acw), (y + ach), srcx, srcy, (srcx + srcw), (srcy + srch), null);
    }

    @Override
    public void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i) {
        AffineTransform oldTransform = bufGraphics.getTransform();
        bufGraphics.translate(x + (acw / 2.0d), y + (ach / 2.0d));
        bufGraphics.rotate((-angle / 360.0d) * (Math.PI * 2.0d));
        bufGraphics.translate(-(acw / 2.0d), -(ach / 2.0d));
        INativeImageHolder nih = (INativeImageHolder) i;
        bufGraphics.drawImage((BufferedImage) nih.getNative(), 0, 0, acw, ach, srcx, srcy, (srcx + srcw), (srcy + srch), null);
        bufGraphics.setTransform(oldTransform);
    }

    @Override
    public void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean blendSub) {
        Blender.blendRotatedScaledImage(this, srcx, srcy, srcw, srch, x, y, acw, ach, angle, i, blendSub);
    }

    @Override
    public void drawText(int x, int y, int r, int cg, int b, @NonNull char[] text, int index, int count, @NonNull NativeFont font) {
        try {
            int textSize = 16;
            if (font instanceof AWTNativeFont) {
                AWTNativeFont nf = (AWTNativeFont) font;
                bufGraphics.setFont(nf.font);
                textSize = nf.size;
            }
            bufGraphics.setColor(new Color(r, cg, b));
            bufGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // --- NOTE before changing this. Offset of +1 causes underscore to be hidden on some fonts.
            bufGraphics.drawString(new String(text, index, count), x, y + (textSize - (textSize / 4)));
        } catch (Exception ex) {
        }
    }

    @Override
    public void clearAll(int i, int i0, int i1) {
        bufGraphics.setColor(new Color(i, i0, i1));
        bufGraphics.fillRect(-translationX, -translationY, buf.getWidth(), buf.getHeight());
    }

    @Override
    public void clearRect(int i, int i0, int i1, int x, int y, int w, int h) {
        bufGraphics.setColor(new Color(i, i0, i1));
        bufGraphics.fillRect(x, y, w, h);
    }

    @Override
    public void shutdown() {
        buf = null;
        bufGraphics = null;
    }

    @Override
    public int[] getLocalST() {
        return localST;
    }

    @Override
    public void updateST() {
        bufGraphics.translate(localST[0] - translationX, localST[1] - translationY);
        translationX = localST[0];
        translationY = localST[1];
        bufGraphics.setClip(localST[2] - translationX, localST[3] - translationY, localST[4] - localST[2], localST[5] - localST[3]);
    }

}
