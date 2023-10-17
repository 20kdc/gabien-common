/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import gabien.ui.UIElement.UIProxy;

import gabien.GaBIEnUI;
import gabien.ui.UIBorderedElement;
import gabien.ui.UILabel;
import gabien.ui.UIScrollLayout;
import gabien.ui.UIScrollbar;
import gabien.ui.UISplitterLayout;
import gabien.uslx.append.HexByteEncoding;
import gabien.uslx.append.Consumer;
import gabien.uslx.append.Rect;
import gabien.wsi.IPeripherals;

/**
 * Viewer of bytes.
 * Created 19th June, 2023.
 */
public class UIBytesEditor extends UIProxy {
    public byte[] data;
    public int rowW = 16;
    public int dataOffset = 0;
    public int addressPad = 0;
    Row[] rows;
    UIScrollLayout usl = new UIScrollLayout(true, 16);
    UIScrollbar usb = new UIScrollbar(true, 16);

    public UIBytesEditor(byte[] init, Consumer<byte[]> res) {
        data = init;
        setRowCount(1);
        proxySetElement(new UISplitterLayout(usl, usb, false, 1), false);
        setForcedBounds(null, new Rect(0, 0, 640, 480));
        setLAFParentOverride(GaBIEnUI.sysThemeRoot);
        refresh();
    }

    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        usb.handleMousewheel(x, y, north);
    }

    void setRowCount(int rc) {
        if (rows != null)
            if (rows.length == rc)
                return;
        rows = new Row[rc];
        usl.panelsClear();
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new Row(i);
            usl.panelsAdd(rows[i]);
        }
        refresh();
    }

    @Override
    public void runLayout() {
        int h = getSize().height;
        int rh = UIBorderedElement.getBorderedTextHeight(getTheme(), 16);
        setRowCount(h / rh);
        super.runLayout();
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        super.update(deltaTime, selected, peripherals);
        int ndo = (int) (usb.scrollPoint * data.length);
        if (ndo != dataOffset) {
            dataOffset = ndo;
            refresh();
        }
    }

    public String formatAddress(int address) {
        String basis = Integer.toHexString(address);
        while (basis.length() < addressPad)
            basis = "0" + basis;
        return basis;
    }

    public void refresh() {
        usb.wheelScale = ((double) ((rows.length / 2) * rowW)) / data.length;
        int largestRelevant = data.length + (rows.length * rowW);
        addressPad = Integer.toHexString(largestRelevant).length();
        for (int i = 0; i < rows.length; i++)
            rows[i].refresh();
    }

    boolean isInBounds(int idx) {
        if (idx < 0 || idx >= data.length)
            return false;
        return true;
    }

    class Row extends UIProxy {
        public int index;
        public final UILabel body = new UILabel("", 16);
        public Row(int idx) {
            index = idx;
            proxySetElement(body, true);
        }
        public void refresh() {
            StringBuilder sb = new StringBuilder();
            int base = dataOffset + (index * rowW);
            int lim = base + rowW;
            sb.append(formatAddress(base));
            sb.append(": ");
            for (int i = base; i < lim; i++) {
                if (isInBounds(i)) {
                    int val = data[i] & 0xFF;
                    sb.append(HexByteEncoding.toHexString(val));
                    sb.append(' ');
                } else {
                    sb.append("   ");
                }
            }
            sb.append("| ");
            for (int i = base; i < lim; i++) {
                if (isInBounds(i)) {
                    int val = data[i] & 0xFF;
                    if (val < 0x20 || val >= 0x7F) {
                        sb.append(".");
                    } else {
                        sb.append((char) val);
                    }
                } else {
                    sb.append(".");
                }
            }
            body.text = sb.toString();
        }
    }
}
