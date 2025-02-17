/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder;

import java.util.HashMap;
import java.util.TreeSet;

import gabien.builder.api.Diagnostics;
import gabien.builder.api.Tool;
import gabien.builder.api.ToolModule;
import gabien.builder.api.ToolRegistry;
import gabien.builder.builtin.HelpTool;

/**
 * Entrypoint for gabien-do. Do not touch from the buildscripts themselves. Please.
 * Created 17th February 2025.
 */
public class Main {
    public static void main(String[] args) {
        // buildscript first so it gets priority on names
        String[] registrarPaths = {"buildscript.Index", "gabien.builder.builtin.Index"};
        HashMap<String, Tool> instances = new HashMap<>();
        TreeSet<String> names = new TreeSet<>();
        for (String s : registrarPaths) {
            ToolModule module = null;
            try {
                module = (ToolModule) Class.forName(s).newInstance();
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
            if (module != null)
                module.register(new ToolRegistryImpl(module.getNamespace(), instances, names));
        }
        String tool = "help";
        if (args.length > 0)
            tool = args[0];
        for (String s : names)
            HelpTool.TOOLS.put(s, instances.get(s));
        DiagnosticsImpl diag = new DiagnosticsImpl();
        Tool t = instances.get(tool);
        if (t == null) {
            diag.error("No such tool '" + tool + "'. Try 'help'.");
        } else {
            try {
                t.parseArgs(args, 1, args.length - 1, diag);
                if (!diag.hasAnyErrorOccurred())
                    t.run(diag);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }
        System.exit(diag.hasAnyErrorOccurred() ? 1 : 0);
    }

    private static class ToolRegistryImpl implements ToolRegistry {
        private final HashMap<String, Tool> referenceInstances;
        private final TreeSet<String> toolNames;
        private final String namespace;

        public ToolRegistryImpl(String namespace, HashMap<String, Tool> referenceInstances, TreeSet<String> toolNames) {
            this.namespace = namespace;
            this.referenceInstances = referenceInstances;
            this.toolNames = toolNames;
        }

        @Override
        public <T extends Tool> void register(Class<T> tool) {
            try {
                Tool t = (Tool) tool.newInstance();
                referenceInstances.put(namespace + ":" + t.name, t);
                if (referenceInstances.putIfAbsent(t.name, t) != null) {
                    toolNames.add(namespace + ":" + t.name);
                } else {
                    toolNames.add(t.name);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static class DiagnosticsImpl implements Diagnostics {
        public volatile boolean error = false;

        @Override
        public void info(String text) {
            System.err.println("[INFO] " + text);
        }

        @Override
        public void warn(String text) {
            System.err.println("[WARN] " + text);
        }

        @Override
        public boolean hasAnyErrorOccurred() {
            return error;
        }

        @Override
        public void error(String text) {
            System.err.println("[ERR] " + text);
            error = true;
        }
    }
}
