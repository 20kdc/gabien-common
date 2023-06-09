/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien;

import java.util.HashMap;

/**
 * Getting this code put here.
 * Created 16th February 2023.
 */
final class ImageCache {
    private HashMap<String, IImage> loadedImages = new HashMap<String, IImage>();

    IImage getImage(String a, boolean res) {
        String ki = a + "_N_N_N" + (res ? 'R' : 'F');
        if (loadedImages.containsKey(ki))
            return loadedImages.get(ki);
        IWSIImage img = GaBIEn.internal.getImage(a, res);
        IImage resImg;
        if (img == null) {
            resImg = GaBIEn.getErrorImage();
        } else {
            resImg = img.upload("ImageCache:" + ki);
        }
        loadedImages.put(ki, resImg);
        return resImg;
    }

    IImage getImageCK(String a, boolean res, int tr, int tg, int tb) {
        String ki = a + "_" + tr + "_" + tg + "_" + tb + (res ? 'R' : 'F');
        if (loadedImages.containsKey(ki))
            return loadedImages.get(ki);
        IWSIImage img = GaBIEn.internal.getImage(a, res);
        IImage resImg;
        if (img == null) {
            resImg = GaBIEn.getErrorImage();
        } else {
            int[] data = img.getPixels();
            for (int i = 0; i < data.length; i++) {
                int c = data[i];
                if ((c & 0xFFFFFF) != (tb | (tg << 8) | (tr << 16))) {
                    data[i] = c | 0xFF000000;
                } else {
                    data[i] = 0;
                }
            }
            resImg = GaBIEn.createImage("ImageCache:" + ki, data, img.getWidth(), img.getHeight());
        }
        loadedImages.put(ki, resImg);
        return resImg;
    }

    public void hintFlushAllTheCaches() {
        loadedImages.clear();
    }

}
