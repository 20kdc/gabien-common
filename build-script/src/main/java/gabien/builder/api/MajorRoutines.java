/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.builder.api;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Created February 18th, 2025.
 */
public class MajorRoutines {
    private MajorRoutines() {
    }

    public static void ready(CommandEnv diag) {
        CommandEnv inHome = diag.cd(CommandEnv.GABIEN_HOME);
        inHome.info("Readying engine...");
        inHome.run(CommandEnv.UMVN_COMMAND, "test", "-q");
        inHome.run(CommandEnv.UMVN_COMMAND, "package-only", "-q");
        inHome.run(CommandEnv.UMVN_COMMAND, "install-only", "-q");
    }

    /**
     * Be CAREFUL with this! (copied from umvn)
     */
    public static void recursivelyDelete(File sourceTargetDir) {
        try {
            sourceTargetDir = sourceTargetDir.getCanonicalFile();
            if (sourceTargetDir.getParentFile() == null)
                throw new RuntimeException("The requested operation would destroy an entire drive, which is generally considered a bad move.");
            Path p = sourceTargetDir.toPath();
            if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                for (File sub : sourceTargetDir.listFiles()) {
                    if (!sub.getCanonicalFile().getParentFile().equals(sourceTargetDir))
                        throw new RuntimeException("Will not recursively delete, weird structure");
                    recursivelyDelete(sub);
                }
            }
            sourceTargetDir.delete();
        } catch (Exception ex) {
            throw new RuntimeException("during recursive delete @ " + sourceTargetDir);
        }
    }
}
