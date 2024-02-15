/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import java.util.Random;

import gabien.media.midi.MIDISynthesizer.Channel;

/**
 * "Eclipse Debug Editing and its applications for MIDI Synthesis"
 * Created February 14th, 2024.
 */
public enum DefaultMIDIPalette implements MIDISynthesizer.Palette {
    INSTANCE;

    @Override
    public Channel create(MIDISynthesizer parent, int bank, int program, int note, int velocity) {
        // System.out.println(bank + ":" + program + ":" + note);
        if (bank >= 128) {
            // stick
            if (note == 28 || note == 31 || note == 39 || note == 40 || (note >= 60 && note <= 66) || note == 70)
                return new CrashChannel(new Random(bank + program + note + velocity), 0.05d, 0.3d, false);
            // shake
            if (note == 42 || note == 44 || note == 46 || note == 51 || note == 52 || note == 59 || note == 69)
                return new CrashChannel(new Random(bank + program + note + velocity), 0.05d, 0.2d, true);
            // big crash
            if (note == 49 || note == 55 || note == 57)
                return new CrashChannel(new Random(bank + program + note + velocity), 1.0d, 0.2d, false);
            //                     A      D      S      R      M      V      PMF               PMT   PMD    PL    MST
            return new MLDIChannel(0.02f, 0,     1,     0.10f, 1.00f, 0.20f, 50 + (note * 2),  25,   1,     true, 0);
        }
        //                     A      D      S      R      M      V      PMF    PMT    PMD
        return new MLDIChannel(0.02f, 0.25f, 0.50f, 0.10f, 0.50f, 0.50f, 1.01f, 0.99f, 0.10f);
    }
}
