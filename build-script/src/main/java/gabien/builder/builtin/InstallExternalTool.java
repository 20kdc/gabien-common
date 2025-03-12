/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.builtin;

import java.util.HashMap;
import java.util.LinkedList;

import gabien.builder.api.CommandEnv;
import gabien.builder.api.ExternalJAR;
import gabien.builder.api.Tool;
import gabien.builder.api.ToolParam;
import gabien.builder.api.ToolSwitch;

/**
 * Readies the engine.
 * Created 18th February, 2025.
 */
public class InstallExternalTool extends Tool {
    public static final HashMap<String, ExternalJAR> EXTERNAL = new HashMap<>();

    @ToolSwitch(name = "--accept", desc = "Accepts licenses.")
    public boolean licenseAccepted;

    @ToolSwitch(name = "--list", desc = "Does not install dependencies, only lists them.")
    public boolean listOnly;

    @ToolParam(name = "", valueMeaning = "<PACKAGE>", desc = "Dependencies to install. If none are provided, all that are not present are installed.", optional = true)
    public String[] packages = new String[0];

    public InstallExternalTool() {
        super("install", "Downloads and installs an external dependency, or all external dependencies.");
    }

    @Override
    public void run(CommandEnv diag) {
        boolean wekaTaso = false;
        String[] effectivePackages = packages;
        if (effectivePackages.length == 0) {
            effectivePackages = EXTERNAL.keySet().toArray(new String[0]);
            wekaTaso = true;
        }

        boolean foundLicenses = false;
        LinkedList<ExternalJAR> wouldInstall = new LinkedList<>();
        for (String s : effectivePackages) {
            ExternalJAR ex = EXTERNAL.get(s);
            if (ex == null)
                diag.error("Unknown dependency " + s);
            if (ex.license != null) {
                diag.info(ex.id + " - license: " + ex.license);
                diag.info(" " + ex.url);
                foundLicenses = true;
            } else {
                diag.info(ex.id + " - license part of project");
                diag.info(" " + ex.url);
            }
            if (ex.appearsInstalled()) {
                if (wekaTaso) {
                    diag.info(" already installed");
                    continue;
                } else {
                    diag.info(" already installed (explicitly invoked, will reinstall)");
                }
            } else {
                diag.info(" not installed");
            }
            wouldInstall.add(ex);
        }

        if (listOnly || diag.hasAnyErrorOccurred())
            return;

        if (foundLicenses && !licenseAccepted) {
            diag.error("Unaccepted licenses; pass --accept to accept them.");
            return;
        }

        for (ExternalJAR e : wouldInstall) {
            e.install(diag);
        }
    }
}
