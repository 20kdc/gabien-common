/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package java.util.function;

/**
 * Created on 22/04/16.
 * Turned into Java 8 polyfill 17th October 2023.
 */
@FunctionalInterface
public interface Function<T, R> {
    R apply(T a);

    /**
     * Chains together Functions.
     * You usually never want to call this API.
     */
    default <V> Function<T, V> andThen(Function<? super R, ? extends V> next) {
        if (next == null)
            throw new NullPointerException("next");
        return (v) -> next.apply(apply(v));
    }

    /**
     * Equivalent to prev.andThen(this).
     * You usually never want to call this API.
     */
    @SuppressWarnings("unchecked")
    default <V> Function<V, R> compose(Function<? super V, ? extends T> prev) {
        return (Function<V, R>) prev.andThen(this);
    }

    /**
     * Equivalent to simply writing the lambda yourself, but longer.
     * By poking with Groovy, I determined that this returns the same object every time, so let's do that. 
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<T, T> identity() {
        return (Function<T, T>) GaBIEnPolyfillIdentityFunction.I;
    }
}
