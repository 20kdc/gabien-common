/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created on 6th August 2022.
 */
public final class ArrayIterable<T> implements Iterable<T> {
    private final T[] source;

    public ArrayIterable(T[] array) {
        source = array;
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator<T>(source);
    }

    public static final class ArrayIterator<T> implements Iterator<T> {
        private final T[] source;
        private int index = 0;

        public ArrayIterator(T[] src) {
            source = src;
        }

        @Override
        public boolean hasNext() {
            return index < source.length;
        }

        @Override
        public T next() {
            if (index >= source.length)
                throw new NoSuchElementException("Read off of end of ArrayIterable");
            return source[index++];
        }
    }
}
