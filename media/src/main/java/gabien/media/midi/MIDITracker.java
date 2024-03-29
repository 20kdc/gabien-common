/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Created February 14th, 2024
 */
public final class MIDITracker implements MIDITimableThing {
    private final MIDISequence sequence;
    private final int[] pointers;

    // in both the array and the minimum, -1 = no events available
    private final int[] eventTicks;
    private int tickOfNextEvent = -1;
    private int currentTick = 0;

    private final byte[] runningStatus;
    // unit per one "delta-time"
    private double deltaTimeToSeconds;
    public @Nullable MIDIEventReceiver receiver;

    public MIDITracker(@NonNull MIDISequence s, @Nullable MIDIEventReceiver receiver) {
        this.receiver = receiver;
        sequence = s;
        pointers = new int[s.tracks.length];
        eventTicks = new int[s.tracks.length];
        runningStatus = new byte[s.tracks.length];
        dtuByTempo(500000);
        for (int i = 0; i < pointers.length; i++)
            readDeltaTime(i);
        recalculateTickOfNextEvent();
    }

    private void dtuByTempo(int tempo) {
        if (sequence.division < 0) {
            int tca = sequence.division >> 8;
            int tcb = sequence.division & 0xFF;
            int fps = 30;
            if (tca == -24)
                fps = 24;
            else if (tca == 25)
                fps = 25;
            deltaTimeToSeconds = 1.0d / (tcb * fps);
        } else {
            deltaTimeToSeconds = tempo / (1000000d * sequence.division);
        }
    }

    /**
     * Read delta time for the given track, increasing pointer
     */
    private void readDeltaTime(int track) {
        if (pointers[track] >= sequence.tracks[track].length) {
            eventTicks[track] = -1;
            return;
        }
        eventTicks[track] = currentTick + MIDIUtils.getVLI(sequence.tracks[track], pointers[track]);
        pointers[track] += MIDIUtils.getVLILength(sequence.tracks[track], pointers[track]);
    }

    private void recalculateTickOfNextEvent() {
        tickOfNextEvent = -1;
        for (int i = 0; i < pointers.length; i++) {
            int rt = eventTicks[i];
            if (rt == -1)
                continue;
            if (tickOfNextEvent == -1 || tickOfNextEvent > rt)
                tickOfNextEvent = rt;
        }
    }

    @Override
    public double getTicksToSeconds() {
        return deltaTimeToSeconds;
    }

    @Override
    public int getCurrentTick() {
        return currentTick;
    }

    @Override
    public int getTickOfNextEvent() {
        return tickOfNextEvent;
    }

    /**
     * Handles internal stuff (timing etc.)
     */
    private void doInternalReceiveEvent(byte status, byte[] data, int offset, int length) {
        if (status == (byte) 0xFF) {
            if (data[offset] == (byte) 0x51) {
                int vlil = MIDIUtils.getVLILength(data, offset + 1);
                if (length >= vlil + 4) {
                    int a = data[offset + vlil + 1] & 0xFF;
                    int b = data[offset + vlil + 2] & 0xFF;
                    int c = data[offset + vlil + 3] & 0xFF;
                    int res = (a << 16) | (b << 8) | c;
                    dtuByTempo(res);
                }
            }
        }
    }

    @Override
    public boolean runNextEvent() {
        // need to run clock forward first
        if (currentTick != tickOfNextEvent) {
            // this also accounts for "no more events"
            if (tickOfNextEvent == -1)
                return false;
            currentTick = tickOfNextEvent;
        }
        for (int i = 0; i < pointers.length; i++) {
            if (currentTick == eventTicks[i]) {
                // read event 1st byte
                byte status = sequence.tracks[i][pointers[i]];
                if (status < 0) {
                    // status
                    // 0xF8 and onwards are real-time events,
                    //  so don't update running status
                    if (status < (byte) -8)
                        runningStatus[i] = status;
                    pointers[i]++;
                } else {
                    // data, status is inherited
                    status = runningStatus[i];
                }
                int remaining = sequence.tracks[i].length - pointers[i];
                int dataLen = MIDIUtils.getEventDataLen(status, sequence.tracks[i], pointers[i]);
                if (dataLen > remaining) {
                    // System.out.println("INVESTIGATE: " + Integer.toHexString(status) + " (given: " + dataLen + ", had: " + remaining + ")");
                    dataLen = remaining;
                }
                doInternalReceiveEvent(status, sequence.tracks[i], pointers[i], dataLen);
                if (receiver != null)
                    receiver.receiveEvent(status, sequence.tracks[i], pointers[i], dataLen);
                pointers[i] += dataLen;
                // and read next event DT (or cancel track if none remaining!)
                readDeltaTime(i);
            }
        }
        recalculateTickOfNextEvent();
        return true;
    }
}
