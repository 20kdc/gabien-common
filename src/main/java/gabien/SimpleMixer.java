/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien;

/**
 * Created on 09/12/15.
 */
public class SimpleMixer implements IRawAudioDriver.IRawAudioSource {
    private Channel[] channels = new Channel[128];

    public class Channel {
        private short[] data;
        private double pos = 0;
        private double VL = 0, VR = 0, P = 1;
        private boolean looping = true;

        private short WrappingGetIndex(double index) {
            return (short) lerp(data[wrapind((int) Math.floor(index))],
                    data[wrapind(((int) Math.floor(index)) + 1)],
                    index - Math.floor(index));
        }

        private int wrapind(int i) {
            while (i < 0)
                i += data.length;
            while (i >= data.length)
                i -= data.length;
            return i;
        }

        private short Get() {
            try {
                if (data == null)
                    return 0;

                short a = WrappingGetIndex(pos);
                pos += P;
                while (pos < 0)
                    pos += data.length;
                if (pos >= data.length) {
                    pos = pos % data.length;
                    if (!looping) {
                        VL = 0;
                        VR = 0;
                    }
                }
                return a;
            } catch (Exception e) {
                pos = 0;
                return 0;
            }
        }

        private Channel() {

        }

        private short Scale(short a, double V) {
            double b = a * V;
            if (b > 32767)
                b = 32767;
            if (b < -32768)
                b = -32768;
            return (short) b;
        }

        private short[] CreateData(int amount) {
            short[] s = new short[amount * 2];
            for (int px = 0; px < amount; px++) {
                short V = Get();
                s[px * 2] = Scale(V, VL);
                s[(px * 2) + 1] = Scale(V, VR);
            }
            return s;
        }

        private double lerp(double s, double s0, double d) {
            double diff = s0 - s;
            diff *= d;
            return s + diff;
        }

        public void playSound(double Pitch, double VolL, double VolR,
                              short[] sound, boolean isLooping) {
            VL = VolL;
            VR = VolR;
            // Sanity check
            if (P <= 0)
                P = 1;
            if (P == Double.POSITIVE_INFINITY)
                P = 1;
            if (P == Double.NEGATIVE_INFINITY)
                P = -1;
            P = Pitch;
            data = new short[sound.length];
            System.arraycopy(sound, 0, data, 0, data.length);
            pos = 0;
            looping = isLooping;
        }

        public void setVolume(double VolL, double VolR) {
            VL = VolL;
            VR = VolR;
        }

        public double volL() {
            return VL;
        }

        public double volR() {
            return VR;
        }

    }

    public Channel createChannel() {
        Channel jc = new Channel();
        for (int p = 0; p < channels.length; p++) {
            if (channels[p] == null) {
                channels[p] = jc;
                break;
            }
        }
        return jc;
    }

    @Override
    public short[] pullData(int amount) {
        short[] data = new short[amount * 2];
        int[] L = new int[amount];
        int[] R = new int[amount];
        for (Channel js : channels) {
            if (js == null)
                continue;
            short[] r = js.CreateData(amount);
            for (int px = 0; px < amount; px++) {
                L[px] += r[(px * 2)];
                R[px] += r[(px * 2) + 1];
            }
        }
        for (int px = 0; px < amount; px++) {
            if (L[px] > 32767)
                L[px] = 32767;
            if (L[px] < -32768)
                L[px] = -32768;
            if (R[px] > 32767)
                R[px] = 32767;
            if (R[px] < -32768)
                R[px] = -32768;
            data[px * 2] = (short) L[px];
            data[(px * 2) + 1] = (short) R[px];
        }
        return data;
    }

    public void deleteChannel(Channel ic) {
        for (int p = 0; p < channels.length; p++) {
            if (channels[p] == ic)
                channels[p] = null;
        }
    }
}
