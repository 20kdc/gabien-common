/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

/**
 * Created on 09/12/15.
 */
public class SimpleMixer implements IRawAudioDriver.IRawAudioSource {
    private Channel[] channels = new Channel[128];
    private boolean lerp;
    // Basic volume adjustment algorithm which should produce passable results.
    private boolean preventClipping;
    private long maxVolLast;

    public SimpleMixer(boolean preventClip, boolean doLerp) {
        preventClipping = preventClip;
        lerp = doLerp;
    }

    public class Channel {
        private short[] data;
        private double pos = 0;
        private double VL = 0, VR = 0, P = 1;
        private boolean looping = true;

        private short wrappingGetIndex(double index) {
            int indexI = (int) Math.floor(index);
            short a = data[indexI % data.length];
            if (lerp) {
                short b = data[(indexI + 1) % data.length];
                int diff = b - a;
                // how this doesn't give type issues...
                diff *= (index - indexI) * 256;
                diff /= 256;
                return (short) (a + diff);
            }
            return a;
        }

        private short get() {
            try {
                if (data == null)
                    return 0;

                short a = wrappingGetIndex(pos);
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

        private int[] createData(int amount) {
            int[] s = new int[amount * 2];
            for (int px = 0; px < amount; px++) {
                short V = get();
                s[px * 2] = (int) (V * VL);
                s[(px * 2) + 1] = (int) (V * VR);
            }
            return s;
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
        int[] totalLeft = new int[amount];
        int[] totalRight = new int[amount];
        for (Channel js : channels) {
            if (js == null)
                continue;
            int[] r = js.createData(amount);
            for (int px = 0; px < amount; px++) {
                totalLeft[px] += r[(px * 2)];
                totalRight[px] += r[(px * 2) + 1];
            }
        }

        // Volume adjustment.
        long maxVol = 32768;
        if (preventClipping) {
            for (int px = 0; px < amount; px++) {
                maxVol = Math.max(maxVol, Math.abs(totalLeft[px]));
                maxVol = Math.max(maxVol, Math.abs(totalRight[px]));
            }
            if (maxVolLast < maxVol)
                maxVolLast = maxVol;
            maxVol = maxVolLast;
            // Sample-rate-dependent, but it's assumed to be @ 22050hz where this algorithm will return to normal in 1.5s or so.
            maxVolLast -= amount;
        }

        for (int px = 0; px < amount; px++) {

            long l = ((totalLeft[px] * 32768L) / maxVol);
            long r = ((totalRight[px] * 32768L) / maxVol);

            if (l > 32767)
                l = 32767;
            if (l < -32768)
                l = -32768;
            if (r > 32767)
                r = 32767;
            if (r < -32768)
                r = -32768;
            data[px * 2] = (short) l;
            data[(px * 2) + 1] = (short) r;
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
