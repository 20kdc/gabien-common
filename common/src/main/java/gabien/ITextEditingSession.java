/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.uslx.append.IFunction;

/**
 * The continued "here goes nothing"ism.
 * This is massively subject to change until the maintain() guts go away.
 * Created 28th July, 2022.
 */
public interface ITextEditingSession {
    // Must be called once every frame to maintain a textbox.
    // Only one can be maintained at a given time.
    // The Y centre is the *centre* - the textbox will be as tall as it wants to be.
    // Note that the textbox is still hooked into key events, so make sure not to respond to anything that could ever be used in normal typing.
    // 'feedback' provides live feedback, and should be null under most circumstances.
    // textHeight is the text height of the textbox content.
    String maintain(int x, int y, int w, int h, String text, int textHeight, IFunction<String, String> feedback);
    boolean isEnterJustPressed();
}
