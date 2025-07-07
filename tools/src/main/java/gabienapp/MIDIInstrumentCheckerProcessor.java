/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import java.io.OutputStream;

import gabien.GaBIEn;
import gabien.media.audio.fileio.ReadAnySupportedAudioSource;
import gabien.media.midi.DefaultMIDIPalette;
import gabien.media.midi.MIDISequence;
import gabien.media.midi.MIDISynthesizer;
import gabien.media.midi.MIDITimer;
import gabien.media.midi.MIDITracker;
import gabien.render.IDrawable;

/**
 * Got to get the levels right
 * Created 17th February, 2024.
 */
public class MIDIInstrumentCheckerProcessor {
    public static final int SAMPLE_RATE = 44100;
    public final int SAMPLE_COUNT = SAMPLE_RATE * 8;
    public final int WAVEFORM_W = 512;
    public final int WAVEFORM_CHUNK = (SAMPLE_COUNT / WAVEFORM_W) * 2;
    public final int WAVEFORM_H = 64;
    public final IDrawable resultDrawable;
    public final float[] sampleReference;
    public final float[] sampleResult;
    public final String REFERENCE = "/media/era3-sync/external/music/modarc/soundfonts/gzdoom.sf2";

    public MIDIInstrumentCheckerProcessor(MIDISynthesizer.Palette pal, int program, int note) {
        String prefix = "sf-comparison/" + program + "-" + note;
        MIDISequence sequence = new MIDISequence((short) 1, new byte[][] {
            new byte[] {
                    0, (byte) 0xC0, (byte) program,
                    0, (byte) 0x90, (byte) note, (byte) 127,
                    8, (byte) 0x80, (byte) note, (byte) 127
            }
        });
        if (GaBIEn.getInFile(prefix + ".wav") == null) {
            try {
                OutputStream os2 = GaBIEn.getOutFile(prefix + ".mid");
                sequence.exportSequenceFile(os2);
                os2.close();
                ProcessBuilder pb = new ProcessBuilder("fluidsynth", "-g1", "-F", prefix + ".wav", REFERENCE, prefix + ".mid");
                pb.start().waitFor();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            sampleReference = ReadAnySupportedAudioSource.open(GaBIEn.getInFile(prefix + ".wav"), true).readAllAsF32();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        MIDISynthesizer ms = new MIDISynthesizer(SAMPLE_RATE, pal, 1);
        MIDITracker mtrk = new MIDITracker(sequence, ms);
        MIDITimer mtmr = new MIDITimer(mtrk);
        ms.globalVolume = 1.0f;
        sampleResult = new float[SAMPLE_COUNT * 2];
        mtmr.resolve();
        for (int i = 0; i < sampleResult.length; i += 2) {
            ms.render(sampleResult, i, 1);
            ms.update(1.0d / SAMPLE_RATE);
            mtmr.currentTime += 1.0d / SAMPLE_RATE;
            mtmr.resolve();
        }
        float[] vcRef = dataIntoVolumeCompare(sampleReference);
        float[] rsRef = dataIntoVolumeCompare(sampleResult);
        resultDrawable = makeVolumeCompare(vcRef, rsRef);
    }

    private float[] dataIntoVolumeCompare(float[] data) {
        float[] res = new float[data.length * 2];
        int iOfs = 0;
        int oOfs = 0;
        for (int i = 0; i < WAVEFORM_W; i++) {
            float valueMax = -1;
            float valueMin = 1;
            for (int j = 0; j < WAVEFORM_CHUNK; j++) {
                if (iOfs < data.length) {
                    float value = data[iOfs++];
                    valueMax = Math.max(valueMax, value);
                    valueMin = Math.min(valueMin, value);
                }
            }
            res[oOfs++] = valueMax;
            res[oOfs++] = valueMin;
        }
        return res;
    }

    private IDrawable makeVolumeCompare(float[] dataA, float[] dataB) {
        int[] colours = new int[WAVEFORM_W * WAVEFORM_H];
        for (int i = 0; i < WAVEFORM_W; i++) {
            float valueMaxA = dataA[(i * 2)];
            float valueMinA = dataA[(i * 2) + 1];
            float valueMaxB = dataB[(i * 2)];
            float valueMinB = dataB[(i * 2) + 1];
            for (int j = 0; j < WAVEFORM_H; j++) {
                float value = ((j / (float) WAVEFORM_H) - 0.5f) * 2.0f;
                boolean inA = (value >= valueMinA && value <= valueMaxA);
                boolean inB = (value >= valueMinB && value <= valueMaxB);
                int colour = 0xFF004000;
                if (inA)
                    colour |= 0x00FF0000;
                if (inB)
                    colour |= 0x000000FF;
                colours[(j * WAVEFORM_W) + i] = colour;
            }
        }
        return GaBIEn.createImage(colours, WAVEFORM_W, WAVEFORM_H);
    }

}
