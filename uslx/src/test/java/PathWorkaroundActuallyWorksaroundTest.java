/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
import static org.junit.Assert.*;

import org.junit.Test;

import gabien.uslx.vfs.impl.DodgyInputWorkaroundFSBackend;
import gabien.uslx.vfs.impl.RAMFSBackend;
import gabien.uslx.vfs.impl.RAMFSBackend.VFSDir;
import gabien.uslx.vfs.impl.RAMFSBackend.VFSFile;

/**
 * Confirms the path workaround is actually fixing things.
 * Created 13th February 2024.
 */
public class PathWorkaroundActuallyWorksaroundTest {

    @Test
    public void test() {
        // note this always uses Unix semantics
        RAMFSBackend ram = new RAMFSBackend();
        VFSDir foo = new VFSDir();
        ram.vfsRoot.contents.put("FOO", foo);
        foo.contents.put("BAR.OBJ", new VFSFile());
        assertEquals(false, ram.into("foo", "bar.obj").exists());
        DodgyInputWorkaroundFSBackend workaround = new DodgyInputWorkaroundFSBackend(ram);
        assertEquals(true, workaround.into("foo", "bar.obj").exists());
        assertEquals(true, workaround.intoPath("foo\\bar.obj").exists());
        assertEquals(true, workaround.intoPath("foo\\magician\\..\\bar.obj").exists());
        assertEquals("/FOO/BAR.OBJ", workaround.into("foo", "bar.obj").getAbsolutePath());
    }

}
