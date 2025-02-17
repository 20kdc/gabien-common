/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import gabien.builder.api.ToolModule;
import gabien.builder.api.ToolRegistry;

/**
 * Created 17th February.
 */
public class Index implements ToolModule {
    @Override
    public String getNamespace() {
        return "gabien";
    }
    @Override
    public void register(ToolRegistry registry) {
        registry.register(HelloTool.class);
        registry.register(HelpTool.class);
        registry.register(D8Tool.class);
    }
}
