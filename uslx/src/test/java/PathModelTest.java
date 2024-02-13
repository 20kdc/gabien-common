/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import gabien.uslx.vfs.impl.UnixPathModel;
import gabien.uslx.vfs.impl.WindowsPathModel;

/**
 * Created 13th February 2024.
 */
public class PathModelTest {
    @Test
    public void testFolding() {
        assertEquals(UnixPathModel.INSTANCE.estimatePathComponentFolding("A"), "A");
        assertEquals(UnixPathModel.INSTANCE.estimatePathComponentFolding("a"), "a");
        assertEquals(UnixPathModel.INSTANCE.estimatePathComponentFolding("."), ".");

        assertEquals(WindowsPathModel.INSTANCE.estimatePathComponentFolding("A"), WindowsPathModel.INSTANCE.estimatePathComponentFolding("a"));
        assertEquals(WindowsPathModel.INSTANCE.estimatePathComponentFolding("."), WindowsPathModel.INSTANCE.estimatePathComponentFolding("."));
    }

    @Test
    public void testThesePathsShouldBeValid() throws IOException {
        assertEquals(UnixPathModel.INSTANCE.absPathToComponents("/").length, 0);
        assertEquals(UnixPathModel.INSTANCE.absPathToComponents("//").length, 0);
        assertEquals(UnixPathModel.INSTANCE.absPathToComponents("/bloop").length, 1);
        assertEquals(UnixPathModel.INSTANCE.absPathToComponents("/bloop/").length, 1);
        assertEquals(UnixPathModel.INSTANCE.absPathToComponents("/bloop//").length, 1);
        assertEquals(UnixPathModel.INSTANCE.absPathToComponents("/bloop//mew").length, 2);

        assertEquals(UnixPathModel.INSTANCE.relPathToComponents("").length, 0);
        assertEquals(UnixPathModel.INSTANCE.relPathToComponents("bloop").length, 1);
        assertEquals(UnixPathModel.INSTANCE.relPathToComponents("bloop/").length, 1);
        assertEquals(UnixPathModel.INSTANCE.relPathToComponents("bloop//").length, 1);
        assertEquals(UnixPathModel.INSTANCE.relPathToComponents("bloop//mew").length, 2);

        assertEquals(WindowsPathModel.INSTANCE.absPathToComponents("C:\\").length, 1);
        assertEquals(WindowsPathModel.INSTANCE.absPathToComponents("C:/").length, 1);
        assertEquals(WindowsPathModel.INSTANCE.absPathToComponents("C:/bloop").length, 2);
        assertEquals(WindowsPathModel.INSTANCE.absPathToComponents("C:/bloop/").length, 2);
        assertEquals(WindowsPathModel.INSTANCE.absPathToComponents("C:/bloop//").length, 2);
        assertEquals(WindowsPathModel.INSTANCE.absPathToComponents("C:/bloop//mew").length, 3);
        // lowercase drive letters accepted too!
        assertEquals(WindowsPathModel.INSTANCE.absPathToComponents("c:/bloop//mew").length, 3);

        assertEquals(WindowsPathModel.INSTANCE.relPathToComponents("").length, 0);
        assertEquals(WindowsPathModel.INSTANCE.relPathToComponents("bloop").length, 1);
        assertEquals(WindowsPathModel.INSTANCE.relPathToComponents("bloop/").length, 1);
        assertEquals(WindowsPathModel.INSTANCE.relPathToComponents("bloop//").length, 1);
        assertEquals(WindowsPathModel.INSTANCE.relPathToComponents("bloop//mew").length, 2);
    }

    @Test
    public void testThesePathsShouldFail() throws IOException {
        // not absolute
        assertThrows(IOException.class, () -> {
            UnixPathModel.INSTANCE.absPathToComponents("");
        });
        assertThrows(IOException.class, () -> {
            UnixPathModel.INSTANCE.absPathToComponents("foo");
        });
        assertThrows(IOException.class, () -> {
            UnixPathModel.INSTANCE.absPathToComponents("foo/");
        });
        assertThrows(IOException.class, () -> {
            UnixPathModel.INSTANCE.absPathToComponents("foo/bar");
        });
        assertThrows(IOException.class, () -> {
            UnixPathModel.INSTANCE.absPathToComponents("foo/bar/");
        });
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.absPathToComponents("");
        });
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.absPathToComponents("foo");
        });
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.absPathToComponents("foo/");
        });
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.absPathToComponents("foo/bar");
        });
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.absPathToComponents("foo/bar/");
        });
        // windows: bad drive pattern
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.absPathToComponents("C::/");
        });
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.absPathToComponents("C /");
        });
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.absPathToComponents("/C:/");
        });
        // windows: valid start but uses a drive pattern midpath
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.absPathToComponents("C:/WINDOWS/D:/BAR");
        });

        // not relative
        assertThrows(IOException.class, () -> {
            UnixPathModel.INSTANCE.relPathToComponents("/");
        });
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.relPathToComponents("C:\\");
        });
        // windows: drive pattern inside relative path
        assertThrows(IOException.class, () -> {
            WindowsPathModel.INSTANCE.relPathToComponents("a/b/c:/d");
        });
    }

    @Test
    public void testPathComponentTheory() throws IOException {
        // obvious check
        assertTrue(WindowsPathModel.INSTANCE.verifyPathComponent(false, "a"));
        assertTrue(WindowsPathModel.INSTANCE.verifyPathComponent(false, "ab"));
        assertTrue(WindowsPathModel.INSTANCE.verifyPathComponent(false, "abc"));
        assertTrue(WindowsPathModel.INSTANCE.verifyPathComponent(false, "abcd"));
        assertTrue(UnixPathModel.INSTANCE.verifyPathComponent(false, "a"));
        assertTrue(UnixPathModel.INSTANCE.verifyPathComponent(false, "ab"));
        assertTrue(UnixPathModel.INSTANCE.verifyPathComponent(false, "abc"));
        assertTrue(UnixPathModel.INSTANCE.verifyPathComponent(false, "abcd"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "a/"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "ab/"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "abc/"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "abcd/"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "a/"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "ab/"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "abc/"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "abcd/"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "/a"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "/ab"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "/abc"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "/abcd"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "/a"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "/ab"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "/abc"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "/abcd"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "a/b"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "a/bc"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "a/bcd"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "a/b"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "a/bc"));
        assertFalse(UnixPathModel.INSTANCE.verifyPathComponent(false, "a/bcd"));
        // windows: these should be drive letters and aren't
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(true, "ab"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(true, "abc"));
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(true, "abcd"));
        // windows: these should be drive letters and are
        assertTrue(WindowsPathModel.INSTANCE.verifyPathComponent(true, "a:"));
        // windows: these shouldn't be drive letters and are
        assertFalse(WindowsPathModel.INSTANCE.verifyPathComponent(false, "a:"));
    }

    @Test
    public void testNameOf() throws IOException {
        assertEquals(WindowsPathModel.INSTANCE.nameOf("a/b/c"), "c");
        assertEquals(WindowsPathModel.INSTANCE.nameOf("a/b/"), "b");
        assertEquals(WindowsPathModel.INSTANCE.nameOf("a"), "a");
        assertEquals(UnixPathModel.INSTANCE.nameOf("a/b/c"), "c");
        assertEquals(UnixPathModel.INSTANCE.nameOf("a/b/"), "b");
        assertEquals(UnixPathModel.INSTANCE.nameOf("a"), "a");
        // and reverse
        assertEquals(WindowsPathModel.INSTANCE.nameOf("a\\b\\c"), "c");
        assertEquals(WindowsPathModel.INSTANCE.nameOf("a\\b\\"), "b");
        assertEquals(UnixPathModel.INSTANCE.nameOf("a\\b\\c"), "a\\b\\c");
        assertEquals(UnixPathModel.INSTANCE.nameOf("a\\b\\"), "a\\b\\");
    }
}
