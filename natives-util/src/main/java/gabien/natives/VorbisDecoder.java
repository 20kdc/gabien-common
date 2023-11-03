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
public final class VorbisDecoder extends VorbisEnum implements AutoCloseable {
    private final long instance;
    public final int channels, sampleRate, maxFrameSize, outputLength;
    private AtomicBoolean valid = new AtomicBoolean(true);

    /**
     * Initializes a Vorbis decoder using the given ID and Setup packets.
     * Throws exceptions if there is any issue.
     */
    public VorbisDecoder(byte[] idPacket, int idPacketOffset, int idPacketLength, byte[] setupPacket, int setupPacketOffset, int setupPacketLength) {
        if (idPacketOffset < 0 || idPacketOffset > idPacket.length || idPacketLength - idPacketOffset > idPacket.length || idPacketLength < 0)
            throw new IndexOutOfBoundsException("idPacket out of bounds");
        if (setupPacketOffset < 0 || setupPacketOffset > setupPacket.length || setupPacketLength - setupPacketOffset > setupPacket.length || setupPacketLength < 0)
            throw new IndexOutOfBoundsException("setupPacket out of bounds");
        instance = VorbisUnsafe.open(idPacket, idPacketOffset, idPacketLength, setupPacket, setupPacketOffset, setupPacketLength, DecoderErrorException.class);
        if (instance == 0)
            throw new DecoderErrorException("Unknown error");
        channels = VorbisUnsafe.getChannels(instance);
        sampleRate = VorbisUnsafe.getSampleRate(instance);
        maxFrameSize = VorbisUnsafe.getMaxFrameSize(instance);
        outputLength = channels * maxFrameSize;
    }

    /**
     * Gets the amount of samples in a packet, at least abstractly.
     * Remember that the first packet decoded after init/flush is of effectively 0 samples.
     */
    public final synchronized int getPacketSampleCount(byte[] packet, int packetOffset, int packetLength) {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        if (packetOffset < 0 || packetOffset > packet.length || packetLength - packetOffset > packet.length || packetLength < 0)
            throw new IndexOutOfBoundsException("packet out of bounds");
        return VorbisUnsafe.getPacketSampleCount(instance, packet, packetOffset, packetLength);
    }

    /**
     * Decodes a packet into samples.
     * The output array must have at least outputLength room available (the API becomes unsafe otherwise).
     * Data is written in the usual interleaved sample-major, channel-minor form.
     */
    public final synchronized int decodeFrame(byte[] packet, int packetOffset, int packetLength, float[] output, int outputOffset) {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        if (packetOffset < 0 || packetOffset > packet.length || packetLength - packetOffset > packet.length || packetLength < 0)
            throw new IndexOutOfBoundsException("packet out of bounds");
        if (outputOffset < 0 || outputOffset > output.length || outputLength - outputOffset > output.length)
            throw new IndexOutOfBoundsException("output out of bounds");
        return VorbisUnsafe.decodeFrame(instance, packet, packetOffset, packetLength, output, outputOffset);
    }

    /**
     * Gets the last error, returning 0 if none. Check with Error enum. 
     */
    public final synchronized int getError() {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        return VorbisUnsafe.getError(instance);
    }

    /**
     * Resets decoder state; next packet will be ignored (0-samples),
     */
    public final synchronized void flush() {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        VorbisUnsafe.flush(instance);
    }

    /**
     * Frees the Vorbis decoder.
     */
    @Override
    public synchronized void close() {
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
