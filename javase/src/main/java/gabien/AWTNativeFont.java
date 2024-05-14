/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.eclipse.jdt.annotation.NonNull;

import gabien.text.IFixedSizeFont;
import gabien.text.ImageRenderedTextChunk;

/**
 * Created 16th Februrary, 2023
 */
public class AWTNativeFont implements IFixedSizeFont {
    public final Font font;
    public final int size;
    private static final FontRenderContext frc = new FontRenderContext(AffineTransform.getTranslateInstance(0, 0), true, false);

    public AWTNativeFont(Font f, int apparentSize) {
        font = f;
        size = apparentSize;
    }

    @Override
    public int getLineHeight() {
        return size;
    }

    @Override
    public int getContentHeight() {
        return size - (size / 8);
    }

    @Override
    public int measureLine(@NonNull char[] text, int index, int count, boolean withLastAdvance) {
        if (GaBIEnImpl.fontsAlwaysMeasure16)
            return 16;
        Rectangle r = font.getStringBounds(text, index, index + count, frc).getBounds();
        return r.width;
    }

    @Override
    public int measureLine(@NonNull String text, boolean withLastAdvance) {
        if (GaBIEnImpl.fontsAlwaysMeasure16)
            return 16;
        Rectangle r = font.getStringBounds(text, frc).getBounds();
        return r.width;
    }

    private Graphics2D createGraphics(BufferedImage bi, int r, int g, int b, int a) {
        Graphics2D bufGraphics = bi.createGraphics();
        bufGraphics.setFont(font);
        bufGraphics.setColor(new Color(r, g, b, a));
        bufGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return bufGraphics;
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull char[] text, int index, int length, int r, int g, int b, int a) {
        try {
            Rectangle2D measure = font.getStringBounds(text, index, index + length, frc);
            int ascent = -(int) Math.floor(measure.getMinY());
            int descent = (int) Math.ceil(measure.getMaxY());
            int margin = 16;
            int mt = (int) Math.ceil(measure.getWidth());
            int offsetX = margin, offsetY = margin + ascent;
            BufferedImage bi = new BufferedImage(margin + mt + margin, margin + ascent + descent + margin, BufferedImage.TYPE_INT_ARGB);
            // --- NOTE before changing this. Offset of +1 causes underscore to be hidden on some fonts.
            createGraphics(bi, r, g, b, a).drawChars(text, index, length, offsetX, offsetY);
            return new ImageRenderedTextChunk.WSI(-offsetX, -offsetY, mt, size, ascent, descent, new AWTWSIImage(bi));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ImageRenderedTextChunk.GPU(0, 0, 0, size, 0, 0, GaBIEn.getErrorImage());
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull String text, int r, int g, int b, int a) {
        try {
            Rectangle2D measure = font.getStringBounds(text, frc);
            int ascent = -(int) Math.floor(measure.getMinY());
            int descent = (int) Math.ceil(measure.getMaxY());
            int margin = 16;
            int mt = (int) Math.ceil(measure.getWidth());
            int offsetX = margin, offsetY = margin + ascent;
            BufferedImage bi = new BufferedImage(margin + mt + margin, margin + ascent + descent + margin, BufferedImage.TYPE_INT_ARGB);
            // --- NOTE before changing this. Offset of +1 causes underscore to be hidden on some fonts.
            createGraphics(bi, r, g, b, a).drawString(text, offsetX, offsetY);
            return new ImageRenderedTextChunk.WSI(-offsetX, -offsetY, mt, size, ascent, descent, new AWTWSIImage(bi));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ImageRenderedTextChunk.GPU(0, 0, 0, size, 0, 0, GaBIEn.getErrorImage());
    }
}
