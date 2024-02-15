/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import org.eclipse.jdt.annotation.NonNull;

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
     * Base changed at this tick.
     */
    private int lastBaseTick;

    public MIDITimer(@NonNull MIDITimableThing target) {
        this.target = target;
        lastBaseTTS = target.getTicksToSeconds();
    }

    /**
     * Ensures all events have been executed up to the given tick.
     * Also updates currentTime accordingly.
     * Notably, some usecases may require this is used in a "pair".
     * Also notably, the actual tick seeked to may be less if no events happened.
     */
    public void resolveTick(int targetTick) {
        int currentTick = target.getCurrentTick();
        while (currentTick < targetTick) {
            runAllEventsSlatedForNow();
            int nextEventAt = target.getCurrentTick();
            if (nextEventAt == -1) {
                // past end, so it's fine
                break;
            } else if (nextEventAt > targetTick) {
                // more than acceptable, stop
                break;
            } else {
                // acceptable
                currentTick = nextEventAt;
                target.runNextEvent();
                updateBaseIfNecessary();
            }
        }
        // No matter what happens, this calculation should always be right
        currentTime = lastBase + ((currentTick - lastBaseTick) * lastBaseTTS);
    }

    /**
     * Runs all events that need to happen immediately.
     * Also is sure to update timebase.
     */
    private void runAllEventsSlatedForNow() {
        int currentTick = target.getCurrentTick();
        while (currentTick == target.getTickOfNextEvent())
            target.runNextEvent();
        updateBaseIfNecessary();
    }

    private void updateBaseIfNecessary() {
        double currentTicksToSeconds = target.getTicksToSeconds();
        if (currentTicksToSeconds != lastBaseTTS) {
            // change base
            int currentTick = target.getCurrentTick();
            lastBase += lastBaseTTS * (currentTick - lastBaseTick);
            lastBaseTTS = currentTicksToSeconds;
            lastBaseTick = currentTick;
        }
    }

    /**
     * Resolves to the current value of absoluteTime.
     * Call once with zero time to run initial events.
     */
    public void resolve() {
        runAllEventsSlatedForNow();
        int targetTick = target.getTickOfNextEvent();
        while (targetTick != -1) {
            // phase 1: can we advance?
            double nextTime = lastBase + ((targetTick - lastBaseTick) * lastBaseTTS);
            // if we can't, we're done for now
            if (currentTime < nextTime)
                break;
            // alright, we can advance
            target.runNextEvent();
            updateBaseIfNecessary();
            // phase 2: clean up
            runAllEventsSlatedForNow();
            targetTick = target.getTickOfNextEvent();
        }
    }
}
