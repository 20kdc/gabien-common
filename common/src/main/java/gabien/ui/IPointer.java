/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

/**
 * Part of multi-touch stuff.
 */
public interface IPointer {
    int getX();
    int getY();
    PointerType getType();
    void performOffset(int x, int y);
    enum PointerType {
        Generic, // LMB, finger-touch
        Middle, // MMB
        Right, // RMB
        X1, // (WARNING: ABSOLUTELY DO NOT RELY ON!)
        X2, // (WARNING: ABSOLUTELY DO NOT RELY ON!)
        Mouse // Unknown
    }
}
