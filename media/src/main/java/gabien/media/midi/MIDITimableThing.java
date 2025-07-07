/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

/**
 * Thing that can be advanced in MIDI time.
 * Created February 14th, 2024.
 */
public interface MIDITimableThing {
    /**
     * The current definition of the time unit.
     * Notably, this definition may change, so the driving element is expected to keep track of 'relative time'. 
     */
    public double getTicksToSeconds();

    /**
     * The current tick.
     */
    public int getCurrentTick();

    /**
     * The tick of the next event.
     * Returns -1 to mean no event exists.
     */
    public int getTickOfNextEvent();

    /**
     * Runs the next event, implicitly advancing to getTickOfNextEvent.
     * This is the only function that can change getTicksToSeconds.
     * Returns false if no events remain.
     */
    public boolean runNextEvent();
}
