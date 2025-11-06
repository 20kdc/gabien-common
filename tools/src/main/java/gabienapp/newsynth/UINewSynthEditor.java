/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp.newsynth;

import java.util.LinkedList;

import gabien.media.midi.newsynth.NSPatch;
import gabien.ui.UIElement;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UIScrollLayout;
import gabien.ui.layouts.UISplitterLayout;
import gabienapp.UIMainMenu;

/**
 * Created 2nd July, 2025
 */
public class UINewSynthEditor extends UIProxy {
    public final UIScrollLayout patchen = new UIScrollLayout(true, 24);
    public final UIScrollLayout ilokalamapinanpa = new UIScrollLayout(true, 24);
    private final UIMainMenu menu;
    private final UINSPatchEditor patchEditor;
    public UINewSynthEditor(UIMainMenu menu) {
        this.menu = menu;
        if (menu.newSynthPalette.patches.isEmpty()) {
            NSPatch init1 = new NSPatch();
            NSPatch init2 = new NSPatch();
            init1.name = "melody";
            init2.name = "percussion";
            menu.newSynthPalette.patches.add(init1);
            menu.newSynthPalette.patches.add(init2);
            for (int i = 0; i < 128; i++) {
                menu.newSynthPalette.programList[i] = init1;
                menu.newSynthPalette.programList[i + 128] = init2;
            }
        }
        patchEditor = new UINSPatchEditor(menu, menu.newSynthPalette.patches.getFirst(), this::rebuild);
        proxySetElement(new UISplitterLayout(patchen, new UISplitterLayout(ilokalamapinanpa, patchEditor, false, 0), false, 0), true);
        rebuild();
    }

    void rebuild() {
        LinkedList<UIElement> contents = new LinkedList<>();
        contents.add(new UILabel(" - patches - ", 16));
        menu.newSynthPalette.patches.sort((a, b) -> {
            return a.name.compareTo(b.name);
        });
        for (NSPatch nsp : menu.newSynthPalette.patches) {
            contents.add(new UITextButton(nsp.name, 16, () -> {
                patchEditor.setPatch(nsp);
                rebuild();
            }).togglable(patchEditor.getPatch() == nsp));
        }
        contents.add(new UITextButton("<new patch>", 16, () -> {
            NSPatch np = new NSPatch();
            patchEditor.setPatch(np);
            menu.newSynthPalette.patches.add(np);
            rebuild();
        }));
        patchen.panelsSet(contents);
        contents.clear();
        contents.add(new UILabel(" - programs - ", 16));
        for (int i = 0; i < 256; i++) {
            String ugly;
            if (i >= 128) {
                ugly = Integer.toString(i - 128);
                while (ugly.length() < 3)
                    ugly = "0" + ugly;
                ugly = "p" + ugly;
            } else {
                ugly = Integer.toHexString(i);
                if (ugly.length() == 1)
                    ugly = "0" + ugly;
            }
            NSPatch patch = menu.newSynthPalette.programList[i];
            if (patch != null) {
                ugly += "\n" + patch.name;
            } else {
                ugly += "\n" + "<none>";
            }
            final int index = i;
            contents.add(new UITextButton(ugly, 16, () -> {
                menu.newSynthPalette.programList[index] = patchEditor.getPatch();
                rebuild();
            }));
        }
        ilokalamapinanpa.panelsSet(contents);
        menu.saveNewSynth();
    }
}
