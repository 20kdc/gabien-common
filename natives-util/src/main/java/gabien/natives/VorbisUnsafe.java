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

    public static native long open(long instance, byte[] id, int idOfs, int idLen, byte[] setup, int setupOfs, int setupLen);
    public static native int getSampleRate(long instance);
    public static native int getChannels(long instance);
    public static native int getMaxFrameSize(long instance);
    public static native int decodeFrame(long instance, byte[] packet, int packetOfs, int packetLen, float[] output, int outputOfs);
    public static native int getError(long instance);
    public static native long getLastFrameRead(long instance);
    public static native void flush(long instance);
    public static native void close(long instance);
}
