/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi.newsynth;

import gabien.media.midi.MIDISynthesizer;
import gabien.uslx.append.MathsX;

/**
 * Adapted out of MLDIChannel but with modifications.
 * Created 2nd July, 2025.
 */
public class NSChannel extends MIDISynthesizer.Channel {
    // Position in wave 0-1
    double internalCounter;

    /**
     * Current volume register
     */
    public float volume;

    /**
     * Time in this stage
     */
    public double timeInStage, renderTimeInStage;

    /**
     * Time in this stage when it ends
     */
    public double stageEndTime;

    /**
     * Pitch multiplier state
     */
    public float pitchMulState;

    /**
     * 0 = strike
     * 1 = s
     * 2 = r
     */
    public int stage;

    /**
     * Strike: Attack/decay
     */
    public final float strike;

    /**
     * Release: How much time it takes to kill the note.
     */
    public final float release;

    /**
     * Waveform, envelope (left/right quarters unused to make curve editing nicer), pitch envelope
     */
    public final float[] waveform, envelope, pitchEnv;

    /**
     * Pitch multiplier becomes an absolute frequency
     */
    public final boolean pitchLock;

    public final boolean skipSustainStage;

    public NSChannel(NSPatch patch) {
        this.stageEndTime = this.strike = patch.strikeMs / 1000f;
        this.skipSustainStage = !patch.sustainEnabled;
        this.release = patch.releaseMs / 1000f;
        this.waveform = patch.getMainWaveform();
        this.envelope = patch.getEnvWaveform();
        this.pitchEnv = patch.getPitchEnvWaveform();
        this.pitchMulState = 1;
        this.pitchLock = false;
        update(0);
    }

    @Override
    public void noteOffInner(int velocity) {
        // do nothing, we don't use the notification to track note release
    }

    @Override
    public void render(float[] buffer, int offset, int frames, float leftVol, float rightVol) {
        double sampleSeconds = getSampleSeconds();
        double effectiveCycleSeconds = (pitchLock ? 1.0d : getCycleSeconds()) / pitchMulState;
        double sampleAdv = sampleSeconds / effectiveCycleSeconds;
        leftVol *= volume;
        rightVol *= volume;
        while (frames > 0) {
            internalCounter = (internalCounter + sampleAdv) % 1;
            float idx = (float) (internalCounter * waveform.length);
            float wf = MathsX.linearSample1d(idx, waveform, true);
            buffer[offset++] += wf * leftVol;
            buffer[offset++] += wf * rightVol;
            frames--;
        }
    }

    @Override
    public boolean update(double time) {
        timeInStage += time;
        // advance from sustain to release
        if (stage == 1 && !isNoteOn())
            timeInStage = stageEndTime;
        while (timeInStage >= stageEndTime) {
            timeInStage -= stageEndTime;
            if (stage == 0) {
                // just finished attack/decay; we hold in sustain
                if (skipSustainStage) {
                    // or not
                    stage = 2;
                    stageEndTime = release;
                } else {
                    stage = 1;
                    // or until note off releases us
                    stageEndTime = Float.MAX_VALUE;
                }
            } else if (stage == 1) {
                // sustain -> release
                stage = 2;
                stageEndTime = release;
            } else if (stage >= 2) {
                // release: nope
                stageEndTime = Float.MIN_VALUE;
                return true;
            }
        }
        int envQuarter = envelope.length / 4;
        float volPtr = MathsX.lerpUnclamped(0, envQuarter, MathsX.clamp((float) (timeInStage / stageEndTime), 0, 1));
        if (stage == 0) {
            volPtr += envQuarter;
        } else if (stage == 1) {
            // sustaining, so hold at a particular point
            volPtr = envelope.length / 2;
        } else if (stage == 2) {
            // releasing
            volPtr += envelope.length / 2;
        }
        volume = MathsX.linearSample1d(volPtr, envelope, false);
        pitchMulState = 0.5f + MathsX.linearSample1d(volPtr, pitchEnv, false);
        return false;
    }

}
