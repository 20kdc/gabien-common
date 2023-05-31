/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Test;

import gabien.uslx.append.ObjectPool;

/**
 * Created 31st May 2023.
 */
public class ObjectPoolTest {
    class ResetCounter {
        int resets = 0;
    }
    @Test
    public void testObjectPool() {
        ObjectPool<ResetCounter> fumbler = new ObjectPool<ResetCounter>(4) {
            @Override
            protected @NonNull ResetCounter gen() {
                return new ResetCounter();
            }

            @Override
            public void reset(@NonNull ResetCounter element) {
                element.resets++;
            }
        };
        ResetCounter a = fumbler.get();
        ResetCounter b = fumbler.get();
        ResetCounter c = fumbler.get();
        ResetCounter d = fumbler.get();
        fumbler.finish(c);
        assert c == fumbler.get();
        fumbler.finish(c);
        fumbler.finish(d);
        assert d == fumbler.get();
        assert c == fumbler.get();
        fumbler.finish(c);
        fumbler.finish(a);
        fumbler.finish(d);
        fumbler.finish(b);
        assert b == fumbler.get();
        assert d == fumbler.get();
        assert a == fumbler.get();
        assert c == fumbler.get();
        fumbler.finish(a);
        fumbler.finish(b);
        fumbler.finish(c);
        fumbler.finish(d);
        assert fumbler.getAndResetMaxInUse() == 4;
        assert fumbler.getCapacity() == 4;
        // reset fumbler and check we really did reset it
        fumbler.setCapacity(0);
        assert fumbler.getCapacity() == 0;
        ResetCounter e = fumbler.get();
        assert e != a && e != b && e != c && e != d;
        // reset counts should equal how often the objects were finished
        // (this is arguably an implementation detail, as the objects could get reset on retrieval)
        assert a.resets == 2;
        assert b.resets == 2;
        assert c.resets == 4;
        assert d.resets == 3;
    }

}
