/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp.newsynth;

import gabien.ui.UIElement.UIProxy;

/**
 * Created 2nd July, 2025
 */
public class UINSPatchEditor extends UIProxy {
    public UINSPatchEditor(NSPatch patch) {
        UINSWaveformEditor waveform = new UINSWaveformEditor(800, 600, patch.mainWaveform);
        waveform.onWaveformChange = () -> patch.markCachesDirty();
        proxySetElement(waveform, false);
    }
}
