/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.natives;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Safe wrapper around the Vorbis decoding logic.
 * Created 20th October, 2023.
 */
public final class VorbisDecoder implements AutoCloseable {
    private final long instance;
    public final int channels, sampleRate, maxFrameSize, outputLength;
    private AtomicBoolean valid = new AtomicBoolean(true);

    public VorbisDecoder(byte[] idPacket, int idPacketOffset, int idPacketLength, byte[] setupPacket, int setupPacketOffset, int setupPacketLength) {
        if (idPacketOffset < 0 || idPacketOffset > idPacket.length || idPacketLength - idPacketOffset > idPacket.length)
            throw new IndexOutOfBoundsException("idPacket out of bounds");
        if (setupPacketOffset < 0 || setupPacketOffset > setupPacket.length || setupPacketLength - setupPacketOffset > setupPacket.length)
            throw new IndexOutOfBoundsException("setupPacket out of bounds");
        instance = VorbisUnsafe.open(idPacket, idPacketOffset, idPacketLength, setupPacket, setupPacketOffset, setupPacketLength, DecoderErrorException.class);
        if (instance == 0)
            throw new DecoderErrorException("Unknown error");
        channels = VorbisUnsafe.getChannels(instance);
        sampleRate = VorbisUnsafe.getSampleRate(instance);
        maxFrameSize = VorbisUnsafe.getMaxFrameSize(instance);
        outputLength = channels * maxFrameSize;
    }

    public final synchronized int decodeFrame(byte[] packet, int packetOffset, int packetLength, float[] output, int outputOffset) {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        if (packetOffset < 0 || packetOffset > packet.length || packetLength - packetOffset > packet.length)
            throw new IndexOutOfBoundsException("packet out of bounds");
        if (outputOffset < 0 || outputOffset > output.length || outputLength - outputOffset > output.length)
            throw new IndexOutOfBoundsException("output out of bounds");
        return VorbisUnsafe.decodeFrame(instance, packet, packetOffset, packetLength, output, outputOffset);
    }

    public final synchronized int getError() {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        return VorbisUnsafe.getError(instance);
    }

    public final synchronized long getLastFrameRead() {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        return VorbisUnsafe.getLastFrameRead(instance);
    }

    public final synchronized void flush() {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        VorbisUnsafe.flush(instance);
    }

    @Override
    public void close() {
        if (valid.getAndSet(false))
            VorbisUnsafe.close(instance);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    @SuppressWarnings("serial")
    public static class DecoderErrorException extends RuntimeException {
        public DecoderErrorException(String text) {
            super(text);
        }
    }
}
