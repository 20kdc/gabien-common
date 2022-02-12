/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.uslx.vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This represents a raw filesystem backend with few capabilities.
 * Paths: "" is the root directory.
 *        "a" is a file or directory called "a" in the root directory.
 *        "a/b" is a file called "b" in the directory called "a" in the root directory.
 */
public abstract class FSBackend {
    /**
     * Gets the state of a specific file or directory.
     * Returns null if the file was not found.
     * @param fileName
     * @return nope
     * @throws IOException
     */
    public abstract XState getState(String fileName);

    /**
     * Opens a stream to read the file.
     * @return file stream
     */
    public abstract InputStream openRead(String fileName) throws IOException;

    /**
     * Opens a stream to write the file.
     * @return file stream
     */
    public abstract OutputStream openWrite(String fileName) throws IOException;

    /**
     * Updates the time of a file.
     * 
     * @param fileName Filename
     * @param time     The new time
     */
    public abstract void changeTime(String fileName, long time);

    /**
     * Deletes a file or an empty directory.
     * If it doesn't exist, this does not throw.
     * If any exception is generated during deletion, this will throw a runtime exception.
     */
    public abstract void delete(String fileName);

    /**
     * Creates an empty directory.
     */
    public abstract void mkdir(String fileName);

    /**
     * Creates directories to contain the given file.
     */
    public void ensureDirsToContain(String fileName) {
        int idx = fileName.lastIndexOf('/');
        if (idx != -1) {
            String dirName = fileName.substring(0, idx);
            ensureDirsToContain(dirName);
            XState state = getState(dirName);
            if (state == null) {
                mkdir(dirName);
            } else if (!(state instanceof DirectoryState)) {
                throw new RuntimeException("Conflict between directory and file at " + this + ":" + fileName);
            }
        }
    }

    /**
     * Splits off the last part of a path.
     * Returns null if there is no directory separator.
     */
    public static String dirname(String fileName) {
        int idx = fileName.lastIndexOf('/');
        if (idx != -1)
            return fileName.substring(0, idx);
        return null;
    }

    /**
     * Splits off the last part of a path.
     */
    public static String basename(String fileName) {
        int idx = fileName.lastIndexOf('/');
        if (idx != -1)
            return fileName.substring(idx + 1);
        return fileName;
    }

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
}
