/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created 17th February 2025, reworked 26th February 2025.
 */
public final class CommandEnv implements Diagnostics {
    /**
     * This requires all sorts of stupid workarounds.
     */
    public static final boolean IS_WINDOWS = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT).startsWith("windows");
    public static final String EXE_SUFFIX = IS_WINDOWS ? ".exe" : "";
    public static final String CMD_SUFFIX = IS_WINDOWS ? ".cmd" : "";
    public static final File GABIEN_HOME = new File(System.getenv("GABIEN_HOME"));
    /**
     * This can be breaky on the Wine testbed if directly invoked, so invoke it via the fixed path instead.
     */
    public static final String UMVN_COMMAND = new File(GABIEN_HOME, "/micromvn/umvn" + CMD_SUFFIX).toString();
    public static final String JAVA_COMMAND;
    public static final String JAVAC_COMMAND;
    public static final String JARSIGNER_COMMAND;
    // Used to fork the JVM.
    public static final String INCEPT_COMMAND = "gabien-incept" + CMD_SUFFIX;
    // This is TEMPORARY and BAD.
    public static final String AAPT_COMMAND = System.getenv("ANDROID_BT") != null ? (System.getenv("ANDROID_BT") + "/aapt") : "aapt";

    static {
        Map<String, String> env = System.getenv();

        // so this uses some special rules, preferring JAVA_1_8_HOME for javac/jarsigner but other stuff for java
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
            JARSIGNER_COMMAND = javacHome + "bin" + File.separator + "jarsigner" + EXE_SUFFIX;
        } else {
            File f = new File(System.getProperty("java.home"));
            if (f.getName().equals("jre"))
                f = f.getParentFile();
            File expectedTool = new File(f, "bin" + File.separator + "javac" + EXE_SUFFIX);
            // validate
            if (expectedTool.exists()) {
                JAVAC_COMMAND = expectedTool.toString();
                JARSIGNER_COMMAND = new File(f, "bin" + File.separator + "jarsigner" + EXE_SUFFIX).toString();
            } else {
                // we could have a problem here. fall back to PATH
                JAVAC_COMMAND = "javac";
                JARSIGNER_COMMAND = "jarsigner";
            }
        }
    }

    /**
     * Current working directory.
     */
    public final File pwd;

    /**
     * Environment overrides.
     */
    private final Map<String, String> envOverrides;

    /**
     * True diagnostics implementation.
     */
    private final Diagnostics trueDiagImpl;

    /**
     * Please don't call this outside of gabien.builder.Main
     */
    public CommandEnv(Diagnostics diag, File pwd, Map<String, String> envOverrides) {
        this.trueDiagImpl = diag;
        this.pwd = pwd;
        this.envOverrides = envOverrides;
    }

    /**
     * Runs a command. Exceptions are converted to ToolEnvironment errors.
     */
    public void run(String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(pwd);
            pb.environment().putAll(envOverrides);
            pb.inheritIO();
            Process px = pb.start();
            if (px.waitFor() != 0)
                error("Subprocess returned error code");
        } catch (Exception ex) {
            trueDiagImpl.reportAndThrowARRE("Running command", ex);
        }
    }

    /**
     * Runs a command. Failure is mostly ignored, though exceptions are warned about.
     */
    public boolean runOptional(String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(pwd);
            pb.environment().putAll(envOverrides);
            pb.inheritIO();
            Process px = pb.start();
            return px.waitFor() == 0;
        } catch (Exception ex) {
            warn("Could not run optional command: " + ex);
            return false;
        }
    }

    /**
     * CDs via a relative path.
     * Does NOT work with absolute paths! Use the other version for that.
     */
    public CommandEnv cd(String string) {
        return cd(new File(pwd, string));
    }

    /**
     * Creates a clone with the given current directory.
     */
    public CommandEnv cd(File f) {
        return new CommandEnv(trueDiagImpl, f, envOverrides);
    }

    /**
     * Creates a clone with the given env override.
     */
    public CommandEnv env(String key, String value) {
        HashMap<String, String> mod = new HashMap<>(envOverrides);
        mod.put(key, value);
        return new CommandEnv(trueDiagImpl, pwd, mod);
    }

    @Override
    public void error(String text) {
        trueDiagImpl.error(text);
    }

    @Override
    public boolean hasAnyErrorOccurred() {
        return trueDiagImpl.hasAnyErrorOccurred();
    }

    @Override
    public void info(String text) {
        trueDiagImpl.info(text);
    }

    @Override
    public void warn(String text) {
        trueDiagImpl.warn(text);
    }

    @Override
    public CommandEnv warningScope() {
        return new CommandEnv(trueDiagImpl.warningScope(), pwd, envOverrides);
    }
}
