/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

/**
 * Wrapper for "stb_vorbis_g", a customized version of stb_vorbis specifically for GaBIEn use.
 * Created 19th October, 2023.
 */
public class VorbisEnum {
    public enum Error {
        None(0),
        OutOfMem(1),
        UnexpectedEOF(10),
        InvalidSetup(11),
        InvalidStream(12),
        InvalidFirstPage(13),
        BadPacketType(14);

        public final int value;
        private Error(int ev) {
            value = ev;
        }
    }
}
