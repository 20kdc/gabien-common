/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.vfs.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.vfs.*;

/**
 * IO backend based on java.io.File.
 */
public final class JavaIOFSBackend extends FSBackend {
    public static final FSBackend ROOT;
    static {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            ROOT = new JavaIOFSBackend(UnixPathModel.INSTANCE, null, new File("/"));
        } else {
            ROOT = WindowsRootJavaIOFSBackend.INSTANCE;
        }
    }

    public final @NonNull File file;

    private static File attemptCanonicalize(File f) {
        try {
            return f.getCanonicalFile();
        } catch (Exception ex) {
            return f;
        }
    }

    /**
     * Figure out a sensible FSBackend chain from a java.io.File.
     */
    public static FSBackend from(File f) {
        return from(f, null, null);
    }

    /**
     * Figure out a sensible FSBackend chain from a java.io.File.
     */
    public static FSBackend from(File f, @Nullable JavaIOFSBackend reuseHint1, @Nullable JavaIOFSBackend reuseHint2) {
        f = attemptCanonicalize(f);
        // do we already have this?
        if (reuseHint1 != null)
            if (f.equals(reuseHint1.file))
                return reuseHint1;
        if (reuseHint2 != null)
            if (f.equals(reuseHint2.file))
                return reuseHint2;
        // no we don't
        File fParentFile = f.getParentFile();
        if (fParentFile == null) {
            // this is a root
            // what this means depends on our platform
            if (ROOT instanceof WindowsRootJavaIOFSBackend) {
                return new JavaIOFSBackend(WindowsPathModel.INSTANCE, ROOT, f);
            } else {
                return ROOT;
            }
        } else {
            FSBackend parent = from(fParentFile, reuseHint1, reuseHint2);
            return new JavaIOFSBackend(parent.pathModel, parent, f);
        }
    }

    /**
     * Only use if you know what you're doing.
     */
    private JavaIOFSBackend(@NonNull PathModel pm, @Nullable FSBackend parent, @NonNull File f) {
        super(parent, pm, false);
        file = f;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JavaIOFSBackend)
            return ((JavaIOFSBackend) obj).file.equals(file);
        return false;
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public FSBackend intoInner(String dirName) {
        // reuse what we can if possible
        JavaIOFSBackend potentialReuseHint = null;
        if (parent instanceof JavaIOFSBackend)
            potentialReuseHint = (JavaIOFSBackend) parent;
        return JavaIOFSBackend.from(new File(file, dirName), this, potentialReuseHint);
    }

    @Override
    public String toString() {
        return "real:" + file;
    }

    @Override
    public XState getState() {
        if (!file.exists())
            return null;
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            if (list == null) {
                System.err.println("File that is a directory but isn't listable: " + file + " (probably an access error)");
                return new DirectoryState(new String[0]);
            }
            String[] ents = new String[list.length];
            for (int i = 0; i < ents.length; i++)
                ents[i] = list[i].getName();
            return new DirectoryState(ents);
        }
        return new FileTimeState(file.length(), file.lastModified());
    }

    @Override
    public @NonNull InputStream openRead() throws IOException {
        return new FileInputStream(file);
    }

    @Override
    public @NonNull OutputStream openWrite() throws IOException {
        return new FileOutputStream(file);
    }

    @Override
    public boolean delete() {
        return file.delete();
    }

    @Override
    public boolean mkdir() {
        return file.mkdir() || file.isDirectory();
    }

    @Override
    public void changeTime(long time) {
        file.setLastModified(time);
    }

    @Override
    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }
}
