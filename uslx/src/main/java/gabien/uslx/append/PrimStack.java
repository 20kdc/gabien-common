/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

/**
 * Implements efficient push/pop operations on an array stack.
 * Intended to reduce GC thrashing as much as possible.
 * Created 14th May, 2024.
 */
public abstract class PrimStack {
    /**
     * Size of array.
     */
    protected int size;
    /**
     * Index next value will be pushed to.
     */
    protected int sp;
    /**
     * As high as SP has gone this cycle.
     */
    private int watermark;

    private PrimStack() {
    }

    /**
     * Resize.
     */
    abstract void resize(int size);

    /**
     * Frees up any space not used this cycle.
     */
    public void cycle() {
        resize(watermark);
        watermark = sp;
    }

    /**
     * Clears the stack.
     */
    public void clear() {
        sp = 0;
        resize(0);
    }

    /**
     * Gets the index to push to.
     */
    protected int getPushIndex(int amount) {
        int index = sp;
        sp += amount;
        if (sp > size)
            resize(sp);
        watermark = Math.max(sp, watermark);
        return index;
    }

    public static class I8 extends PrimStack {
        private byte[] data = new byte[0];
        @Override
        void resize(int size) {
            byte[] nv = new byte[size];
            System.arraycopy(data, 0, nv, 0, Math.min(data.length, size));
            data = nv;
            this.size = size;
        }
        public byte pop() {
            return data[--sp];
        }
        public void pop(byte[] a, int offset, int length) {
            sp -= length;
            System.arraycopy(data, sp, a, offset, length);
        }
        public void push(byte v) {
            int idx = getPushIndex(1);
            data[idx] = v;
        }
        public void push(byte[] a, int offset, int length) {
            int idx = getPushIndex(length);
            System.arraycopy(a, offset, data, idx, length);
        }
    }

    public static class I16 extends PrimStack {
        private short[] data = new short[0];
        @Override
        void resize(int size) {
            short[] nv = new short[size];
            System.arraycopy(data, 0, nv, 0, Math.min(data.length, size));
            data = nv;
            this.size = size;
        }
        public short pop() {
            return data[--sp];
        }
        public void pop(short[] a, int offset, int length) {
            sp -= length;
            System.arraycopy(data, sp, a, offset, length);
        }
        public void push(short v) {
            int idx = getPushIndex(1);
            data[idx] = v;
        }
        public void push(short[] a, int offset, int length) {
            int idx = getPushIndex(length);
            System.arraycopy(a, offset, data, idx, length);
        }
    }

    public static class U16 extends PrimStack {
        private char[] data = new char[0];
        @Override
        void resize(int size) {
            char[] nv = new char[size];
            System.arraycopy(data, 0, nv, 0, Math.min(data.length, size));
            data = nv;
            this.size = size;
        }
        public char pop() {
            return data[--sp];
        }
        public void pop(char[] a, int offset, int length) {
            sp -= length;
            System.arraycopy(data, sp, a, offset, length);
        }
        public void push(char v) {
            int idx = getPushIndex(1);
            data[idx] = v;
        }
        public void push(char[] a, int offset, int length) {
            int idx = getPushIndex(length);
            System.arraycopy(a, offset, data, idx, length);
        }
    }

    public static class I32 extends PrimStack {
        private int[] data = new int[0];
        @Override
        void resize(int size) {
            int[] nv = new int[size];
            System.arraycopy(data, 0, nv, 0, Math.min(data.length, size));
            data = nv;
            this.size = size;
        }
        public int pop() {
            return data[--sp];
        }
        public void pop(int[] a, int offset, int length) {
            sp -= length;
            System.arraycopy(data, sp, a, offset, length);
        }
        public void push(int v) {
            int idx = getPushIndex(1);
            data[idx] = v;
        }
        public void push(int[] a, int offset, int length) {
            int idx = getPushIndex(length);
            System.arraycopy(a, offset, data, idx, length);
        }
    }

    public static class I64 extends PrimStack {
        private long[] data = new long[0];
        @Override
        void resize(int size) {
            long[] nv = new long[size];
            System.arraycopy(data, 0, nv, 0, Math.min(data.length, size));
            data = nv;
            this.size = size;
        }
        public long pop() {
            return data[--sp];
        }
        public void pop(long[] a, int offset, int length) {
            sp -= length;
            System.arraycopy(data, sp, a, offset, length);
        }
        public void push(long v) {
            int idx = getPushIndex(1);
            data[idx] = v;
        }
        public void push(long[] a, int offset, int length) {
            int idx = getPushIndex(length);
            System.arraycopy(a, offset, data, idx, length);
        }
    }

    public static class F32 extends PrimStack {
        private float[] data = new float[0];
        @Override
        void resize(int size) {
            float[] nv = new float[size];
            System.arraycopy(data, 0, nv, 0, Math.min(data.length, size));
            data = nv;
            this.size = size;
        }
        public float pop() {
            return data[--sp];
        }
        public void pop(float[] a, int offset, int length) {
            sp -= length;
            System.arraycopy(data, sp, a, offset, length);
        }
        public void push(float v) {
            int idx = getPushIndex(1);
            data[idx] = v;
        }
        public void push(float[] a, int offset, int length) {
            int idx = getPushIndex(length);
            System.arraycopy(a, offset, data, idx, length);
        }
    }

    public static class F64 extends PrimStack {
        private double[] data = new double[0];
        @Override
        void resize(int size) {
            double[] nv = new double[size];
            System.arraycopy(data, 0, nv, 0, Math.min(data.length, size));
            data = nv;
            this.size = size;
        }
        public double pop() {
            return data[--sp];
        }
        public void pop(double[] a, int offset, int length) {
            sp -= length;
            System.arraycopy(data, sp, a, offset, length);
        }
        public void push(double v) {
            int idx = getPushIndex(1);
            data[idx] = v;
        }
        public void push(double[] a, int offset, int length) {
            int idx = getPushIndex(length);
            System.arraycopy(a, offset, data, idx, length);
        }
    }
}
