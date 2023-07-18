/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.backend;

import org.eclipse.jdt.annotation.NonNull;

import gabien.natives.BadGPU;
import gabien.render.AtlasPage;
import gabien.render.ITexRegion;

/**
 * Created 18th July, 2023.
 */
public class NullAtlasPage extends AtlasPage {
    public NullAtlasPage() {
        super(null, 0, 0);
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull int[] buffer, int bufferOfs, @NonNull Runnable onDone) {
        onDone.run();
    }

    @Override
    public void getPixelsAsync(int x, int y, int w, int h, BadGPU.TextureLoadFormat format, @NonNull byte[] buffer, int bufferOfs, @NonNull Runnable onDone) {
        onDone.run();
    }

    @Override
    public void copyFrom(int srcx, int srcy, int srcw, int srch, int targetx, int targety, ITexRegion base) {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public void batchFlush() {
    }
}
