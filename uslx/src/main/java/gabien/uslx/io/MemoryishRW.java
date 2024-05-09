/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

/**
 * Writable memory-like device with strong guarantees to proxies.
 * Created 9th May 2024 because yet again Java 8 has something missing I need to implement to make R48 work
 */
public abstract class MemoryishRW extends MemoryishR {
    public MemoryishRW(long l) {
        super(l);
    }

    // -- 8 --

    /**
     * Writes go through this or setBulk.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract void set8(long at, int v);

    /**
     * Writes go through this or set8.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract void setBulk(long at, byte[] data, int offset, int length);

    // -- 16 --

    /**
     * Sets an unsigned 16-bit little-endian value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void set16LE(long at, int v) {
        set8(at, v);
        set8(at + 1, v >> 8);
    }

    /**
     * Sets an unsigned 16-bit big-endian value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void set16BE(long at, int v) {
        set8(at + 1, v);
        set8(at, v >> 8);
    }

    // -- 32 --

    /**
     * Sets a signed 32-bit little-endian value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void set32LE(long at, int v) {
        set16LE(at, v);
        set16LE(at + 2, v >> 16);
    }

    /**
     * Sets a signed 32-bit big-endian value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void set32BE(long at, int v) {
        set16BE(at + 2, v);
        set16BE(at, v >> 16);
    }

    // -- 64 --

    /**
     * Sets a signed 64-bit little-endian value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void set64LE(long at, long v) {
        set32LE(at, (int) v);
        set32LE(at + 4, (int) (v >> 32));
    }

    /**
     * Sets a signed 64-bit big-endian value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void set64BE(long at, long v) {
        set32BE(at + 4, (int) v);
        set32BE(at, (int) (v >> 32));
    }

    // -- FP --

    /**
     * Sets a 32-bit little-endian floating-point value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void setF32LE(long at, float v) {
        set32LE(at, Float.floatToRawIntBits(v));
    }

    /**
     * Sets a 32-bit big-endian floating-point value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void setF32BE(long at, float v) {
        set32BE(at, Float.floatToRawIntBits(v));
    }

    /**
     * Sets a 64-bit little-endian floating-point value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void setF64LE(long at, double v) {
        set64LE(at, Double.doubleToRawLongBits(v));
    }

    /**
     * Sets a 64-bit big-endian floating-point value at the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final void setF64BE(long at, double v) {
        set64BE(at, Double.doubleToRawLongBits(v));
    }
}
