/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.nio.charset.StandardCharsets;
import java.util.function.ToLongFunction;
import java.nio.charset.Charset;

/**
 * UNA C library wrappers.
 * Created 25th May, 2023.
 */
public abstract class UNAC {
    private UNAC() {
    }
    
    /* Baseline wrappers and so forth */

    /**
     * Checked version of malloc.
     */
    public static long mallocChk(long length) {
        if (length <= 0)
            throw new RuntimeException("Invalid length of malloc");
        long res = malloc(length);
        if (res == 0)
            throw new RuntimeException("Out of memory / malloc failed");
        return res;
    }

    /**
     * Mallocs a copy of the given region of memory.
     */
    public static long memdup(long mem, long len) {
        long m2 = mallocChk(len);
        memcpy(m2, mem, len);
        return m2;
    }

    /**
     * Mallocs a null-terminated copy of the given string as UTF-8.
     */
    public static long strdup(String str) {
        return strdup(str, StandardCharsets.UTF_8);
    }

    /**
     * Mallocs a null-terminated copy of the given string in the given charset.
     */
    public static long strdup(String str, Charset cs) {
        byte[] bytes = str.getBytes(cs);
        return UNAPoke.allocAB(bytes, 0, bytes.length, true);
    }

    /**
     * Given a string, opens a dynamic library by name. See dlclose.
     */
    public static long dlopen(String str) {
        // because on Windows LoadLibraryA is used, let's be careful about this...
        long tmp = strdup(str, Charset.defaultCharset());
        long res = dlopen(tmp);
        free(tmp);
        return res;
    }

    /**
     * Looks up a function in a dynamic library, then prepares it for calling.
     */
    public static UNAFn dlsym(long module, String str, UNAABI abi, String sig) {
        long res = dlsym(module, str);
        if (res == 0)
            return null;
        return new UNAFn(res, abi.of(sig));
    }

    /**
     * Looks up a function using a lambda, then prepares it for calling.
     */
    public static UNAFn dlsym(ToLongFunction<String> module, String str, UNAABI abi, String sig) {
        long res = module.applyAsLong(str);
        if (res == 0)
            return null;
        return new UNAFn(res, abi.of(sig));
    }

    /**
     * Creates a dlsym lambda.
     */
    public static ToLongFunction<String> dlsymLambda(long module) {
        return (str) -> dlsym(module, str);
    }

    /**
     * Looks up a function in a dynamic library.
     */
    public static long dlsym(long module, String str) {
        long tmp = strdup(str);
        long res = dlsym(module, tmp);
        free(tmp);
        return res;
    }

    /* Natives - DL */

    /**
     * Given the location of a C null-terminated string, opens a dynamic library by that name.
     */
    public static native long dlopen(long str);
    /**
     * Given a module (as returned from dlopen) and a C null-terminated string, finds a function in the module.
     */
    public static native long dlsym(long module, long str);
    /**
     * Given a module (as returned from dlopen), closes it.
     */
    public static native void dlclose(long module);

    /* Natives - libc - string */
    /**
     * Returns the amount of bytes inside a C null-terminated string (not counting the null).
     */
    public static native long strlen(long str);
    /**
     * Copies a C null-terminated string into freshly malloced memory.
     * This version is not checked, and will return 0 on failure.
     */
    public static native long strdup(long str);
    /**
     * Checks for differences between null-terminated A and B of len bytes each.
     * If they are different, returns non-zero.
     * The sign is based on the unsigned difference, so this is a sorting function.
     */
    public static native int strcmp(long a, long b);
    /**
     * Copies len bytes from the source to the destination.
     */
    public static native long memcpy(long dst, long src, long len);
    /**
     * Checks for differences between buffers A and B of len bytes each.
     * If they are different, returns non-zero.
     * The sign is based on the unsigned difference, so this is a sorting function.
     */
    public static native int memcmp(long a, long b, long len);

    /* Natives - libc - malloc */
    /**
     * Allocates the given amount of bytes and returns the resulting pointer.
     * Don't pass 0, it's weird. Check the manual page for more details.
     * This version is not checked, and will return 0 on failure.
     */
    public static native long malloc(long sz);
    /**
     * Frees memory allocated with malloc (or functions that allocate using malloc).
     * Does nothing if passed 0.
     */
    public static native void free(long address);
    /**
     * This function can be considered to duplicate the memory at the given address, but expanding or decreasing the allocation size to sz.
     * (If the allocation size is decreased, the data at the end is lost.)
     * The old memory is then freed.
     * Strictly, the same pointer may be returned as came in, but this is not a guarantee by any means.
     * Check the manual page for more details.
     */
    public static native long realloc(long address, long sz);
    /**
     * Returns the page size. This is important for the RWX functions.
     */
    public static native long getpagesize();

    /* JIT */
    /**
     * Allocates RWX memory. The size must be a multiple of the page size.
     * RWX memory can be used to perform dynamic code generation.
     */
    public static native long rwxAlloc(long sz);
    /**
     * Frees RWX memory. The size must be what was allocated in the first place.
     */
    public static native void rwxFree(long address, long sz);
}
