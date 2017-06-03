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

    // -- Drawing functions --

    public static interface IImage {
        int getWidth();

        int getHeight();

        // 0xAARRGGBB. The buffer is safe to edit, but changes do not propagate back.
        int[] getPixels();
    }

    // Returns true if the display is still visible.
    boolean stillRunning();

    // These return the size of the drawing buffer.
    int getWidth();
    // These return the size of the drawing buffer.
    int getHeight();

    void blitImage(int srcx, int srcy, int srcw, int srch, int x, int y, IImage i);

    // Support optional but recommended. Lack of support should result in a RuntimeException.
    void blitScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, IImage i);

    // Support optional. Should not be used unless absolutely required - cannot be scissored.
    // Lack of support should result in a RuntimeException. When scissoring, this is just directly forwarded - nothing can be done here.
    void blitRotatedScaledImage(int srcx, int srcy, int srcw, int srch, int x, int y, int acw, int ach, int angle, IImage i);

    // Now as official as you can get for a graphics interface nobody uses.
    // This is "The Way" that text is drawn if the "styled" way doesn't work.
    // The UI package uses this in case international text comes along.
    // Now, this still isn't going to be consistent.
    // It probably never will be.
    // But it will work, and it means I avoid having to include Unifont.
    void drawText(int x, int y, int r, int g, int b, int i, String text);

    void clearAll(int i, int i0, int i1);

    void clearRect(int r, int g, int b, int x, int y, int width, int height);

    boolean flush(); // This shows the results of drawing on the screen. Returns true if the drawing buffer was lost.

    void shutdown(); // Immediately end this IGrInDriver.

    // -- Basic Keyboard Input

    boolean isKeyDown(int KEYID);

    boolean isKeyJustPressed(int KEYID);// Note that if this returns
    // true,it won't return true
    // again for any other call for
    // that key until another press
    // happens.

    void clearKeys();// This will clear all key-related buffers.
    // This includes the Typist extension's
    // keybuffer,and the mouse.


    // -- Mouse Input


    // Note that these are only guaranteed to apply when the mouse is down (on touch devices)

    int getMouseX();

    int getMouseY();

    boolean getMouseDown();

    boolean getMouseJustDown();// Note that if this returns true,it won't
    // return true again for any other call
    // until another press happens.

    int getMouseButton();// Get the Mouse button currently pressed.
    // 0:None 1:Left,3:Right
    // if it's completely impossible to provide 320x240,or the player requests
    // it,put the true values in and hope it works.

    // -- Text Editing Support
    // This got completely rewritten, because the last iteration sucked for just about every language but especially those with IMEs.
    // The catch is that now it sucks to *use* textboxes. This was the tradeoff I hoped not to make.
    // Bloody UI frameworks... *sigh*

    // Must be called once every frame to maintain a textbox.
    // Only one can be maintained at a given time.
    // The Y position is the *centre* - the textbox will be as tall as it wants to be.
    // Note that the textbox is still hooked into key events, so make sure not to respond to anything that could ever be used in normal typing.
    String maintain(int x, int y, int width, String text);
}
