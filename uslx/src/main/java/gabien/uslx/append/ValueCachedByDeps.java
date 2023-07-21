/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.util.Arrays;

import org.eclipse.jdt.annotation.*;

/**
 * Useful for really small situation-specific caches. Not thread-safe.
 * Created 20th July, 2023.
 */
public abstract class ValueCachedByDeps<V> {
    private V cachedValue;
    private Object[] cachedDeps;

    /**
     * Returns true if the change between oldDeps and newDeps requires a rebuild.
     */
    public boolean depChangeRequiresRebuild(@Nullable Object[] oldDeps, @NonNull Object[] newDeps) {
        if (oldDeps == null)
            return true;
        return !Arrays.deepEquals(oldDeps, newDeps);
    }

    /**
     * Checks the dependencies against the cached versions and rebuilds if a change occurs.
     */
    public final V get(Object... deps) {
        if (depChangeRequiresRebuild(cachedDeps, deps)) {
            cachedValue = create(deps);
            cachedDeps = deps;
        }
        return cachedValue;
    }

    /**
     * Actual factory method for the contents.
     */
    protected abstract V create(Object[] deps);

    /**
     * Peek at the contents without "putting in the effort" of a full query.
     * Can thus return null.
     */
    public final V peek() {
        return cachedValue;
    }
}
