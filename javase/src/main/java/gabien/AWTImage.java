/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.backendhelp.INativeImageHolder;

import java.awt.image.BufferedImage;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Created on 04/06/17.
 */
public class AWTImage implements IImage, INativeImageHolder {
    protected BufferedImage buf;

    @Override
    public int getWidth() {
        return buf.getWidth();
    }

    @Override
    public int getHeight() {
        return buf.getHeight();
    }

    @Override
    public void getPixelsAsync(@NonNull int[] buffer, @NonNull Runnable onDone) {
        buf.getRGB(0, 0, buf.getWidth(), buf.getHeight(), buffer, 0, buf.getWidth());
        onDone.run();
    }

    @Override
    public Object getNative() {
        return buf;
    }
}
