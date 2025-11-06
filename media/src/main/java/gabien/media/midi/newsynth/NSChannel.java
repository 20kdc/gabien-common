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
    // Position in wave 0-waveform.length
    float internalCounter;

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

    public final NSPatch patch;

    public final float octaveShiftMultiplier;

    /**
     * Waveform, envelope (left/right quarters unused to make curve editing nicer), pitch envelope
     */
    public final float[] waveform, envelope, pitchEnv;

    public NSChannel(NSPatch patch) {
        this.stageEndTime = patch.strikeMs / 1000f;
        this.octaveShiftMultiplier = (float) Math.pow(2, patch.octaveShift);
        this.patch = patch;
        this.waveform = patch.getMainWaveform();
        this.envelope = patch.getEnvWaveform();
        this.pitchEnv = patch.getPitchEnvWaveform();
        this.pitchMulState = 1;
        update(0);
    }

    @Override
    public void noteOffInner(int velocity) {
        // do nothing, we don't use the notification to track note release
    }

    @Override
    public void render(float[] buffer, int offset, int frames, float leftVol, float rightVol) {
        float sampleSeconds = (float) getSampleSeconds();
        float effectiveCycleSeconds = (float) (((patch.fixedFrequency != 0) ? (1.0d / patch.fixedFrequency) : getCycleSeconds()) / pitchMulState);
        // this is a divisor since we are dividing cycleSeconds
        effectiveCycleSeconds /= octaveShiftMultiplier;
        float sampleAdv = (sampleSeconds / effectiveCycleSeconds) * waveform.length;
        leftVol *= volume;
        rightVol *= volume;
        while (frames > 0) {
            internalCounter = (internalCounter + sampleAdv) % waveform.length;
            float wf = MathsX.linearSample1d(internalCounter, waveform, true);
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
                if (patch.sustainEnabled) {
                    stage = 1;
                    // or until note off releases us
                    stageEndTime = Float.MAX_VALUE;
                } else {
                    // or not
                    stage = 2;
                    stageEndTime = patch.releaseMs / 1000f;
                }
            } else if (stage == 1) {
                // sustain -> release
                stage = 2;
                stageEndTime = patch.releaseMs / 1000f;
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
