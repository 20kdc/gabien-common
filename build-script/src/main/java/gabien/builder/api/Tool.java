/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

/**
 * Defines a tool.
 * The options to this tool are typically defined by annotations on fields.
 * Created 17th February, 2025.
 */
public abstract class Tool extends ParamSet {
    public final String name;
    public final String description;
    public Tool(String n, String d) {
        name = n;
        description = d;
    }

    public abstract void run(Diagnostics diag);
}
