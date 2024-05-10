/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import gabien.ui.WindowCreatingUIElementConsumer;
import gabien.ui.dialogs.UICredits;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UIPublicPanel;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UIFlowLayout;
import gabien.ui.layouts.UIScrollLayout;
import gabien.uslx.append.EmptyLambdas;
import gabien.uslx.append.Rect;
import gabien.uslx.io.HexByteEncoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import gabien.GaBIEn;
import gabien.GaBIEnUI;
import gabien.atlas.AtlasSet;
import gabien.atlas.BinaryTreeAtlasStrategy;
import gabien.atlas.ImageAtlasDrawable;
import gabien.atlas.SimpleAtlasBuilder;
import gabien.media.audio.AudioIOFormat;
import gabien.media.audio.fileio.ReadAnySupportedAudioSource;
import gabien.media.audio.fileio.WavIO;
import gabien.media.midi.MIDISequence;
import gabien.media.midi.MIDITracker;
import gabien.media.riff.RIFFNode;
import gabien.pva.PVAFile;
import gabien.render.IGrDriver;
import gabien.render.ITexRegion;
import gabien.ui.UIElement;
import gabien.ui.UIElement.UIProxy;

/**
 * Main menu!
 * Created 19th June, 2023.
 */
public class UIMainMenu extends UIProxy {
    public final UIScrollLayout vsl = new UIScrollLayout(true, 16);
    public final UILabel lbl = new UILabel("RIFF Clipboard: (none!)", 16);
    public RIFFNode riffClipboard;
    public WindowCreatingUIElementConsumer ui;
    public UIMainMenu(WindowCreatingUIElementConsumer ui) {
        this.ui = ui;
        proxySetElement(vsl, false);
        LinkedList<UIElement> ve = new LinkedList<>();
        ve.add(new UITextButton("Open PVA File...", 16, () -> {
            GaBIEn.startFileBrowser("Open PVA File", false, "", (str) -> {
                if (str != null) {
                    try {
                        PVAFile pf = new PVAFile(GaBIEn.getInFile(str), false);
                        ui.accept(new UIPVAViewer(pf));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }));
        ve.add(new UITextButton("Convert To WAV...", 16, () -> {
            GaBIEn.startFileBrowser("Convert Audio File", false, "", (str) -> {
                if (str != null) {
                    try {
                        InputStream inp = GaBIEn.getInFileOrThrow(str);
                        OutputStream os = GaBIEn.getOutFileOrThrow("tmp.wav");
                        WavIO.writeWAV(os, ReadAnySupportedAudioSource.open(inp, true), AudioIOFormat.F_F32);
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }));
        ve.add(new UITextButton("MIDI to TXT", 16, () -> {
            GaBIEn.startFileBrowser("Convert from MIDI", false, "", (str) -> {
                if (str != null) {
                    try {
                        InputStream inp = GaBIEn.getInFile(str);
                        MIDISequence mf = MIDISequence.from(inp)[0];
                        OutputStream os = GaBIEn.getOutFile("tmp.txt");
                        AtomicInteger time = new AtomicInteger(0);
                        MIDITracker mt = new MIDITracker(mf, (status, data, offset, length) -> {
                            String res = time.get() + ": " + HexByteEncoding.toHexString(status) + ":" + HexByteEncoding.toHexString(data, offset, length) + ": ";
                            try {
                                os.write(res.getBytes(StandardCharsets.UTF_8));
                                os.write(data, offset, length);
                                os.write(10);
                            } catch (IOException ioe) {
                                // :(
                                ioe.printStackTrace();
                            }
                        });
                        while (true) {
                            int ticks = mt.getTickOfNextEvent();
                            if (ticks == -1)
                                break;
                            time.set(ticks);
                            mt.runNextEvent();
                        }
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }));
        ve.add(new UITextButton("MIDI Testing Range", 16, () -> {
            UIMIDIPlayer player = new UIMIDIPlayer();
            ui.accept(player);
        }));
        ve.add(new UITextButton("MIDI Instrument Checker", 16, () -> {
            UIElement[] check = new UIElement[128];
            for (int i = 0; i < 128; i++)
                check[i] = new UIMIDIInstrumentChecker(i);
            ui.accept(new UIScrollLayout(true, 16, check));
        }));
        ve.add(new UITextButton("Start RIFF Editor", 16, () -> {
            ui.accept(new UIRIFFEditor(this));
        }));
        ve.add(new UITextButton("Start Atlasing Tester (flashing lights ahead)", 16, () -> {
            ui.accept(new UIAtlasTester());
        }));
        ve.add(new UITextButton("Compile Sphere Atlases", 16, () -> {
            SimpleAtlasBuilder sab = new SimpleAtlasBuilder(512, 512, BinaryTreeAtlasStrategy.INSTANCE);
            Consumer<ITexRegion> itr = EmptyLambdas.emptyConsumer();
            sab.add(itr, new ImageAtlasDrawable(GaBIEn.getImage("sphere32.png")));
            for (int i = 0; i < 32; i++)
                sab.add(itr, new ImageAtlasDrawable(GaBIEn.getImage("sphere64.png")));
            sab.add(itr, new ImageAtlasDrawable(GaBIEn.getImage("sphere128.png")));
            sab.add(itr, new ImageAtlasDrawable(GaBIEn.getImage("sphere256.png")));
            sab.add(itr, new ImageAtlasDrawable(GaBIEn.getImage("sphere512.png")));
            AtlasSet as = sab.compile();
            for (IGrDriver ap : as.pages) {
                UIPublicPanel upp = new UIPublicPanel(ap.width, ap.height);
                upp.baseImage = ap;
                upp.imageSW = ap.width;
                upp.imageSH = ap.height;
                ui.accept(upp);
            }
        }));
        ve.add(new UITextButton("GaBIEn Credits", 16, () -> {
            UICredits uic = new UICredits(16, 16);
            uic.setLAFParent(GaBIEnUI.sysThemeRoot);
            ui.accept(uic);
        }));
        ve.add(new UITextButton("UIFlowLayout please", 16, () -> {
            UIFlowLayout ufl = new UIFlowLayout(new UILabel("OUA", 48), new UILabel("Central", 32), new UILabel("Office", 16));
            ui.accept(ufl);
        }));
        ve.add(lbl);
        vsl.panelsSet(ve);
        setForcedBounds(null, new Rect(0, 0, 640, 480));
    }
    public void copyRIFF(RIFFNode rn) {
        riffClipboard = rn.copy();
        lbl.setText("RIFF Clipboard: " + rn.chunkId);
    }
}
