/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Efficient componentized data.
 * This speeds up reads over a HashMap-based approach while keeping the code extensible.
 * This class should be subclassed with a final singleton that sets TypeMarker to itself.
 * Created 8th May, 2024.
 */
public class EntityType<TypeMarker> {
    private static final Object[] dataEmpty = new Object[0];
    private final AtomicInteger nextAllocatedIndex = new AtomicInteger();

    /**
     * Creates the EntityType.
     * Remember this is intended to be a singleton.
     */
    protected EntityType() {
    }

    /**
     * Creates a new key for a field of the given type.
     * The idiom here is Key MYKEY = TYPE.key();
     */
    public <T> Key<T> key() {
        return new Key<T>(this);
    }

    /**
     * Creates a new Entity.
     */
    public V entity() {
        return new V();
    }

    /**
     * A key into the parent Type.
     */
    public class Key<T> {
        public final int index;
        private Key(EntityType<TypeMarker> t) {
            index = t.nextAllocatedIndex.getAndIncrement();
        }
    }

    /**
     * An entity of the type.
     */
    public class V {
        private Object[] data = dataEmpty;

        public V() {
        }

        /**
         * Gets data of the given type, if present.
         */
        @SuppressWarnings("unchecked")
        public <T> T get(Key<T> key) {
            if (data.length > key.index)
                return (T) data[key.index];
            return null;
        }

        public <T> void set(Key<T> key, T value) {
            if (data.length <= key.index) {
                Object[] n = new Object[key.index + 1];
                System.arraycopy(data, 0, n, 0, data.length);
                data = n;
            }
            data[key.index] = value;
        }
    }
}
