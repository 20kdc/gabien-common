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
import gabien.media.audio.fileio.ReadAnySupportedAudioSource;
import gabien.media.midi.MIDISequence;
import gabien.media.midi.MIDISynthesizer;
import gabien.media.midi.MIDITimer;
import gabien.media.midi.MIDITracker;
import gabien.ui.UIElement;
import gabien.ui.UIElement.UIProxy;
import gabien.ui.elements.UILabel;
import gabien.ui.elements.UIScrollbar;
import gabien.ui.elements.UITextButton;
import gabien.ui.layouts.UIScrollLayout;
import gabien.ui.layouts.UISplitterLayout;
import gabien.wsi.IPeripherals;

/**
 * MIDI player
 * Created 15th February, 2024.
 */
public class UIMIDIPlayer extends UIProxy {
    public final UITextButton open = new UITextButton("open", 32, () -> {
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
    public final UITextButton play = new UITextButton("play", 32, () -> {}).togglable(false);
    public final UIScrollbar scrollbar;
    public final UIScrollbar volume;
    public double synthViewOfSeekPoint;
    public AtomicReference<Double> uiSeekRequest = new AtomicReference<Double>();
    private final MIDISynthesizer.Palette palette;
    private final UITextButton[] channels = new UITextButton[16];
    private final UILabel programSummary = new UILabel("program summary", 16);
    String programSummaryShunt = "";

    public UIMIDIPlayer(MIDISynthesizer.Palette palette) {
        this.palette = palette;
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
        volume.scrollPoint = MIDISynthesizer.DEFAULT_GLOBAL_VOLUME;
        UIElement mainControls = new UISplitterLayout(new UISplitterLayout(open, play, false, 0), new UISplitterLayout(scrollbar, volume, true, 0.5d), false, 0);

        for (int i = 0; i < 16; i++)
            channels[i] = new UITextButton(Integer.toHexString(i).toUpperCase(), 32, () -> {}).togglable(true);
        UIScrollLayout channelsSc = new UIScrollLayout(false, 16, channels);

        proxySetElement(new UISplitterLayout(mainControls, new UISplitterLayout(channelsSc, programSummary, true, 0), true, 0), true);
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        super.update(deltaTime, selected, peripherals);
        programSummary.setText(programSummaryShunt);
    }

    @Override
    public void onWindowClose() {
        GaBIEn.hintShutdownRawAudio();
    }

    private class TheThingThatDoesTheStuff implements IRawAudioSource {
        final MIDISequence sequence;
        final MIDISequence.TimingInformation seqTiming;
        MIDISynthesizer synth = new MIDISynthesizer(22050, palette, ReadAnySupportedAudioSource.MIDI_POLYPHONY);
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
            synth.channelEnableSwitches = 0;
            StringBuilder programSummaryBuilder = new StringBuilder();
            for (int i = 0; i < 16; i++) {
                if (channels[i].state)
                    synth.channelEnableSwitches |= 1 << i;
                int calcVal = synth.midiChannels[i].program;
                if (synth.midiChannels[i].bank >= 128)
                    calcVal += 128;
                programSummaryBuilder.append(Integer.toHexString(i).toUpperCase() + ": ");
                String calcX = Integer.toHexString(calcVal);
                if (calcX.length() == 1)
                    programSummaryBuilder.append("0");
                programSummaryBuilder.append(calcX);
                programSummaryBuilder.append(" ");
                int ac = synth.midiChannels[i].getActivity();
                for (int j = 0; j < ac; j++)
                    programSummaryBuilder.append("=");
                programSummaryBuilder.append("\n");
            }
            programSummaryShunt = programSummaryBuilder.toString();
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
            synth.globalVolume = (float) volume.scrollPoint * 2;
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
                interleaved[ofs++] = (short) (AudioIOFormat.cF64toS32(data[dataPtr++]) >> 16);
                interleaved[ofs++] = (short) (AudioIOFormat.cF64toS32(data[dataPtr++]) >> 16);
                frames--;
            }
            synthViewOfSeekPoint = timer.currentTime / seqTiming.lengthSeconds;
        }
    }
}
