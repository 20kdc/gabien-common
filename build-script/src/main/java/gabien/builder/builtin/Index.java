/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import java.io.File;
import java.io.InputStream;

import gabien.builder.api.CommandEnv;
import gabien.builder.api.Constants;
import gabien.builder.api.ExternalJAR;
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
        registry.register(DumpArscTool.class);
        registry.register(CheckTool.class);
        registry.register(D8Tool.class);
        registry.register(ReadyTool.class);
        registry.register(InstallExternalTool.class);
        registry.register(new ExternalJAR("natives", Constants.COORDS_NATIVES, Constants.NATIVES_URL, null));
        registry.register(new ExternalJAR("android-platform", Constants.COORDS_ANDROID_PLATFORM, Constants.ANDROID_PLATFORM_URL, Constants.ANDROID_PLATFORM_LICENSE));

        PrerequisiteSet set = new PrerequisiteSet("Core");
        // this probably can't fail as the buildscript is built with this toolchain :(
        set.with("JAVA_1_8_HOME present", PrerequisiteSet.envPrerequisite("JAVA_1_8_HOME", (val) -> {
            return new File(val + "/bin/javac" + CommandEnv.EXE_SUFFIX).exists();
        }, "C:\\Program Files\\ExampleVendor\\jdk1.8", "/usr/lib/jvm/java-8-openjdk-amd64"));
        set.with("java present", () -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(CommandEnv.JAVA_COMMAND, "-version");
                Process px = pb.start();
                if (px.waitFor() != 0)
                    throw new RuntimeException("java returned an error");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        set.with("javac looks right", () -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(CommandEnv.JAVAC_COMMAND, "-version");
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
