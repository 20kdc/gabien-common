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
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.vfs.FSBackend;

/**
 * Represents the union of several filesystems.
 * Think ID Software/Valve style moddable games.
 * Note that using this with conflicting path models is allowed but not necessarily a good idea.
 * Created 13th February, 2024.
 */
public final class UnionFSBackend extends FSBackend {
    private final FSBackend mutable;
    private final FSBackend[] parents;

    public UnionFSBackend(FSBackend mutable, FSBackend... parents) {
        this(null, mutable, parents);
    }

    private UnionFSBackend(FSBackend parent, FSBackend mutable, FSBackend... parents) {
        super(parent, mutable.pathModel, mutable.usesRootPathLogic);
        this.mutable = mutable;
        this.parents = parents;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UnionFSBackend) {
            UnionFSBackend other = (UnionFSBackend) obj;
            if (!other.mutable.equals(mutable))
                return false;
            // alright, detailed check
            if (other.parents.length != parents.length)
                return false;
            for (int i = 0; i < parents.length; i++)
                if (!parents[i].equals(other.parents[i]))
                    return false;
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mutable.hashCode();
    }

    @Override
    public String toString() {
        return "union:" + mutable.toString();
    }

    @Override
    protected @NonNull FSBackend intoInner(String dirName) {
        if (dirName.equals(".."))
            return parentOrRoot;
        FSBackend[] newParents = new FSBackend[parents.length];
        for (int i = 0; i < newParents.length; i++)
            newParents[i] = parents[i].into(dirName);
        return new UnionFSBackend(this, mutable.into(dirName), newParents);
    }

    @Override
    public String getAbsolutePath() {
        return mutable.getAbsolutePath();
    }

    @Override
    public boolean mkdir() {
        return mutable.mkdir();
    }

    @Override
    public @Nullable XState getState() {
        // Ok, so this is a fun one, because directories exist!
        XState[] allStates = new XState[parents.length + 1];
        XState firstState = allStates[0] = mutable.getState();
        for (int i = 0; i < parents.length; i++) {
            XState thisState = parents[i].getState();
            allStates[i + 1] = thisState;
            if (firstState == null)
                firstState = thisState;
        }
        // Alright, see if this is a directory
        if (firstState instanceof DirectoryState) {
            // It is, merge contents
            ArrayList<String> children = new ArrayList<String>(((DirectoryState) firstState).entries.length);
            HashSet<String> childrenOverlap = new HashSet<String>();
            for (XState chk : allStates) {
                if (!(chk instanceof DirectoryState))
                    continue;
                for (String s : ((DirectoryState) chk).entries)
                    if (childrenOverlap.add(pathModel.estimatePathComponentFolding(s)))
                        children.add(s);
            }
            return new DirectoryState(children.toArray(new String[children.size()]));
        }
        return firstState;
    }

    @Override
    public @NonNull InputStream openRead() throws IOException {
        try {
            return mutable.openRead();
        } catch (IOException savedEX) {
            for (FSBackend fsb : parents) {
                try {
                    return fsb.openRead();
                } catch (IOException ex) {
                }
            }
            throw savedEX;
        }
    }

    @Override
    public @NonNull OutputStream openWrite() throws IOException {
        // when using a union fs, we need to account for directories that might not exist here
        // but they do exist elsewhere
        FSBackend parent = this.parent;
        if (parent != null)
            if (parent.getState() instanceof DirectoryState)
                parent.mkdirs();
        return mutable.openWrite();
    }

    @Override
    public void changeTime(long time) {
        mutable.changeTime(time);
    }

    @Override
    public boolean delete() {
        return mutable.delete();
    }

}
