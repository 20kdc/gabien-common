/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.text;

/**
 * Variable-size font interface.
 * This can be, and is intentionally allowed to be, implemented both by userspace and by the gabien backend.
 * Also, some platforms don't require allocation to change font size, and some do.
 * Lowest common denominator says require the allocation.
 * Created 16th February 2023.
 */
public interface IVariableSizeFont {
    /**
     * Specializes the variable-size font into a fixed-size font for actual use.
     * GaBIEn measures size as inter-line height in pixels.
     * This was chosen to integrate well with low-resolution UI.
     */
    IFixedSizeFont specialize(int size);
}
