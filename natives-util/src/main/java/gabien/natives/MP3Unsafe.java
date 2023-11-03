/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

/**
 * Wrapper for "minimp3_g", a customized version of minimp3 mainly for build reasons
 * Created 3rd November, 2023.
 */
public abstract class MP3Unsafe extends MP3Enum {
    private MP3Unsafe() {
    }

    /**
     * The passed in class is used for the open error exception.
     */
    public static native long alloc(Class<?> ex);
    public static native void reset(long instance);
    public static native int getLastFrameBytes(long instance);
    public static native int getLastFrameSampleRate(long instance);
    public static native int getLastFrameChannels(long instance);
    public static native int decodeFrame(long instance, byte[] packet, int packetOfs, int packetLen, float[] output, int outputOfs);
    public static native void free(long instance);
}
