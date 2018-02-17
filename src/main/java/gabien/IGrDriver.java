/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

/**
 * Represents a buffer that can be drawn to.
 * Created on 04/06/17.
 */
public interface IGrDriver extends IImage {
    // These return the size of the drawing buffer.
    int getWidth();
    // These return the size of the drawing buffer.
    int getHeight();

    void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i);

    // Support optional but recommended. Lack of support should result in no-op.
    void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i);

    // Support optional. Should not be used unless absolutely required - cannot be scissored.
    // Lack of support should result in no-op. When scissoring, this is just directly forwarded - nothing can be done here.
    void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i);
    void blendRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i, boolean blendSub);

    // Now as official as you can get for a graphics interface nobody uses.
    // This is "The Way" that text is drawn if the "styled" way doesn't work.
    // The UI package uses this in case international text comes along.
    // Now, this still isn't going to be consistent.
    // It probably never will be.
    // But it will work, and it means I avoid having to include Unifont.
    void drawText(int x, int y, int r, int g, int b, int i, String text);

    void clearAll(int i, int i0, int i1);

    void clearRect(int r, int g, int b, int x, int y, int width, int height);

    // Adjusts Scissoring & Transform (not just scissoring, but oh well)
    void clearScissoring();
    void adjustScissoring(int x, int y, int tx, int ty, int w, int h);

    // Stop all drawing operations. Makes an OsbDriver unusable.
    void shutdown();
}
