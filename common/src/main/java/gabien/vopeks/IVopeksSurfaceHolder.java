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
    /**
     * Flushes any queued operations to the Vopeks task queue.
     * This ensures that any operations you subsequently schedule run after the preceding operations.
     */
    void batchFlush();

    /**
     * This is used when the caller is writing this image into its own batch.
     * Ensures that any changes to this image first flush the batch of the caller.
     * This ensures that the ordering remains correct.
     * This can and will lead to false positives, but it's better than the nightmare I envisioned for perfection.
     */
    void batchReference(IVopeksSurfaceHolder caller);

    /**
     * Gets a texture. Run from task code.
     */
    BadGPU.Texture getTextureFromTask();
}
