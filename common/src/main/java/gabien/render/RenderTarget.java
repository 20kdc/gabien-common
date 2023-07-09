/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.render;

import java.util.ArrayList;

import org.eclipse.jdt.annotation.Nullable;

import gabien.GaBIEn;
import gabien.natives.BadGPU;
import gabien.vopeks.VopeksImage;

/**
 * Implements essential functions of IImage for render targets.
 * (This is as opposed to immutable images, which don't need, say, a reference list)
 * Created 9th July 2023.
 */
public abstract class RenderTarget extends IImage {
    /**
     * Reference list.
     */
    private final ArrayList<IImage> referencedBy = new ArrayList<>();

    public RenderTarget(@Nullable String id, int w, int h) {
        super(id, w, h);
    }

    /**
     * Flushes batches of things that have batches attached to this surface.
     * Call immediately before any call to putTask that writes to this surface.
     * Will be internally called before batchStartGroup.
     */
    protected final synchronized void batchReferenceBarrier() {
        while (!referencedBy.isEmpty())
            referencedBy.remove(referencedBy.size() - 1).batchFlush();
    }

    @Override
    public final synchronized void batchReference(IImage caller) {
        // If this wasn't here, the caller could refer to an unfinished batch.
        // Where this becomes a problem is that the caller could submit the task, and the batch might still not be submitted.
        // Since any changes we do make would require a flush (because we have a reference), just flush now,
        //  rather than flushing on unreference or something.
        // (Also, we could be holding a reference to caller, which implies the ability to create reference loops.)
        batchFlush();
        referencedBy.add(caller);
    }

    @Override
    public final synchronized void batchUnreference(IImage caller) {
        referencedBy.remove(caller);
    }

    /**
     * Stop all drawing operations. Makes an OsbDriver unusable.
     */
    public abstract void shutdown();

    /**
     * Ok, so this is a particularly evil little call.
     * This is like getTextureFromTask, but it releases custody of the texture.
     * This irrevocably alters the image (to being non-existent).
     * As such, you can only do this on an IGrDriver.
     */
    public final @Nullable BadGPU.Texture releaseTextureCustodyFromTask() {
        BadGPU.Texture tex = texture;
        texture = null;
        return tex;
    }

    /**
     * This converts an IGrDriver into an immutable image.
     * Data-wise, this is an in-place operation and shuts down the IGrDriver.
     */
    public final synchronized IImage convertToImmutable(@Nullable String debugId) {
        batchFlush();
        VopeksImage res = new VopeksImage(GaBIEn.vopeks, debugId, getWidth(), getHeight(), (consumer) -> {
            GaBIEn.vopeks.putTask((instance) -> {
                consumer.accept(releaseTextureCustodyFromTask());
            });
        });
        shutdown();
        return res;
    }
}
