/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum.test;

import static org.junit.Assert.*;
import static gabien.datum.DatumSymbol.sym;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import gabien.datum.DatumReaderStream;
import gabien.datum.DatumTreeVisitor;
import gabien.datum.DatumWriter;

/**
 * Reader/writer test.
 * Created 16th February 2022.
 */
public class DatumIOTest {
    private Object genTestCase() {
        return Arrays.asList(
                    Arrays.asList(sym("moku"), sym("sina")),
                    sym("li"),
                    Arrays.asList(sym("pona")),
                    Arrays.asList(sym("tawa"), sym("mi"))
                );
    }
    @Test
    public void testRead() {
        Object input = genTestCase();
        DatumReaderStream drs = new DatumReaderStream(new StringReader("((moku sina) li (pona) (tawa mi))"));
        AtomicBoolean signalWasVisited = new AtomicBoolean();
        drs.visit(new DatumTreeVisitor() {
            @Override
            public void visitEnd() {
            }
            
            @Override
            public void visitTree(Object obj) {
                assertEquals(input, obj);
                signalWasVisited.set(true);
            }
        });
        assertTrue(signalWasVisited.get());
    }
    @Test
    public void testWrite() {
        Object input = genTestCase();
        StringWriter sw = new StringWriter();
        DatumWriter dw = new DatumWriter(sw);
        dw.visitTreeManually(input);
        assertEquals("((moku sina)li(pona)(tawa mi))", sw.toString());
    }

}
