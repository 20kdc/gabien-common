/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.atlas;

import gabien.render.AtlasPage;
import gabien.render.ITexRegion;

/**
 * Image that can be drawn to an AtlasPage.
 * Created 18th July, 2023.
 */
public final class ImageAtlasDrawable extends AtlasDrawable {
    public final ITexRegion source;

    public ImageAtlasDrawable(ITexRegion itr) {
        super((int) itr.getRegionWidth(), (int) itr.getRegionHeight());
        source = itr;
    }

    @Override
    public void drawTo(AtlasPage ap, int x, int y) {
        ap.copyFrom(0, 0, width, height, x, y, source);
    }
}
