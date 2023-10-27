/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

import org.junit.Assert;
import org.junit.Test;

import gabien.uslx.io.CRC32Forward;
import gabien.uslx.io.CRC32Reversed;
import gabien.uslx.io.HexByteEncoding;

/**
 * Created 27th October 2023.
 */
public class CRC32Test {
    @Test
    public void testPNGCRC32() {
        byte[] test = HexByteEncoding.fromHexString("4948445200000A28000001C20806000000");
        int res = CRC32Reversed.CRC32_EDB88320.update(-1, test, 0, test.length) ^ -1;
        // System.out.println(Integer.toHexString(res));
        Assert.assertEquals(0x7ff9d3b9, res);
    }
    @Test
    public void testOGGCRC32() {
        // oggz-tools is very useful for this sort of thing
        byte[] test = HexByteEncoding.fromHexString(
            // 0
            "4f 67 67 53 00 02 00 00" +
            // 8
            "00 00 00 00 00 00 00 00" +
            // 16
            "00 00 00 00 00 00 00 00" +
            // 24
            "00 00 01 00"
        );
        int res = CRC32Forward.CRC32_04C11DB7.update(0, test, 0, test.length);
        //System.out.println(Integer.toHexString(res));
        Assert.assertEquals(0x66B718E8, res);
    }
}
