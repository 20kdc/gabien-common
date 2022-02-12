/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.backendhelp.INativeImageHolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

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
    public int[] getPixels() {
        int[] arr = new int[buf.getWidth() * buf.getHeight()];
        buf.getRGB(0, 0, buf.getWidth(), buf.getHeight(), arr, 0, buf.getWidth());
        return arr;
    }

    @Override
    public byte[] createPNG() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(buf, "PNG", baos);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }


    @Override
    public Runnable[] getLockingSequenceN() {
        return null;
    }

    @Override
    public Object getNative() {
        return buf;
    }
}
