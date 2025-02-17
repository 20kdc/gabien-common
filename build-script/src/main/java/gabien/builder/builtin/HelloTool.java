/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import gabien.builder.api.ToolEnvironment;
import gabien.builder.api.Tool;
import gabien.builder.api.ToolParam;

/**
 * A test of the tool infrastructure.
 * Created 17th February, 2025.
 */
public class HelloTool extends Tool {
    @ToolParam(name = "--message", desc = "The message to show to the user.", optional = true, valueMeaning = "<TEXT>")
    public String message = "HELLO.";

    public HelloTool() {
        super("hello", "Shows a message to the user.");
    }

    @Override
    public void run(ToolEnvironment diag) {
        diag.info(message);
    }
}
