/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNull;

import gabien.GaBIEn;
import gabien.audio.IRawAudioDriver.IRawAudioSource;
import gabien.media.audio.AudioIOFormat;
import gabien.media.midi.DefaultMIDIPalette;
import gabien.media.midi.MIDISequence;
import gabien.media.midi.MIDISynthesizer;
import gabien.media.midi.MIDITimer;
import gabien.media.midi.MIDITracker;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.elements.UIScrollbar;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UISplitterLayout;
import gabien.wsi.IPeripherals;

/**
 * MIDI player
 * Created 15th February, 2024.
 */
public class UIMIDIPlayer extends UIProxy {
    public final UITextButton open = new UITextButton("open", 16, () -> {
        GaBIEn.startFileBrowser("Input MIDI", false, "", (str) -> {
            if (str != null) {
                try {
                    InputStream inp = GaBIEn.getInFile(str);
                    MIDISequence mf = MIDISequence.from(inp)[0];
                    GaBIEn.getRawAudio().setRawAudioSource(new TheThingThatDoesTheStuff(mf));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    });
    public final UITextButton play = new UITextButton("play", 16, () -> {}).togglable(false);
    public final UIScrollbar scrollbar;
    public final UIScrollbar volume;
    public double synthViewOfSeekPoint;
    public AtomicReference<Double> uiSeekRequest = new AtomicReference<Double>();

    public UIMIDIPlayer() {
        scrollbar = new UIScrollbar(false, 16) {
            double myViewOfMyValue = 0;
            @Override
            public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
                if (myViewOfMyValue != scrollPoint)
                    uiSeekRequest.set(scrollPoint);
                scrollPoint = myViewOfMyValue = synthViewOfSeekPoint;
                super.update(deltaTime, selected, peripherals);
            }
        };
        volume = new UIScrollbar(false, 16);
        volume.scrollPoint = 1.0d;
        proxySetElement(new UISplitterLayout(new UISplitterLayout(open, play, false, 0), new UISplitterLayout(scrollbar, volume, true, 0.5d), false, 0), true);
    }

    @Override
    public void onWindowClose() {
        GaBIEn.hintShutdownRawAudio();
    }

    private class TheThingThatDoesTheStuff implements IRawAudioSource {
        final MIDISequence sequence;
        final MIDISequence.TimingInformation seqTiming;
        MIDISynthesizer synth = new MIDISynthesizer(22050, DefaultMIDIPalette.INSTANCE, 8);
        MIDITracker tracker;
        MIDITimer timer;
        float[] data = new float[256];
        int dataPtr = 256;
        public TheThingThatDoesTheStuff(MIDISequence seq) {
            sequence = seq;
            seqTiming = seq.calcTimingInformation();
            reinitTrackerAndTimer();
        }
        private void reinitTrackerAndTimer() {
            // System.out.println("Doing reinit");
            synth.resetParameters();
            synth.clear();
            tracker = new MIDITracker(sequence, synth);
            timer = new MIDITimer(tracker);
        }
        @Override
        public void pullData(@NonNull short[] interleaved, int ofs, int frames) {
            // System.out.println("Oooo");
            Double seekRequest = uiSeekRequest.getAndSet(null);
            if (seekRequest != null) {
                dataPtr = data.length;
                reinitTrackerAndTimer();
                int target = seqTiming.secondsToTick(seekRequest * seqTiming.lengthSeconds);
                timer.resolveTick(target - 1);
                synth.clear();
                timer.resolveTick(target);
            }
            if (!play.state) {
                Arrays.fill(interleaved, ofs, ofs + (frames * 2), (short) 0);
                return;
            }
            float vol = (float) volume.scrollPoint;
            while (frames > 0) {
                if (dataPtr == data.length) {
                    Arrays.fill(data, 0);
                    synth.render(data, 0, data.length / 2);
                    dataPtr = 0;
                    double nav = data.length / 44100d;
                    synth.update(nav);
                    timer.currentTime += nav;
                    timer.resolve();
                    if (tracker.getTickOfNextEvent() == -1) {
                        // loop
                        reinitTrackerAndTimer();
                        timer.resolveTick(0);
                    }
                }
                interleaved[ofs++] = (short) (AudioIOFormat.cF64toS32(data[dataPtr++] * vol) >> 16);
                interleaved[ofs++] = (short) (AudioIOFormat.cF64toS32(data[dataPtr++] * vol) >> 16);
                frames--;
            }
            synthViewOfSeekPoint = timer.currentTime / seqTiming.lengthSeconds;
        }
    }
}
