/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum.test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import gabien.datum.*;
import static gabien.datum.DatumTreeUtils.*;

/**
 * Test encoding/decoding datum trees.
 * Created 15th February 2023.
 */
public class DatumTreeTest {

    @Test
    public void test() {
        List<Object> input = Arrays.asList(
                "Example 1",
                new DatumSymbol("Example 2"),
                3.3d,
                4d,
                5L,
                6L,
                Arrays.asList("A", "B", "C")
        );
        // Test that the tree was visited and that it faithfully reproduces contents
        AtomicBoolean signalWasVisited = new AtomicBoolean();
        DatumDecodingVisitor visitor = new DatumDecodingVisitor() {
            @Override
            public void visitTree(Object obj, DatumSrcLoc srcLoc) {
                assertEquals(input, obj);
                signalWasVisited.set(true);
            }

            @Override
            public void visitEnd(DatumSrcLoc srcLoc) {
            }
        };
        new DatumEncodingProxyVisitor(visitor).visitTree(input, DatumSrcLoc.NONE);
        assertTrue(signalWasVisited.get());
        // Test conversion of arrays
        visitor = new DatumDecodingVisitor() {
            @Override
            public void visitTree(Object obj, DatumSrcLoc srcLoc) {
                assertEquals(Arrays.asList("A", "B", "C"), obj);
                signalWasVisited.set(true);
            }

            @Override
            public void visitEnd(DatumSrcLoc srcLoc) {
            }
        };
        signalWasVisited.set(false);
        new DatumEncodingProxyVisitor(visitor).visitTree(new String[] {"A", "B", "C"}, DatumSrcLoc.NONE);
        assertTrue(signalWasVisited.get());
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testSymbolOps() {
        assertEquals(-1, sym("A").compareTo(sym("B")));
        assertEquals("A".hashCode(), sym("A").hashCode());
        assertTrue(sym("A").equals(sym("A")));
        assertFalse(sym("A").equals(sym("B")));
        // this is part of API
        assertEquals("A", sym("A").toString());
        assertFalse(sym("A").equals(123));
        assertTrue(isSym(sym("A"), "A"));
        assertFalse(isSym(sym("A"), "B"));
        assertFalse(isSym("A", "A"));
    }

    @Test
    public void testCastOps() {
        assertEquals((Object) 0, cInt(0d));
        assertEquals((Object) 0L, cLong(0d));
        assertEquals((Object) 0f, cFloat(0d));
        assertEquals((Object) 0d, cDouble(0d));
        assertEquals(Arrays.asList(1L, 2L, 3L), cList(Arrays.asList(1L, 2L, 3L)));
    }
}
