/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.atlas;

import java.util.HashMap;
import java.util.LinkedList;

import gabien.render.AtlasPage;
import gabien.render.ITexRegion;

/**
 * Created 18th July, 2023.
 */
public final class AtlasSet<K> {
    public final HashMap<K, ITexRegion> contents = new HashMap<>();
    public final LinkedList<AtlasPage> pages = new LinkedList<>();

    public void shutdown() {
        for (AtlasPage ap : pages)
            ap.shutdown();
    }
}
