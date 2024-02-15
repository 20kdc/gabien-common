/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.audio.fileio;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;

import gabien.media.audio.AudioIOCRSet;
import gabien.media.audio.AudioIOSource;
import gabien.media.midi.MIDISequence;
import gabien.media.midi.MIDISynthesizer;
import gabien.media.midi.MIDITimer;
import gabien.media.midi.MIDITracker;

/**
 * Created 14th February 2024.
 */
public class MIDISynthesizerSource extends AudioIOSource.SourceF32 {
    private final MIDITracker tracker;
    private final MIDITimer timer;
    private final MIDISynthesizer synthesizer;
    private final int frameCount, chunkSize;
    private int totalFramesConsumed;

    public MIDISynthesizerSource(@NonNull MIDISequence sequence, @NonNull MIDISynthesizer synth, double cooloff) {
        super(new AudioIOCRSet(2, synth.sampleRate));
        MIDITracker mt = new MIDITracker(sequence, null);
        double totalTime = cooloff;
        while (true) {
            int ticks = mt.getTicksToNextEvent();
            if (ticks == -1)
                break;
            totalTime += mt.getTicksToSeconds() * ticks;
            mt.runNextEvent();
        }
        synthesizer = synth;
        tracker = new MIDITracker(sequence, synthesizer);
        timer = new MIDITimer(tracker);
        frameCount = (int) (totalTime * synth.sampleRate);
        chunkSize = synth.sampleRate > 100 ? (synth.sampleRate / 100) : 1;
        // get all the early events over with
        timer.resolve();
    }

    @Override
    public int frameCount() {
        return frameCount;
    }

    @Override
    public void nextFrames(@NonNull float[] frame, int at, int frames) throws IOException {
        while (frames > 0) {
            if (frames > chunkSize) {
                nextFramesChunk(frame, at, chunkSize);
                frames -= chunkSize;
                at += chunkSize * 2;
            } else {
                nextFramesChunk(frame, at, frames);
                break;
            }
        }
    }

    private void nextFramesChunk(float[] frame, int at, int frames) throws IOException {
        int samples = frames * 2;
        for (int i = 0; i < samples; i++)
            frame[at + i] = 0;
        synthesizer.render(frame, at, frames);
        // now update
        synthesizer.update(frames / (double) synthesizer.sampleRate);
        totalFramesConsumed += frames;
        timer.currentTime = totalFramesConsumed / (double) synthesizer.sampleRate;
        timer.resolve();
    }
}
