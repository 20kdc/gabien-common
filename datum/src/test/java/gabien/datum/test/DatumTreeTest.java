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

import gabien.datum.DatumSymbol;
import gabien.datum.DatumTreeVisitor;

/**
 * Test encoding/decoding datum trees.
 * Created 15th February 2022.
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
        DatumTreeVisitor visitor = new DatumTreeVisitor() {
            @Override
            public void visitTree(Object obj) {
                assertEquals(input, obj);
                signalWasVisited.set(true);
            }

            @Override
            public void visitEnd() {
            }
        };
        visitor.visitTreeManually(input);
        assertTrue(signalWasVisited.get());
        // Test conversion of arrays
        DatumTreeVisitor testOfCorrectFunctionVisitor = new DatumTreeVisitor() {
            @Override
            public void visitTree(Object obj) {
                assertEquals(Arrays.asList("A", "B", "C"), obj);
                signalWasVisited.set(true);
            }

            @Override
            public void visitEnd() {
            }
        };
        signalWasVisited.set(false);
        testOfCorrectFunctionVisitor.visitTreeManually(new String[] {"A", "B", "C"});
        assertTrue(signalWasVisited.get());
    }

}
