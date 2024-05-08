/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import gabien.uslx.append.EntityType;

/**
 * Created 8th May, 2024.
 */
public class EntityTypeTest {
    @Test
    public void testTest() {
        ExampleET.V data = ExampleET.I.entity();
        assertNull(data.get(ExampleET.EXAMPLE_ONE));
        data.set(ExampleET.EXAMPLE_TWO, (Integer) 25);
        // tests expansion
        data.set(ExampleET.EXAMPLE_THREE, (Integer) 26);
        assertEquals(data.get(ExampleET.EXAMPLE_TWO), (Integer) 25);
    }

    final static class ExampleET extends EntityType<ExampleET> {
        public static final ExampleET I = new ExampleET();
        public static final Key<Integer> EXAMPLE_ONE = I.key();
        public static final Key<Integer> EXAMPLE_TWO = I.key();
        public static final Key<Integer> EXAMPLE_THREE = I.key();
    }
}
