/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
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
        return new Iterator<T>() {
            private int index = 0;
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
        };
    }
}
