/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.vfs.impl;

import java.io.IOException;

import gabien.uslx.vfs.PathModel;

/**
 * Oh dear goodness.
 * Created 13th February, 2024.
 */
public class DodgyInputWorkaroundPathModel extends PathModel {
    public final PathModel base;

    public DodgyInputWorkaroundPathModel(PathModel pm) {
        base = pm;
    }

    @Override
    public String estimatePathComponentFolding(String a) {
        return a.toLowerCase();
    }

    @Override
    public boolean estimatePathComponentEquality(String a, String b) {
        return a.equalsIgnoreCase(b);
    }

    @Override
    public boolean verifyPathComponent(boolean isFirstAbsComponent, String dirName) {
        if (dirName.contains("\\"))
            return false;
        if (dirName.contains("¥"))
            return false;
        if (dirName.contains("₩"))
            return false;
        return base.verifyPathComponent(isFirstAbsComponent, dirName);
    }

    /**
     * Adjusts the slashes to account for encoding errors such as the infamous "C:¥".
     */
    public String adjustSlashes(String anyPath) {
        return anyPath.replace('\\', '/').replace('¥', '/').replace('₩', '/');
    }

    @Override
    public String nameOf(String anyPath) {
        return base.nameOf(adjustSlashes(anyPath));
    }

    @Override
    public String[] relPathToComponents(String absolutePath) throws IOException {
        return base.relPathToComponents(adjustSlashes(absolutePath));
    }

    @Override
    public String[] absPathToComponents(String absolutePath) throws IOException {
        return base.absPathToComponents(adjustSlashes(absolutePath));
    }
}
