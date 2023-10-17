/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package java.util.function;

/**
 * Identity function implementation.
 * You should never access or even see this class outside of the java.util.function.Function implementation.
 * Created 17th October 2023.
 */
enum GaBIEnPolyfillIdentityFunction implements Function<Object, Object> {
    I;

    @Override
    public Object apply(Object a) {
        return a;
    }
}
