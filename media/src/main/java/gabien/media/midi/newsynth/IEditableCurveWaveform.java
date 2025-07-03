/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.midi.newsynth;

import java.util.LinkedList;

import datum.DatumInvalidVisitor;
import datum.DatumSrcLoc;
import datum.DatumVisitor;
import gabien.datum.DatumExpectListVisitor;

/**
 * Created 2nd July, 2025
 */
public interface IEditableCurveWaveform extends ICurveWaveform {
    /**
     * Adds a point at the given index. Returns false on failure.
     */
    boolean addPointRaw(int index);
    /**
     * Adds a point after the given index, returning what should be the new selected index.
     */
    default int addPoint(int index) {
        index++;
        if (!addPointRaw(index))
            return index - 1;
        // fix coordinates
        float prevX = pointX(index - 1);
        float nextX = pointX(index + 1);
        float prevY = pointY(index - 1);
        float nextY = pointY(index + 1);
        movePoint(index, Math.min(1, Math.max(0, (prevX + nextX) / 2)), (prevY + nextY) / 2);
        return index;
    }
    /**
     * Removes the point at the given index. (Cannot remove all points.)
     */
    void rmPoint(int index);
    /**
     * Moves a point to a new location.
     */
    void movePoint(int index, float x, float y);
    /**
     * Imports a new point set.
     */
    void importPoints(float[] data);
    /**
     * Implements a datum read visitor for the points.
     */
    default DatumVisitor createDatumReadVisitor() {
        return new DatumExpectListVisitor(() -> {
            return new DatumInvalidVisitor() {
                LinkedList<Float> flts = new LinkedList<>();
                @Override
                public void visitFloat(double value, DatumSrcLoc loc) {
                    flts.add((float) value);
                }
                @Override
                public void visitInt(long value, DatumSrcLoc loc) {
                    flts.add((float) value);
                }
                @Override
                public void visitEnd(DatumSrcLoc loc) {
                    float[] res = new float[flts.size()];
                    for (int i = 0; i < res.length; i++)
                        res[i] = flts.get(i);
                    importPoints(res);
                }
            };
        });
    }
}
