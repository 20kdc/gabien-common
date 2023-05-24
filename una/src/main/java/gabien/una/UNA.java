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

/**
 * Maybe the start of something new.
 * Created 23rd May, 2023.
 */
public class UNA {
    /* Loader */
    public static boolean defaultLoader() {
        // all supported CPUs
        String[] cpu = {
            "x86_64",
            "aarch64",
            "arm",
            "riscv64",
            // OpenJDK will complain about disabling stack guard if this is loaded early
            "x86"
        };
        String[] os = {
            "linux-gnu",
            "windows-gnu",
            "macos",
            // this shouldn't really get extracted due to how the packaging works
            "android"
        };
        // for Android
        try {
            System.loadLibrary("gabien-una");
            return true;
        } catch (Throwable ex) {
        }
        // try for anything obvious
        for (String o : os)
            for (String c : cpu)
                if (defaultLoaderConfig(o + "-" + c))
                    return true;
        // get desperate
        String osArch = System.getProperty("os.arch");
        if (osArch.equalsIgnoreCase("amd64"))
            osArch = "x86_64";
        else if (osArch.equalsIgnoreCase("i686"))
            osArch = "x86";
        else if (osArch.contains("arm64"))
            osArch = "aarch64";
        else if (osArch.contains("arm"))
            osArch = "arm";
        for (String c : cpu)
            if (defaultLoaderConfigTmpfile(c + "-" + osArch))
                return true;
        // get REALLY desperate
        for (String o : os)
            for (String c : cpu)
                if (defaultLoaderConfigTmpfile(c + "-" + o))
                    return true;
        return false;
    }
    private static boolean defaultLoaderConfig(String config) {
        try {
            System.loadLibrary("gabien-una-" + config);
            return true;
        } catch (Throwable ex) {
        }
        String fn = "una." + config;
        try {
            System.load(new File(fn).getAbsolutePath());
            return true;
        } catch (Throwable ex) {
        }
        return false;
    }
    private static boolean defaultLoaderConfigTmpfile(String config) {
        String fn = "una." + config;
        String fnf = "lib/" + fn;
        try {
            // This can fail on Android, but that shouldn't run this anyway
            File tmp;
            try (InputStream inp = UNA.class.getResourceAsStream(fnf)) {
                if (inp == null)
                    return false;
                tmp = File.createTempFile(fnf, null);
                tmp.deleteOnExit();
                Files.copy(inp, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            System.load(tmp.getAbsolutePath());
            return true;
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return false;
    }
    /* Library */

    /* Natives - Baseline */
    public static native long getPurpose();
    public static native long getSizeofPtr();
    public static native long lookupBootstrap(long str);

    /* Natives - libc */
    public static native long strlen(long str);
    public static native long malloc(long sz);
    public static native void free(long address);
    public static native long realloc(long address, long sz);

    /* Natives - JNIEnv - Get/Set (Opposite polarity to JNIEnv functions) */
    public static native void getBooleanArrayRegion(boolean[] array, long index, long length, long address);
    public static native void getByteArrayRegion(byte[] array, long index, long length, long address);
    public static native void getCharArrayRegion(char[] array, long index, long length, long address);
    public static native void getShortArrayRegion(short[] array, long index, long length, long address);
    public static native void getIntArrayRegion(int[] array, long index, long length, long address);
    public static native void getLongArrayRegion(long[] array, long index, long length, long address);
    public static native void getFloatArrayRegion(float[] array, long index, long length, long address);
    public static native void getDoubleArrayRegion(double[] array, long index, long length, long address);

    public static native void setBooleanArrayRegion(boolean[] array, long index, long length, long address);
    public static native void setByteArrayRegion(byte[] array, long index, long length, long address);
    public static native void setCharArrayRegion(char[] array, long index, long length, long address);
    public static native void setShortArrayRegion(short[] array, long index, long length, long address);
    public static native void setIntArrayRegion(int[] array, long index, long length, long address);
    public static native void setLongArrayRegion(long[] array, long index, long length, long address);
    public static native void setFloatArrayRegion(float[] array, long index, long length, long address);
    public static native void setDoubleArrayRegion(double[] array, long index, long length, long address);

    /* Natives - JNIEnv */
    public static native ByteBuffer newDirectByteBuffer(long address, long length);
    public static native long getDirectByteBufferAddress(ByteBuffer obj);
}
