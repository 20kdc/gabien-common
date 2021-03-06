/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.backendhelp;

import gabien.GaBIEn;
import gabien.IGrDriver;
import gabien.IImage;

/**
 * Created on 8/2/17.
 */
public class Blender {
    public static void blendRotatedScaledImage(IGrDriver igd, int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean subtractive) {
        if (acw <= 0)
            return;
        if (ach <= 0)
            return;

        // Need a better calculation for this (keep rotation firmly in mind)
        int borderLeft = ((Math.max(acw, ach) * 2) - acw) / 2;
        int borderUp = ((Math.max(acw, ach) * 2) - ach) / 2;
        int twid = acw + (borderLeft * 2);
        int thei = ach + (borderUp * 2);

        if (angle == 0) {
            // No rotation
            twid = acw;
            thei = ach;
            borderLeft = 0;
            borderUp = 0;
        }
        IGrDriver buf1 = GaBIEn.makeOffscreenBuffer(twid, thei, true);
        int[] localST = igd.getLocalST();
        buf1.blitImage((x - borderLeft) + localST[0], (y - borderUp) + localST[1], twid, thei, 0, 0, igd);
        int[] bufferA = buf1.getPixels();
        buf1.clearAll(0, 0, 0);
        // Note regarding these calculations: blitRotatedScaledImage assumes you treat it like an ordinary blit,
        //  i.e. it's definition of X/Y is based on a non-rotated blit.
        // So this has been using horribly wrong behavior and correcting over itself. Oops.
        buf1.blitRotatedScaledImage(srcx, srcy, srcw, srch, borderLeft, borderUp, acw, ach, angle, i);
        int[] bufferB = buf1.getPixels();
        blendImage(bufferA, bufferB, subtractive);
        buf1.shutdown();
        igd.blitImage(0, 0, twid, thei, x - borderLeft, y - borderUp, GaBIEn.createImage(bufferA, twid, thei));
    }

    private static void blendImage(int[] bufferA, int[] bufferB, boolean subtractive) {
        for (int i = 0; i < bufferA.length; i++) {
            int rgb = bufferA[i];
            int a = (rgb & 0xFF000000) >> 24;
            a &= 0xFF;
            int r = (rgb & 0xFF0000) >> 16;
            int g = (rgb & 0xFF00) >> 8;
            int b = rgb & 0xFF;
            int rgb2 = bufferB[i];
            int a2 = (rgb2 & 0xFF000000) >> 24;
            a2 &= 0xFF;
            int r2 = (rgb2 & 0xFF0000) >> 16;
            int g2 = (rgb2 & 0xFF00) >> 8;
            int b2 = rgb2 & 0xFF;
            if (subtractive) {
                r2 = -r2;
                g2 = -g2;
                b2 = -b2;
            }
            a = Math.max(Math.min(255, a + a2), 0);
            r = Math.max(Math.min(255, r + r2), 0);
            g = Math.max(Math.min(255, g + g2), 0);
            b = Math.max(Math.min(255, b + b2), 0);
            int rgb3 = 0;
            rgb3 |= a << 24;
            rgb3 |= r << 16;
            rgb3 |= g << 8;
            rgb3 |= b;
            bufferA[i] = rgb3;
        }
    }
}
