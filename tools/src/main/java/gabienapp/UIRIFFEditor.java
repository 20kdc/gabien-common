/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.function.Consumer;

import gabien.GaBIEn;
import gabien.media.riff.RIFFNode;
import gabien.ui.UIElement;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UITextBox;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UIScrollLayout;
import gabien.ui.layouts.UISplitterLayout;
import gabien.uslx.append.Rect;

/**
 * Created 19th June, 2023.
 */
public class UIRIFFEditor extends UIProxy {
    public final UIScrollLayout mainPanel = new UIScrollLayout(true, 16);
    public final UIMainMenu menu;
    RIFFNode node = new RIFFNode.CList("RIFF", "TEST");
    public UIRIFFEditor(UIMainMenu p) {
        menu = p;
        UIScrollLayout menuBar = new UIScrollLayout(false, 8, new UITextButton("Open RIFF", 16, () -> {
            GaBIEn.startFileBrowser("Open RIFF", false, "", (res) -> {
                if (res != null) {
                    try (InputStream inp = GaBIEn.getInFileOrThrow(res)) {
                        node = RIFFNode.read(inp);
                        regenerateContents();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }), new UILabel(" ", 16), new UITextButton("Save", 16, () -> {
            GaBIEn.startFileBrowser("Save RIFF", true, "", (res) -> {
                if (res != null) {
                    try (OutputStream oup = GaBIEn.getOutFileOrThrow(res)) {
                        node.write(oup);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }));
        UISplitterLayout menuBarAndMainPanel = new UISplitterLayout(menuBar, mainPanel, true, 0);
        regenerateContents();
        menuBarAndMainPanel.setForcedBounds(null, new Rect(0, 0, 640, 480));
        proxySetElement(menuBarAndMainPanel, false);
    }
    public void regenerateContents() {
        LinkedList<UIElement> uie = new LinkedList<>();
        addNode(uie, "", null, node);
        mainPanel.panelsSet(uie);
    }
    public void addNode(LinkedList<UIElement> uie, String indent, RIFFNode.CList parent, RIFFNode node) {
        UIElement indentC = new UILabel(indent, 16);
        UIElement controls = new UITextButton("Copy", 16, () -> {
            menu.copyRIFF(node);
        }); 
        UIElement chunkDetail;
        UITextBox chunkIdEditor = new UITextBox(node.chunkId, 16);
        chunkIdEditor.onEdit = cidEditor(chunkIdEditor, (s) -> node.chunkId = s);
        if (node instanceof RIFFNode.CData) {
            final RIFFNode.CData cd = (RIFFNode.CData) node;
            UIElement titleInnard = new UISplitterLayout(chunkIdEditor, new UITextButton("...", 16, () -> {
                menu.ui.accept(new UIBytesEditor(cd.contents, (b) -> {
                    cd.contents = b;
                    regenerateContents();
                }));
            }), false, 0);
            UIElement title = new UISplitterLayout(indentC, titleInnard, false, 0);
            chunkDetail = new UISplitterLayout(title, new UILabel(" " + cd.contents.length + " bytes", 16), false, 0);
        } else if (node instanceof RIFFNode.CList) {
            final RIFFNode.CList cd = (RIFFNode.CList) node;
            UIElement listControls = new UILabel("OT", 16);
            UITextBox subChunkIdEditor = new UITextBox(cd.subChunkId, 16);
            subChunkIdEditor.onEdit = cidEditor(subChunkIdEditor, (s) -> cd.subChunkId = s);
            UIElement csc = new UISplitterLayout(chunkIdEditor, subChunkIdEditor, false, 0.5d);
            UIElement titleInnard = new UISplitterLayout(csc, new UILabel(" " + cd.contents.size() + " entries", 16), false, 0);
            UIElement title = new UISplitterLayout(indentC, titleInnard, false, 0);
            chunkDetail = new UISplitterLayout(title, listControls, false, 1);
        } else {
            chunkDetail = new UILabel("?: " + node.chunkId, 16);
        }
        if (parent != null) {
            controls = new UISplitterLayout(controls, new UITextButton("-", 16, () -> {
                parent.contents.remove(node);
                regenerateContents();
            }), false, 1);
        }
        uie.add(new UISplitterLayout(chunkDetail, controls, false, 1));
        if (node instanceof RIFFNode.CList) {
            final RIFFNode.CList cd = (RIFFNode.CList) node;
            int idx = 0;
            for (RIFFNode ch : cd.contents) {
                addPasteButton(uie, " " + indent, cd, idx++);
                addNode(uie, " " + indent, cd, ch);
            }
            addPasteButton(uie, " " + indent, cd, idx);
        }
    }
    public Runnable cidEditor(UITextBox tb, Consumer<String> res) {
        return () -> {
            char[] tmp = new char[4];
            String tbt = tb.getText();
            int l = tbt.length();
            if (l > 4)
                l = 4;
            tbt.getChars(0, l, tmp, 0);
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] &= 0xFF;
                // could be bad, but what can you do
                if (tmp[i] < 32 || tmp[i] >= 127)
                    tmp[i] = ' ';
            }
            res.accept(new String(tmp));
            regenerateContents();
        };
    }
    public void addPasteButton(LinkedList<UIElement> uie, String indent, RIFFNode.CList parent, int index) {
        UIElement indent2 = new UILabel(indent, 16);
        UIElement pastePtr = new UITextButton("Paste", 16, () -> {
            if (menu.riffClipboard != null) {
                parent.contents.add(index, menu.riffClipboard.copy());
                regenerateContents();
            }
        });
        pastePtr = new UISplitterLayout(pastePtr, new UILabel(" ", 16), false, 0);
        pastePtr = new UISplitterLayout(pastePtr, new UITextButton("Add CData", 16, () -> {
            parent.contents.add(index, new RIFFNode.CData());
            regenerateContents();
        }), false, 0);
        pastePtr = new UISplitterLayout(pastePtr, new UILabel(" ", 16), false, 0);
        pastePtr = new UISplitterLayout(pastePtr, new UITextButton("CList", 16, () -> {
            parent.contents.add(index, new RIFFNode.CList("LIST", "test"));
            regenerateContents();
        }), false, 0);
        pastePtr = new UISplitterLayout(pastePtr, new UILabel(" ", 16), false, 0);
        uie.add(new UISplitterLayout(indent2, pastePtr, false, 0));
    }
}
