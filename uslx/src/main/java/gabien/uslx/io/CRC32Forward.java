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
public final class CRC32Forward implements ImmutableChecksum32 {
    public final int polynomial;

    private final int[] crc32tab = new int[256];

    /**
     * CRC32 as used in Ogg
     */
    public static final CRC32Forward CRC32_04C11DB7 = new CRC32Forward(0x04C11DB7);

    public CRC32Forward(int p) {
        polynomial = p;
        for (int i = 0; i < crc32tab.length; i++) {
            int crc = i << 24;
            for (int j = 0; j < 8; j++) {
                if (crc < 0) {
                    crc = (crc << 1) ^ p;
                } else {
                    crc = crc << 1;
                }
            }
            crc32tab[i] = crc;
        }
    }

    @Override
    public int update(int v, byte[] data, int offset, int length) {
        while (length > 0) {
            int lb = (data[offset++] & 0xFF) ^ (v >>> 24);
            v = crc32tab[lb] ^ (v << 8);
            length--;
        }
        return v;
    }

    @Override
    public int update(int v, byte b) {
        int lb = (b & 0xFF) ^ (v >>> 24);
        v = crc32tab[lb] ^ (v << 8);
        return v;
    }
}
