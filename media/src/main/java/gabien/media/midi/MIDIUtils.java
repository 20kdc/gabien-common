/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi;

/**
 * Created February 14th, 2024.
 */
public class MIDIUtils {
    /**
     * Gets a MIDI variable-length integer.
     */
    public static int getVLI(byte[] data, int offset) {
        int v = 0;
        while (true) {
            int b = data[offset++] & 0xFF;
            v <<= 7;
            v |= b & 0x7F;
            if ((b & 0x80) == 0)
                break;
        }
        return v;
    }

    /**
     * Gets a MIDI variable-length integer's length.
     */
    public static int getVLILength(byte[] data, int offset) {
        int len = 0;
        while (true) {
            len++;
            int b = data[offset++] & 0xFF;
            if ((b & 0x80) == 0)
                break;
        }
        return len;
    }

    /**
     * Gets the length of a MIDI event's data.
     */
    public static int getEventDataLen(byte status, byte[] data, int offset) {
        int b = status & 0xFF;
        // 8x: off, 9x: on, Ax: note pressure, Bx: CC
        if (b >= 0x80 && b <= 0xBF)
            return 2;
        // Cx: prg, Dx: channel pressure
        if (b >= 0xC0 && b <= 0xDF)
            return 1;
        // Ex: pitch bend
        if (b >= 0xE0 && b <= 0xEF)
            return 2;
        // F
        if (b == 0xF0 || b == 0xF7) {
            // the two different SMF SysEx kinds
            // so depending on the MIDI spec this is formatted differently
            return getVLILength(data, offset) + getVLI(data, offset);
        }
        // "time code quarter frame"???
        if (b == 0xF1)
            return 1;
        // spp
        if (b == 0xF2)
            return 2;
        // song select
        if (b == 0xF3)
            return 1;
        if (b == 0xFF) {
            // so depending on the MIDI spec this means different things
            // but for what we care about, it's meta-event
            return 1 + getVLILength(data, offset + 1) + getVLI(data, offset + 1);
        }
        return 0;
    }

    public static double getNoteHz(double note) {
        return 55.0d * Math.pow(2, (note - 33d) / 12.0d);
    }
}
