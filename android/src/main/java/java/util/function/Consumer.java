/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package java.util.function;

/**
 * replacement for 1.8 stuff Created on 22/04/16.
 * Turned into Java 8 polyfill 17th October 2023.
 */
@FunctionalInterface
public interface Consumer<T> {
    /**
     * The actual consumer itself: receives an object.
     */
    void accept(T t);

    /**
     * Chains Consumers.
     * You usually never want to call this API.
     */
    default Consumer<T> andThen(Consumer<? super T> next) {
        return (v) -> {
            this.accept(v);
            next.accept(v);
        };
    }
}
