/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import java.io.InputStream;

import gabien.builder.api.Commands;
import gabien.builder.api.NativesInstallTester;
import gabien.builder.api.PrerequisiteSet;
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
        registry.register(CheckTool.class);
        registry.register(D8Tool.class);
        
        PrerequisiteSet set = new PrerequisiteSet("Core");
        set.with("JAVA_1_8_HOME present", () -> {
            if (System.getenv("JAVA_1_8_HOME") == null)
                throw new RuntimeException("JAVA_1_8_HOME not set.");
        });
        set.with("java present", () -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(Commands.JAVA_COMMAND, "-version");
                Process px = pb.start();
                if (px.waitFor() != 0)
                    throw new RuntimeException("java returned an error");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        set.with("javac looks right", () -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(Commands.JAVAC_COMMAND, "-version");
                Process px = pb.start();
                String version = "";
                try (InputStream inp = px.getErrorStream()) {
                    while (true) {
                        int c = inp.read();
                        if (c == -1)
                            break;
                        version += (char) c;
                    }
                }
                if (!version.contains(" 1.8"))
                    throw new RuntimeException("javac '" + version + "' does not look like Java 8");
                if (px.waitFor() != 0)
                    throw new RuntimeException("javac returned an error");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        set.with("gabien-natives is installed and correct", NativesInstallTester.PREREQUISITE);
        registry.register(set);
    }
}
