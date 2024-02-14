/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
    public static String toHexString(byte[] vals, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (; length > 0; length--)
            sb.append(toHexString(vals[offset++]));
        return sb.toString();
    }

    /**
     * Converts the given string of hex data into bytes.
     */
    public static byte[] fromHexString(String string) {
        char[] ca = string.toCharArray();
        return fromHexString(ca, 0, ca.length);
    }

    /**
     * Converts the given list of hex digits into bytes.
     */
    public static byte[] fromHexString(char[] charArray, int offset, int length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            fromHexString(charArray, offset, length, baos);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return baos.toByteArray();
    }

    /**
     * Converts the given list of hex digits into bytes.
     */
    public static void fromHexString(char[] charArray, int offset, int length, OutputStream out) throws IOException {
        int existing = -1;
        while (length > 0) {
            char c = charArray[offset++];
            int digit = decodeHexDigit(c);
            if (digit != -1) {
                if (existing != -1) {
                    int b = (existing << 4) | digit;
                    out.write(b);
                    existing = -1;
                } else {
                    existing = digit;
                }
            }
            length--;
        }
    }

    /**
     * Decodes a hex digit or returns -1 for unrecognized
     */
    public static int decodeHexDigit(char c) {
        if (c >= '0' && c <= '9')
            return c - '0';
        if (c >= 'A' && c <= 'F')
            return (c - 'A') + 0xA;
        if (c >= 'a' && c <= 'f')
            return (c - 'a') + 0xA;
        return -1;
    }
}
