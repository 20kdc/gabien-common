/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package gabien;

/**
 * A graphics and input driver.The idea is to speed up work on ports.
 */
public interface IGrInDriver {
    // keys! I'm switching the keymap to some custom thing
    // so Oracle can't hold a sword over my head
    // just because I use the same keymap as them.
    public static int VK_ESCAPE = 0;
    public static int VK_1 = 1;
    public static int VK_2 = 2;
    public static int VK_3 = 3;
    public static int VK_4 = 4;
    public static int VK_5 = 5;
    public static int VK_6 = 6;
    public static int VK_7 = 7;
    public static int VK_8 = 8;
    public static int VK_9 = 9;
    public static int VK_0 = 10;
    public static int VK_MINUS = 11;
    public static int VK_EQUALS = 12;
    public static int VK_BACK_SPACE = 13;
    public static int VK_TAB = 14;
    public static int VK_Q = 15;
    public static int VK_W = 16;
    public static int VK_E = 17;
    public static int VK_R = 18;
    public static int VK_T = 19;
    public static int VK_Y = 20;
    public static int VK_U = 21;
    public static int VK_I = 22;
    public static int VK_O = 23;
    public static int VK_P = 24;
    public static int VK_OPEN_BRACKET = 25;
    public static int VK_CLOSE_BRACKET = 26;
    public static int VK_ENTER = 27;
    public static int VK_CONTROL = 28;
    public static int VK_A = 29;
    public static int VK_S = 30;
    public static int VK_D = 31;
    public static int VK_F = 32;
    public static int VK_G = 33;
    public static int VK_H = 34;
    public static int VK_J = 35;
    public static int VK_K = 36;
    public static int VK_L = 37;
    public static int VK_SEMICOLON = 38;
    public static int VK_AT = 39;
    public static int VK_QUOTE = 39;
    public static int VK_HASH = 40;
    public static int VK_TILDE = 40;
    public static int VK_SHIFT = 41;
    public static int VK_BACK_SLASH = 42;
    public static int VK_Z = 43;
    public static int VK_X = 44;
    public static int VK_C = 45;
    public static int VK_V = 46;
    public static int VK_B = 47;
    public static int VK_N = 48;
    public static int VK_M = 49;
    public static int VK_COMMA = 50;
    public static int VK_PERIOD = 51;
    public static int VK_SLASH = 52;
    public static int VK_KP_MULTIPLY = 53;
    public static int VK_ALT = 54;
    public static int VK_SPACE = 55;
    public static int VK_CAPS_LOCK = 56;
    public static int VK_F1 = 57;
    public static int VK_F2 = 58;
    public static int VK_F3 = 59;
    public static int VK_F4 = 60;
    public static int VK_F5 = 61;
    public static int VK_F6 = 62;
    public static int VK_F7 = 63;
    public static int VK_F8 = 64;
    public static int VK_F9 = 65;
    public static int VK_F10 = 66;
    public static int VK_NUM_LOCK = 67;
    public static int VK_SCROLL_LOCK = 68;
    public static int VK_KP_7 = 69;
    public static int VK_KP_8 = 70;
    public static int VK_KP_9 = 71;
    public static int VK_KP_SUBTRACT = 72;
    public static int VK_KP_4 = 73;
    public static int VK_KP_5 = 74;
    public static int VK_KP_6 = 75;
    public static int VK_KP_ADD = 76;
    public static int VK_KP_1 = 77;
    public static int VK_KP_2 = 78;
    public static int VK_KP_3 = 79;
    public static int VK_KP_0 = 80;
    public static int VK_KP_PERIOD = 81;
    public static int VK_F11 = 82;
    public static int VK_F12 = 83;
    public static int VK_KP_ENTER = 84;
    public static int VK_KP_DIVIDE = 85;
    public static int VK_ALTGR = 86;
    public static int VK_BREAK = 87;
    public static int VK_UP = 88;
    public static int VK_LEFT = 89;
    public static int VK_RIGHT = 90;
    public static int VK_DOWN = 91;
    public static int VK_INSERT = 92;

    public static int KEYS = 93;

    // BASIC
    public static interface IImage {
        public int getWidth();
        public int getHeight();
        // 0xAARRGGBB
        public int[] getPixels();
    }

    public boolean stillRunning();

    public int getWidth();

    public int getHeight();
    
    public void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i);
    // Support optional but recommended. Lack of support should result in a RuntimeException.
    public void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i);

    // Now as official as you can get for a graphics interface nobody uses.
    // This is "The Way" that text is drawn if the "styled" way doesn't work.
    // The UI package uses this in case international text comes along.
    // Now, this still isn't going to be consistent.
    // It probably never will be.
    // But it will work, and it means I avoid having to include Unifont.
    public void drawText(int x, int y, int r, int g, int b, int i, String text);

    // 'but colour key'
    public void blitBCKImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i);

    public void clearAll(int i, int i0, int i1);

    public void clearRect(int r, int g, int b, int x, int y, int width, int height);

    public void flush();// This copies the IsKeyDown buffer to WasKeyDown.
    // INPUT-BASIC

    public boolean isKeyDown(int KEYID);

    public boolean isKeyJustPressed(int KEYID);// Note that if this returns
                                               // true,it won't return true
                                               // again for any other call for
                                               // that key until another press
                                               // happens.

    public void clearKeys();// This will clear all key-related buffers.
                            // This includes the Typist extension's
                            // keybuffer,and the mouse.
    // INPUT-ADVANCED
    // Note that these are only guaranteed to apply when the mouse is down (on touch devices)

    public int getMouseX();

    public int getMouseY();

    public boolean getMouseDown();

    public boolean getMouseJustDown();// Note that if this returns true,it won't
                                      // return true again for any other call
                                      // until another press happens.

    public int getMouseButton();// Get the Mouse button currently pressed.
                                // 0:None 1:Left,3:Right
    // if it's completely impossible to provide 320x240,or the player requests
    // it,put the true values in and hope it works.
    // INPUT-TYPIST
    // Typist allows the use of Typed events.
    // Thus,it's good to implement it.

    // This can show a keyboard covering the lower half of the screen or less.
    // If one is already up,don't show it again.

    public void setTypeBuffer(String s);

    public String getTypeBuffer();

    public void shutdown();
}
