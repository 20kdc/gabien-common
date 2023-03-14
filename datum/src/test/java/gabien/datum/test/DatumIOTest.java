/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum.test;

import static org.junit.Assert.*;
import static gabien.datum.DatumTreeUtils.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import gabien.datum.DatumReaderTokenSource;
import gabien.datum.DatumRuntimeIOException;
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
                    true, false, sym(""), sym("#escapethis"), sym("1234"), null,
                    0.125d, "Hello\r\n\t\u5000\ud800\udc00", Arrays.asList(sym("quote"), sym("hi"))
                );
    }
    @Test
    public void testReadSimple() {
        simpleTestCase(123L, "123;a");
        // errors
        assertThrows(RuntimeException.class, () -> parseTestCase("\\"));
        assertThrows(RuntimeException.class, () -> parseTestCase("\\x"));
        assertThrows(RuntimeException.class, () -> parseTestCase("\\xZ;"));
        assertThrows(RuntimeException.class, () -> parseTestCase("#nope"));
        assertThrows(RuntimeException.class, () -> parseTestCase("\"a"));
        assertThrows(RuntimeException.class, () -> parseTestCase("("));
        assertThrows(RuntimeException.class, () -> parseTestCase(")"));
        assertThrows(RuntimeException.class, () -> parseTestCase("123notarealnumber"));
        assertThrows(RuntimeException.class, () -> parseTestCase("'"));
        // position
        assertEquals("testL1", new DatumReaderTokenSource("test", "").position());
        
    }
    private void parseTestCase(String test) {
        new DatumReaderTokenSource("test", test).visit(decVisitor((v, srcLoc) -> {
        }));
    }
    private void simpleTestCase(Object obj, String test) {
        AtomicBoolean signalWasVisited = new AtomicBoolean();
        new DatumReaderTokenSource("test", test).visit(decVisitor((v, srcLoc) -> {
            assertEquals(srcLoc.filename, "test");
            assertTrue(srcLoc.toString().startsWith("test:"));
            assertEquals(obj, v);
            signalWasVisited.set(true);
        }));
        assertTrue(signalWasVisited.get());
    }
    @Test
    public void testWriteSimple() {
        assertEquals("norn", DatumWriter.objectToString(sym("norn")));
        assertEquals("#{}#", DatumWriter.objectToString(sym("")));
        assertEquals("a\\ ", DatumWriter.objectToString(sym("a ")));
        assertEquals("#nil", DatumWriter.objectToString(null));
        assertEquals("123", DatumWriter.objectToString(123));
        assertEquals("\"\\\"\"", DatumWriter.objectToString("\""));
        StringWriter sw = new StringWriter();
        DatumWriter dw = new DatumWriter(sw);
        dw.visitComment("Hello world\nFish are nice");
        assertEquals("; Hello world\n; Fish are nice\n", sw.toString());
        // errors due to these being, well, weird and bad and no-good
        // this might have to be removed in future if a representation of these is decided on 
        assertThrows(RuntimeException.class, () -> DatumWriter.objectToString(Double.POSITIVE_INFINITY));
        // very bad errors
        assertThrows(DatumRuntimeIOException.class, () -> new DatumWriter(new Appendable() {
            @Override
            public Appendable append(char var1) throws IOException {
                throw new IOException("No");
            }
            @Override
            public Appendable append(CharSequence var1) throws IOException {
                throw new IOException("No");
            }
            @Override
            public Appendable append(CharSequence var1, int var2, int var3) throws IOException {
                throw new IOException("No");
            }
        }).visitComment("Hello"));
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
                "#t #f #{}# \\#escapethis \\1234 #nil\n" +
                "; Floats, strings\n" +
                "0.125 \"Hello\\r\\n\\t\\x5000;\\x10000;\" 'hi\n" +
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
        assertEquals("((moku sina)li(pona)(tawa mi)#t #f #{}# \\#escapethis \\1234 #nil 0.125\"Hello\\r\\n\\t\u5000\ud800\udc00\"(quote hi))", sw.toString());
    }

}
