/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.wsi;

/**
 * The continued "here goes nothing"ism.
 * This is massively subject to change until the maintain() guts go away.
 * Created 28th July, 2022.
 */
public interface ITextEditingSession {
    /**
     * Called to update the textbox.
     * The Y centre is the *centre* - the textbox will be as tall as it wants to be.
     * Note that the textbox is still hooked into key events, so make sure not to respond to anything that could ever be used in normal typing.
     */
    String maintain(int x, int y, int w, int h);

    /**
     * Updates the text in the textbox.
     */
    void setText(String text);

    /**
     * If true, "Enter has been pressed" (the user wants to leave the session and accept the changes).
     */
    boolean isEnterJustPressed();

    /**
     * If true, the session is dead.
     */
    boolean isSessionDead();

    /**
     * Ends the session from the game side (i.e. if the box got deselected).
     * No-op if session is already dead.
     */
    void endSession();
}
