/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Efficient componentized data.
 * This speeds up reads over a HashMap-based approach while keeping the code extensible.
 * This class should be subclassed with a final singleton of a Type subclass (which the type marker points to).
 * Created 8th May, 2024.
 */
public class Entity<TypeMarker extends Entity<?>> {
    private static final Object[] dataEmpty = new Object[0];
    private Object[] data = dataEmpty;

    protected static <T extends Entity<?>> Registrar<T> newRegistrar() {
        return new Registrar<T>();
    }

    /**
     * Gets data of the given type, if present.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull Key<TypeMarker, T> key) {
        if (data.length > key.index)
            return (T) data[key.index];
        return null;
    }

    public <T> void set(@NonNull Key<TypeMarker, T> key, T value) {
        if (data.length <= key.index) {
            Object[] n = new Object[key.index + 1];
            System.arraycopy(data, 0, n, 0, data.length);
            data = n;
        }
        data[key.index] = value;
    }

    /**
     * Creates an entity.
     */
    protected Entity() {
    }

    /**
     * A key into the parent Type.
     * This is expected to be extended for syntactic convenience, i.e.
     * public static class Key (T) extends Entity.Key (MyEntityType, T)
     */
    public abstract static class Key<TypeMarker extends Entity<?>, T> {
        public final int index;
        /**
         * You're expected to override this to point at the relevant singleton.
         */
        public Key(@NonNull Registrar<TypeMarker> t) {
            index = t.nextAllocatedIndex.getAndIncrement();
        }
    }

    /**
     * Registry.
     */
    public static class Registrar<TypeMarker extends Entity<?>> {
        private final AtomicInteger nextAllocatedIndex = new AtomicInteger();
        protected Registrar() {
        }
    }
}
