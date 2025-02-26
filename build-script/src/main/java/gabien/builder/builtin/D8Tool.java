/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import com.android.tools.r8.D8;

import gabien.builder.api.CommandEnv;
import gabien.builder.api.Tool;
import gabien.builder.api.ToolParam;

/**
 * A test of the tool infrastructure.
 * Created 17th February, 2025.
 */
public class D8Tool extends Tool {
    @ToolParam(name = "", desc = "Arguments to pass to D8.", optional = true, valueMeaning = "<ARG>")
    public String[] args = new String[0];

    public D8Tool() {
        super("d8", "Invokes Android D8 4.0.63");
    }

    @Override
    public void run(CommandEnv diag) {
        D8.main(args);
        System.exit(0);
    }
}
