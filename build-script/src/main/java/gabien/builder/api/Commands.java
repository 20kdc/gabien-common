/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

import java.io.File;
import java.util.Locale;

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

        // so this uses some special rules, preferring JAVA_1_8_HOME for javac but other stuff for java
        // the basic assumption is that later Java will remain runtime-compatible but will lose compilation support
        String javaHome = System.getenv("MICROMVN_JAVA_HOME");
        String javacHome = System.getenv("JAVA_1_8_HOME");

        if (javacHome == null)
            javacHome = System.getenv("MICROMVN_JAVA_HOME");

        if (javaHome == null)
            javaHome = System.getenv("JAVA_HOME");
        if (javacHome == null)
            javacHome = System.getenv("JAVA_HOME");

        if (javaHome != null) {
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

        if (javacHome != null) {
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
}
