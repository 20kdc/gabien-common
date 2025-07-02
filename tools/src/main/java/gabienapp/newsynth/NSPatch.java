/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabienapp.newsynth;

/**
 * New Synth patch.
 * Created 2nd July, 2025
 */
public class NSPatch {
    private static int CACHED_WAVEFORM_LEN = 512;

    public String identifier = "";
    public final NSWaveform volumeWaveform = new NSWaveform();
    public final NSWaveform mainWaveform = new NSWaveform();
    public int attackMs = 1000;
    public int decayMs = 1000;

    private final float[] mainWaveformCache = new float[CACHED_WAVEFORM_LEN];
    private volatile boolean cacheDirty = true;

    public float[] getMainWaveform() {
        if (cacheDirty)
            regenCaches();
        return mainWaveformCache;
    }

    public synchronized void markCachesDirty() {
        cacheDirty = true;
    }

    public synchronized void regenCaches() {
        // make sure only one thread is writing here at a time
        cacheDirty = false;
        CurvePlotter.resolve(mainWaveform, mainWaveformCache, true);
    }
}
