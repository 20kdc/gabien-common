/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

import org.junit.Assert;
import org.junit.Test;

import gabien.uslx.append.PrimStack;

/**
 * Created 14st May 2024.
 */
public class PrimStackTest {
    @Test
    public void testPrimStack() {
        PrimStack.I32 test = new PrimStack.I32();

        // test push/pop
        test.push(1);
        test.push(2);
        test.push(3);
        Assert.assertEquals(test.pop(), 3);
        Assert.assertEquals(test.pop(), 2);
        Assert.assertEquals(test.pop(), 1);

        // test array push
        test.push(new int[] {9, 8, 4, 5, 6}, 2, 3);
        Assert.assertEquals(test.pop(), 6);
        Assert.assertEquals(test.pop(), 5);
        Assert.assertEquals(test.pop(), 4);

        // test array pop
        test.push(1);
        test.push(2);
        test.push(3);
        int[] tmp = new int[6];
        test.pop(tmp, 2, 3);
        Assert.assertEquals(tmp[2], 1);
        Assert.assertEquals(tmp[3], 2);
        Assert.assertEquals(tmp[4], 3);
    }
}
