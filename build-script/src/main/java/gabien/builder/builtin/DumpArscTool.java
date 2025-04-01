/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import java.io.File;

import com.reandroid.arsc.chunk.TableBlock;

import gabien.builder.api.CommandEnv;
import gabien.builder.api.Tool;
import gabien.builder.api.ToolParam;

/**
 * Dumps Arsc structure.
 * Created 1st April, 2025.
 */
public class DumpArscTool extends Tool {
    @ToolParam(name = "", desc = "The file to load.", optional = false, valueMeaning = "<FILE>")
    public File file;

    public DumpArscTool() {
        super("dump-arsc", "Dumps a .arsc file");
    }

    @Override
    public void run(CommandEnv diag) {
        try {
            TableBlock tb = TableBlock.load(file);
            System.out.println(tb.toJson().toString(4));
        } catch (Exception ex) {
            diag.reportAndThrowARRE("loading TableBlock", ex);
        }
    }
}
