/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.wsi;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.render.IGrDriver;
import gabien.render.IImage;

/**
 * A graphics and input driver.The idea is to speed up work on ports.
 */
public interface IGrInDriver {
    // keys! I'm switching the keymap to some custom thing
    // so Oracle can't hold a sword over my head
    // just because I use the same keymap as them.

    // (later: Copyright-paranoid much, old me? Oh well. Good point.)

    int VK_ESCAPE = 0;
    int VK_1 = 1;
    int VK_2 = 2;
    int VK_3 = 3;
    int VK_4 = 4;
    int VK_5 = 5;
    int VK_6 = 6;
    int VK_7 = 7;
    int VK_8 = 8;
    int VK_9 = 9;
    int VK_0 = 10;
    int VK_MINUS = 11;
    int VK_EQUALS = 12;
    int VK_BACK_SPACE = 13;
    int VK_TAB = 14;
    int VK_Q = 15;
    int VK_W = 16;
    int VK_E = 17;
    int VK_R = 18;
    int VK_T = 19;
    int VK_Y = 20;
    int VK_U = 21;
    int VK_I = 22;
    int VK_O = 23;
    int VK_P = 24;
    int VK_OPEN_BRACKET = 25;
    int VK_CLOSE_BRACKET = 26;
    int VK_ENTER = 27;
    int VK_CONTROL = 28;
    int VK_A = 29;
    int VK_S = 30;
    int VK_D = 31;
    int VK_F = 32;
    int VK_G = 33;
    int VK_H = 34;
    int VK_J = 35;
    int VK_K = 36;
    int VK_L = 37;
    int VK_SEMICOLON = 38;
    int VK_AT = 39;
    int VK_QUOTE = 39;
    int VK_HASH = 40;
    int VK_TILDE = 40;
    int VK_SHIFT = 41;
    int VK_BACK_SLASH = 42;
    int VK_Z = 43;
    int VK_X = 44;
    int VK_C = 45;
    int VK_V = 46;
    int VK_B = 47;
    int VK_N = 48;
    int VK_M = 49;
    int VK_COMMA = 50;
    int VK_PERIOD = 51;
    int VK_SLASH = 52;
    int VK_KP_MULTIPLY = 53;
    int VK_ALT = 54;
    int VK_SPACE = 55;
    int VK_CAPS_LOCK = 56;
    int VK_F1 = 57;
    int VK_F2 = 58;
    int VK_F3 = 59;
    int VK_F4 = 60;
    int VK_F5 = 61;
    int VK_F6 = 62;
    int VK_F7 = 63;
    int VK_F8 = 64;
    int VK_F9 = 65;
    int VK_F10 = 66;
    int VK_NUM_LOCK = 67;
    int VK_SCROLL_LOCK = 68;
    int VK_KP_7 = 69;
    int VK_KP_8 = 70;
    int VK_KP_9 = 71;
    int VK_KP_SUBTRACT = 72;
    int VK_KP_4 = 73;
    int VK_KP_5 = 74;
    int VK_KP_6 = 75;
    int VK_KP_ADD = 76;
    int VK_KP_1 = 77;
    int VK_KP_2 = 78;
    int VK_KP_3 = 79;
    int VK_KP_0 = 80;
    int VK_KP_PERIOD = 81;
    int VK_F11 = 82;
    int VK_F12 = 83;
    int VK_KP_ENTER = 84;
    int VK_KP_DIVIDE = 85;
    int VK_ALTGR = 86;
    int VK_BREAK = 87;
    int VK_UP = 88;
    int VK_LEFT = 89;
    int VK_RIGHT = 90;
    int VK_DOWN = 91;
    int VK_INSERT = 92;

    int KEYS = 93;

    // -- Window management functions --

    /**
     * Returns true if the display is still visible.
     */
    boolean stillRunning();

    /**
     * Gets the current width.
     * This value should only change after flush calls.
     */
    int getWidth();

    /**
     * Gets the current height.
     * This value should only change after flush calls.
     */
    int getHeight();

    /**
     * This will eventually show the backbuffer on the screen.
     * It used to be that this notified you with a boolean when the backbuffer was lost.
     * This has changed in preparation for BadGPU, because we need to get clever about buffer management now.
     */
    void flush(IImage backBuffer);

    /**
     * Closes the window.
     */
    void shutdown();

    // Gets an IPeripherals object with a zero offset (if relevant) suitable for passing to UI.

    IPeripherals getPeripherals();

    // Estimates UI scale (for DPI support)
    int estimateUIScaleTenths();

    /**
     * Helper to maintain backbuffer width/height.
     * Otherwise, shuts down the passed-in backbuffer and returns a new one.
     */
    default @NonNull IGrDriver ensureBackBuffer(@Nullable IGrDriver backBuffer) {
        return ensureBackBuffer(backBuffer, 1);
    }

    /**
     * Helper to maintain backbuffer width/height.
     * Otherwise, shuts down the passed-in backbuffer and returns a new one.
     */
    default @NonNull IGrDriver ensureBackBuffer(@Nullable IGrDriver backBuffer, int supersampling) {
        int w = getWidth() * supersampling;
        int h = getHeight() * supersampling;
        if (backBuffer != null) {
            if (backBuffer.getWidth() == w && backBuffer.getHeight() == h)
                return backBuffer;
            backBuffer.shutdown();
        }
        return GaBIEn.makeOffscreenBuffer(w, h, "BackBuffer:" + this);
    }
}
