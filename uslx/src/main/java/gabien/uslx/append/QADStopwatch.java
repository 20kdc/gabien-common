/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.append;

import org.eclipse.jdt.annotation.NonNull;

/**
 * TimeLogger is too much effort for profiling that doesn't need to be deep.
 * Created 20th October, 2023.
 */
public final class QADStopwatch implements AutoCloseable {
    public final long startTimeMillis;
    public final String name;

    public QADStopwatch(@NonNull String name) {
        this.name = name;
        startTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void close() {
        long ms = System.currentTimeMillis() - startTimeMillis;
        System.out.println(name + ": " + ms);
    }
}
