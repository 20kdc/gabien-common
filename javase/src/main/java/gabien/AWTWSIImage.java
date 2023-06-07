/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Copied from AWTImage and OsbDriverCore 7th June 2023.
 */
public class AWTWSIImage implements IWSIImage.RW {
    private final BufferedImage buf;
    private final WritableRaster bufWR;

    public AWTWSIImage(@NonNull int[] colours, int width, int height) {
        if (width <= 0 || height <= 0) {
            buf = null;
            bufWR = null;
        } else {
            buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            bufWR = buf.getRaster();
            bufWR.setDataElements(0, 0, width, height, colours);
        }
    }

    @Override
    public int getWidth() {
        if (buf == null)
            return 0;
        return buf.getWidth();
    }

    @Override
    public int getHeight() {
        if (buf == null)
            return 0;
        return buf.getHeight();
    }

    @Override
    public void getPixels(@NonNull int[] colours) {
        if (buf == null)
            return;
        bufWR.getDataElements(0, 0, buf.getWidth(), buf.getHeight(), colours);
    }

    @Override
    public void setPixels(@NonNull int[] colours) {
        if (bufWR != null)
            bufWR.setDataElements(0, 0, buf.getWidth(), buf.getHeight(), colours);
    }

    @Override
    public @NonNull byte[] createPNG() {
        // Not possible to create a PNG with zero width and height.
        if (buf == null)
            return GaBIEn.createWSIImage(new int[1], 1, 1).createPNG();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(buf, "PNG", baos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
