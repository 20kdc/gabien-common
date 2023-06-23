/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

import gabien.GaBIEn;
import gabien.backend.IGaBIEn;
import gabien.render.IImage;

/**
 * Container to try and pull this stuff out of FontManager.
 * Created 23rd June, 2023.
 */
public final class EngineFonts {
    public final IImmFixedSizeFont f6;
    public final IImmFixedSizeFont f8;
    public final IImmFixedSizeFont f16;

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
}
