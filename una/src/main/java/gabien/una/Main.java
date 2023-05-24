/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        System.out.println("UNA Self-Test");
        System.out.println("Loaded? " + UNA.defaultLoader());
        UNA.setupSysFlags();
        System.out.println("isWin32: " + UNA.isWin32);
        System.out.println("isBigEndian: " + UNA.isBigEndian);
        System.out.println("is32Bit: " + UNA.is32Bit);

        System.out.println("Architecture/OS: " + UNA.getArchOS());
        System.out.println("Size of pointers: " + UNA.getSizeofPtr());
        long purpose = UNA.getTestStringRaw();
        long strlen = UNA.strlen(purpose);

        ByteBuffer obj = UNA.newDirectByteBuffer(purpose, strlen);
        byte[] data = new byte[(int) strlen];
        obj.get(data);
        System.out.println("UTF-8 ByteBuffer retrieval test: " + new String(data, StandardCharsets.UTF_8));

        byte[] data2 = new byte[(int) strlen];
        UNA.getAB(purpose, strlen, data2, 0);
        System.out.println("UTF-8 GetRegion retrieval test: " + new String(data2, StandardCharsets.UTF_8));

        //System.out.println("dlsym: " + UNA.lookupBootstrap("dlsym"));
    }
}
