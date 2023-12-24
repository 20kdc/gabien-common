/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import gabien.wsi.IPeripherals;

/**
 * Fancy internal APIs as part of Textbox rework
 * Created on July 28th, 2022.
 */
public interface IGJSEPeripheralsInternal extends IPeripherals {
    String aroundTheBorderworldMaintain(TextboxMaintainer tm, int x, int y, int w, int h);

    void finishRemovingEditingSession();
}
