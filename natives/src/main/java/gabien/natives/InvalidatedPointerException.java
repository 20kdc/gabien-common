/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

/**
 * Thrown when a pointer that is not valid anymore is used.
 * Created 30th May, 2023.
 */
@SuppressWarnings("serial")
public class InvalidatedPointerException extends IllegalArgumentException {
    public InvalidatedPointerException(Object obj) {
        super("Invalid pointer: " + obj);
    }
}
