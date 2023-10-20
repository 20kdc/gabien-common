/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

/**
 * Presently part of a system used to pin down complex reader bugs in R48.
 * But in general, this will cover hex encoding as it crops up.
 * Created on 26th December 2022.
 */
public class HexByteEncoding {
    /**
     * Converts the given byte to a hexadecimal string.
     */
    public static String toHexString(int v) {
        v &= 0xFF;
        // Will return either "x" or "xx" as v is bounded to 0-255.
        String res = Integer.toHexString(v);
        if (res.length() == 1)
            return "0" + res;
        return res;
    }

    /**
     * Converts the given list of bytes to a hexadecimal string.
     */
    public static String toHexString(int... vals) {
        StringBuilder sb = new StringBuilder();
        for (int v : vals)
            sb.append(toHexString(v));
        return sb.toString();
    }

    /**
     * Converts the given list of bytes to a hexadecimal string.
     */
    public static String toHexString(byte... vals) {
        StringBuilder sb = new StringBuilder();
        for (byte v : vals)
            sb.append(toHexString(v));
        return sb.toString();
    }
}
