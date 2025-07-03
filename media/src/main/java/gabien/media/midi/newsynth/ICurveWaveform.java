/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.midi.newsynth;

import datum.DatumSrcLoc;
import datum.DatumWriter;

/**
 * The waveform.
 * Created 2nd July, 2025.
 */
public interface ICurveWaveform {
    /**
     * Counts the control points in the waveform. There must be at least one.
     */
    int pointCount();

    /**
     * X position of a point. Points are automatically looped if needed (or clamped if needed).
     * Inside the original waveform, X values are from 0 to 1.
     * However, to loop the points, X values are adjusted accordingly.
     */
    float pointX(int pointIdx);

    /**
     * Y position of a point. Points are automatically looped if needed (or clamped if needed).
     */
    float pointY(int pointIdx);

    /**
     * Dumps waveform contents to a Datum writer.
     */
    default void writeToDatum(DatumWriter writer) {
        DatumWriter sw = writer.visitList(DatumSrcLoc.NONE);
        int pc = pointCount();
        for (int i = 0; i < pc; i++) {
            sw.visitFloat(pointX(i), DatumSrcLoc.NONE);
            sw.visitFloat(pointY(i), DatumSrcLoc.NONE);
        }
        sw.visitEnd(DatumSrcLoc.NONE);
    }
}
