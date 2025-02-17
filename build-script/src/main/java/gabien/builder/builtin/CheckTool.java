/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import java.util.LinkedList;

import gabien.builder.api.ToolEnvironment;
import gabien.builder.api.PrerequisiteSet;
import gabien.builder.api.Tool;
import gabien.builder.api.ToolSwitch;

/**
 * Checks prerequisites.
 * Created 17th February, 2025.
 */
public class CheckTool extends Tool {
    @ToolSwitch(name = "--detailed", desc = "Show detailed errors.")
    public boolean detailed = false;

    public static LinkedList<PrerequisiteSet> ALL_SETS = new LinkedList<>();

    public CheckTool() {
        super("check", "Checks prerequisites.");
    }

    @Override
    public void run(ToolEnvironment diag) {
        for (PrerequisiteSet set : ALL_SETS) {
            diag.info(set.description + ":");
            for (PrerequisiteSet.Prerequisite pq : set.prerequisites) {
                try {
                    pq.check.run();
                    diag.info(" " + pq.name + " OK");
                } catch (Exception ex) {
                    diag.error(" " + pq.name + ": " + ex.getMessage());
                    if (detailed)
                        ex.printStackTrace();
                }
            }
        }
    }
}
