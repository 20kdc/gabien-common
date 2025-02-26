/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import gabien.builder.api.ToolEnvironment;
import gabien.builder.api.CommandEnv;
import gabien.builder.api.MajorRoutines;
import gabien.builder.api.Tool;

/**
 * Readies the engine.
 * Created 18th February, 2025.
 */
public class ReadyTool extends Tool {
    public ReadyTool() {
        super("ready", "Readies the engine.");
    }

    @Override
    public void run(ToolEnvironment diag) {
        MajorRoutines.ready(new CommandEnv(diag));
    }
}
