/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import gabien.uslx.append.Entity;

/**
 * Created 8th May, 2024.
 */
public class EntityTypeTest {
    @Test
    public void testTest() {
        ExampleET data = new ExampleET();
        assertNull(data.get(ExampleET.EXAMPLE_ONE));
        data.set(ExampleET.EXAMPLE_TWO, (Integer) 25);
        // tests expansion
        data.set(ExampleET.EXAMPLE_THREE, (Integer) 26);
        data.exampleIntrinsicField = 123;
        assertEquals(data.get(ExampleET.EXAMPLE_TWO), (Integer) 25);
    }

    final static class ExampleET extends Entity<ExampleET> {
        public static final Entity.Registrar<ExampleET> I = newRegistrar();
        public static final Key<Integer> EXAMPLE_ONE = new Key<>();
        public static final Key<Integer> EXAMPLE_TWO = new Key<>();
        public static final Key<Integer> EXAMPLE_THREE = new Key<>();
        // adding intrinsic fields
        int exampleIntrinsicField;
        static final class Key<T> extends Entity.Key<ExampleET, T> {
            public Key() {
                super(I);
            }
        }
    }
}
