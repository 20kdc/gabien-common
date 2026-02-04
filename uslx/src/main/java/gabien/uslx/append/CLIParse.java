/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Handles simple command-line parsing.
 * Will automatically System.exit(1) if a parsing error occurs.
 * Created 4th February, 2026.
 */
public class CLIParse {
    /**
     * Not to be called
     */
    @SuppressWarnings("unused")
    private CLIParse() {
    }

    /**
     * Handles simple command-line parsing.
     * Will automatically System.exit(1) if a parsing error occurs.
     * Switches and parameters should be given in their 'real form', i.e. "-v" or "-o" or "--output"
     * Created 4th February, 2026.
     */
    public static String[] cliParse(@NonNull String appName, @NonNull String[] args, @Nullable Map<String, Runnable> switchesO, @Nullable Map<String, Consumer<String>> parametersO) {
        Map<String, Runnable> switches = switchesO;
        if (switches == null)
            switches = Collections.emptyMap();

        Map<String, Consumer<String>> parameters = parametersO;
        if (parameters == null)
            parameters = Collections.emptyMap();

        LinkedList<String> remainder = new LinkedList<>();
        boolean dashdash = false;
        for (int i = 0; i < args.length; i++) {
            if (!dashdash) {
                if (args[i].equals("--")) {
                    dashdash = true;
                    continue;
                }
                if (args[i].startsWith("-")) {
                    Consumer<String> isParam = parameters.get(args[i]);
                    Runnable isSwitch = switches.get(args[i]);
                    if (isParam != null) {
                        if (i >= (args.length - 1)) {
                            System.err.println(appName + ": " + args[i] + " requires parameter");
                            System.exit(1);
                        }
                        i++;
                        isParam.accept(args[i]);
                    } else if (isSwitch != null) {
                        isSwitch.run();
                    } else {
                        // not obvious. is this an invalid long switch/param?
                        if (args[i].startsWith("--")) {
                            System.err.println(appName + ": " + args[i] + " invalid");
                            System.exit(1);
                        }
                        // ok, no, so it's a series of switches, right?
                        for (int j = 1; j < args[i].length(); j++) {
                            char chr = args[i].charAt(j);
                            String resSwitch = "-" + chr;
                            isSwitch = switches.get(resSwitch);
                            if (isSwitch == null) {
                                System.err.println(appName + ": " + args[i] + " contains invalid switch " + resSwitch);
                                System.exit(1);
                                break;
                            }
                            isSwitch.run();
                        }
                    }
                    continue;
                }
            }
            remainder.add(args[i]);
        }
        return remainder.toArray(new String[0]);
    }
}
