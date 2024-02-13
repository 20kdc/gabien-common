/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.vfs.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.uslx.vfs.FSBackend;

/**
 * Created 13th February, 2024.
 */
public class WindowsRootJavaIOFSBackend extends FSBackend {
    static final WindowsRootJavaIOFSBackend INSTANCE = new WindowsRootJavaIOFSBackend();

    private WindowsRootJavaIOFSBackend() {
        super(null, WindowsPathModel.INSTANCE);
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
    public @NonNull FSBackend intoInner(String dirName) {
        // dirName should be "C:" etc.
        return JavaIOFSBackend.from(new File(dirName + "/"));
    }

    @Override
    public boolean mkdir() {
        return true;
    }

    public static String doRootStrip(String str) {
        if (str.endsWith("\\") || str.endsWith("/"))
            str = str.substring(0, str.length() - 1);
        return str;
    }

    @Override
    public @Nullable XState getState() {
        File[] fn = File.listRoots();
        String[] res = new String[fn.length];
        for (int i = 0; i < fn.length; i++) {
            // right, so, it's not described how these work
            // so I had to write a little test program and run it on Wine to find out
            // getName() returns ""
            // toString() returns the full drive prefix, i.e. "E:\"
            // getParent() returns null
            // With this in mind, we'll strip off the final "\" but otherwise leave it alone
            // The mental model you should have in mind here is "/E:/TG01"
            String str = fn.toString();
            str = doRootStrip(str);
            res[i] = str;
        }
        DirectoryState ds = new DirectoryState(res);
        return ds;
    }

    @Override
    public @NonNull InputStream openRead() throws IOException {
        throw new IOException("is root");
    }

    @Override
    public @NonNull OutputStream openWrite() throws IOException {
        throw new IOException("is root");
    }

    @Override
    public void changeTime(long time) {
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public String getAbsolutePath() {
        return "";
    }
}
