/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp.newsynth;

import gabien.ui.UIElement.UIProxy;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UITextBox;
import gabien.ui.layouts.UIScrollLayout;
import gabien.ui.layouts.UISplitterLayout;

/**
 * Created 2nd July, 2025
 */
public class UINSPatchEditor extends UIProxy {
    public UINSPatchEditor(NSPatch patch) {
        UILabel patchNameLabel = new UILabel("Name: ", 24);
        UITextBox patchName = new UITextBox("", 24);
        UISplitterLayout patchNameAndLabel = new UISplitterLayout(patchNameLabel, patchName, false, 0);
        patchName.setText(patch.identifier);
        patchName.onEdit = () -> patch.identifier = patchName.getText();

        UINSWaveformEditor waveform = new UINSWaveformEditor(400, 200, patch.mainWaveform);
        waveform.onWaveformChange = () -> patch.markCachesDirty();

        UINSWaveformEditor envelope = new UINSWaveformEditor(400, 200, patch.volumeWaveform);
        envelope.normalized = false;
        envelope.onWaveformChange = () -> patch.markCachesDirty();

        UINSWaveformEditor pitch = new UINSWaveformEditor(400, 200, patch.pitchEnvWaveform);
        pitch.normalized = false;
        pitch.onWaveformChange = () -> patch.markCachesDirty();

        UIScrollLayout waveColumn = new UIScrollLayout(true, 24, new UILabel("WAVE", 16), waveform, new UILabel("ENVELOPE", 16), envelope, new UILabel("PITCH-ENV", 16), pitch);
        UIScrollLayout checkColumn = new UIScrollLayout(true, 24, new UILabel("CHECK", 16), new UILabel("temp", 16));
        proxySetElement(new UISplitterLayout(patchNameAndLabel, new UISplitterLayout(waveColumn, checkColumn, false, 1), true, 0), true);
    }
}
