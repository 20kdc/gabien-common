/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.io.InputStream;
import java.util.function.Function;

/**
 * Asset access abstraction.
 * Defaults to something reasonably normal and sane, but some platforms (read: Android and only Android) need different code.
 * Created 5th November, 2025.
 */
public class AssetFS {
    /**
     * Function to read an asset by name.
     * Defaults to the JavaSE handling.
     * In practice, gabien-common overrides this in GaBIEn.setupNativesAndAssets.
     */
    public static Function<String, InputStream> READER = (s) -> {
        return ClassLoader.getSystemClassLoader().getResourceAsStream("assets/" + s);
    };

    private AssetFS() {
    }
}
