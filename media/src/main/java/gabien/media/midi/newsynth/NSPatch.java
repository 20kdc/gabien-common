/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.midi.newsynth;

import java.util.Random;

import datum.DatumInvalidVisitor;
import datum.DatumSrcLoc;
import datum.DatumVisitor;
import datum.DatumWriter;
import gabien.datum.DatumExpectListVisitor;
import gabien.datum.DatumKVDVisitor;
import gabien.datum.DatumTreeCallbackVisitor;
import gabien.media.midi.newsynth.CurvePlotter.NormalizationMode;

/**
 * New Synth patch.
 * Created 2nd July, 2025
 */
public class NSPatch {
    private static int CACHED_WAVEFORM_LEN = 512;

    public String name = "new patch";
    public final NSUnloopedWaveform volumeWaveform = new NSUnloopedWaveform();
    public final NSLoopedWaveform mainWaveform = new NSLoopedWaveform();
    public final NSUnloopedWaveform pitchEnvWaveform = new NSUnloopedWaveform();
    public int strikeMs = 100;
    public int releaseMs = 500;
    public int fixedFrequency = 0;
    public int octaveShift = 0;
    /**
     * Particularly when noise is involved, the resolution of the main waveform is paramount.
     */
    public int mainWaveformSamples = 8192;
    public boolean sustainEnabled = true;
    public boolean noiseEnabled = false;

    private float[] mainWaveformCache = new float[1];
    private final float[] envWaveformCache = new float[CACHED_WAVEFORM_LEN];
    private final float[] pitchEnvWaveformCache = new float[CACHED_WAVEFORM_LEN];
    private volatile boolean cacheDirty = true;

    public NSPatch() {
        volumeWaveform.pointData = new float[] {
                0, 0,
                0.125f, 0,
                0.25f, 0,
                0.26f, 0.5f,
                0.27f, 0.5f,
                0.28f, 0.5f,
                0.40f, 0.125f,
                0.50f, 0.125f,
                0.75f, 0,
                1, 0
        };
    }

    public float[] getMainWaveform() {
        if (cacheDirty)
            regenCaches();
        return mainWaveformCache;
    }

    public float[] getEnvWaveform() {
        if (cacheDirty)
            regenCaches();
        return envWaveformCache;
    }

    public float[] getPitchEnvWaveform() {
        if (cacheDirty)
            regenCaches();
        return pitchEnvWaveformCache;
    }

    public synchronized void markCachesDirty() {
        cacheDirty = true;
    }

    public synchronized void regenCaches() {
        // make sure only one thread is writing here at a time
        cacheDirty = false;
        if (mainWaveformCache.length != mainWaveformSamples)
            mainWaveformCache = new float[mainWaveformSamples];
        CurvePlotter.resolve(mainWaveform, mainWaveformCache, noiseEnabled ? NormalizationMode.None : NormalizationMode.RecentreDC);
        if (noiseEnabled) {
            Random r = new Random(name.hashCode());
            for (int i = 0; i < mainWaveformCache.length; i++)
                mainWaveformCache[i] *= r.nextGaussian();
        }
        CurvePlotter.resolve(volumeWaveform, envWaveformCache, NormalizationMode.None);
        CurvePlotter.resolve(pitchEnvWaveform, pitchEnvWaveformCache, NormalizationMode.None);
    }

    public void writeToDatum(DatumWriter writer) {
        DatumWriter lst = writer.visitList(DatumSrcLoc.NONE);
        lst.visitId("name", DatumSrcLoc.NONE);
        lst.visitString(name, DatumSrcLoc.NONE);
        lst.indent++;
        lst.visitNewline();
        lst.visitId("sustainEnabled", DatumSrcLoc.NONE);
        lst.visitBoolean(sustainEnabled, DatumSrcLoc.NONE);
        lst.visitId("noiseEnabled", DatumSrcLoc.NONE);
        lst.visitBoolean(noiseEnabled, DatumSrcLoc.NONE);
        lst.visitId("strikeMs", DatumSrcLoc.NONE);
        lst.visitInt(strikeMs, DatumSrcLoc.NONE);
        lst.visitId("releaseMs", DatumSrcLoc.NONE);
        lst.visitInt(releaseMs, DatumSrcLoc.NONE);
        lst.visitId("fixedFrequency", DatumSrcLoc.NONE);
        lst.visitInt(fixedFrequency, DatumSrcLoc.NONE);
        lst.visitId("octaveShift", DatumSrcLoc.NONE);
        lst.visitInt(octaveShift, DatumSrcLoc.NONE);
        lst.visitId("mainWaveformSamples", DatumSrcLoc.NONE);
        lst.visitInt(mainWaveformSamples, DatumSrcLoc.NONE);
        lst.visitNewline();
        lst.visitId("env", DatumSrcLoc.NONE);
        volumeWaveform.writeToDatum(writer);
        lst.visitNewline();
        lst.visitId("wave", DatumSrcLoc.NONE);
        mainWaveform.writeToDatum(writer);
        lst.visitNewline();
        lst.visitId("pitchEnv", DatumSrcLoc.NONE);
        pitchEnvWaveform.writeToDatum(writer);
        lst.indent--;
        lst.visitNewline();
        lst.visitEnd(DatumSrcLoc.NONE);
    }

    public DatumVisitor createDatumReadVisitor() {
        return new DatumExpectListVisitor(() -> new DatumKVDVisitor() {
            @SuppressWarnings("null")
            @Override
            public DatumVisitor handle(String key, DatumSrcLoc loc) {
                if (key.equals("name"))
                    return new DatumTreeCallbackVisitor<String>((obj) -> name = obj);
                if (key.equals("sustainEnabled"))
                    return new DatumTreeCallbackVisitor<Boolean>((obj) -> sustainEnabled = obj);
                if (key.equals("noiseEnabled"))
                    return new DatumTreeCallbackVisitor<Boolean>((obj) -> noiseEnabled = obj);
                if (key.equals("strikeMs"))
                    return new DatumTreeCallbackVisitor<Long>((obj) -> strikeMs = (int) (long) obj);
                if (key.equals("releaseMs"))
                    return new DatumTreeCallbackVisitor<Long>((obj) -> releaseMs = (int) (long) obj);
                if (key.equals("fixedFrequency"))
                    return new DatumTreeCallbackVisitor<Long>((obj) -> fixedFrequency = (int) (long) obj);
                if (key.equals("octaveShift"))
                    return new DatumTreeCallbackVisitor<Long>((obj) -> octaveShift = (int) (long) obj);
                if (key.equals("mainWaveformSamples"))
                    return new DatumTreeCallbackVisitor<Long>((obj) -> mainWaveformSamples = (int) (long) obj);
                if (key.equals("env"))
                    return volumeWaveform.createDatumReadVisitor();
                if (key.equals("wave"))
                    return mainWaveform.createDatumReadVisitor();
                if (key.equals("pitchEnv"))
                    return pitchEnvWaveform.createDatumReadVisitor();
                return DatumInvalidVisitor.INSTANCE;
            }
        });
    }
}
