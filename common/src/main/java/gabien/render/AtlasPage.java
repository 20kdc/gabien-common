/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.render;

/**
 * This is pretty much intended to be a thing you can cheaply blit textures onto.
 * Beware: Ordering is not guaranteed!
 *
 * 18th July, 2023.
 */
public abstract class AtlasPage extends RenderTarget {

    public AtlasPage(String id, int w, int h) {
        super(id, w, h);
    }

    public abstract void copyFrom(int srcx, int srcy, int srcw, int srch, int targetx, int targety, ITexRegion base);
}
