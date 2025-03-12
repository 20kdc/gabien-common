/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

/**
 * Universal constants.
 * Created 12th March, 2025.
 */
public class Constants {
    // This line is changed after each release.
    public static final String NATIVES_VERSION = "musical-sparrow";
    public static final String NATIVES_URL = "jar:https://github.com/20kdc/gabien-common/releases/download/natives." + NATIVES_VERSION + "/natives-sdk.zip!/natives.jar";
    // ./umvn install-file '-Durl=jar:https://github.com/20kdc/gabien-common/releases/download/natives.musical-sparrow/natives-sdk.zip!/natives.jar' -DgroupId=t20kdc.scratchpad -DartifactId=exampleNativesDL -Dversion=8.6 -Dpackaging=jar
    public static final String ANDROID_PLATFORM_URL = "https://chromium.googlesource.com/android_tools/+/e429db7f48cd615b0b408cda259ffbc17d3945bb/sdk/platforms/android-24/android.jar";
    public static final String ANDROID_PLATFORM_VERSION = "25.0";
    public static final String ANDROID_PLATFORM_LICENSE = "https://chromium.googlesource.com/android_tools/+/e429db7f48cd615b0b408cda259ffbc17d3945bb/LICENSE";

    public static final MavenCoordinates COORDS_NATIVES = new MavenCoordinates("t20kdc.hs2", "gabien-natives", "0.666-SNAPSHOT");
    public static final MavenCoordinates COORDS_NATIVES_UTIL = new MavenCoordinates("t20kdc.hs2", "gabien-natives-util", "0.666-SNAPSHOT");
    public static final MavenCoordinates COORDS_USLX = new MavenCoordinates("t20kdc.hs2", "gabien-uslx", "0.666-SNAPSHOT");
    public static final MavenCoordinates COORDS_ANDROID_PLATFORM = new MavenCoordinates("t20kdc.bpi", "android-platform", ANDROID_PLATFORM_VERSION);
}
