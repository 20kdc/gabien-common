/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A weak/soft synchronized entry set.
 * This is useful for parent/child chains.
 * Created 15th June, 2023.
 */
public final class RefSyncSet<T> implements Iterable<T> {
    private final HashSet<Reference<Holder>> held = new HashSet<Reference<Holder>>();

    private synchronized void fill(Collection<T> c) {
        for (Reference<Holder> rh : held)
            c.add(rh.get().value);
    }

    /**
     * Creates an ArrayList with a snapshot of the contents.
     */
    public synchronized ArrayList<T> toArrayList() {
        ArrayList<T> lst = new ArrayList<>(held.size());
        fill(lst);
        return lst;
    }

    /**
     * Iterates over entries.
     */
    @Override
    public Iterator<T> iterator() {
        return toArrayList().iterator();
    }

    /**
     * Adds a WeakReference entry.
     * Returns a holder, which should be put into the object in question.
     */
    public synchronized Holder addWeak(T object) {
        Holder h = new Holder(object);
        held.add(h.thisRef);
        return h;
    }

    /**
     * Removes an entry.
     */
    public synchronized void remove(Holder object) {
        held.remove(object.thisRef);
    }

    /**
     * This is the "reference holder".
     * The purpose of this object is to get finalized so that references cleanly leave the RefSyncSet.
     */
    public final class Holder {
        public final T value;
        private final Reference<Holder> thisRef;

        Holder(T obj) {
            value = obj;
            thisRef = new WeakReference<RefSyncSet<T>.Holder>(this);
        }

        @Override
        protected void finalize() throws Throwable {
            remove(this);
        }
    }
}
