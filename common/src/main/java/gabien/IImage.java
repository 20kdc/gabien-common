/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

/**
 * An image. All IImages that are not IGrDrivers must be immutable.
 * Created on 11/08/17.
 */
public interface IImage {
    int getWidth();

    int getHeight();

    // 0xAARRGGBB. The buffer is safe to edit, but changes do not propagate back.
    int[] getPixels();

    // Creates a PNG file.
    byte[] createPNG();
}
