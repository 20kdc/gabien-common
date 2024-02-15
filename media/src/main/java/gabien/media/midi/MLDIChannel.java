/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import gabien.uslx.append.MathsX;

/**
 * Emulates a hypothetical ADSR+AM+Fun chip.
 * Definitely not just a made-up excuse to not have to write a real SF2 synth...
 * Created 14th February, 2024.
 */
public class MLDIChannel extends MIDISynthesizer.Channel {
    // Position in wave 0-1
    double internalCounter;

    /**
     * Current volume register
     */
    public float volume;

    /**
     * Volume at last stage (while it lerps)
     */
    public float volumeAtLastStage;

    /**
     * Volume at next stage
     */
    public float volumeAtNextStage;

    /**
     * Time in this stage
     */
    public double timeInStage;

    /**
     * Time in this stage when it ends
     */
    public double stageEndTime;

    /**
     * Pitch multiplier state
     */
    public float pitchMulState;

    /**
     * Progress in the pitch bend
     */
    public double pitchMulProgress;

    /**
     * 0 = a
     * 1 = d
     * 2 = s
     * 3 = r
     */
    public int stage;

    /**
     * Attack: How much time it takes to get to nominal volume.
     */
    public final float attack;

    /**
     * Decay: How much time it takes to get to sustain volume.
     */
    public final float decay;

    /**
     * Sustain: The volume at which we hold after decay.
     */
    public final float sustain;

    /**
     * Limit on sustain
     */
    public final float maxSustainTime;

    /**
     * Release: How much time it takes to kill the note.
     */
    public final float release;

    /**
     * Mix: Square versus sine.
     */
    public final float mixSquare, mixSine;

    /**
     * Pitch controls
     */
    public final float pitchMulFrom, pitchMulTo, pitchMulTime;

    /**
     * Pitch multiplier becomes an absolute frequency
     */
    public final boolean pitchLock;

    public MLDIChannel(float attack, float decay, float sustain, float release, float mix, float gvm, float pitchMulFrom, float pitchMulTo, float pitchMulTime) {
        this(attack, decay, sustain, release, mix, gvm, pitchMulFrom, pitchMulTo, pitchMulTime, false, Float.MAX_VALUE);
    }
    public MLDIChannel(float attack, float decay, float sustain, float release, float mix, float gvm, float pitchMulFrom, float pitchMulTo, float pitchMulTime, boolean pitchLock, float maxSustainTime) {
        this.stageEndTime = this.attack = attack;
        this.volumeAtNextStage = gvm;
        this.decay = decay;
        this.sustain = sustain * gvm;
        this.release = release;
        this.mixSquare = mix;
        this.mixSine = 1.0f - mix;
        this.pitchMulState = this.pitchMulFrom = pitchMulFrom;
        this.pitchMulTo = pitchMulTo;
        this.pitchMulTime = pitchMulTime;
        this.pitchLock = pitchLock;
        this.maxSustainTime = maxSustainTime;
        update(0);
    }

    @Override
    public void noteOffInner(int velocity) {
        stage = 3;
        volumeAtLastStage = volume;
        volumeAtNextStage = 0;
        timeInStage = 0;
        stageEndTime = release;
    }

    @Override
    public void render(float[] buffer, int offset, int frames, float leftVol, float rightVol) {
        float gVol = volume * getVelocityVol();
        double sampleAdv = (pitchLock ? getSampleSeconds() : getSampleAdv()) * pitchMulState;
        leftVol *= gVol;
        rightVol *= gVol;
        while (frames > 0) {
            internalCounter = (internalCounter + sampleAdv) % 1;
            float waveform;
            if (internalCounter > 0.5f) {
                waveform = mixSquare;
            } else {
                waveform = -mixSquare;
            }
            waveform += Math.sin(internalCounter * MathsX.PI2) * mixSine;
            buffer[offset++] += waveform * leftVol;
            buffer[offset++] += waveform * rightVol;
            frames--;
        }
    }

    @Override
    public boolean update(double time) {
        timeInStage += time;
        pitchMulProgress += time / pitchMulTime;
        if (pitchMulProgress > 1.0d)
            pitchMulProgress = 1.0d;
        while (timeInStage >= stageEndTime) {
            timeInStage -= stageEndTime;
            volumeAtLastStage = volumeAtNextStage;
            if (stage == 0) {
                // attack -> decay
                stage = 1;
                stageEndTime = decay;
                volumeAtNextStage = sustain;
            } else if (stage == 1) {
                // decay -> sustain
                stage = 2;
                volume = sustain;
                volumeAtLastStage = sustain;
                volumeAtNextStage = sustain;
                stageEndTime = maxSustainTime;
            } else if (stage == 2) {
                stage = 3;
                stageEndTime = release;
                volumeAtNextStage = 0;
            } else if (stage >= 3) {
                // release: nope
                stageEndTime = -1;
                return true;
            }
        }
        volume = MathsX.lerpUnclamped(volumeAtLastStage, volumeAtNextStage, (float) (timeInStage / stageEndTime));
        pitchMulState = MathsX.lerpUnclamped(pitchMulFrom, pitchMulTo, (float) pitchMulProgress);
        return false;
    }

}
