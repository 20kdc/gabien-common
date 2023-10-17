/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien;

import gabien.uslx.append.*;

public interface ITextboxImplementation {
    // Gets the last known text.
    String getLastKnownText();
    // Makes the textbox active, overwriting text/etc.
    // Immediately updates last known text.
    void setActive(final String contents, final boolean multiLine, final Function<String, String> feedback);
    // Makes the textbox inactive.
    void setInactive();
    // Returns true if the textbox is currently active, false otherwise.
    boolean checkupUsage();
    // Returns false for the "dead" textbox implementation which won't return text you put into it.
    boolean isTrustworthy();
}
