/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.midi.newsynth;

import java.util.Arrays;

import gabien.uslx.append.MathsX;

/**
 * Plots curves into waveform buffers.
 * Created 2nd July, 2025
 */
public class CurvePlotter {
    /**
     * Resolves the waveform.
     */
    public static void resolve(ICurveWaveform wave, float[] data, NormalizationMode normalized) {
        // Use -2 as a marker value to indicate missing data.
        Arrays.fill(data, -2f);
        int pointCount = wave.pointCount();
        // We render the curve using segments of 4 points.
        // (0, 1, 2, 3), (2, 3, 4, 5)...
        // Here, the line goes from point 0 to point 2.
        for (int i = -2; i < pointCount; i += 2) {
            // The first two points are as they come in.
            float x1 = wave.pointX(i);
            float y1 = wave.pointY(i);
            float x2 = wave.pointX(i + 1);
            float y2 = wave.pointY(i + 1);
            // These two points are flipped.
            float x3 = wave.pointX(i + 3);
            float y3 = wave.pointY(i + 3);
            float x4 = wave.pointX(i + 2);
            float y4 = wave.pointY(i + 2);
            // The 'direction' of point 3 relative to 4 must be reversed.
            x3 = x4 - (x3 - x4);
            y3 = y4 - (y3 - y4);
            plotCurve(data, x1, y1, x2, y2, x3, y3, x4, y4);
        }
        float lastRealValue = 0;
        float total = 0.0f;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int j = 0; j < data.length; j++) {
            float val = data[j];
            if (val == -2f) {
                val = lastRealValue;
            } else {
                lastRealValue = data[j];
            }
            data[j] = val;
            if (normalized != NormalizationMode.None) {
                total += val;
                min = Math.min(val, min);
                max = Math.max(val, max);
            }
        }
        if (normalized != NormalizationMode.None) {
            total /= data.length;
            float subtract = (normalized == NormalizationMode.RecentreDC) ? total : ((max + min) / 2);
            float vol = Math.max(0.01f, max - min) / 2;
            for (int j = 0; j < data.length; j++) {
                data[j] = (data[j] - subtract) / vol;
            }
        }
    }

    public enum NormalizationMode {
        None,
        RecentreDC,
        RecentreMinmax
    }

    /**
     * Plots a cubic Bezier curve into the data.
     */
    private static void plotCurve(float[] data, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        int stepCount = data.length * 2;
        for (int step = 0; step < stepCount; step++) {
            float lerpValue = step / (float) stepCount;
            // decompose to the three-point sub-curve
            float xa = MathsX.lerpUnclamped(x1, x2, lerpValue);
            float ya = MathsX.lerpUnclamped(y1, y2, lerpValue);
            float xm = MathsX.lerpUnclamped(x2, x3, lerpValue);
            float ym = MathsX.lerpUnclamped(y2, y3, lerpValue);
            float xb = MathsX.lerpUnclamped(x3, x4, lerpValue);
            float yb = MathsX.lerpUnclamped(y3, y4, lerpValue);
            // decompose to the line
            float xc = MathsX.lerpUnclamped(xa, xm, lerpValue);
            float yc = MathsX.lerpUnclamped(ya, ym, lerpValue);
            float xd = MathsX.lerpUnclamped(xm, xb, lerpValue);
            float yd = MathsX.lerpUnclamped(ym, yb, lerpValue);
            // finish
            float x = MathsX.lerpUnclamped(xc, xd, lerpValue);
            float y = MathsX.lerpUnclamped(yc, yd, lerpValue);
            int xInt = (int) (x * data.length);
            if (xInt < 0 || xInt >= data.length)
                continue;
            data[xInt] = y;
            //debugPlot(data, xa, ya);
            //debugPlot(data, xm, ym);
            //debugPlot(data, xb, yb);
        }
    }
    // for debug
    @SuppressWarnings("unused")
    private static void debugPlot(float[] data, float x, float y) {
        int xInt = (int) (x * data.length);
        if (xInt < 0 || xInt >= data.length)
            return;
        data[xInt] = y;
    }
}
