/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import gabien.ui.UIScrollLayout;
import gabien.ui.UITextButton;
import gabien.ui.WindowCreatingUIElementConsumer;
import gabien.ui.dialogs.UICredits;
import gabien.uslx.append.EmptyLambdas;
import gabien.uslx.append.QADStopwatch;
import gabien.uslx.append.Rect;
import gabien.uslx.io.LEDataOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNull;

import gabien.GaBIEn;
import gabien.GaBIEnUI;
import gabien.atlas.AtlasSet;
import gabien.atlas.BinaryTreeAtlasStrategy;
import gabien.atlas.ImageAtlasDrawable;
import gabien.atlas.SimpleAtlasBuilder;
import gabien.media.audio.AudioIOCRSet;
import gabien.media.audio.AudioIOFormat;
import gabien.media.audio.AudioIOSource;
import gabien.media.audio.fileio.WavIO;
import gabien.media.ogg.OggPacketsFromSegments;
import gabien.media.ogg.OggReader;
import gabien.media.riff.RIFFNode;
import gabien.natives.VorbisDecoder;
import gabien.pva.PVAFile;
import gabien.render.IGrDriver;
import gabien.render.ITexRegion;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.UILabel;
import gabien.ui.UIPublicPanel;

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
        vsl.panelsAdd(new UITextButton("Open PVA File...", 16, () -> {
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
        vsl.panelsAdd(new UITextButton("Convert OGG Vorbis To WAV...", 16, () -> {
            GaBIEn.startFileBrowser("Convert OGG Vorbis File", false, "", (str) -> {
                if (str != null) {
                    try {
                        InputStream inp = GaBIEn.getInFile(str);
                        OggReader or = new OggReader();
                        LinkedList<byte[]> packets = new LinkedList<>();
                        OggPacketsFromSegments opfs = new OggPacketsFromSegments((data, ofs, len) -> {
                            byte[] res = new byte[len];
                            System.arraycopy(data, ofs, res, 0, len);
                            packets.add(res);
                        });
                        try (QADStopwatch profile = new QADStopwatch("ogg sync loop")) {
                            byte[] chunk = new byte[512];
                            while (true) {
                                int amount = inp.read(chunk);
                                if (amount == -1)
                                    break;
                                for (byte ib : chunk) {
                                    or.addByteToSyncWindow(ib);
                                    if (or.isPageValid()) {
                                        or.sendSegmentsTo(opfs, false);
                                        or.skipSyncWindow(or.getPageLength());
                                    }
                                }
                            }
                        }
                        inp.close();
                        byte[] id = packets.removeFirst();
                        packets.removeFirst();
                        byte[] setup = packets.removeFirst();
                        ByteArrayOutputStream baosTmp = new ByteArrayOutputStream();
                        AudioIOCRSet crSet;
                        final int channels;
                        try (VorbisDecoder res = new VorbisDecoder(id, 0, id.length, setup, 0, setup.length)) {
                            channels = res.channels;
                            crSet = new AudioIOCRSet(channels, res.sampleRate);
                            float[] resBuf = new float[res.outputLength];
                            LEDataOutputStream xe = new LEDataOutputStream(baosTmp);
                            try (QADStopwatch profile = new QADStopwatch("vorbis decode loop")) {
                                while (packets.size() > 0) {
                                    byte[] pkt = packets.removeFirst();
                                    int sampleFrames = res.decodeFrame(pkt, 0, pkt.length, resBuf, 0);
                                    int total = sampleFrames * channels;
                                    for (int i = 0; i < total; i++)
                                        xe.writeFloat(resBuf[i]);
                                }
                            }
                        }
                        OutputStream os = GaBIEn.getOutFile("tmp.wav");
                        byte[] baosFin = baosTmp.toByteArray();
                        WavIO.writeWAV(os, new AudioIOSource.SourceBytes(crSet, AudioIOFormat.F_F32) {
                            int ptr = 0;

                            @Override
                            public void nextFrames(@NonNull byte[] frame, int at, int frames) throws IOException {
                                System.arraycopy(baosFin, ptr, frame, at, channels * 4 * frames);
                                ptr += channels * 4 * frames;
                            }

                            @Override
                            public int frameCount() {
                                return baosFin.length / (channels * 4);
                            }
                        }, AudioIOFormat.F_F32);
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }));
        vsl.panelsAdd(new UITextButton("Start RIFF Editor", 16, () -> {
            ui.accept(new UIRIFFEditor(this));
        }));
        vsl.panelsAdd(new UITextButton("Start Atlasing Tester (flashing lights ahead)", 16, () -> {
            ui.accept(new UIAtlasTester());
        }));
        vsl.panelsAdd(new UITextButton("Compile Sphere Atlases", 16, () -> {
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
        vsl.panelsAdd(new UITextButton("GaBIEn Credits", 16, () -> {
            UICredits uic = new UICredits(16, 16);
            uic.setLAFParentOverride(GaBIEnUI.sysThemeRoot);
            ui.accept(uic);
        }));
        vsl.panelsAdd(lbl);
        setForcedBounds(null, new Rect(0, 0, 640, 480));
        setLAFParentOverride(GaBIEnUI.sysThemeRoot);
    }
    public void copyRIFF(RIFFNode rn) {
        riffClipboard = rn.copy();
        lbl.text = "RIFF Clipboard: " + rn.chunkId;
    }
}
