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

import gabien.uslx.vfs.FSBackend.*;

public final class FSHandle {
    public final FSBackend host;
    public final String name;

    public FSHandle(FSBackend o, String n) {
        host = o;
        name = n;
    }

    @Override
    public String toString() {
        return host + ":" + name;
    }

    public String getName() {
        return FSBackend.basename(name);
    }

    public FSHandle[] listFiles() {
        XState state = host.getState(name);
        if (state == null)
            return null;
        if (!(state instanceof DirectoryState))
            return null;
        String[] ip = ((DirectoryState) state).entries;
        FSHandle[] fsh = new FSHandle[ip.length];
        for (int i = 0; i < ip.length; i++)
            fsh[i] = new FSHandle(host, name + "/" + ip[i]);
        return fsh;
    }

    public void setLastModified(long time) {
        host.changeTime(name, time);
    }

    public XState getState() {
        return host.getState(name);
    }

    public boolean exists() {
        return getState() != null;
    }

    public boolean isFile() {
        return getState() instanceof FileState;
    }

    public long length() {
        return ((FileState) getState()).size;
    }

    public boolean isDirectory() {
        return getState() instanceof DirectoryState;
    }

    public InputStream openRead() throws IOException {
        return host.openRead(name);
    }

    public OutputStream openWrite() throws IOException {
        return host.openWrite(name);
    }

    public void delete() {
        host.delete(name);
    }

}
