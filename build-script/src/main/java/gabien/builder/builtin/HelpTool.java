/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import gabien.builder.api.Diagnostics;
import gabien.builder.api.Tool;
import gabien.builder.api.ToolParam;

/**
 * Helpful information.
 * Created 17th February, 2025.
 */
public class HelpTool extends Tool {
    public static final HashMap<String, Tool> TOOLS = new HashMap<>();

    @ToolParam(name = "", desc = "Tool to describe", optional = true, valueMeaning = "<NAME>")
    public String toolName = null;

    public HelpTool() {
        super("help", "Writes helpful information to standard output");
    }

    @Override
    public void run(Diagnostics diag) {
        if (toolName != null) {
            if (!TOOLS.containsKey(toolName)) {
                diag.error(toolName + " does not exist.");
            } else {
                displayTool(toolName);
            }
        } else {
            LinkedList<String> namesInOrder = new LinkedList<>(TOOLS.keySet());
            Collections.sort(namesInOrder);
            for (String t : namesInOrder)
                displayTool(t);
        }
    }

    private void displayTool(String t) {
        Tool tool = TOOLS.get(t);
        System.out.println(t + ": " + tool.description);
        HashSet<Param> params = new HashSet<>();
        tool.buildParams(params);
        LinkedList<Param> paramsList = new LinkedList<>(params);
        paramsList.sort((a, b) -> a.name.compareTo(b.name));
        for (Param p : params)
            System.out.println(" " + p.describeInline() + ": " + p.description);
    }
}
