/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

import java.io.InputStream;
import java.util.HashMap;

import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.render.IImage;
import gabien.render.WSIImage;

/**
 * Getting this code put here.
 * Created 16th February 2023.
 */
public final class ImageCache {
    private HashMap<String, WSIImage> loadedWSIImages = new HashMap<String, WSIImage>();
    private HashMap<String, IImage> loadedImages = new HashMap<String, IImage>();
    private final IGaBIEn backend; 

    public ImageCache(IGaBIEn backend) {
        this.backend = backend;
        GaBIEn.verify(backend);
    }

    public @Nullable WSIImage getWSIImage(String a, boolean res) {
        String ki = a + (res ? 'R' : 'F');
        if (loadedWSIImages.containsKey(ki))
            return loadedWSIImages.get(ki);
        InputStream ip = res ? GaBIEn.getResource(a) : GaBIEn.getInFile(a);
        WSIImage img = ip != null ? backend.decodeWSIImage(ip) : null;
        loadedWSIImages.put(ki, img);
        return img;
    }

    public IImage getImage(String a, boolean res) {
        String ki = a + "_N_N_N" + (res ? 'R' : 'F');
        if (loadedImages.containsKey(ki))
            return loadedImages.get(ki);
        WSIImage img = getWSIImage(a, res);
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
        WSIImage img = getWSIImage(a, res);
        IImage resImg;
        if (img == null) {
            resImg = GaBIEn.getErrorImage();
        } else {
            resImg = GaBIEn.wsiToCK("ImageCache:" + ki, img, tr, tg, tb);
        }
        loadedImages.put(ki, resImg);
        return resImg;
    }

    public void hintFlushAllTheCaches() {
        loadedImages.clear();
        loadedWSIImages.clear();
    }

}
