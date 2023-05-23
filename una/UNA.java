/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
import java.io.File;
import java.nio.ByteBuffer;

/**
 * Maybe the start of something new.
 * Created 23rd May, 2023.
 */
public class UNA {
    /* Loader */
    public static void defaultLoader() {
        System.load(new File("una.so").getAbsolutePath());
    }
    /* Library */

    /* Natives - Baseline */
    public static native long getPurpose();
    public static native long getSizeofPtr();
    public static native long lookupBootstrap(long str);
    public static native long strlen(long str);

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
