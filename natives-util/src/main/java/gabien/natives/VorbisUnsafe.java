/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

/**
 * Wrapper for "stb_vorbis_g", a customized version of stb_vorbis specifically for GaBIEn use.
 * Created 19th October, 2023.
 */
public abstract class VorbisUnsafe extends VorbisEnum {
    private VorbisUnsafe() {
    }

    /**
     * The passed in class is used for the open error exception.
     */
    public static native long open(byte[] id, int idOfs, int idLen, byte[] setup, int setupOfs, int setupLen, Class<?> ex);
    public static native int getSampleRate(long instance);
    public static native int getChannels(long instance);
    public static native int getMaxFrameSize(long instance);
    public static native int getPacketSampleCount(long instance, byte[] packet, int packetOfs, int packetLen);
    /**
     * Important difference!
     * Output is expected to be maxFrameSize * channels floats in length.
     * Data is written there pre-interleaved. This interleaving is done in the JNI layer.
     * As such the first (returnedSamples * channels) floats are modified and the rest are unchanged.
     * It's done this way because I hear rumours of "unsafe" JNI functions being removed soon.
     * And DirectByteBuffer is on that list.
     * Never mind all the projects that will no longer be able to have sane APIs because of this...
     */
    public static native int decodeFrame(long instance, byte[] packet, int packetOfs, int packetLen, float[] output, int outputOfs);
    public static native int getError(long instance);
    public static native long getLastFrameRead(long instance);
    public static native void flush(long instance);
    public static native void close(long instance);
}
