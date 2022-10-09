/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien;

// WARNING! I am fully aware this is deprecated.
// However, we target API level 9. Deal with it.
import android.text.ClipboardManager;
import android.content.Context;

@SuppressWarnings("deprecation")
public class ClipboardImpl implements IGaBIEnClipboard {

    @Override
    public void copyText(String text) {
        ClipboardManager cm = (ClipboardManager) AndroidPortGlobals.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setText(text);
    }

}
