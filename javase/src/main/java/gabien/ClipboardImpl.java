/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

public class ClipboardImpl implements IGaBIEnClipboard {

    @Override
    public void copyText(String text) {
        try {
            StringSelection textSel = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(textSel, textSel);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}