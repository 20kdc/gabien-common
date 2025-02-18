/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * Created 17th February 2025.
 */
public final class Commands {
    public static final String EXE_SUFFIX;
    public static final String JAVA_COMMAND;
    public static final String JAVAC_COMMAND;
    public static final String UMVN_COMMAND = "umvn";

    static {
        EXE_SUFFIX = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT).startsWith("windows") ? ".exe" : "";

        Map<String, String> env = System.getenv();

        // so this uses some special rules, preferring JAVA_1_8_HOME for javac but other stuff for java
        // the basic assumption is that later Java will remain runtime-compatible but will lose compilation support
        String javaHome = env.getOrDefault("MICROMVN_JAVA_HOME", "");
        String javacHome = env.getOrDefault("JAVA_1_8_HOME", "");

        if (javacHome.equals(""))
            javacHome = env.getOrDefault("MICROMVN_JAVA_HOME", "");

        if (javaHome.equals(""))
            javaHome = env.getOrDefault("JAVA_HOME", "");
        if (javacHome.equals(""))
            javacHome = env.getOrDefault("JAVA_HOME", "");

        if (!javaHome.equals("")) {
            if (!javaHome.endsWith(File.separator))
                javaHome += File.separator;
            JAVA_COMMAND = javaHome + "bin" + File.separator + "java" + EXE_SUFFIX;
        } else {
            File f = new File(System.getProperty("java.home"));
            File expectedTool = new File(f, "bin" + File.separator + "java" + EXE_SUFFIX);
            // validate
            if (expectedTool.exists()) {
                JAVA_COMMAND = expectedTool.toString();
            } else {
                // we could have a problem here. fall back to PATH
                JAVA_COMMAND = "java";
            }
        }

        if (!javacHome.equals("")) {
            if (!javacHome.endsWith(File.separator))
                javacHome += File.separator;
            JAVAC_COMMAND = javacHome + "bin" + File.separator + "javac" + EXE_SUFFIX;
        } else {
            File f = new File(System.getProperty("java.home"));
            if (f.getName().equals("jre"))
                f = f.getParentFile();
            File expectedTool = new File(f, "bin" + File.separator + "javac" + EXE_SUFFIX);
            // validate
            if (expectedTool.exists()) {
                JAVAC_COMMAND = expectedTool.toString();
            } else {
                // we could have a problem here. fall back to PATH
                JAVAC_COMMAND = "javac";
            }
        }
    }

    private Commands() {
    }

    /**
     * Runs a command. Exceptions are converted to ToolEnvironment errors.
     */
    public static void run(ToolEnvironment env, String pwd, String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.inheritIO();
            Process px = pb.start();
            if (px.waitFor() != 0)
                env.error("Subprocess returned error code");
        } catch (Exception ex) {
            env.error("Error: " + ex);
            ex.printStackTrace();
        }
    }
}
