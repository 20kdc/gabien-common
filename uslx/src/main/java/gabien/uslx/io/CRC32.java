/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.io;

/**
 * CRC32!
 * Created 19th October, 2023.
 */
public final class CRC32 {
    public final int polynomialReversed;

    private final int[] crc32tab = new int[256];

    /**
     * Pretty much the standard CRC32
     */
    public static final CRC32 CRC32_04C11DB7 = new CRC32(0xEDB88320);

    public CRC32(int p) {
        polynomialReversed = p;
        for (int i = 0; i < crc32tab.length; i++) {
            int crc = i;
            for (int j = 0; j < 8; j++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ p;
                } else {
                    crc = crc >>> 1;
                }
            }
            crc32tab[i] = crc;
        }
    }

    public int update(int v, byte[] data) {
        for (byte b : data) {
            int lb = (b & 0xFF) ^ (v & 0xFF);
            v = crc32tab[lb] ^ (v >>> 8);
         }
        return v;
    }

    public int update(int v, byte b) {
        int lb = (b & 0xFF) ^ (v & 0xFF);
        v = crc32tab[lb] ^ (v >>> 8);
        return v;
    }
}
