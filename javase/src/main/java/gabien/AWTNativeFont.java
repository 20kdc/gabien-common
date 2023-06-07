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

    /**
     * Basically implements GaBIEnImpl.getNativeFont
     */
    public static IFixedSizeFont getFont(int textSize, String s) {
        String modified = s == null ? GaBIEnImpl.getDefaultFont() : null; 
        try {
            return new AWTNativeFont(new Font(modified, Font.PLAIN, textSize - (textSize / 8)), textSize);
        } catch (Exception ex) {
        }
        if (s == null) {
            // Shouldn't happen, so return a fake font as if we know what we're doing.
            System.err.println("AWTNativeFont failed to get fallback font, so a completely fake NativeFont has been generated. Text will probably not display.");
            return new IFixedSizeFont() {
                @Override
                public int getSize() {
                    return textSize;
                }
                @Override
                public int measureLine(@NonNull char[] text, int index, int count) {
                    if (GaBIEnImpl.fontsAlwaysMeasure16)
                        return 16;
                    return (count * textSize) / 2;
                }
                @Override
                public int measureLine(@NonNull String text) {
                    if (GaBIEnImpl.fontsAlwaysMeasure16)
                        return 16;
                    return (text.length() * textSize) / 2;
                }
                @Override
                public ImageRenderedTextChunk renderLine(@NonNull char[] text, int index, int length, boolean textBlack) {
                    return new ImageRenderedTextChunk.GPU(0, 0, measureLine(text, index, length), textSize, GaBIEn.getErrorImage());
                }
                @Override
                public ImageRenderedTextChunk renderLine(@NonNull String text, boolean textBlack) {
                    return new ImageRenderedTextChunk.GPU(0, 0, measureLine(text), textSize, GaBIEn.getErrorImage());
                }
            };
        }
        return null;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public int measureLine(@NonNull char[] text, int index, int count) {
        if (GaBIEnImpl.fontsAlwaysMeasure16)
            return 16;
        Rectangle r = font.getStringBounds(text, index, index + count, frc).getBounds();
        return r.width;
    }

    @Override
    public int measureLine(@NonNull String text) {
        if (GaBIEnImpl.fontsAlwaysMeasure16)
            return 16;
        Rectangle r = font.getStringBounds(text, frc).getBounds();
        return r.width;
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull char[] text, int index, int length, boolean textBlack) {
        return renderLine(new String(text, index, length), textBlack);
    }

    @Override
    public ImageRenderedTextChunk renderLine(@NonNull String text, boolean textBlack) {
        try {
            int mt = measureLine(text);
            int margin = 16;
            BufferedImage bi = new BufferedImage(margin + mt + margin, margin + size + margin, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bufGraphics = bi.createGraphics();
            bufGraphics.setFont(font);
            int cV = textBlack ? 0 : 255;
            bufGraphics.setColor(new Color(cV, cV, cV));
            bufGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // --- NOTE before changing this. Offset of +1 causes underscore to be hidden on some fonts.
            bufGraphics.drawString(text, margin, margin + (size - (size / 4)));
            return new ImageRenderedTextChunk.CPU(-margin, -margin, mt, size, new AWTWSIImage(bi));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ImageRenderedTextChunk.GPU(0, 0, 0, size, GaBIEn.getErrorImage());
    }
}
