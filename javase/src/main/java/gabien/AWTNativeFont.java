/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;

import org.eclipse.jdt.annotation.NonNull;

import gabien.text.NativeFont;

/**
 * Created 16th Februrary, 2023
 */
public class AWTNativeFont extends NativeFont {
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
    public static NativeFont getFont(int textSize, String s) {
        String modified = s == null ? GaBIEnImpl.getDefaultFont() : null; 
        try {
            return new AWTNativeFont(new Font(modified, Font.PLAIN, textSize - (textSize / 8)), textSize);
        } catch (Exception ex) {
        }
        if (s == null) {
            // Shouldn't happen, so return a fake font as if we know what we're doing.
            System.err.println("AWTNativeFont failed to get fallback font, so a completely fake NativeFont has been generated. Text will probably not display.");
            return new NativeFont() {
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
        Rectangle r = font.getStringBounds(text, index, count, frc).getBounds();
        return r.width;
    }
    
}
