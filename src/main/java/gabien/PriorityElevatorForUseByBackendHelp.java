/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien;

/**
 * ...Do NOT touch this.
 * It's used as part of a window management quirk when dealing with single-window systems.
 * Created on 04/03/2020.
 */
public abstract class PriorityElevatorForUseByBackendHelp {
    protected WindowSpecs newWindowSpecs() {
        return new WindowSpecs();
    }
    protected void elevateToSystemPriority(WindowSpecs ws) {
        ws.hasSystemPriority = true;
    }
    protected boolean isOfSystemPriority(WindowSpecs ws) {
        return ws.hasSystemPriority;
    }
}
