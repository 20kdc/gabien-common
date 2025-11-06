/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import datum.DatumReaderTokenSource;
import gabien.media.midi.newsynth.NSPalette;
import gabien.uslx.append.AssetFS;

/**
 * Created February 14th, 2024. Changed to NewSynth, 5th November, 2025.
 */
public class DefaultMIDIPalette {
    public static final MIDISynthesizer.Palette INSTANCE = new NSPalette();

    private DefaultMIDIPalette() {
    }

    /**
     * Initializes the default MIDI palette if it does not already look initialized.
     * That in mind, call in accessing code.
     */
    public static void initialize() {
        synchronized (INSTANCE) {
            try {
                String fn = "gabien.media.midi.scm";
                new DatumReaderTokenSource(fn, new InputStreamReader(AssetFS.READER.apply(fn), StandardCharsets.UTF_8)).visit(((NSPalette) INSTANCE).createDatumReadVisitor());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // if (program < 0x00 || program > 0x30 || bank != 0) return null;
    // System.out.println(bank + ":" + Integer.toHexString(program) + ":" + note);
    /*
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
        // 3rd july: this honestly ruins some tracks
        // so while we're using this percussion bank for more complete NewSynth tests...
        //                     A      D      S      R      M      V      PMF               PMT   PMD    PL    MST
        //return new MLDIChannel(0.01f, 0,     1,     0.10f, 1.00f, 0.30f, 50 + (note * 2),  25,   1,     true, 0);
        return null;
    }
    */
}
