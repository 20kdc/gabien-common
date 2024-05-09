/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

/**
 * Readable memory-like device with strong guarantees to proxies.
 * Created 9th May 2024 because yet again Java 8 has something missing I need to implement to make R48 work
 */
public abstract class MemoryishR {
    /**
     * This has to be here because there's no good way to mixin it without potentially deoptimizing the final methods.
     * If it's not relevant to your usecase, ignore it!
     */
    public final long length;

    public MemoryishR(long l) {
        length = l;
    }

    // -- 8 --

    /**
     * Reads either go through this or copyAsByteArray.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract byte getS8(long at);

    /**
     * Reads either go through this or getS8.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public abstract void getBulk(long at, byte[] data, int offset, int length);

    /**
     * Copies out some data to a separate byte array.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final byte[] getBulk(long at, int length) {
        byte[] data = new byte[length];
        getBulk(at, data, 0, length);
        return data;
    }

    /**
     * Gets an unsigned byte.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final int getU8(long at) {
        return getS8(at) & 0xFF;
    }

    // -- 16 --

    /**
     * Gets an unsigned 16-bit little-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final int getU16LE(long at) {
        return getS16LE(at) & 0xFFFF;
    }

    /**
     * Gets a signed 16-bit little-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final short getS16LE(long at) {
        return (short) ((getU8(at + 1) << 8) | getU8(at));
    }

    /**
     * Gets an unsigned 16-bit big-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final int getU16BE(long at) {
        return getS16BE(at) & 0xFFFF;
    }

    /**
     * Gets a signed 16-bit big-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final short getS16BE(long at) {
        return (short) ((getU8(at) << 8) | getU8(at + 1));
    }

    // -- 32 --

    /**
     * Gets an unsigned 32-bit little-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final long getU32LE(long at) {
        return getS32LE(at) & 0xFFFFFFFFL;
    }

    /**
     * Gets a signed 32-bit little-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final int getS32LE(long at) {
        return (getU16LE(at + 2) << 16) | getU16LE(at);
    }

    /**
     * Gets an unsigned 32-bit big-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final long getU32BE(long at) {
        return getS32BE(at) & 0xFFFFFFFFL;
    }

    /**
     * Gets a signed 32-bit big-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final int getS32BE(long at) {
        return (getU16BE(at) << 16) | getU16BE(at + 2);
    }

    // -- 64 --

    /**
     * Gets a signed 64-bit little-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final long getS64LE(long at) {
        return (getU32LE(at + 4) << 32) | getU32LE(at);
    }

    /**
     * Gets a signed 64-bit big-endian value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final long getS64BE(long at) {
        return (getU32BE(at) << 32) | getU32BE(at + 4);
    }

    // -- FP --

    /**
     * Gets a 32-bit little-endian floating-point value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final float getF32LE(long at) {
        return Float.intBitsToFloat(getS32LE(at));
    }

    /**
     * Gets a 32-bit big-endian floating-point value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final float getF32BE(long at) {
        return Float.intBitsToFloat(getS32BE(at));
    }

    /**
     * Gets a 64-bit little-endian floating-point value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final double getF64LE(long at) {
        return Double.longBitsToDouble(getS64LE(at));
    }

    /**
     * Gets a 64-bit big-endian floating-point value from the given location.
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public final double getF64BE(long at) {
        return Double.longBitsToDouble(getS64BE(at));
    }
}
