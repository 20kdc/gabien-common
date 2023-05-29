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
public abstract class UNA {
    private UNA() {
    }

    /* Sysflags */
    public static long sysFlags;
    public static boolean isWin32;
    public static boolean isBigEndian;
    public static boolean is32Bit;
    public static UNASysTypeInfo typeInfo;
    public static UNAABI defaultABI;
    private static boolean setup;

    /**
     * Confirms that setup has been called.
     */
    public static void checkSetup() {
        if (!setup)
            throw new RuntimeException("UNA.setup must be called, ideally just after the native library is loaded.");
    }

    /**
     * Sets up UNA's static fields.
     */
    public static void setup() {
        setup = true;
        sysFlags = getSysFlags();
        isWin32 = (sysFlags & SYSFLAG_W32) != 0;
        isBigEndian = (sysFlags & SYSFLAG_BE) != 0;
        is32Bit = (sysFlags & SYSFLAG_32) != 0;
        typeInfo = is32Bit ? UNASysTypeInfo.si32 : UNASysTypeInfo.si64;
        defaultABI = UNAABIFinder.getABI(UNAABIFinder.Convention.Default);
        IUNAFnType p = defaultABI.of("i(ifififififififif)");
        int res = (int) p.call(getSanityTester(),
            1, Float.floatToRawIntBits(2),
            3, Float.floatToRawIntBits(4),
            5, Float.floatToRawIntBits(6),
            7, Float.floatToRawIntBits(8),
            9, Float.floatToRawIntBits(10),
            11, Float.floatToRawIntBits(12),
            13, Float.floatToRawIntBits(14),
            15, Float.floatToRawIntBits(16)
        );
        if (res == 0)
            throw new RuntimeException("Sanity test failed, so the calling convention implementation is broken. Information: " + p);
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

    /* Natives - Misc */

    /**
     * The value in errno is recorded after every UNA invoke. This returns it.
     */
    public static native int getErrno();

    /**
     * The result of GetLastError is recorded after every UNA invoke. This returns it.
     * This function only has meaning on Windows, but always exists.
     */
    public static native int wGetLastError();

    /**
     * Runs through the Windows procedure to create a window for GL use.
     * This function only exists on Windows and exists to skip an awful lot of pain.
     */
    public static native long wCreateInvisibleGLWindowHDC();

    /**
     * Sets a sane pixel format on a DC and returns the number for future use.
     * This function only exists on Windows and exists to skip an awful lot of pain.
     */
    public static native int wChooseAndSetSanePixelFormatHDC(long hdc);

    /* Natives - JNIEnv */
    public static native String newStringUTF(long address);
    public static native ByteBuffer newDirectByteBuffer(long address, long length);
    public static native long getDirectByteBufferAddress(ByteBuffer obj);
}