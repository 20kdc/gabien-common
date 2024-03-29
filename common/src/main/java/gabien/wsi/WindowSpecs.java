/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.wsi;

import gabien.GaBIEn;
import gabien.backend.IGaBIEn;

public final class WindowSpecs {
    /**
     * Creates a truly blank WindowSpecs.
     * This can only be done from the backend or GaBIEn itself.
     */
    public WindowSpecs(IGaBIEn backend) {
        GaBIEn.verify(backend);
    }

    /**
     * Scale. This only works on JSE.
     * This dates to the really old days when this framework was being used for pixel-art stuff.
     */
    public int scale = 1;

    /**
     * If fullscreen is set or you are on an SWA platform, this is essentially forced to true.
     */
    public boolean resizable = false;

    /**
     * NOTE: On SWA platforms, this is totally ignored.
     * Should attempt to "follow the screen the last window was already on".
     */
    public boolean fullscreen = false;

    /**
     * Controls the default background colour for WSIs that care, to reduce flicker.
     */
    public boolean backgroundLight = false;

    /**
     * Creates the window with 'system priority'.
     * This is used for file browsers or other backend-level modal dialogs.
     * This matters for SWA platfomrs.
     */
    boolean hasSystemPriority = false;

    /**
     * Only gabien-common or backend code is supposed to call this.
     */
    public void engineElevateToSystemPriority(IGaBIEn backend) {
        GaBIEn.verify(backend);
        hasSystemPriority = true;
    }

    /**
     * Only gabien-common or backend code is supposed to call this.
     */
    public boolean engineIsOfSystemPriority(IGaBIEn backend) {
        GaBIEn.verify(backend);
        return hasSystemPriority;
    }
}
