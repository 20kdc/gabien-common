/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.vopeks;

import org.eclipse.jdt.annotation.NonNull;

import gabien.uslx.append.ObjectPool;

/**
 * Storage for megabuffers.
 * Created 8th June, 2023.
 */
public final class VopeksFloatPool {
    private final Sized pool64 = new Sized(64, 1);
    private final Sized pool1024 = new Sized(1024, 1);
    private final Sized pool16384 = new Sized(16384, 1);
    private final Sized pool65536 = new Sized(65536, 1);
    private final Sized pool786432 = new Sized(786432, 1);

    /**
     * Gets a megabuffer.
     */
    public float[] get(int len) {
        if (len <= 64)
            return pool64.get();
        if (len <= 1024)
            return pool1024.get();
        if (len <= 16384)
            return pool16384.get();
        if (len <= 65536)
            return pool65536.get();
        if (len <= 786432)
            return pool786432.get();
        return new float[len];
    }

    /**
     * Finishes using a megabuffer.
     */
    public void finish(float[] megabuffer) {
        if (megabuffer.length == 64) {
            pool64.finish(megabuffer);
        } else if (megabuffer.length == 1024) {
            pool1024.finish(megabuffer);
        } else if (megabuffer.length == 16384) {
            pool16384.finish(megabuffer);
        } else if (megabuffer.length == 65536) {
            pool65536.finish(megabuffer);
        } else if (megabuffer.length == 786432) {
            pool786432.finish(megabuffer);
        }
    }

    private static class Sized extends ObjectPool<float[]> {
        public final int length;

        Sized(int l, int e) {
            super(e);
            length = l;
        }

        @Override
        protected @NonNull float[] gen() {
            return new float[length];
        }

        @Override
        public void reset(@NonNull float[] element) {
        }
    }
}
