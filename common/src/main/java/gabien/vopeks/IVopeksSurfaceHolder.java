/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.vopeks;

import gabien.IImage;
import gabien.natives.BadGPU;

/**
 * All images must be backed by one of these.
 * Created on August 21th, 2017, severely altered 7th June, 2023.
 */
public interface IVopeksSurfaceHolder extends IImage {
    BadGPU.Texture getTextureFromTask();
}
