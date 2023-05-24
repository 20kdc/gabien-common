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

    /* Baseline wrappers and so forth */

    public static long checkedMalloc(long length) {
        if (length <= 0)
            throw new RuntimeException("Invalid length of malloc");
        long res = malloc(length);
        if (res == 0)
            throw new RuntimeException("Out of memory / malloc failed");
        return res;
    }

    public static long strdup(String str) {
        return strdup(str, StandardCharsets.UTF_8);
    }

    public static long strdup(String str, Charset cs) {
        byte[] bytes = str.getBytes(cs);
        long res = checkedMalloc(bytes.length + 1);
        setAB(res, bytes.length, bytes, 0);
        setB(res + bytes.length, (byte) 0);
        return res;
    }

    public static long dlOpen(String str) {
        long tmp = strdup(str);
        long res = dlOpen(tmp);
        free(tmp);
        return res;
    }

    public static long dlSym(long module, String str) {
        long tmp = strdup(str);
        long res = dlSym(module, tmp);
        free(tmp);
        return res;
    }

    public static String getString(long address) {
        return getString(address, StandardCharsets.UTF_8);
    }

    public static String getString(long address, Charset cs) {
        return new String(getAB(address, (int) strlen(address)), cs);
    }

    /* Bulk Allocating Peek */

    public static boolean[] getAZ(long addr, int length) {
        boolean[] data2 = new boolean[length];
        getAZ(addr, length, data2, 0);
        return data2;
    }

    public static byte[] getAB(long addr, int length) {
        byte[] data2 = new byte[length];
        getAB(addr, length, data2, 0);
        return data2;
    }

    public static char[] getAC(long addr, int length) {
        char[] data2 = new char[length];
        getAC(addr, length, data2, 0);
        return data2;
    }

    public static short[] getAS(long addr, int length) {
        short[] data2 = new short[length];
        getAS(addr, length, data2, 0);
        return data2;
    }

    public static int[] getAI(long addr, int length) {
        int[] data2 = new int[length];
        getAI(addr, length, data2, 0);
        return data2;
    }

    public static long[] getAJ(long addr, int length) {
        long[] data2 = new long[length];
        getAJ(addr, length, data2, 0);
        return data2;
    }

    public static float[] getAF(long addr, int length) {
        float[] data2 = new float[length];
        getAF(addr, length, data2, 0);
        return data2;
    }

    public static double[] getAD(long addr, int length) {
        double[] data2 = new double[length];
        getAD(addr, length, data2, 0);
        return data2;
    }

    /* Natives */

    /* Natives - Core */
    public static native String getArchOS();
    public static native long getTestStringRaw();
    public static final long SYSFLAG_W32 = 1;
    public static native long getSysFlags();
    public static native long getSizeofPtr();

    /* Natives - DL */
    public static native long dlOpen(long str);
    public static native long dlSym(long module, long str);
    public static native void dlClose(long module);

    /* Natives - libc - string */
    public static native long strlen(long str);
    public static native long strdup(long str);
    public static native long memcpy(long dst, long src, long len);
    public static native int memcmp(long a, long b, long len);

    /* Natives - libc - malloc */
    public static native long malloc(long sz);
    public static native void free(long address);
    public static native long realloc(long address, long sz);

    /* JIT */
    public static native long getPageSize();
    public static native long rwxAlloc(long sz);
    public static native void rwxFree(long address, long sz);

    /* Natives - Peek/Poke */
    public static native byte getB(long addr);
    public static native short getS(long addr);
    public static native int getI(long addr);
    public static native long getJ(long addr);
    public static native float getF(long addr);
    public static native double getD(long addr);
    public static native long getPtr(long addr);

    public static native void setB(long addr, byte data);
    public static native void setS(long addr, short data);
    public static native void setI(long addr, int data);
    public static native void setJ(long addr, long data);
    public static native void setF(long addr, float data);
    public static native void setD(long addr, double data);
    public static native void setPtr(long addr, long data);

    /* Natives - JNIEnv - Get/Set (Opposite polarity to JNIEnv functions) */
    public static native void getAZ(long addr, long length, boolean[] array, long index);
    public static native void getAB(long addr, long length, byte[] array, long index);
    public static native void getAC(long addr, long length, char[] array, long index);
    public static native void getAS(long addr, long length, short[] array, long index);
    public static native void getAI(long addr, long length, int[] array, long index);
    public static native void getAJ(long addr, long length, long[] array, long index);
    public static native void getAF(long addr, long length, float[] array, long index);
    public static native void getAD(long addr, long length, double[] array, long index);

    public static native void setAZ(long addr, long length, boolean[] array, long index);
    public static native void setAB(long addr, long length, byte[] array, long index);
    public static native void setAC(long addr, long length, char[] array, long index);
    public static native void setAS(long addr, long length, short[] array, long index);
    public static native void setAI(long addr, long length, int[] array, long index);
    public static native void setAJ(long addr, long length, long[] array, long index);
    public static native void setAF(long addr, long length, float[] array, long index);
    public static native void setAD(long addr, long length, double[] array, long index);

    /* Natives - JNIEnv */
    public static native String newStringUTF(long address);
    public static native ByteBuffer newDirectByteBuffer(long address, long length);
    public static native long getDirectByteBufferAddress(ByteBuffer obj);
}
