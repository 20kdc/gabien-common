/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.vfs.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.vfs.FSBackend;

/**
 * RAM backend. Useful for tests and such. Always uses the Unix path model.
 * Created 13th February, 2024.
 */
public final class RAMFSBackend extends FSBackend {
    public final VFSDir vfsRoot;
    public final String myName;

    public RAMFSBackend() {
        super(null, UnixPathModel.INSTANCE);
        vfsRoot = new VFSDir();
        myName = "";
    }

    private RAMFSBackend(@NonNull RAMFSBackend parent, String myName) {
        super(parent, UnixPathModel.INSTANCE);
        vfsRoot = parent.vfsRoot;
        this.myName = myName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RAMFSBackend) {
            RAMFSBackend r = (RAMFSBackend) obj;
            if (r.vfsRoot != vfsRoot)
                return false;
            if ((r.parent == null) != (this.parent == null))
                return false;
            if (r.parent != null) 
                if (!r.parent.equals(parent))
                    return false;
            return r.myName.equals(myName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ((parent != null) ? parent.hashCode() : 0) ^ myName.hashCode();
    }

    @Override
    public String toString() {
        return "ram:" + getAbsolutePath();
    }

    public @Nullable VFSNode getParentVFSNode() {
        if (parent == null)
            return null;
        return ((RAMFSBackend) parent).getVFSNode();
    }

    public @Nullable VFSNode getVFSNode() {
        if (parent == null)
            return vfsRoot;
        VFSNode parentVFSNode = ((RAMFSBackend) parent).getVFSNode();
        if (parentVFSNode == null)
            return null;
        return parentVFSNode.descend(myName);
    }

    @Override
    public void changeTime(long time) {
    }

    @Override
    public boolean delete() {
        VFSNode vn = getParentVFSNode();
        if (vn instanceof VFSDir) {
            VFSDir vd = (VFSDir) vn;
            VFSNode me = vd.contents.get(myName);
            if (me == null)
                return false;
            if (!me.isDeletable())
                return false;
            return vd.contents.remove(myName) != null;
        }
        return false;
    }

    @Override
    public String getAbsolutePath() {
        if (parent == null)
            return "/";
        if (parent.parent == null)
            return parent.getAbsolutePath() + myName;
        return parent.getAbsolutePath() + "/" + myName;
    }

    @Override
    public @Nullable XState getState() {
        VFSNode vn = getVFSNode();
        return vn != null ? vn.toState() : null;
    }

    @Override
    public @NonNull FSBackend intoInner(String dirName) {
        if (dirName.equals(".."))
            return parent;
        return new RAMFSBackend(this, dirName);
    }

    @Override
    public boolean mkdir() {
        VFSNode parent = getParentVFSNode();
        if (parent == null)
            return false;
        if (!(parent instanceof VFSDir))
            return false;
        VFSDir pd = (VFSDir) parent;
        VFSNode existing = pd.contents.get(myName);
        if (existing instanceof VFSDir) {
            return true;
        } else if (existing == null) {
            pd.contents.put(myName, new VFSDir());
            return true;
        } else {
            // file/dir overlap!
            return false;
        }
    }

    @Override
    public @NonNull InputStream openRead() throws IOException {
        VFSNode vn = getVFSNode();
        if (!(vn instanceof VFSFile))
            throw new FileNotFoundException(getAbsolutePath());
        return new ByteArrayInputStream(((VFSFile) vn).contents.toByteArray());
    }

    @Override
    public @NonNull OutputStream openWrite() throws IOException {
        VFSNode parent = getParentVFSNode();
        if (parent == null)
            throw new FileNotFoundException(getAbsolutePath());
        if (!(parent instanceof VFSDir))
            throw new FileNotFoundException(getAbsolutePath());
        VFSDir pd = (VFSDir) parent;
        VFSNode existing = pd.contents.get(myName);
        if (existing instanceof VFSFile || existing == null) {
            VFSFile vf = new VFSFile();
            pd.contents.put(myName, vf);
            return vf.contents;
        } else {
            // overlap
            throw new FileNotFoundException(getAbsolutePath());
        }
    }

    // -- actual VFS --

    public static abstract class VFSNode {
        public abstract @Nullable VFSNode descend(String value);
        public abstract boolean isDeletable();
        public abstract XState toState();
    }

    public static class VFSDir extends VFSNode {
        public final HashMap<String, VFSNode> contents = new HashMap<>();

        @Override
        public @Nullable VFSNode descend(String value) {
            return contents.get(value);
        }

        @Override
        public boolean isDeletable() {
            return contents.isEmpty();
        }

        @Override
        public XState toState() {
            return new DirectoryState(contents.keySet().toArray(new String[0]));
        }
    }

    public static class VFSFile extends VFSNode {
        public final ByteArrayOutputStream contents = new ByteArrayOutputStream();

        @Override
        public @Nullable VFSNode descend(String value) {
            return null;
        }

        @Override
        public boolean isDeletable() {
            return true;
        }

        @Override
        public XState toState() {
            return new FileState(contents.size());
        }
    }
}
