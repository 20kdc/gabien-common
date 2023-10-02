/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

import org.junit.Assert;
import org.junit.Test;

import gabien.uslx.append.MathsX;

/**
 * Created 2nd October 2023.
 */
public class SaneModuloTest {
    @Test
    public void testSaneModuloInt() {
        for (int i = 0; i < 10; i++)
            for (int j = 1; j <= 10; j++)
                Assert.assertEquals(i % j, MathsX.seqModulo(i, j));
        Assert.assertEquals(9, MathsX.seqModulo(-1, 10));
        Assert.assertEquals(8, MathsX.seqModulo(-2, 10));
        Assert.assertEquals(7, MathsX.seqModulo(-3, 10));
        Assert.assertEquals(6, MathsX.seqModulo(-4, 10));
        Assert.assertEquals(5, MathsX.seqModulo(-5, 10));
        Assert.assertEquals(4, MathsX.seqModulo(-6, 10));
        Assert.assertEquals(3, MathsX.seqModulo(-7, 10));
        Assert.assertEquals(2, MathsX.seqModulo(-8, 10));
        Assert.assertEquals(1, MathsX.seqModulo(-9, 10));
        Assert.assertEquals(0, MathsX.seqModulo(-10, 10));
        Assert.assertEquals(9, MathsX.seqModulo(-11, 10));
    }
    @Test
    public void testSaneModuloLong() {
        for (long i = 0; i < 10; i++)
            for (long j = 1; j <= 10; j++)
                Assert.assertEquals(i % j, MathsX.seqModulo(i, j));
        Assert.assertEquals(9L, MathsX.seqModulo(-1L, 10L));
        Assert.assertEquals(8L, MathsX.seqModulo(-2L, 10L));
        Assert.assertEquals(7L, MathsX.seqModulo(-3L, 10L));
        Assert.assertEquals(6L, MathsX.seqModulo(-4L, 10L));
        Assert.assertEquals(5L, MathsX.seqModulo(-5L, 10L));
        Assert.assertEquals(4L, MathsX.seqModulo(-6L, 10L));
        Assert.assertEquals(3L, MathsX.seqModulo(-7L, 10L));
        Assert.assertEquals(2L, MathsX.seqModulo(-8L, 10L));
        Assert.assertEquals(1L, MathsX.seqModulo(-9L, 10L));
        Assert.assertEquals(0L, MathsX.seqModulo(-10L, 10L));
        Assert.assertEquals(9L, MathsX.seqModulo(-11L, 10L));
    }
    @Test
    public void testSaneModuloFloat() {
        for (float i = 0; i < 10; i++)
            for (float j = 1; j <= 10; j++)
                Assert.assertEquals(i % j, MathsX.seqModulo(i, j), 0);
        Assert.assertEquals(9f, MathsX.seqModulo(-1f, 10f), 0);
        Assert.assertEquals(8f, MathsX.seqModulo(-2f, 10f), 0);
        Assert.assertEquals(7f, MathsX.seqModulo(-3f, 10f), 0);
        Assert.assertEquals(6f, MathsX.seqModulo(-4f, 10f), 0);
        Assert.assertEquals(5f, MathsX.seqModulo(-5f, 10f), 0);
        Assert.assertEquals(4f, MathsX.seqModulo(-6f, 10f), 0);
        Assert.assertEquals(3f, MathsX.seqModulo(-7f, 10f), 0);
        Assert.assertEquals(2f, MathsX.seqModulo(-8f, 10f), 0);
        Assert.assertEquals(1f, MathsX.seqModulo(-9f, 10f), 0);
        Assert.assertEquals(0f, MathsX.seqModulo(-10f, 10f), 0);
        Assert.assertEquals(9f, MathsX.seqModulo(-11f, 10f), 0);
    }
    @Test
    public void testSaneModuloDouble() {
        for (double i = 0; i < 10; i++)
            for (double j = 1; j <= 10; j++)
                Assert.assertEquals(i % j, MathsX.seqModulo(i, j), 0);
        Assert.assertEquals(9d, MathsX.seqModulo(-1d, 10d), 0);
        Assert.assertEquals(8d, MathsX.seqModulo(-2d, 10d), 0);
        Assert.assertEquals(7d, MathsX.seqModulo(-3d, 10d), 0);
        Assert.assertEquals(6d, MathsX.seqModulo(-4d, 10d), 0);
        Assert.assertEquals(5d, MathsX.seqModulo(-5d, 10d), 0);
        Assert.assertEquals(4d, MathsX.seqModulo(-6d, 10d), 0);
        Assert.assertEquals(3d, MathsX.seqModulo(-7d, 10d), 0);
        Assert.assertEquals(2d, MathsX.seqModulo(-8d, 10d), 0);
        Assert.assertEquals(1d, MathsX.seqModulo(-9d, 10d), 0);
        Assert.assertEquals(0d, MathsX.seqModulo(-10d, 10d), 0);
        Assert.assertEquals(9d, MathsX.seqModulo(-11d, 10d), 0);
    }
}
