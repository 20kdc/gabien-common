/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.builder.api;

/**
 * Created February 18th, 2025.
 */
public class MajorRoutines {
    private MajorRoutines() {
    }

    public static void ready(ToolEnvironment diag) {
        diag.info("Readying engine...");
        Commands.run(diag, Commands.GABIEN_HOME, Commands.UMVN_COMMAND, "test", "-q");
        if (diag.hasAnyErrorOccurred())
            return;
        Commands.run(diag, Commands.GABIEN_HOME, Commands.UMVN_COMMAND, "package-only", "-q");
        if (diag.hasAnyErrorOccurred())
            return;
        Commands.run(diag, Commands.GABIEN_HOME, Commands.UMVN_COMMAND, "install-only", "-q");
    }
}
