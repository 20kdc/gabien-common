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
     */
    public double getTicksToSeconds();

    /**
     * The amount of Arbitrary Time Units to the next event.
     * Returns -1 to mean no event exists.
     */
    public int getTicksToNextEvent();

    /**
     * Runs the next event, implicitly advancing by getTicksToNextEvent.
     * Returns false if no events remain.
     */
    public boolean runNextEvent();
}
