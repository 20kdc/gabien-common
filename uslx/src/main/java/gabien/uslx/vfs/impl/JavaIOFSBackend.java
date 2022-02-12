/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.uslx.vfs.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import gabien.uslx.vfs.*;

/**
 * IO backend based on java.io.File
 */
public class JavaIOFSBackend extends FSBackend {
    public String prefix;

    public JavaIOFSBackend(String pfx) {
        prefix = pfx;
    }

    @Override
    public String toString() {
        return "real:" + prefix;
    }

    public File asFile(String fileName) {
        return new File(prefix + fileName);
    }

    @Override
    public XState getState(String fileName) {
        File f = asFile(fileName);
        if (!f.exists())
            return null;
        if (f.isDirectory()) {
            File[] list = f.listFiles();
            String[] ents = new String[list.length];
            for (int i = 0; i < ents.length; i++)
                ents[i] = list[i].getName();
            return new DirectoryState(ents);
        }
        return new FileTimeState(f.length(), f.lastModified());
    }

    @Override
    public InputStream openRead(String fileName) throws IOException {
        return new FileInputStream(asFile(fileName));
    }

    @Override
    public OutputStream openWrite(String fileName) throws IOException {
        return new FileOutputStream(asFile(fileName));
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
