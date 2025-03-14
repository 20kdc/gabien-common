/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

/**
 * Created 17th February, 2025.
 */
public interface ToolRegistry {
    /**
     * Registers a tool class.
     */
    <T extends Tool> void register(Class<T> tool);

    /**
     * Registers a prerequisite set.
     */
    void register(PrerequisiteSet set);

    /**
     * Registers an external dependency
     */
    void register(ExternalJAR extern);
}
