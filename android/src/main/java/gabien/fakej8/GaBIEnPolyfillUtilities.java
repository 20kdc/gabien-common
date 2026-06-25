/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.fakej8;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * When Android actually implements a Java functional interface, it takes priority over GaBIEn's fake implementation.
 * Problem: D8's handling of default interface functions does not get the memo.
 * Created 25th June, 2026.
 */
public class GaBIEnPolyfillUtilities {
    public static <V> Consumer<V> andThen(Consumer<V> a, Consumer<? super V> b) {
        return (v) -> {
            a.accept(v);
            b.accept(v);
        };
    }

    public static <T, R, V> Function<T, V> andThen(Function<T, R> function, Function<? super R, ? extends V> next) {
        if (next == null)
            throw new NullPointerException("next");
        return (v) -> next.apply(function.apply(v));
    }
}
