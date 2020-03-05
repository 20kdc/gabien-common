/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien;

import gabien.ui.IConsumer;

/**
 * This abstracts specifically file browser functionality,
 *  so that a backendhelp class can provide a reference implementation.
 * Created on 04/03/2020.
 */
public interface IGaBIEnFileBrowser {
    // Sets the file browser directory path.
    // Same path format as usual.
    void setBrowserDirectory(String s);

    // Starts a file browser.
    // This is a replacement for UIFileBrowser, and uses native elements whenever possible.
    // Regarding the path, the only guarantee is that it'll be null or a valid file path.
    // It does not necessarily have to match the standard gabien path separator.
    void startFileBrowser(String text, boolean saving, String exts, IConsumer<String> result);
}
