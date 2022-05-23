/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.uslx.audio;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a multi-channel float stream.
 * 
 * The difference between this and ISample is that ISample is:
 * + Mono
 * + Relatively stateless
 * 
 * This meanwhile is:
 * + Many-channel
 * + Stateful and intended to be changed on-the-fly
 */
public abstract class MultiChannelFloatStream {
    /**
     * The current content of the channels.
     */
    public final float[] channels;

    /**
     * Used to implement a two-stage system to make sure each stream is only calculated once.
     */
    private boolean calculateRequested = false;

    public MultiChannelFloatStream(int ch) {
        channels = new float[ch];
    }

    public MultiChannelFloatStream(float[] ch) {
        channels = ch;
    }

    /**
     * Requests that the stream state be recalculated.
     */
    public final void requestCalculate() {
        calculateRequested = true;
    }

    /**
     * Ensures that the current stream state is recalculated if it was requested.
     */
    public final void ensureCalculated() {
        if (calculateRequested) {
            calculate();
            calculateRequested = false;
        }
    }

    /**
     * Forwards calculation requests.
     */
    protected abstract void forwardRequestCalculate();

    /**
     * Calculates the current stream state.
     */
    protected abstract void calculate();

    /**
     * Advances the stream. This may assume a prior get call.
     */
    public abstract void advance(double time);

    /**
     * Non-advancing proxy.
     */
    public static final class NonAdvancing extends MultiChannelFloatStream {
        public final MultiChannelFloatStream base;

        public NonAdvancing(MultiChannelFloatStream b) {
            super(b.channels);
            base = b;
        }

        @Override
        protected void forwardRequestCalculate() {
            base.requestCalculate();
        }

        @Override
        protected void calculate() {
        }

        @Override
        public void advance(double time) {
        }
    }

    /**
     * Stream to play back a sample.
     */
    public static class Sample extends MultiChannelFloatStream {
        public ISample sample;
        public double position;

        public Sample(ISample s) {
            super(1);
            sample = s;
        }

        @Override
        protected void forwardRequestCalculate() {
        }

        @Override
        protected void calculate() {
            channels[0] = sample.get(position);
        }

        @Override
        public void advance(double time) {
            position += time;
        }
    }

    /**
     * Stream to modify another stream.
     */
    public static class Pitch extends MultiChannelFloatStream {
        public MultiChannelFloatStream base;
        public double pitch = 1.0d;

        public Pitch(MultiChannelFloatStream s) {
            super(s.channels);
            base = s;
        }
        
        @Override
        protected void forwardRequestCalculate() {
            base.requestCalculate();
        }

        @Override
        protected void calculate() {
        }

        @Override
        public void advance(double time) {
            base.advance(time * pitch);
        }
    }

    /**
     * Stream to convert a mono stream to an N-channel stream.
     */
    public static class MonoBroadcast extends MultiChannelFloatStream {
        public MultiChannelFloatStream base;
        public float[] volumes;

        public MonoBroadcast(MultiChannelFloatStream s, int channels) {
            super(channels);
            base = s;
            volumes = new float[channels];
            for (int i = 0; i < volumes.length; i++)
                volumes[i] = 1;
        }

        @Override
        protected void forwardRequestCalculate() {
            base.requestCalculate();
        }

        @Override
        protected void calculate() {
            float src = base.channels[0];
            for (int i = 0; i < volumes.length; i++)
                channels[i] = src * volumes[i];
        }

        @Override
        public void advance(double time) {
            base.advance(time);
        }
    }

    /**
     * Adds multiple streams together.
     */
    public static class Adder extends MultiChannelFloatStream {
        public final LinkedList<MultiChannelFloatStream> streams = new LinkedList<MultiChannelFloatStream>();

        public Adder(int ch) {
            super(ch);
        }

        @Override
        protected void forwardRequestCalculate() {
            for (MultiChannelFloatStream mcfs : streams)
                mcfs.requestCalculate();
        }

        @Override
        protected void calculate() {
            for (int i = 0; i < channels.length; i++)
                channels[i] = 0;
            for (MultiChannelFloatStream mcfs : streams)
                for (int i = 0; i < channels.length; i++)
                    channels[i] += mcfs.channels[i];
        }

        @Override
        public void advance(double time) {
            for (MultiChannelFloatStream mcfs : streams)
                mcfs.advance(time);
        }
    }

    /**
     * A simple compressor.
     */
    public static class SimpleCompressor extends MultiChannelFloatStream {
        public MultiChannelFloatStream base;
        public float maxAbsLast;
        public float decayRate = 1.5f;

        public SimpleCompressor(MultiChannelFloatStream s) {
            super(s.channels.length);
            base = s;
        }

        @Override
        protected void forwardRequestCalculate() {
            base.requestCalculate();
        }

        @Override
        protected void calculate() {
            float maxAbs = 1.0f;
            for (int i = 0; i < channels.length; i++) {
                float val = base.channels[i];
                channels[i] = val;
                float abs = Math.abs(val);
                if (abs > maxAbs)
                    maxAbs = abs;
            }
            if (maxAbsLast < maxAbs)
                maxAbsLast = maxAbs;
            for (int i = 0; i < channels.length; i++)
                channels[i] /= maxAbsLast;
        }

        @Override
        public void advance(double time) {
            base.advance(time);
            maxAbsLast -= decayRate * time;
        }
    }
}
