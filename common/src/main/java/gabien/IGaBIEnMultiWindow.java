/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.uslx.append.*;

/**
 * This represents a multi-window environment.
 * Created on 04/03/2020.
 */
public interface IGaBIEnMultiWindow {
    // Returns true if this is a single-window environment pretending to be multi-window
    //  through the use of a "window stack".
    boolean isActuallySingleWindow();
    
    // Gets the default window details.
    WindowSpecs defaultWindowSpecs(String name, int w, int h);

    //On SingleWindowApp-style platforms,where windowing doesn't exist,ignore windowspecs.
    IGrInDriver makeGrIn(String name, int w, int h, WindowSpecs windowspecs);
}
