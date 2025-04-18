/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

import java.util.LinkedList;
import java.util.function.Predicate;

/**
 * Defines a prerequisite set to check for.
 * Created 17th February, 2025.
 */
public final class PrerequisiteSet {
    public final String description;
    public final LinkedList<Prerequisite> prerequisites = new LinkedList<>();

    public PrerequisiteSet(String desc) {
        this.description = desc;
    }

    public PrerequisiteSet with(String name, Runnable check) {
        prerequisites.add(new Prerequisite(name, check));
        return this;
    }

    public final class Prerequisite {
        public final String name;
        public final Runnable check;
        public Prerequisite(String name, Runnable r) {
            this.name = name;
            this.check = r;
        }
    }

    public static Runnable envPrerequisite(String env, Predicate<String> checkValue, String exampleValueW, String exampleValueU) {
        String msgDetail = "On Windows, use `setx " + env + "=" + exampleValueW + "`. On other platforms, add a line such as `export " + env + "=\"" + exampleValueU + "\"` to ~/.profile";
        return () -> {
            String eval = System.getenv(env);
            if (eval == null || eval.isEmpty())
                throw new RuntimeException(env + " not set. " + msgDetail);
            if (!checkValue.test(eval))
                throw new RuntimeException(env + " set (to \"" + eval + "\"), but looks incorrect. " + msgDetail);
        };
    }
}
