/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.natives.examples;

import gabien.natives.BadGPU;

/**
 * Interface for Main as seen by states
 * Logical counterpart to IState
 * Created 3rd June, 2023
 */
public interface IMain {
    BadGPU.Instance getInstance();
    void setState(State state);
    public static final int KEY_W = 0;
    public static final int KEY_A = 1;
    public static final int KEY_S = 2;
    public static final int KEY_D = 3;
    public static final int KEY_Z = 4;
    public static final int KEY_X = 5;
    public static final int KEY_SPACE = 6;
    boolean getKey(int keyID);
    boolean getKeyEvent(int keyID);
    float getDeltaTime();
}
