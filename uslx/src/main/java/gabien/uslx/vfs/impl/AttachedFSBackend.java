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
 * The dumbest FS backend that ever lived.
 * In short, this is a root that creates a "directory" pointing to another FSBackend.
 * Created 13th February 2024.
 */
public class AttachedFSBackend extends FSBackend {
    public final String attachmentName;
    public final FSBackend attachment;

    public AttachedFSBackend(FSBackend target, String name) {
        this(target.pathModel, target, name);
    }

    public AttachedFSBackend(PathModel pm, FSBackend target, String name) {
        super(null, pm);
        attachment = target;
        attachmentName = name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    protected @NonNull FSBackend intoInner(String dirName) {
        if (pathModel.estimatePathComponentEquality(dirName, attachmentName)) {
            return attachment;
        } else {
            return new FSBackend.Null(pathModel, parent, dirName);
        }
    }

    @Override
    public String getAbsolutePath() {
        return "/";
    }

    @Override
    public boolean mkdir() {
        return true;
    }

    @Override
    public @Nullable XState getState() {
        return new DirectoryState(new String[] {attachmentName});
    }

    @Override
    public @NonNull InputStream openRead() throws IOException {
        return null;
    }

    @Override
    public @NonNull OutputStream openWrite() throws IOException {
        return null;
    }

    @Override
    public void changeTime(long time) {
    }

    @Override
    public boolean delete() {
        return false;
    }

}
