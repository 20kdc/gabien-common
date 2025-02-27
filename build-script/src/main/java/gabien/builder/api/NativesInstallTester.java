/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Function;

/**
 * Tests natives for you.
 * Created 17th February 2025
 */
public class NativesInstallTester {
    // This line is changed after each release.
    public static final String AUTHORITATIVE_VERSION = "musical-sparrow";
    public static final String AUTHORITATIVE_URL = "jar:https://github.com/20kdc/gabien-common/releases/download/natives." + AUTHORITATIVE_VERSION + "/natives-sdk.zip!/natives.jar";
    // ./umvn install-file '-Durl=jar:https://github.com/20kdc/gabien-common/releases/download/natives.musical-sparrow/natives-sdk.zip!/natives.jar' -DgroupId=t20kdc.scratchpad -DartifactId=exampleNativesDL -Dversion=8.6 -Dpackaging=jar

    public static final Runnable PREREQUISITE = () -> {
        // THIS IS THE ONLY PLACE WHERE THIS ENVIRONMENT VARIABLE MAY BE USED.
        // It acts as a - disablable - safety guard with no other side effects.
        String dev = System.getenv("GABIEN_NATIVES_DEV");
        boolean isDev = (dev != null) && dev.equals("1");
        String res = getNativesVersion();
        if (!(isDev || res.equals(AUTHORITATIVE_VERSION)))
            throw new RuntimeException("'" + AUTHORITATIVE_VERSION + "' (correct) != '" + res + "' (installed). Must be in dev-mode (export GABIEN_NATIVES_DEV=1) OR must be the last released gabien-natives version. This procedure is to prevent accidentally releasing an unreproducible binary.");
    };

    /**
     * Gets the natives version currently installed, or throws a loud exception.
     */
    @SuppressWarnings({ "resource" })
    public static String getNativesVersion() {
        File nativesJAR = MavenRepository.getJARFile("t20kdc.hs2", "gabien-natives", "0.666-SNAPSHOT");
        if (!nativesJAR.exists())
            throw new RuntimeException("Natives JAR is expected to exist");
        File nativesUtilJAR = MavenRepository.getJARFile("t20kdc.hs2", "gabien-natives-util", "0.666-SNAPSHOT");
        if (!nativesUtilJAR.exists())
            throw new RuntimeException("Natives util JAR is expected to exist");
        File uslxJAR = MavenRepository.getJARFile("t20kdc.hs2", "gabien-uslx", "0.666-SNAPSHOT");
        if (!uslxJAR.exists())
            throw new RuntimeException("USLX JAR is expected to exist");
        try {
            URLClassLoader loader = new URLClassLoader(new URL[] {nativesJAR.toURI().toURL(), nativesUtilJAR.toURI().toURL(), uslxJAR.toURI().toURL()}, ClassLoader.getSystemClassLoader());
            Class<?> loaderClass = loader.loadClass("gabien.natives.Loader");
            loaderClass.getMethod("defaultLoader", Function.class, Function.class).invoke(null, (Function<String, InputStream>) (a) -> {
                try {
                    return loader.getResourceAsStream("assets/" + a);
                } catch (Exception ex2) {
                    throw new RuntimeException(ex2);
                }
            }, (Function<String, File>) (a) -> {
                try {
                    return (File) loaderClass.getMethod("destinationSetupJavaSE", String.class).invoke(null, a);
                } catch (Exception ex2) {
                    throw new RuntimeException(ex2);
                }
            });
            return (String) loaderClass.getMethod("getNativesVersion").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException("Error in natives load", e);
        }
    }
}
