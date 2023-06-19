/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import gabien.ui.UIElement.UIProxy;

import java.nio.charset.StandardCharsets;

import gabien.GaBIEn;
import gabien.ui.Rect;
import gabien.ui.UILabel;
import gabien.ui.UIScrollLayout;
import gabien.uslx.append.HexByteEncoding;
import gabien.uslx.append.IConsumer;

/**
 * Viewer of bytes.
 * Created 19th June, 2023.
 */
public class UIBytesEditor extends UIProxy {
    public UIBytesEditor(byte[] init, IConsumer<byte[]> res) {
        UIScrollLayout usl = new UIScrollLayout(true, 16);
        usl.panelsAdd(new UILabel(HexByteEncoding.toHexString(init) + "\n" + new String(init, StandardCharsets.ISO_8859_1), 16));
        proxySetElement(usl, false);
        setForcedBounds(null, new Rect(0, 0, 640, 480));
        setLAFParentOverride(GaBIEn.sysThemeRoot);
    }
}
