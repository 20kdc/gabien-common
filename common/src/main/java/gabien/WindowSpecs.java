/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

public final class WindowSpecs {
    // Stop non-GaBIen classes from creating this object.
    // This ensures that it has to be created via the relevant GaBIEn get-defaults function.
    protected WindowSpecs() {
    }

    public int scale = 1;
    // NOTE: If fullscreen is set or you are on an SWA platform, this is essentially true.
    public boolean resizable = false;
    // NOTE: On SWA platforms, this is totally ignored.
    // Should attempt to "follow the screen the last window was already on".
    public boolean fullscreen = false;
    
    // Creates the window with 'system priority'.
    // This is used for file browsers or other backend-level modal dialogs.
    boolean hasSystemPriority = false;
}
