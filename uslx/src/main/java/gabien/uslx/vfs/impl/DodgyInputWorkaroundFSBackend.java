/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.vfs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.vfs.FSBackend;
import gabien.uslx.vfs.PathModel;

/**
 * Some stuff doesn't properly handle case/etc.
 * This is Very Bad.
 * Thus, this.
 * Created 13th February 2024.
 */
public class DodgyInputWorkaroundFSBackend extends FSBackend {
    public final FSBackend target;

    public DodgyInputWorkaroundFSBackend(FSBackend target) {
        this(target.parent != null ? new DodgyInputWorkaroundFSBackend(target.parent) : null, new DodgyInputWorkaroundPathModel(target.pathModel), target);
    }

    private DodgyInputWorkaroundFSBackend(FSBackend myParent, PathModel pm, FSBackend target) {
        super(myParent, pm, target.usesRootPathLogic);
        this.target = target;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DodgyInputWorkaroundFSBackend) {
            DodgyInputWorkaroundFSBackend o = (DodgyInputWorkaroundFSBackend) obj;
            return target.equals(o.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    protected @NonNull FSBackend intoInner(String dirName) {
        if (dirName.equals(".."))
            return parentOrRoot;
        // Fast-path if the entry already exists.
        FSBackend tmp = target.into(dirName);
        if (tmp.getState() != null)
            return new DodgyInputWorkaroundFSBackend(tmp);
        // Try to fix case.
        XState xs = getState();
        if (xs instanceof DirectoryState)
            for (String s2 : ((DirectoryState) xs).entries)
                if (pathModel.estimatePathComponentEquality(s2, dirName))
                    return new DodgyInputWorkaroundFSBackend(target.into(s2));
        // Well, didn't work.
        return new DodgyInputWorkaroundFSBackend(tmp);
    }

    @Override
    public String getAbsolutePath() {
        return target.getAbsolutePath();
    }

    @Override
    public boolean mkdir() {
        return target.mkdir();
    }

    @Override
    public @Nullable XState getState() {
        return target.getState();
    }

    @Override
    public @NonNull InputStream openRead() throws IOException {
        return target.openRead();
    }

    @Override
    public @NonNull OutputStream openWrite() throws IOException {
        return target.openWrite();
    }

    @Override
    public void changeTime(long time) {
        target.changeTime(time);
    }

    @Override
    public boolean delete() {
        return target.delete();
    }

}
