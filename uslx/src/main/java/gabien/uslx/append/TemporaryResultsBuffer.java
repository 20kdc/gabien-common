/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.uslx.append;

import java.lang.reflect.Array;

/**
 * Used for serious maths.
 * Created on 10th June 2022 as part of project WTFr7
 */
public abstract class TemporaryResultsBuffer<T> {
    private final ThreadLocal<T> threadLocal = new ThreadLocal<T>();

    public T get() {
        T res = threadLocal.get();
        if (res == null) {
            res = make();
            threadLocal.set(res);
        }
        return res;
    }

    protected abstract T make();

    public static abstract class Ar<T> extends TemporaryResultsBuffer<T> {
        public final int length;
        @SuppressWarnings("rawtypes")
        public final Class componentType;

        @SuppressWarnings("rawtypes")
        public Ar(int len, Class c) {
            length = len;
            componentType = c;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected T make() {
            return (T) Array.newInstance(componentType, length);
        }
    }

    public static final class I8 extends Ar<byte[]> {
        public I8(int len) {
            super(len, byte.class);
        }
    }

    public static final class I16 extends Ar<short[]> {
        public I16(int len) {
            super(len, short.class);
        }
    }

    public static final class I32 extends Ar<int[]> {
        public I32(int len) {
            super(len, int.class);
        }
    }

    public static final class I64 extends Ar<long[]> {
        public I64(int len) {
            super(len, long.class);
        }
    }

    public static final class F32 extends Ar<float[]> {
        public F32(int len) {
            super(len, float.class);
        }
    }

    public static final class F64 extends Ar<double[]> {
        public F64(int len) {
            super(len, double.class);
        }
    }
}
