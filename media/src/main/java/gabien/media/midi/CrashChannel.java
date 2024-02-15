/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import java.util.Random;

/**
 * As in a literal crash sound.
 * Created 15th February, 2024.
 */
public class CrashChannel extends MIDISynthesizer.Channel {
    public final Random myRandom;
    public double volume;
    public boolean shake;
    public final double timeToCollapse, volumeMul;

    public CrashChannel(Random r, double time, double volume, boolean shake) {
        this.shake = shake;
        // shake starts from 0, goes up to 1, then goes back down
        if (!shake)
            this.volume = 1.0d;
        myRandom = r;
        timeToCollapse = time;
        volumeMul = volume;
    }

    @Override
    public void noteOffInner(int velocity) {
    }

    @Override
    public void render(float[] buffer, int offset, int frames, float leftVol, float rightVol) {
        double v2 = volume * volume * volumeMul * getVelocityVol();
        leftVol *= v2;
        rightVol *= v2;
        while (frames > 0) {
            float waveform = (float) myRandom.nextGaussian();
            buffer[offset++] += waveform * leftVol;
            buffer[offset++] += waveform * rightVol;
            frames--;
        }
    }

    @Override
    public boolean update(double time) {
        if (shake) {
            volume += time / timeToCollapse;
            if (volume >= 1.0d) {
                volume = 1.0d;
                shake = false;
            }
            return false;
        } else {
            volume -= time / timeToCollapse;
            return volume <= 0;
        }
    }
}
