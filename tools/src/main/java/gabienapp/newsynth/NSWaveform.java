/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabienapp.newsynth;

/**
 * Synth waveform impl.
 * Created 2nd July, 2025
 */
public class NSWaveform implements IEditableCurveWaveform {
    public float[] pointData = new float[] {
        0, 0.5f,
        0.5f, 0.25f
    };

    @Override
    public int pointCount() {
        return pointData.length / 2;
    }

    @Override
    public boolean addPointRaw(int index) {
        if (index < 0)
            return false;
        // can only be equal to the end, not past it
        if (index > (pointData.length / 2))
            return false;
        float[] newPointArray = new float[pointData.length + 2];
        System.arraycopy(pointData, 0, newPointArray, 0, index << 1);
        System.arraycopy(pointData, index << 1, newPointArray, (index + 1) << 1, pointData.length - (index << 1));
        pointData = newPointArray;
        return true;
    }

    @Override
    public void rmPoint(int index) {
        if (pointData.length <= 2)
            return;
        index = mapPointIdx(index);
        float[] newPointArray = new float[pointData.length - 2];
        System.arraycopy(pointData, 0, newPointArray, 0, index);
        System.arraycopy(pointData, index + 2, newPointArray, index, pointData.length - (index + 2));
        pointData = newPointArray;
    }

    private int mapPointIdx(int pointIdx) {
        int count = pointData.length / 2;
        while (pointIdx < 0)
            pointIdx += count;
        return (pointIdx % count) << 1;
    }

    private int mapPointGen(int pointIdx) {
        int count = pointData.length / 2;
        if (pointIdx < 0)
            pointIdx -= count;
        return pointIdx / count;
    }

    @Override
    public float pointX(int pointIdx) {
        return pointData[mapPointIdx(pointIdx)] + mapPointGen(pointIdx);
    }

    @Override
    public float pointY(int pointIdx) {
        return pointData[(mapPointIdx(pointIdx)) + 1];
    }

    @Override
    public void movePoint(int pointIdx, float x, float y) {
        pointIdx = mapPointIdx(pointIdx);
        pointData[pointIdx] = x;
        pointData[pointIdx + 1] = y;
    }
}
