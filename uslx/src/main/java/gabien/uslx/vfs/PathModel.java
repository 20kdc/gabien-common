/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.vfs;

import java.io.IOException;

/**
 * Oh, what fun...
 * Created 13th February, 2024.
 */
public abstract class PathModel {
    /**
     * Used to fake Windows case-folding in code that's pretending to be a Windows FS
     */
    public abstract boolean estimatePathComponentEquality(String a, String b);

    /**
     * Used to fake Windows case-folding in code that's pretending to be a Windows FS
     */
    public abstract String estimatePathComponentFolding(String a);

    /**
     * Verifies a path component does not contain separators.
     * "isFirstAbsComponent" determines if this is the first absolute component (i.e. for Windows stuff).
     * Notably, empty path components are NOT ALLOWED
     */
    public abstract boolean verifyPathComponent(boolean isFirstAbsComponent, String dirName);

    /**
     * Gets the last element of any non-empty path.
     * This does not throw errors.
     */
    public abstract String nameOf(String anyPath);

    /**
     * Splits a relative path into a list of components.
     * These components are then verified.
     * Must actually be a relative path.
     * Notably, this function doesn't mind empty path components (it ignores them).
     */
    public abstract String[] relPathToComponents(String absolutePath) throws IOException;

    /**
     * Splits an absolute path into a list of components.
     * These components are then verified.
     * Must actually be an absolute path.
     * Notably, this function doesn't mind empty path components (it ignores them).
     */
    public abstract String[] absPathToComponents(String absolutePath) throws IOException;
}
