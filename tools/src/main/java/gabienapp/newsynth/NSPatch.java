/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabienapp.newsynth;

import gabienapp.newsynth.CurvePlotter.NormalizationMode;

/**
 * New Synth patch.
 * Created 2nd July, 2025
 */
public class NSPatch {
    private static int CACHED_WAVEFORM_LEN = 512;

    public String identifier = "new patch";
    public final NSUnloopedWaveform volumeWaveform = new NSUnloopedWaveform();
    public final NSLoopedWaveform mainWaveform = new NSLoopedWaveform();
    public final NSUnloopedWaveform pitchEnvWaveform = new NSUnloopedWaveform();
    public int attackMs = 1000;
    public int decayMs = 1000;

    private final float[] mainWaveformCache = new float[CACHED_WAVEFORM_LEN];
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
        CurvePlotter.resolve(mainWaveform, mainWaveformCache, NormalizationMode.RecentreDC);
        CurvePlotter.resolve(volumeWaveform, envWaveformCache, NormalizationMode.None);
        CurvePlotter.resolve(pitchEnvWaveform, pitchEnvWaveformCache, NormalizationMode.None);
    }
}
