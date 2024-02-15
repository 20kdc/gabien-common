/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

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
            //                     A      D      S      R      M      V      PMF               PMT   PMD    PL    MST
            return new MLDIChannel(0.02f, 0,     1,     0.10f, 1.00f, 0.25f, 50 + (note * 2),  25,   1,     true, 0);
        }
        //                     A      D      S      R      M      V      PMF    PMT    PMD
        return new MLDIChannel(0.02f, 0.25f, 0.50f, 0.10f, 0.50f, 0.50f, 1.01f, 0.99f, 0.10f);
    }
}
