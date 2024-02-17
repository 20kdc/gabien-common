/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.audio.IRawAudioDriver.IRawAudioSource;
import gabien.media.audio.AudioIOCRSet;
import gabien.media.audio.AudioIOFormat;
import gabien.media.audio.AudioIOSample;
import gabien.media.audio.AudioIOSource;
import gabien.ui.UIDynamicProxy;
import gabien.ui.UIElement;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UITextButton;
import gabien.ui.elements.UIThumbnail;
import gabien.ui.layouts.UISplitterLayout;
import gabien.uslx.io.HexByteEncoding;
import gabien.wsi.IPeripherals;

/**
 * Got to get the levels right
 * Created 17th February, 2024.
 */
public class UIMIDIInstrumentChecker extends UIDynamicProxy {
    public final int program;

    private @Nullable Thread rebuilderThread;
    private volatile MIDIInstrumentCheckerProcessor rebuilderThreadSavedResult;

    public UIMIDIInstrumentChecker(int program) {
        this.program = program;
        startRebuild();
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        if (rebuilderThread != null) {
            if (!rebuilderThread.isAlive()) {
                try {
                    rebuilderThread.join();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                rebuilderThread = null;
                UIElement elm = new UIThumbnail(rebuilderThreadSavedResult.resultDrawable);
                UIElement label = new UITextButton(HexByteEncoding.toHexString(program), 32, this::startRebuild);
                UIElement cL = new UITextButton("R", 32, () -> {
                    launchSample(rebuilderThreadSavedResult.sampleReference);
                });
                UIElement cR = new UITextButton("G", 32, () -> {
                    launchSample(rebuilderThreadSavedResult.sampleResult);
                });
                UIElement cLR = new UISplitterLayout(cL, cR, false, 0.5d);
                UIElement cPanel = new UISplitterLayout(label, cLR, true, 0);
                elm = new UISplitterLayout(cPanel, elm, false, 0);
                dynProxySet(elm);
            }
        }
        super.update(deltaTime, selected, peripherals);
    }

    private void launchSample(float[] data) {
        AudioIOSample sample;
        try {
            sample = new AudioIOSample(new AudioIOSource.SourceF32(new AudioIOCRSet(2, MIDIInstrumentCheckerProcessor.SAMPLE_RATE)) {
                int srcPtr = 0;
                @Override
                public void nextFrames(@NonNull float[] frame, int at, int frames) throws IOException {
                    while (frames > 0) {
                        frame[at++] = data[srcPtr++];
                        frame[at++] = data[srcPtr++];
                        frames--;
                    }
                }
                
                @Override
                public int frameCount() {
                    return data.length / 2;
                }
            }, AudioIOFormat.F_F32);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        GaBIEn.getRawAudio().setRawAudioSource(new IRawAudioSource() {
            int srcFrame = 0;
            float[] tmp = new float[2];
            @Override
            public void pullData(@NonNull short[] interleaved, int ofs, int frames) {
                while (frames > 0) {
                    sample.getInterpolatedF32(srcFrame, tmp, false);
                    interleaved[ofs++] = (short) (AudioIOFormat.cF64toS32(tmp[0]) >> 16);
                    interleaved[ofs++] = (short) (AudioIOFormat.cF64toS32(tmp[1]) >> 16);
                    srcFrame += 2;
                    frames--;
                }
            }
        });
    }

    private void startRebuild() {
        dynProxySet(new UILabel("Please wait...", 16));
        if (rebuilderThread != null) {
            try {
                rebuilderThread.join();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        rebuilderThread = new Thread(() -> {
            rebuilderThreadSavedResult = new MIDIInstrumentCheckerProcessor(program);
        });
        rebuilderThread.start();
    }
}
