/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum.test;

import static org.junit.Assert.*;
import static gabien.datum.DatumTreeUtils.sym;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import gabien.datum.DatumReaderTokenSource;
import gabien.datum.DatumSrcLoc;
import gabien.datum.DatumDecodingVisitor;
import gabien.datum.DatumWriter;

/**
 * Reader/writer test.
 * Created 16th February 2023.
 */
public class DatumIOTest {
    private Object genTestCase() {
        return Arrays.asList(
                    Arrays.asList(sym("moku"), sym("sina")),
                    sym("li"),
                    Arrays.asList(sym("pona")),
                    Arrays.asList(sym("tawa"), sym("mi")),
                    true, false, sym(""), sym("#escapethis"), sym("1234")
                );
    }
    @Test
    public void testRead() {
        Object input = genTestCase();
        String tcs = "(\n" +
                "; Symbols & lists test\n" +
                "(moku sina)\n" +
                "li\n" +
                "(pona)\n" +
                "(tawa mi)\n" +
                "; Exceptional cases\n" +
                "#t #f #{}# \\#escapethis \\1234\n" +
                ")";
        DatumReaderTokenSource drs = new DatumReaderTokenSource("string", tcs);
        AtomicBoolean signalWasVisited = new AtomicBoolean();
        drs.visit(new DatumDecodingVisitor() {
            @Override
            public void visitEnd(DatumSrcLoc srcLoc) {
            }
            
            @Override
            public void visitTree(Object obj, DatumSrcLoc srcLoc) {
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
        dw.visitTree(input, DatumSrcLoc.NONE);
        assertEquals("((moku sina)li(pona)(tawa mi)#t #f #{}# \\#escapethis \\1234)", sw.toString());
    }

}
