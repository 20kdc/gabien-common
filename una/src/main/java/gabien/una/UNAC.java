/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

/**
 * UNA C library wrappers.
 * Created 25th May, 2023.
 */
public class UNAC {
    /* Baseline wrappers and so forth */

    public static long mallocChk(long length) {
        if (length <= 0)
            throw new RuntimeException("Invalid length of malloc");
        long res = malloc(length);
        if (res == 0)
            throw new RuntimeException("Out of memory / malloc failed");
        return res;
    }

    public static long memdup(long mem, long len) {
        long m2 = mallocChk(len);
        memcpy(m2, mem, len);
        return m2;
    }

    public static long strdup(String str) {
        return strdup(str, StandardCharsets.UTF_8);
    }

    public static long strdup(String str, Charset cs) {
        byte[] bytes = str.getBytes(cs);
        return UNAPoke.allocAB(bytes, 0, bytes.length, true);
    }

    public static long dlopen(String str) {
        long tmp = strdup(str);
        long res = dlopen(tmp);
        free(tmp);
        return res;
    }

    public static long dlsym(long module, String str) {
        long tmp = strdup(str);
        long res = dlsym(module, tmp);
        free(tmp);
        return res;
    }

    /* Natives - DL */
    public static native long dlopen(long str);
    public static native long dlsym(long module, long str);
    public static native void dlclose(long module);

    /* Natives - libc - string */
    public static native long strlen(long str);
    public static native long strdup(long str);
    public static native long memcpy(long dst, long src, long len);
    public static native int memcmp(long a, long b, long len);

    /* Natives - libc - malloc */
    public static native long malloc(long sz);
    public static native void free(long address);
    public static native long realloc(long address, long sz);
    public static native long getpagesize();

    /* JIT */
    public static native long rwxAlloc(long sz);
    public static native void rwxFree(long address, long sz);
}
