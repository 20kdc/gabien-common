/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.natives.tests;

import org.junit.Test;

import gabien.natives.Loader;
import static org.junit.Assert.*;

import org.eclipse.jdt.annotation.Nullable;

/**
 * I will not permit mistakes, even my own.
 * Created 24th June, 2023.
 */
public class ReleaseNativesAreInstalledAndReproducibleTest {
    public static boolean hasLoadedNativesInThisProcess = false;
    public static void load() {
        if (!hasLoadedNativesInThisProcess) {
            hasLoadedNativesInThisProcess = true;
            assertTrue("Could not load natives. Download from gabien-common releases page.", Loader.defaultLoaderJavaSE());
        }
    }

    @Test
    public void testThatNativesWork() {
        load();
    }

    @Test
    public void testThatNativesAreReproducible() {
        load();
        String ver = Loader.getNativesVersion();
        // This line is changed after each release.
        String vrl = "watching-canary";
        // THIS IS THE ONLY PLACE WHERE THIS ENVIRONMENT VARIABLE MAY BE USED.
        // It acts as a - disablable - safety guard with no other side effects.
        @Nullable String dev = System.getenv("GABIEN_NATIVES_DEV");
        boolean isCorrect = ver.equals(vrl);
        boolean isDev = (dev != null) && dev.equals("1");
        System.out.println("gabien-natives version: " + ver);
        System.out.println("gabien-natives-util release-lock: " + vrl);
        System.out.println("isCorrect: " + isCorrect);
        System.out.println("isDev: " + isDev);
        assertTrue("Must be in dev-mode (export GABIEN_NATIVES_DEV=1) OR must be the last released gabien-natives version. This procedure is to prevent accidentally releasing an unreproducible binary.", isDev || isCorrect);
    }
}
