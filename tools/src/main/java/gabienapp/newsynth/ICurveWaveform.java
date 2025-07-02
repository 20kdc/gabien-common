/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabienapp.newsynth;

import gabien.uslx.append.MathsX;

/**
 * Created 2nd July, 2025
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
     * Resolves the waveform.
     */
    default void resolve(float[] data) {
        float point1X = pointX(-1);
        float point1Y = pointY(-1);
        int point2 = 0;
        float point2X = pointX(0);
        float point2Y = pointY(0);

        float time = 0.0f;
        float timeStep = 1.0f / data.length;

        float lerpFactor = ((time - point1X) / (point2X - point1X));
        float lerpFactorStep = timeStep / (point2X - point1X);

        for (int i = 0; i < data.length; i++) {
            while (time >= point2X) {
                point2++;
                point1X = point2X;
                point1Y = point2Y;
                point2X = pointX(point2);
                point2Y = pointY(point2);
                lerpFactor = ((time - point1X) / (point2X - point1X));
                lerpFactorStep = timeStep / (point2X - point1X);
            }
            float ec1 = MathsX.lerpUnclamped(point1Y, MathsX.lerpUnclamped(point1Y, point2Y, lerpFactor), lerpFactor);
            float ec2 = MathsX.lerpUnclamped(MathsX.lerpUnclamped(point1Y, point2Y, lerpFactor), point2Y, lerpFactor);
            data[i] = MathsX.lerpUnclamped(ec1, ec2, lerpFactor);
            /* advance */
            time += timeStep;
            lerpFactor += lerpFactorStep;
        }
    }
}
