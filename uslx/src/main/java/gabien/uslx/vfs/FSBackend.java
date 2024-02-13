/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.vfs;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is meant to be a reliable version of Java's File class.
 */
public abstract class FSBackend {
    /**
     * Root FSBackend.
     * Note that this may not be where you started if crossing FSBackends.
     * This happens in tests for RO access to real FS.
     * Arguably, tests should have their own resources directory for this. Oh well.
     */
    public final @NonNull FSBackend root;

    /**
     * Parent FSBackend, if any.
     */
    public final @Nullable FSBackend parent;

    /**
     * How paths work on this system.
     */
    public final @NonNull PathModel pathModel;

    public final boolean usesRootPathLogic;

    public FSBackend(@Nullable FSBackend parent, @NonNull PathModel pathModel, boolean rpl) {
        this.root = parent != null ? parent.root : this;
        this.parent = parent;
        this.pathModel = pathModel;
        this.usesRootPathLogic = rpl;
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    /**
     * Descends an element.
     * Note that you can descend into an element which does not exist.
     * Returns PathModel.InvalidPath if invalid.
     */
    public final @NonNull FSBackend into(String dirName) {
        if (dirName.equals("."))
            return this;
        if (!pathModel.verifyPathComponent(usesRootPathLogic, dirName))
            return new InvalidPath(pathModel, this, dirName);
        return intoInner(dirName);
    }
    /**
     * into but chained.
     */
    public final @NonNull FSBackend into(String... dirNames) {
        FSBackend res = this;
        for (String s : dirNames)
            res = res.into(s);
        return res;
    }

    /**
     * Descends an element.
     * Note that you can descend into an element which does not exist.
     * Returns PathModel.InvalidPath if invalid.
     */
    protected abstract @NonNull FSBackend intoInner(String dirName);

    /**
     * Like into, but works on any path.
     */
    public final @NonNull FSBackend intoPath(String path) {
        FSBackend fsb = intoRelPath(path);
        if (fsb instanceof InvalidPath)
            fsb = intoAbsPath(path);
        return fsb;
    }

    /**
     * Like into, but works on a relative path.
     */
    public final @NonNull FSBackend intoRelPath(String path) {
        FSBackend res = this;
        try {
            for (String s : pathModel.relPathToComponents(path))
                res = res.intoInner(s);
        } catch (IOException ioe) {
            return new InvalidPath(pathModel, this, path);
        }
        return res;
    }

    /**
     * Like into, but works on a relative path.
     */
    public final @NonNull FSBackend intoAbsPath(String path) {
        FSBackend res = root;
        try {
            for (String s : pathModel.absPathToComponents(path))
                res = res.intoInner(s);
        } catch (IOException ioe) {
            return new InvalidPath(pathModel, root, path);
        }
        return res;
    }

    /**
     * Returns an absolute path.
     * Notably, if one is known, this is specifically defined to return the absolute path of the Java File object despite any wrapping.
     */
    public abstract String getAbsolutePath();

    /**
     * Attempts to create a directory here. Returns true if the directory was created or already exists.
     * Returns false otherwise. Note that failure can happen due to the parent simply not existing.
     * It can also happen due to a very sudden deletion.
     */
    public abstract boolean mkdir();

    /**
     * Make this directory and parents.
     */
    public final void mkdirs() {
        if (parent != null)
            parent.mkdirs();
        mkdir();
    }

    /**
     * Make parent directories.
     */
    public final FSBackend parentMkdirs() {
        if (parent != null)
            parent.mkdirs();
        return this;
    }

    /**
     * Gets the state of a specific file or directory.
     * Returns null if the file was not found.
     * @param fileName
     * @return nope
     * @throws IOException
     */
    public abstract @Nullable XState getState();

    /**
     * If this is an existing directory.
     */
    public final boolean isDirectory() {
        return getState() instanceof DirectoryState; 
    }

    /**
     * If this exists in any fashion.
     */
    public final boolean exists() {
        return getState() != null;
    }

    /**
     * Opens a stream to read the file.
     * @return file stream
     */
    public abstract @NonNull InputStream openRead() throws IOException;

    /**
     * Will either mmap the file for reading or read it in and create a ByteBuffer from the contents.
     */
    public @NonNull ByteBuffer mapOrRead() throws IOException {
        InputStream inp = openRead();
        try {
            if (inp instanceof FileInputStream) {
                FileInputStream fis = (FileInputStream) inp;
                FileChannel fc = fis.getChannel();
                MappedByteBuffer mbb = fc.map(MapMode.PRIVATE, 0, fc.size());
                fis.close();
                return mbb;
            }
        } catch (Exception ex) {
            // just in case :D
        }
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        byte[] tmp2 = new byte[256];
        while (true) {
            int am = inp.read(tmp2);
            if (am < 0)
                break;
            tmp.write(tmp2, 0, am);
        }
        return ByteBuffer.wrap(tmp.toByteArray());
    }

    /**
     * Opens a stream to write the file.
     * @return file stream
     */
    public abstract @NonNull OutputStream openWrite() throws IOException;

    /**
     * Updates the time of a file.
     * 
     * @param time     The new time
     */
    public abstract void changeTime(long time);

    /**
     * Deletes a file or an empty directory.
     * Returns true if successfully deleted, false otherwise.
     * Importantly, if it didn't exist, it can't be deleted, so returns false.
     */
    public abstract boolean delete();

    public static class XState {
    }

    public static class FileState extends XState {
        /**
         * File size in bytes.
         */
        public final long size;

        public FileState(long s) {
            size = s;
        }
    }

    /**
     * Like FileState, but implies that modification time is supported on this filesystem.
     */
    public static class FileTimeState extends FileState {
        /**
         * Unix time in milliseconds.
         */
        public final long time;

        public FileTimeState(long s, long t) {
            super(s);
            time = t;
        }
    }

    public static class DirectoryState extends XState {
        public final String[] entries;

        public DirectoryState(String[] ents) {
            entries = ents;
        }
    }

    /**
     * Generic "this path does not work" FSBackend.
     * All Null FSBackends are guaranteed to never resolve to anything.
     * Can be escaped from with "..", though.
     */
    public static class Null extends FSBackend {
        public final String myName;

        public Null(PathModel pathModel, FSBackend parent, String myName, boolean rpl) {
            super(parent, pathModel, rpl);
            this.myName = myName;
        }

        public Null makeDownLevel(String name) {
            return new Null(pathModel, this, name, false);
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
        public String toString() {
            return "invalid:" + getAbsolutePath();
        }

        @Override
        public String getAbsolutePath() {
            return parent.getAbsolutePath() + "/" + myName;
        }

        @Override
        public @NonNull FSBackend intoInner(String dirName) {
            if (dirName.equals(".."))
                return parent;
            return makeDownLevel(dirName);
        }

        @Override
        public boolean mkdir() {
            return false;
        }

        @Override
        public @Nullable XState getState() {
            return null;
        }

        @Override
        public @NonNull InputStream openRead() throws IOException {
            throw new IOException("Invalid access: " + getAbsolutePath());
        }

        @Override
        public @NonNull OutputStream openWrite() throws IOException {
            throw new IOException("Invalid access: " + getAbsolutePath());
        }

        @Override
        public void changeTime(long time) {
        }

        @Override
        public boolean delete() {
            return false;
        }
    }

    /**
     * Signal that indicates an invalid path.
     */
    public static class InvalidPath extends Null {
        public InvalidPath(PathModel pathModel, FSBackend parent, String path) {
            super(pathModel, parent, path, false);
        }
        @Override
        public Null makeDownLevel(String name) {
            return new InvalidPath(pathModel, this, name);
        }
    }
}
