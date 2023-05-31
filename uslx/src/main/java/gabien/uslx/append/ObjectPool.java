/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Object pooling helps reduce GC lag by deliberately putting objects into the "permanent" generation.
 * This seems contradictory at first, but doing this reduces the rate of actual object allocation.
 * This ultimately improves allocation performance and reduces the amount of "world" GCs required.
 *
 * Beware! Object pools do not automatically free their contents, ever.
 *
 * Created 31st May 2023.
 */
public abstract class ObjectPool<T> {
    // Values. All freshly reset objects are at the start, and then there is a sea of null afterwards.
    // The barrier is measured using firstFree.
    // A useful side effect is that objects that were recently used will likely return to use.
    private Object[] values = new Object[0];
    // First free (null) value index.
    // Equals values.length when no slots exist.
    private int firstFree = 0;
    private int inUse = 0;
    private int maxInUse = 0;
    private final int expandChunkSize;

    public ObjectPool(int expandChunkSize) {
        this.expandChunkSize = expandChunkSize;
    }

    /**
     * Returns the current capacity of the pool.
     * The pool is liable to dynamically expand as required.
     */
    public final int getCapacity() {
        return values.length;
    }

    /**
     * Resets the "max in use" tracker to the current amount in use, and returns the value.
     * Checking this every so often can help plan dynamic changes to object pool capacity.
     */
    public final synchronized int getAndResetMaxInUse() {
        int miu = maxInUse;
        maxInUse = inUse;
        return miu;
    }

    /**
     * Sets a specific object capacity.
     * This may actually free objects.
     */
    public final synchronized void setCapacity(int newCap) {
        if (newCap == values.length)
            return;
        Object[] oldValues = values;
        values = new Object[newCap];
        // Reallocate carefully.
        // If we run out of storage, assume that's intended.
        // There's definitely a better algorithm for this.
        firstFree = 0;
        for (Object o : oldValues)
            if (o != null && firstFree != values.length)
                values[firstFree++] = o;
    }

    private synchronized void putIntoList(@NonNull T object) {
        inUse--;
        if (firstFree == values.length) {
            setCapacity(values.length + expandChunkSize);
            // If this fails, we can't store the object in the list.
            if (firstFree == values.length)
                return;
        }
        values[firstFree++] = object;
    }

    /**
     * Creates a new element.
     */
    protected abstract @NonNull T gen();

    /**
     * Resets an element.
     */
    public abstract void reset(@NonNull T element);

    /**
     * Retrieves an element from the pool or generates it.
     */
    @SuppressWarnings("unchecked")
    public synchronized final T get() {
        inUse++;
        if (inUse > maxInUse)
            maxInUse = inUse;
        if (firstFree > 0) {
            firstFree--;
            Object obj = values[firstFree];
            values[firstFree] = null;
            return (T) obj;
        }
        return gen();
    }

    /**
     * Finishes with an element.
     */
    public final void finish(@NonNull T object) {
        // This seems redundant, but it isn't.
        // putIntoList will do weird stuff if null is passed.
        assert object != null;
        reset(object);
        putIntoList(object);
    }
}
