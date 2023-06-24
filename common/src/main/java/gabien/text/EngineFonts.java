/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import org.eclipse.jdt.annotation.NonNull;

import gabien.GaBIEn;
import gabien.backend.IGaBIEn;
import gabien.render.IGrDriver;
import gabien.render.IImage;

/**
 * Container to try and pull this stuff out of FontManager.
 * Created 23rd June, 2023.
 */
public final class EngineFonts implements ITypeface {
    public final SimpleImageGridFont f6;
    public final SimpleImageGridFont f8;
    public final SimpleImageGridFont f16;

    public EngineFonts(IGaBIEn backend) {
        GaBIEn.verify(backend);
        IImage i6 = GaBIEn.getImageCKEx("fonttiny.png", false, true, 0, 0, 0);
        IImage i8 = GaBIEn.getImageCKEx("font.png", false, true, 0, 0, 0);
        IImage i16 = GaBIEn.getImageCKEx("font2x.png", false, true, 0, 0, 0);
        //                                 W  H   C   A  S
        f6 =  new SimpleImageGridFont(i6,  3,  5, 16, 4,  6);
        f8 =  new SimpleImageGridFont(i8,  7,  7, 16, 8,  8);
        f16 = new SimpleImageGridFont(i16, 7, 14, 16, 8, 16);
    }

    /**
     * Immediate low-quality but fast draw.
     * This is for when you don't have any other good options.
     * Just to be clear, this code cheats.
     */
    public void drawString(IGrDriver igd, int x, int y, String text, boolean noBackground, boolean textBlack, int wantedTextSize) {
        IImmFixedSizeFont exactMatch = null;
        if (wantedTextSize <= 7) {
            exactMatch = f6;
        } else if (wantedTextSize <= 15) {
            exactMatch = f8;
        } else if (wantedTextSize <= 31) {
            exactMatch = f16;
        }
        if (exactMatch != null) {
            if (noBackground) {
                exactMatch.drawLine(igd, x, y, text, textBlack);
            } else {
                exactMatch.drawLAB(igd, x, y, text, textBlack);
            }
            return;
        }
        // alright, we have to scale. work out base metrics...
        int scale = (wantedTextSize / 16);
        int charWidth = 7 * scale;
        int charHeight = 14 * scale;
        // calculate...
        int len = text.length();
        int advance = 8 * scale;
        if (!noBackground) {
            int textTotalW = SimpleImageGridFont.measureLineCommon(advance, charWidth, len, false);
            int cc = textBlack ? 255 : 0;
            ImageRenderedTextChunk.background(igd, x, y, textTotalW, charHeight, 1, cc, cc, cc, 255);
        }
        // well, this is going to be really awkward...
        final IImage font = textBlack ? f16.fontBlack : f16.fontWhite;
        for (int i = 0; i < len; i++) {
            int chr = text.charAt(i);
            if (chr >= 256)
                chr = 0;
            int srcx = 0;
            int srcy = 0;
            igd.blitScaledImage(srcx, srcy, 7, 14, x, y, charWidth, charHeight, font);
            x += advance;
        }
    }

    /**
     * Returns an approximate IFixedSizeFont for the given line height.
     * This doesn't scale.
     * @param lineHeight The target font height (pixels per line).
     * @return An internal font with a 128-character image covering ASCII (with some codepage 437)
     */
    @Override
    public @NonNull IFixedSizeFont derive(int lineHeight) {
        if (lineHeight >= 16) {
            return f16;
        } else if (lineHeight >= 8) {
            return f8;
        } else {
            return f6;
        }
    }
}
