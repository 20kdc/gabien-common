/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

/**
 * Fancy internal APIs as part of Textbox rework
 * Created on July 28th, 2022.
 */
public interface IGJSEPeripheralsInternal extends IPeripherals {
    String aroundTheBorderworldMaintain(TextboxMaintainer tm, int x, int y, int w, int h, String text);

    void finishRemovingEditingSession();
}