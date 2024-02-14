/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

/**
 * Responsible for driving a MIDITracker in the land of seconds.
 * Created February 14th, 2024.
 */
public class MIDITimer {
    public final MIDITimableThing target;

    /**
     * Current absolute time in seconds.
     */
    public double currentTime;

    /**
     * Absolute time of last timing base.
     */
    private double lastBase;

    /**
     * Ticks to seconds value of last timing base.
     */
    private double lastBaseTTS;

    /**
     * Consumed ticks since last base change.
     */
    private int ticksSinceLastBase;

    public MIDITimer(MIDITimableThing target) {
        this.target = target;
        lastBaseTTS = target.getTicksToSeconds();
    }

    /**
     * Resolves to the current value of absoluteTime.
     * Call once with zero time to run initial events.
     * Returns the amount of events that ran.
     */
    public int resolve() {
        int eventCount = 0;
        int ticks = target.getTicksToNextEvent();
        while (ticks != -1) {
            // phase 1: run all events slated for NOW
            while (ticks == 0) {
                eventCount++;
                target.runNextEvent();
                ticks = target.getTicksToNextEvent();
            }
            if (ticks == -1)
                break;
            // phase 2: do timebase change?
            double currentTicksToSeconds = target.getTicksToSeconds();
            if (currentTicksToSeconds != lastBaseTTS) {
                // change base
                lastBase += lastBaseTTS * ticksSinceLastBase;
                lastBaseTTS = currentTicksToSeconds;
                ticksSinceLastBase = 0;
            }
            // phase 3: can we advance?
            int nextTimeTicks = ticksSinceLastBase + ticks;
            double nextTime = lastBase + (nextTimeTicks * lastBaseTTS);
            if (currentTime >= nextTime) {
                // alright, we can advance
                ticksSinceLastBase += ticks;
                eventCount++;
                target.runNextEvent();
                ticks = target.getTicksToNextEvent();
            }
        }
        return eventCount;
    }
}
