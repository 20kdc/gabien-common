/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * An object that can be owned by a thread and transferred.
 * Defaults to being owned by the creating thread.
 *
 * Created 30th May 2023 as part of BadGPU.
 */
public class ThreadOwned {
    /**
     * The bound thread.
     * Very important safety rules:
     * The monitor MUST be locked to change boundThread from null to any other value.
     * When boundThread is not null, boundThread may only be changed FROM THAT THREAD.
     */
    private volatile @Nullable Thread boundThread;

    public ThreadOwned() {
        boundThread = Thread.currentThread();
    }

    /**
     * Asserts that the object is bound to this thread.
     * Notably, objects can't be stolen, so if this is fine, it'll stay so.
     * Unless the current thread unbinds it, anyway.
     */
    public final void assertBound() {
        // If the bound thread is this thread, then it can't possibly change.
        // If the bound thread isn't this thread, then it's not going to become this thread.
        Thread sample = boundThread;
        if (sample != Thread.currentThread()) {
            if (sample == null) {
                throw new RuntimeException("A request was made from " + Thread.currentThread() + " while the instance was not bound.");
            } else {
                throw new RuntimeException("A request was made from " + Thread.currentThread() + " while the instance was bound to " + sample + ".");
            }
        }
    }

    /**
     * Custom bind logic can be implemented here for objects with particular thread-binding requirements.
     */
    protected void bindImpl() {
    }

    /**
     * Custom unbind logic can be implemented here for objects with particular thread-binding requirements.
     */
    protected void unbindImpl() {
    }

    /**
     * Binds the object to another thread.
     */
    public final void bind() {
        // This is the only synchronized block needed.
        // Need to do a compare-and-set on thread transfers.
        // Doing this anywhere else would be very inefficient.
        synchronized (this) {
            Thread otherThread = boundThread;
            if (otherThread == null) {
                boundThread = Thread.currentThread();
            } else {
                throw new RuntimeException("Cannot transfer to " + Thread.currentThread() + ", as the object is owned by " + otherThread + ".");
            }
        }
        // Object is now bound to this thread, do the thing.
        bindImpl();
    }

    /**
     * Unbinds the object from this thread.
     */
    public final void unbind() {
        assertBound();
        unbindImpl();
        boundThread = null;
    }

    /**
     * For a reduction in efficiency, manages a ThreadOwned with a lock.
     * This is a wrapper, so that it can be applied to any ThreadOwned.
     * The idea is that you bind/unbind via this.
     */
    public static final class Locked implements Block {
        public final @NonNull ThreadOwned underlying;
        private final @NonNull ReentrantLock lock = new ReentrantLock(true);

        /**
         * Creates the Locked wrapper around a ThreadOwned.
         * The ThreadOwned must be bound, and the lock starts held.
         * That in mind, use code such as `try (ThreadOwned.Locked tmp = instanceLock) {` or call close() later.
         *
         * @param base Base ThreadOwned.
         */
        public Locked(@NonNull ThreadOwned base) {
            base.assertBound();
            underlying = base;
            lock.lock();
        }

        /**
         * Locks the lock and binds the ThreadOwned.
         * The ThreadOwned will only be bound if the lock wasn't previously held by this thread.
         * Therefore, if there are many bind/unbind sequences causing performance issues, a wider lock can fix that.
         */
        public Locked open() {
            boolean actuallyBind = !lock.isHeldByCurrentThread();
            lock.lock();
            if (actuallyBind)
                underlying.bind();
            return this;
        }

        @Override
        public void close() {
            if (lock.getHoldCount() == 1)
                underlying.unbind();
            lock.unlock();
        }
    }
}
