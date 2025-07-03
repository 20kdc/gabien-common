/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp.newsynth;

import datum.DatumReaderTokenSource;
import datum.DatumWriter;
import gabien.media.midi.MIDISynthesizer;
import gabien.media.midi.MIDISynthesizer.Channel;
import gabien.media.midi.MIDISynthesizer.Palette;
import gabien.media.midi.newsynth.NSChannel;
import gabien.media.midi.newsynth.NSPatch;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UINumberBox;
import gabien.ui.elements.UITextBox;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UIScrollLayout;
import gabien.ui.layouts.UISplitterLayout;
import gabienapp.UIMIDIPlayer;
import gabienapp.UIMainMenu;

/**
 * Created 2nd July, 2025
 */
public class UINSPatchEditor extends UIProxy {
    private NSPatch patch;
    public UIMainMenu menu;
    public final UITextButton openMidi = new UITextButton("single instrument melody player", 16, () -> {
        menu.ui.accept(new UIMIDIPlayer(new Palette() {
            @Override
            public Channel create(MIDISynthesizer parent, int bank, int program, int note, int velocity) {
                if (bank >= 128)
                    return null;
                return new NSChannel(patch);
            }
        }));
    });
    public final UITextButton bonk = new UITextButton("bonk", 16, () -> {
    });
    public final UITextBox datumScript = new UITextBox("", 8);
    public final UITextBox patchName = new UITextBox("", 24);
    public final UINumberBox strikeBox = new UINumberBox(0, 16);
    public final UINumberBox releaseBox = new UINumberBox(0, 16);
    public final UITextButton sustainEnabled = new UITextButton("SUSTAIN", 16, this::onAnyInternalChange).togglable(false);
    public final UITextButton noiseEnabled = new UITextButton("NOISE", 16, this::onAnyInternalChange).togglable(false);
    public final Runnable alertParentOfChanges;
    public final UINSWaveformEditor waveform, envelope, pitch;
    public UINSPatchEditor(UIMainMenu menu, NSPatch initPatch, Runnable rebuild) {
        alertParentOfChanges = rebuild;
        this.menu = menu;
        this.patch = initPatch;
        UILabel patchNameLabel = new UILabel("Name: ", 24);
        UISplitterLayout patchNameAndLabel = new UISplitterLayout(patchNameLabel, patchName, false, 0);
        patchName.setText(patch.name);
        patchName.onEdit = this::onAnyInternalChange;

        waveform = new UINSWaveformEditor(400, 200, patch.mainWaveform);
        waveform.onWaveformChange = this::onAnyInternalChange;

        envelope = new UINSWaveformEditor(400, 200, patch.volumeWaveform);
        envelope.normalized = false;
        envelope.onWaveformChange = this::onAnyInternalChange;

        pitch = new UINSWaveformEditor(400, 200, patch.pitchEnvWaveform);
        pitch.normalized = false;
        pitch.onWaveformChange = this::onAnyInternalChange;

        strikeBox.onEdit = this::onAnyInternalChange;
        releaseBox.onEdit = this::onAnyInternalChange;

        UISplitterLayout envAttrStrike = new UISplitterLayout(new UILabel(" Strike (2nd quarter) ms. ", 16), strikeBox, false, 0);
        UISplitterLayout envAttrRelease = new UISplitterLayout(new UILabel(" Release (3nd quarter) ms. ", 16), releaseBox, false, 0);
        UIScrollLayout waveAttributes = new UIScrollLayout(false, 16, noiseEnabled);
        UIScrollLayout envelopeAttributes = new UIScrollLayout(false, 16, envAttrStrike, envAttrRelease, sustainEnabled);
        UIScrollLayout pitchAttributes = new UIScrollLayout(false, 16);
        UIScrollLayout waveColumn = new UIScrollLayout(true, 24,
                new UILabel("WAVE", 16).centred(), waveform, waveAttributes,
                new UILabel("ENVELOPE", 16).centred(), envelope, envelopeAttributes,
                new UILabel("PITCH-ENV", 16).centred(), pitch, pitchAttributes);
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
            onInternalChangeNoCopyBoxValues();
        };
        onAnyExternalChange();
    }

    public void setPatch(NSPatch patch) {
        this.patch = patch;
        waveform.setWaveform(patch.mainWaveform);
        envelope.setWaveform(patch.volumeWaveform);
        pitch.setWaveform(patch.pitchEnvWaveform);
        onAnyExternalChange();
    }

    public NSPatch getPatch() {
        return patch;
    }

    public void onAnyInternalChange() {
        // update patch values
        patch.name = patchName.getText();
        patch.strikeMs = (int) strikeBox.getNumber();
        patch.releaseMs = (int) releaseBox.getNumber();
        patch.sustainEnabled = sustainEnabled.state;
        patch.noiseEnabled = noiseEnabled.state;
        onInternalChangeNoCopyBoxValues();
    }

    public void onInternalChangeNoCopyBoxValues() {
        // mark dirty!
        patch.markCachesDirty();
        // now update other components
        alertParentOfChanges.run();
        onAnyExternalChange();
    }

    public void onAnyExternalChange() {
        patchName.setText(patch.name);
        strikeBox.setNumber(patch.strikeMs);
        releaseBox.setNumber(patch.releaseMs);
        sustainEnabled.state = patch.sustainEnabled;
        noiseEnabled.state = patch.noiseEnabled;
        waveform.normalized = !patch.noiseEnabled;
        StringBuilder sb = new StringBuilder();
        DatumWriter dw = new DatumWriter(sb);
        patch.writeToDatum(dw);
        datumScript.setText(sb.toString());
    }
}
