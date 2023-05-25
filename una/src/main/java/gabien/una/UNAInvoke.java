/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

/**
 * Invoke functions.
 * Created 25th May, 2023.
 */
public class UNAInvoke {
    /* Invocation Setup */

    public static int getVariant(char ret, String args) {
        UNA.checkSetup();
        int variant = encodeV(ret);
        for (char ch : args.toCharArray()) {
            variant *= 3;
            variant += encodeV(ch);
        }
        return variant;
    }

    private static int encodeV(char ch) {
        if (ch == 'I')
            return 0;
        if (ch == 'L')
            return 1;
        if (ch == 'F')
            return 2;
        // alias V (void) to i32
        if (ch == 'V')
            return 0;
        // P (pointer) is really why variants exist
        if (ch == 'P')
            return UNA.is32Bit ? 0 : 1;
        throw new RuntimeException("Unknown variant-char " + ch);
    }

    /* Invoke */
    public static native long c0(long code, int variant);
    public static native long c1(long a0, long code, int variant);
    public static native long c2(long a0, long a1, long code, int variant);
    public static native long c3(long a0, long a1, long a2, long code, int variant);
    public static native long c4(long a0, long a1, long a2, long a3, long code, int variant);
    public static native long c5(long a0, long a1, long a2, long a3, long a4, long code, int variant);
    public static native long c6(long a0, long a1, long a2, long a3, long a4, long a5, long code, int variant);

    /* Invoke - Special */
    // glReadPixels
    public static native long LcIIIIIIP(int a0, int a1, int a2, int a3, int a4, int a5, long a6, long code);
    // glCompressedTexImage2D
    public static native long LcIIIIIIIP(int a0, int a1, int a2, int a3, int a4, int a5, int a6, long a7, long code);
    // glTexImage2D, glTexSubImage2D, glCompressedTexSubImage2D
    public static native long LcIIIIIIIIP(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, int a8, long code);
    // glCopyTexImage2D, glCopyTexSubImage2D
    public static native long LcIIIIIIII(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, long code);
}
