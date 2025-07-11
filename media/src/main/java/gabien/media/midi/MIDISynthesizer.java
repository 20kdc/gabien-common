/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Created 14th February, 2024.
 */
public final class MIDISynthesizer implements MIDIEventReceiver {
    public static final float DEFAULT_GLOBAL_VOLUME = 1.0f;
    public final int sampleRate;
    public final double sampleTime;
    public float globalVolume = DEFAULT_GLOBAL_VOLUME;

    public int channelEnableSwitches = 0xF;

    private final Palette pal;
    public final MIDIChannel[] midiChannels = new MIDIChannel[16];

    public MIDISynthesizer(int rate, @NonNull Palette pal, int capacity) {
        sampleRate = rate;
        sampleTime = 1.0d / rate;
        this.pal = pal;
        for (int i = 0; i < 16; i++)
            midiChannels[i] = new MIDIChannel(capacity);
        resetParameters();
    }

    public void resetParameters() {
        for (int i = 0; i < 16; i++) {
            midiChannels[i].bank = 0;
            midiChannels[i].program = 0;
            midiChannels[i].volume = 1.0f;
            midiChannels[i].pan = 0.5f;
        }
        midiChannels[9].bank = 128;
    }

    @Override
    public void receiveEvent(byte status, byte[] data, int offset, int length) {
        int si = status & 0xFF;
        int mch = si & 0xF;
        if (si >= 0x80 && si <= 0x8F && length >= 2) {
            midiChannels[mch].noteOff(data[offset] & 0x7F, data[offset + 1] & 0x7F);
        } else if (si >= 0x90 && si <= 0x9F && length >= 2) {
            if ((channelEnableSwitches & (1 << mch)) == 0) {
                // channel disabled; perform note off but don't perform note on
                midiChannels[mch].noteOff(data[offset] & 0x7F, 127);
            } else {
                midiChannels[mch].noteOn(data[offset] & 0x7F, data[offset + 1] & 0x7F);
            }
        } else if (si >= 0xB0 && si <= 0xBF && length >= 2) {
            // Control Change
            int cc = data[offset] & 0x7F;
            int cv = data[offset + 1] & 0x7F;
            if (cc == 0) {
                // Bank
                midiChannels[mch].bank = mch == 9 ? (cv | 128) : cv;
            } else if (cc == 7) {
                // Volume
                midiChannels[mch].volume = cv / 127.0f;
            } else if (cc == 8 || cc == 10) {
                // Pan/Balance (assume the file only uses one)
                float pan;
                if (cv == 64) {
                    pan = 0.5f;
                } else {
                    pan = cv / 127.0f;
                }
                // reduce the level of pan to be a bit less extreme
                // this prevents "pan deafening"
                midiChannels[mch].pan = ((pan * 2) + 0.5f) / 3;
            } else if (cc == 123) {
                // shut everything off
                midiChannels[mch].noteOffAll();
            }
        } else if (si >= 0xC0 && si <= 0xCF && length >= 1) {
            // program change
            midiChannels[mch].program = data[offset] & 0x7F;
        } else if (si >= 0xE0 && si <= 0xEF && length >= 2) {
            // pitch wheel change
            int val = data[offset] & 0x7F;
            val |= (data[offset + 1] & 0x7F) << 7;
            val -= 8192;
            midiChannels[mch].setPitchBend(val / 4096d);
        }
    }

    /**
     * Clear all running channels with a hard cut.
     */
    public void clear() {
        for (int i = 0; i < 16; i++)
            midiChannels[i].clear();
    }

    /**
     * Render (additively) into the given buffer.
     */
    public void render(float[] buffer, int offset, int frames) {
        for (int i = 0; i < 16; i++)
            midiChannels[i].render(buffer, offset, frames);
    }

    /**
     * Do broad updates.
     */
    public void update(double time) {
        for (int i = 0; i < 16; i++)
            midiChannels[i].update(time);
    }

    /**
     * MIDI channel (as opposed to synthesis channel)
     */
    public final class MIDIChannel {
        /**
         * MIDI bank
         */
        public int bank;

        /**
         * MIDI program
         */
        public int program;

        /**
         * Linear volume
         */
        public float volume;
        /**
         * Pan as 0.0 (L) to 1.0 (R)
         */
        public float pan;

        private final Channel[] synthChannels;
        // [note]
        private final Channel[] noteChannels = new Channel[128];

        /**
         * Saved pitch bend in notes.
         */
        private double savedPitchBend;
        /**
         * Used to assign channels "age" numbers to know what to remove
         */
        private int channelAgeCounter;

        private MIDIChannel(int capacity) {
            synthChannels = new Channel[capacity];
        }

        public void setPitchBend(double val) {
            savedPitchBend = val;
            for (int i = 0; i < synthChannels.length; i++)
                if (synthChannels[i] != null)
                    synthChannels[i].setPitchBend(savedPitchBend);
        }

        public void noteOn(int note, int velocity) {
            if (noteChannels[note] != null)
                noteChannels[note].noteOff(127);

            Channel target = pal.create(MIDISynthesizer.this, bank, program, note, velocity);
            if (target == null)
                return;

            // age (for deletion)
            target.age = channelAgeCounter++;

            // calculate volumes
            float velocityVol = (velocity / 127f) * volume;
            // boost energy for heavy pans
            float cPanL = 1.0f - pan;
            float cPanR = pan;
            float panCMul = (float) (1.0f / Math.sqrt((cPanL * cPanL) + (cPanR * cPanR)));
            target.extLVol = velocityVol * panCMul;
            target.extRVol = velocityVol * panCMul;

            target.savedNote = note;
            target.sampleRate = sampleRate;
            target.sampleSeconds = sampleTime;
            target.setPitchBend(savedPitchBend);
            target.update(0);
            noteChannels[note] = target;
            // attach to any spare channel
            for (int i = 0; i < synthChannels.length; i++) {
                if (synthChannels[i] == null) {
                    synthChannels[i] = target;
                    return;
                }
            }
            // attach to the oldest channel
            int age = Integer.MAX_VALUE;
            int ageIndex = 0;
            for (int i = 0; i < synthChannels.length; i++) {
                if (synthChannels[i].age <= age) {
                    age = synthChannels[i].age;
                    ageIndex = i;
                }
            }
            synthChannels[ageIndex] = target;
        }

        public void noteOff(int note, int velocity) {
            if (noteChannels[note] != null)
                noteChannels[note].noteOff(velocity);
        }

        public void noteOffAll() {
            for (Channel c : noteChannels)
                if (c != null)
                    c.noteOff(127);
        }

        public void clear() {
            for (int i = 0; i < noteChannels.length; i++)
                noteChannels[i] = null;
            for (int i = 0; i < synthChannels.length; i++)
                synthChannels[i] = null;
        }

        public void render(float[] buffer, int offset, int frames) {
            for (Channel c : synthChannels)
                if (c != null)
                    c.render(buffer, offset, frames, c.extLVol * globalVolume, c.extRVol * globalVolume);
        }

        public void update(double time) {
            for (int i = 0; i < synthChannels.length; i++)
                if (synthChannels[i] != null)
                    if (synthChannels[i].update(time))
                        synthChannels[i] = null;
        }

        public int getActivity() {
            int activity = 0;
            for (int i = 0; i < synthChannels.length; i++)
                if (synthChannels[i] != null)
                    activity++;
            return activity;
        }
    }

    /**
     * Synthesizer palette.
     */
    public interface Palette {
        /**
         * Creates the channel for a given setting.
         */
        Channel create(MIDISynthesizer parent, int bank, int program, int note, int velocity);
    }

    /**
     * Synthesizer channel.
     */
    public abstract static class Channel {
        private boolean noteOn = true;
        private int savedNote, sampleRate;
        private double frequency, cycleSeconds, halfCycleSeconds, sampleSeconds;
        // Saved for use by synthesizer
        private float extLVol;
        private float extRVol;
        // Used to determine which channel to delete if we exceed polyphony.
        private int age;

        public Channel() {
        }

        /**
         * Gets ideal frequency.
         */
        public final double getFrequencyHz() {
            return frequency;
        }

        /**
         * Gets frequency in seconds per cycle.
         */
        public final double getCycleSeconds() {
            return cycleSeconds;
        }

        /**
         * Gets frequency in seconds per half-cycle.
         */
        public final double getHalfCycleSeconds() {
            return halfCycleSeconds;
        }

        /**
         * Get the sample rate.
         */
        public final int getSampleRate() {
            return sampleRate;
        }

        /**
         * Get how long a sample lasts.
         */
        public final double getSampleSeconds() {
            return sampleSeconds;
        }

        public final void setPitchBend(double pitchBend) {
            frequency = MIDIUtils.getNoteHz(savedNote + pitchBend);
            cycleSeconds = 1.0d / frequency;
            halfCycleSeconds = 0.5d / frequency;
            // sampleAdv = sampleSeconds / cycleSeconds;
        }

        public final boolean isNoteOn() {
            return noteOn;
        }

        /**
         * Note-off received.
         */
        public final void noteOff(int velocity) {
            noteOn = false;
            noteOffInner(velocity);
        }

        /**
         * Note-off received.
         */
        public abstract void noteOffInner(int velocity);

        /**
         * Renders this channel into the target stereo buffer.
         * Note that frames is length/2.
         * This is an additive process.
         */
        public abstract void render(float[] buffer, int offset, int frames, float leftVol, float rightVol);

        /**
         * Update.
         * Returns true if the channel should be deleted.
         */
        public abstract boolean update(double time);
    }
}
