/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.uslx.vfs.impl;

import java.io.IOException;
import java.util.LinkedList;

import gabien.uslx.vfs.PathModel;

/**
 * Path model that should work decently well for all Java IO uses.
 * Created 13th February, 2024.
 */
public class UnixPathModel extends PathModel {
    public static final UnixPathModel INSTANCE = new UnixPathModel();
    private UnixPathModel() {}

    @Override
    public boolean estimatePathComponentEquality(String a, String b) {
        return a.equals(b);
    }

    @Override
    public String estimatePathComponentFolding(String a) {
        return a;
    }

    @Override
    public boolean verifyPathComponent(boolean isFirstAbsComponent, String dirName) {
        if (dirName.isEmpty())
            return false;
        if (dirName.contains("/"))
            return false;
        return true;
    }

    @Override
    public String nameOf(String anyPath) {
        String[] opts = anyPath.split("/");
        return opts[opts.length - 1];
    }

    @Override
    public String[] relPathToComponents(String absolutePath) throws IOException {
        if (absolutePath.startsWith("/"))
            throw new IOException("Empty component at start not allowed in relative paths");
        String[] components = absolutePath.split("/");
        LinkedList<String> finale = new LinkedList<>();
        for (String component : components) {
            if (component.equals(""))
                continue;
            if (!verifyPathComponent(false, component))
                throw new IOException("Invalid path component in " + absolutePath);
            finale.add(component);
        }
        return finale.toArray(new String[finale.size()]);
    }

    @Override
    public String[] absPathToComponents(String absolutePath) throws IOException {
        if (!absolutePath.startsWith("/"))
            throw new IOException("Empty component at start required in absolute paths");
        String[] components = absolutePath.split("/");
        LinkedList<String> finale = new LinkedList<>();
        for (String component : components) {
            if (component.equals(""))
                continue;
            if (!verifyPathComponent(false, component))
                throw new IOException("Invalid path component in " + absolutePath);
            finale.add(component);
        }
        return finale.toArray(new String[finale.size()]);
    }
}
