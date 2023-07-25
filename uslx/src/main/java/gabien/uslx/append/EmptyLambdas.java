/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

/**
 * Created on 28th July 2022.
 */
public final class EmptyLambdas {
    public static final Runnable emptyRunnable = () -> {};
    private static final IConsumer<?> emptyConsumer = (res) -> {};

    private EmptyLambdas() {
        
    }

    @SuppressWarnings("unchecked")
    public static <T> IConsumer<T> emptyConsumer() {
        return (IConsumer<T>) emptyConsumer;
    }
}
