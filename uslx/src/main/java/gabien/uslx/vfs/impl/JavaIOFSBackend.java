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

import gabien.uslx.vfs.*;

/**
 * IO backend based on java.io.File
 */
public class JavaIOFSBackend extends FSBackend {
    public JavaIOFSBackend() {
    }

    @Override
    public String toString() {
        return "real";
    }

    public File asFile(String fileName) {
        return new File(fileName);
    }

    @Override
    public XState getState(String fileName) {
        File f = asFile(fileName);
        if (!f.exists())
            return null;
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            if (list == null) {
                System.err.println("File that is a directory but isn't listable: " + fileName + " (probably an access error)");
                return new DirectoryState(new String[0]);
            }
            String[] ents = new String[list.length];
            for (int i = 0; i < ents.length; i++)
                ents[i] = list[i].getName();
            return new DirectoryState(ents);
        }
        return new FileTimeState(f.length(), f.lastModified());
    }

    @Override
    public @NonNull InputStream openRead(String fileName) throws IOException {
        return new FileInputStream(asFile(fileName));
    }

    @Override
    public @NonNull OutputStream openWrite(String fileName) throws IOException {
        return new FileOutputStream(asFile(fileName));
    }

    @Override
    public String parentOf(String fileName) {
        File fn = asFile(fileName);
        String parent = fn.getParent();
        if (parent == null)
            parent = asFile(absolutePathOf(fileName)).getParent();
        return parent;
    }

    @Override
    public String nameOf(String fileName) {
        return asFile(fileName).getName();
    }

    @Override
    public String absolutePathOf(String fileName) {
        return asFile(fileName).getAbsolutePath();
    }

    @Override
    public void delete(String fileName) {
        File fn = asFile(fileName);
        fn.delete();
        if (fn.exists())
            throw new RuntimeException("Failed to delete file.");
    }

    @Override
    public void mkdir(String fileName) {
        File fn = asFile(fileName);
        fn.mkdir();
        if (!fn.isDirectory())
            throw new RuntimeException("Failed to create directory.");
    }

    @Override
    public void changeTime(String fileName, long time) {
        asFile(fileName).setLastModified(time);
    }
}
