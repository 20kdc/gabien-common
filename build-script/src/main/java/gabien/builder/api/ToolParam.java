/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Created 17th February, 2025.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface ToolParam {
    /**
     * Name. Can be empty, in which case this is a "content arg" (arg not associated with an option).
     * Otherwise should be prefixed with --
     */
    public String name();
    /**
     * Value meaning, displayed in help. Usually surrounded with lt/gt.
     */
    public String valueMeaning();
    /**
     * Description.
     */
    public String desc();
    /**
     * If this parameter is optional or not.
     */
    public boolean optional();
}
