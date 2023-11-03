/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.natives;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Safe wrapper around the MP3 decoding logic.
 * Created 3rd November, 2023.
 */
public final class MP3Decoder extends MP3Enum implements AutoCloseable {
    private final long instance;
    private AtomicBoolean valid = new AtomicBoolean(true);

    /**
     * Allocates/initializes an MP3 decoder.
     * Throws exceptions if there is any issue.
     */
    public MP3Decoder() {
        instance = MP3Unsafe.alloc(DecoderErrorException.class);
        if (instance == 0)
            throw new DecoderErrorException("Unknown error");
    }

    /**
     * Resets the decoder.
     */
    public final synchronized void reset() {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        MP3Unsafe.reset(instance);
    }

    /**
     * Get the last frame's byte count.
     */
    public final synchronized int getLastFrameBytes() {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        return MP3Unsafe.getLastFrameBytes(instance);
    }

    /**
     * Get the last frame's sample rate.
     */
    public final synchronized int getLastFrameSampleRate() {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        return MP3Unsafe.getLastFrameSampleRate(instance);
    }

    /**
     * Get the last frame's channel count.
     */
    public final synchronized int getLastFrameChannels() {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        return MP3Unsafe.getLastFrameChannels(instance);
    }

    /**
     * Decodes part of the input buffer into samples.
     * The output array must have at least MAX_SAMPLES_PER_FRAME room available (the API becomes unsafe otherwise).
     * Data is written in the usual interleaved sample-major, channel-minor form.
     */
    public final synchronized int decodeFrame(byte[] packet, int packetOffset, int packetLength, float[] output, int outputOffset) {
        if (!valid.get())
            throw new InvalidatedPointerException(this);
        if (packetOffset < 0 || packetOffset > packet.length || packetLength - packetOffset > packet.length || packetLength < 0)
            throw new IndexOutOfBoundsException("packet out of bounds");
        if (output != null)
            if (outputOffset < 0 || outputOffset > output.length || MAX_SAMPLES_PER_FRAME - outputOffset > output.length)
                throw new IndexOutOfBoundsException("output out of bounds");
        return MP3Unsafe.decodeFrame(instance, packet, packetOffset, packetLength, output, outputOffset);
    }

    /**
     * Frees the MP3 decoder.
     */
    @Override
    public synchronized void close() {
        if (valid.getAndSet(false))
            MP3Unsafe.free(instance);
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
