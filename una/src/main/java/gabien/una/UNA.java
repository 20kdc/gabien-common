/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.nio.ByteBuffer;

/**
 * Maybe the start of something new.
 * Created 23rd May, 2023.
 */
public class UNA {
    /* Sysflags */
    public static long sysFlags;
    public static boolean isWin32;
    public static boolean isBigEndian;
    public static boolean is32Bit;
    private static boolean setup;

    public static void checkSetup() {
        if (!setup)
            throw new RuntimeException("UNA.setup must be called, ideally just after the native library is loaded.");
    }

    public static void setup() {
        setup = true;
        sysFlags = getSysFlags();
        isWin32 = (sysFlags & SYSFLAG_W32) != 0;
        isBigEndian = (sysFlags & SYSFLAG_BE) != 0;
        is32Bit = (sysFlags & SYSFLAG_32) != 0;
    }

    /* Natives */

    /* Natives - Core */
    public static native String getArchOS();
    public static native long getTestStringRaw();

    public static final long SYSFLAG_W32 = 1;
    public static final long SYSFLAG_BE = 2;
    public static final long SYSFLAG_32 = 4;
    public static native long getSysFlags();

    public static native long getSizeofPtr();

    /**
     * Returns a C function designed to test things.
     * The function's arguments are int32_t followed by float, repeated 8 times for a total of 16 arguments.
     * The function returns an int32_t, specifically 1 if and only if the arguments count from 1 to 16.
     */
    public static native long getSanityTester();

    /* Natives - JNIEnv */
    public static native String newStringUTF(long address);
    public static native ByteBuffer newDirectByteBuffer(long address, long length);
    public static native long getDirectByteBufferAddress(ByteBuffer obj);
}
