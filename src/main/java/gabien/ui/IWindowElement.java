/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;

/**
 * Should be used for UIElement subclasses that might be windows.
 * The methods are only called by things that act as windowing interfaces.
 * Created on 12/30/16.
 */
public interface IWindowElement {
    boolean wantsSelfClose();

    // The window will not be processed after this occurs.
    void windowClosed();
}
