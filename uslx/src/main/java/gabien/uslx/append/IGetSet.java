/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Single getter/setter property interface
 * Created 1st December, 2023.
 */
public interface IGetSet<T> extends Supplier<T>, Consumer<T> {

}
