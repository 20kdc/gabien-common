/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.render;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Replicated texture region.
 * Created 17th July, 2023.
 */
public final class ReplicatedTexRegion implements IReplicatedTexRegion {
    private ITexRegion[] regions = new ITexRegion[0];
    public final float width, height;

    public ReplicatedTexRegion(float w, float h) {
        this.width = w;
        this.height = h;
    }

    public synchronized void addRegion(ITexRegion n) {
        ITexRegion[] last = regions;
        regions = new ITexRegion[last.length + 1];
        System.arraycopy(last, 0, regions, 0, last.length);
        regions[last.length] = n;
    }

    @Override
    public float getRegionWidth() {
        return width;
    }

    @Override
    public float getRegionHeight() {
        return height;
    }

    @Override
    public ITexRegion pickTexRegion(@Nullable IImage lastSurface) {
        ITexRegion[] rs = regions;
        if (lastSurface != null)
            for (ITexRegion itr : rs)
                if (itr.getSurface() == lastSurface)
                    return itr;
        return regions[0];
    }

    @Override
    @NonNull
    public ReplicatedTexRegion subRegion(float x, float y, float w, float h) {
        ReplicatedTexRegion res = new ReplicatedTexRegion(w, h);
        res.regions = new ITexRegion[regions.length];
        for (int i = 0; i < regions.length; i++)
            res.regions[i] = regions[i].subRegion(x, y, w, h);
        return res;
    }
}
