/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

import java.io.InputStream;
import java.util.HashMap;

import gabien.GaBIEn;
import gabien.natives.BadGPU;
import gabien.render.IImage;
import gabien.render.WSIImage;
import gabien.vopeks.VopeksImage;

/**
 * Getting this code put here.
 * Created 16th February 2023.
 */
public final class ImageCache {
    private HashMap<String, IImage> loadedImages = new HashMap<String, IImage>();
    private final IGaBIEn backend; 

    public ImageCache(IGaBIEn backend) {
        this.backend = backend;
        GaBIEn.verify(backend);
    }

    public IImage getImage(String a, boolean res) {
        String ki = a + "_N_N_N" + (res ? 'R' : 'F');
        if (loadedImages.containsKey(ki))
            return loadedImages.get(ki);
        InputStream ip = res ? GaBIEn.getResource(a) : GaBIEn.getInFile(a);
        WSIImage img = ip != null ? backend.decodeWSIImage(ip) : null;
        IImage resImg;
        if (img == null) {
            resImg = GaBIEn.getErrorImage();
        } else {
            resImg = img.upload("ImageCache:" + ki);
        }
        loadedImages.put(ki, resImg);
        return resImg;
    }

    public IImage getImageCK(String a, boolean res, int tr, int tg, int tb) {
        String ki = a + "_" + tr + "_" + tg + "_" + tb + (res ? 'R' : 'F');
        if (loadedImages.containsKey(ki))
            return loadedImages.get(ki);
        InputStream ip = res ? GaBIEn.getResource(a) : GaBIEn.getInFile(a);
        WSIImage img = ip != null ? backend.decodeWSIImage(ip) : null;
        IImage resImg;
        if (img == null) {
            resImg = GaBIEn.getErrorImage();
        } else {
            // do the conversion on the VOPEKS thread
            resImg = new VopeksImage(GaBIEn.vopeks, "ImageCache:" + ki, img.width, img.height, (consumer) -> {
                GaBIEn.vopeks.putTask((instance) -> {
                    int[] data = img.getPixels();
                    int colourKey = tb | (tg << 8) | (tr << 16);
                    for (int i = 0; i < data.length; i++) {
                        int c = data[i];
                        if ((c & 0xFFFFFF) != colourKey) {
                            data[i] = c | 0xFF000000;
                        } else {
                            data[i] = 0;
                        }
                    }
                    consumer.accept(instance.newTexture(img.width, img.height, BadGPU.TextureLoadFormat.ARGBI32, data, 0));
                });
            });
        }
        loadedImages.put(ki, resImg);
        return resImg;
    }

    public void hintFlushAllTheCaches() {
        loadedImages.clear();
    }

}
