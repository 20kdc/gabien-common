/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp.newsynth;

import datum.DatumReaderTokenSource;
import datum.DatumWriter;
import gabien.media.midi.newsynth.NSPatch;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UINumberBox;
import gabien.ui.elements.UITextBox;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UIScrollLayout;
import gabien.ui.layouts.UISplitterLayout;

/**
 * Created 2nd July, 2025
 */
public class UINSPatchEditor extends UIProxy {
    public final NSPatch patch;
    public final UITextBox datumScript = new UITextBox("", 8);
    public final UINumberBox strikeBox = new UINumberBox(0, 16);
    public final UINumberBox releaseBox = new UINumberBox(0, 16);
    public final UITextButton sustainEnabled = new UITextButton("SUSTAIN", 16, this::onAnyInternalChange).togglable(false);
    public UINSPatchEditor(NSPatch patch) {
        this.patch = patch;
        UILabel patchNameLabel = new UILabel("Name: ", 24);
        UITextBox patchName = new UITextBox("", 24);
        UISplitterLayout patchNameAndLabel = new UISplitterLayout(patchNameLabel, patchName, false, 0);
        patchName.setText(patch.name);
        patchName.onEdit = () -> {
            patch.name = patchName.getText();
            onAnyInternalChange();
        };

        UINSWaveformEditor waveform = new UINSWaveformEditor(400, 200, patch.mainWaveform);
        waveform.onWaveformChange = this::onAnyInternalChange;

        UINSWaveformEditor envelope = new UINSWaveformEditor(400, 200, patch.volumeWaveform);
        envelope.normalized = false;
        envelope.onWaveformChange = this::onAnyInternalChange;

        UINSWaveformEditor pitch = new UINSWaveformEditor(400, 200, patch.pitchEnvWaveform);
        pitch.normalized = false;
        pitch.onWaveformChange = this::onAnyInternalChange;

        strikeBox.onEdit = this::onAnyInternalChange;
        releaseBox.onEdit = this::onAnyInternalChange;

        UISplitterLayout envAttrStrike = new UISplitterLayout(new UILabel(" Strike (2nd quarter) ms. ", 16), strikeBox, false, 0);
        UISplitterLayout envAttrRelease = new UISplitterLayout(new UILabel(" Release (3nd quarter) ms. ", 16), releaseBox, false, 0);
        UIScrollLayout envelopeAttributes = new UIScrollLayout(false, 16, envAttrStrike, envAttrRelease, sustainEnabled);
        UIScrollLayout waveColumn = new UIScrollLayout(true, 24,
                new UILabel("WAVE", 16).centred(), waveform,
                new UILabel("ENVELOPE", 16).centred(), envelope, envelopeAttributes,
                new UILabel("PITCH-ENV", 16).centred(), pitch);
        UIScrollLayout checkColumn = new UIScrollLayout(true, 24,
                new UILabel("IMPORT/EXPORT", 16).centred(), datumScript);
        proxySetElement(new UISplitterLayout(patchNameAndLabel, new UISplitterLayout(waveColumn, checkColumn, false, 1), true, 0), true);

        datumScript.onEdit = () -> {
            try {
                DatumReaderTokenSource drts = new DatumReaderTokenSource("prompt", datumScript.getText());
                drts.visit(patch.createDatumReadVisitor());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            onAnyInternalChange();
        };
        onAnyExternalChange();
    }

    public void onAnyInternalChange() {
        patch.markCachesDirty();
        patch.strikeMs = (int) strikeBox.getNumber();
        patch.releaseMs = (int) releaseBox.getNumber();
        patch.sustainEnabled = sustainEnabled.state;
        onAnyExternalChange();
    }

    public void onAnyExternalChange() {
        strikeBox.setNumber(patch.strikeMs);
        releaseBox.setNumber(patch.releaseMs);
        sustainEnabled.state = patch.sustainEnabled;
        StringBuilder sb = new StringBuilder();
        DatumWriter dw = new DatumWriter(sb);
        patch.writeToDatum(dw);
        datumScript.setText(sb.toString());
    }
}
