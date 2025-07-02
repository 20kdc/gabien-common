/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp.newsynth;

import gabien.media.midi.MIDISynthesizer;
import gabien.media.midi.MIDISynthesizer.Channel;
import gabien.media.midi.MIDISynthesizer.Palette;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UISplitterLayout;
import gabienapp.UIMIDIPlayer;
import gabienapp.UIMainMenu;

/**
 * Created 2nd July, 2025
 */
public class UINewSynthEditor extends UIProxy {
    private UIMainMenu menu = null;
    private NSPatch patch = new NSPatch();
    private UITextButton openMidi = new UITextButton("open MIDI player", 16, () -> {
        menu.ui.accept(new UIMIDIPlayer(new Palette() {
            @Override
            public Channel create(MIDISynthesizer parent, int bank, int program, int note, int velocity) {
                if (bank >= 128)
                    return null;
                return new NSChannel(0.5f, 0.5f, patch, false, false);
            }
        }));
    });
    public UINewSynthEditor(UIMainMenu menu) {
        this.menu = menu;
        proxySetElement(new UISplitterLayout(openMidi, new UINSPatchEditor(patch), true, 0), true);
    }
}
