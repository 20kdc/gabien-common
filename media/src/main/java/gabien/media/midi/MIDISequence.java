/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

/**
 * Created February 14th, 2024.
 */
public final class MIDISequence {
    public final short division;
    public final byte[][] tracks;

    public MIDISequence(short div, byte[][] tracks) {
        division = div;
        this.tracks = tracks;
    }

    public static MIDISequence[] from(InputStream inp) throws IOException {
        DataInputStream dis = new DataInputStream(inp);
        if (dis.readInt() != 0x4d546864)
            throw new IOException("not MThd");
        if (dis.readInt() != 6)
            throw new IOException("MThd should have 6 bytes");
        int fmt = dis.readUnsignedShort();
        int trk = dis.readUnsignedShort();
        short div = dis.readShort();
        byte[][] trks = new byte[trk][];
        for (int i = 0; i < trk; i++) {
            if (dis.readInt() != 0x4d54726b)
                throw new IOException("not MTrk");
            int len = dis.readInt();
            byte[] trkData = new byte[len];
            dis.readFully(trkData);
            trks[i] = trkData;
        }
        if (fmt == 2) {
            // "patterns" form
            MIDISequence[] res = new MIDISequence[trks.length];
            for (int i = 0; i < res.length; i++)
                res[i] = new MIDISequence(div, new byte[][] {trks[i]});
            return res;
        } else {
            // "single sequence" form
            return new MIDISequence[] {new MIDISequence(div, trks)};
        }
    }

    /**
     * Calculate timing information.
     */
    public TimingInformation calcTimingInformation() {
        MIDITracker mt = new MIDITracker(this, null);
        // "warm up" to ensure we have a correct initial getTicksToSeconds value
        while (mt.getTickOfNextEvent() == 0)
            mt.runNextEvent();
        // create initial segment & list
        LinkedList<TimingSegment> ll = new LinkedList<>();
        TimingSegment lastBase = new TimingSegment(0, 0, mt.getTicksToSeconds());
        ll.add(lastBase);
        // move forward, finding last tick & collating segments
        // hypothetically, things like loop points could be found here also
        int lastTick = 0;
        while (true) {
            int tick = mt.getTickOfNextEvent();
            if (tick == -1)
                break;
            lastTick = tick;
            mt.runNextEvent();
            double newTTS = mt.getTicksToSeconds();
            if (newTTS != lastBase.ticksToSeconds) {
                // Change of base
                lastBase = new TimingSegment(tick, (tick - lastBase.startTick) * lastBase.ticksToSeconds, newTTS);
                ll.add(lastBase);
            }
        }
        return new TimingInformation(ll.toArray(new TimingSegment[0]), lastTick);
    }

    /**
     * Contains a tempo map, among other things.
     */
    public static class TimingInformation {
        // segmentStartTicks[0] is always 0.
        public final TimingSegment[] segments;
        public final int lastTick;
        public final double lengthSeconds;
        public TimingInformation(TimingSegment[] segments, int lastTick) {
            if (segments[0] == null || segments[0].startTick != 0 || segments[0].startTime != 0)
                throw new RuntimeException("Missing or invalid TimingInformation opening segment");
            this.segments = segments;
            this.lastTick = lastTick;
            double totalLength = 0.0d;
            int at = 0;
            double currentTTS = 1.0d;
            for (TimingSegment ts : segments) {
                if (ts.startTick > lastTick) {
                    totalLength += currentTTS * (lastTick - at);
                    break;
                }
                totalLength += currentTTS * (ts.startTick - at);
                at = ts.startTick;
                currentTTS = ts.ticksToSeconds;
            }
            totalLength += currentTTS * (lastTick - at);
            lengthSeconds = totalLength;
        }

        /**
         * Convert seconds to ticks.
         */
        public int secondsToTick(double d) {
            int candidate = 0;
            for (TimingSegment s : segments)
                if (d >= s.startTime)
                    candidate = s.startTick + (int) Math.floor((d - s.startTime) / s.ticksToSeconds);
            return candidate;
        }
    }
    public static class TimingSegment {
        public final int startTick;
        public final double startTime;
        public final double ticksToSeconds;
        public TimingSegment(int startTick, double startTime, double ticksToSeconds) {
            this.startTick = startTick;
            this.startTime = startTime;
            this.ticksToSeconds = ticksToSeconds;
        }
    }
}
