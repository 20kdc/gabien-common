/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

/**
 * Default loader for UNA.
 * Created 25th May, 2023.
 */
public class UNALoader {
    /* Loader */
    public static boolean defaultLoader() {
        // all supported CPUs
        String[] cpu = {
            "x86_64",
            "aarch64",
            "arm",
            "riscv64",
            // OpenJDK will complain about disabling stack guard if this is loaded early
            "x86"
        };
        String[] os = {
            "linux-gnu",
            "windows-gnu",
            "macos",
            // this shouldn't really get extracted due to how the packaging works
            "android"
        };
        // for Android
        try {
            System.loadLibrary("gabien-una");
            return true;
        } catch (Throwable ex) {
        }
        // try for anything obvious
        for (String o : os)
            for (String c : cpu)
                if (defaultLoaderConfig(o + "-" + c))
                    return true;
        // get desperate
        String osArch = System.getProperty("os.arch");
        if (osArch.equalsIgnoreCase("amd64"))
            osArch = "x86_64";
        else if (osArch.equalsIgnoreCase("i686"))
            osArch = "x86";
        else if (osArch.contains("arm64"))
            osArch = "aarch64";
        else if (osArch.contains("arm"))
            osArch = "arm";
        for (String c : cpu)
            if (defaultLoaderConfigTmpfile(c + "-" + osArch))
                return true;
        // get REALLY desperate
        for (String o : os)
            for (String c : cpu)
                if (defaultLoaderConfigTmpfile(c + "-" + o))
                    return true;
        return false;
    }
    private static boolean defaultLoaderConfig(String config) {
        try {
            System.loadLibrary("gabien-una-" + config);
            return true;
        } catch (Throwable ex) {
        }
        String fn = "una." + config;
        try {
            System.load(new File(fn).getAbsolutePath());
            return true;
        } catch (Throwable ex) {
        }
        return false;
    }
    private static boolean defaultLoaderConfigTmpfile(String config) {
        String fn = "una." + config;
        String fnf = "lib/" + fn;
        try {
            // This can fail on Android, but that shouldn't run this anyway
            File tmp;
            try (InputStream inp = UNA.class.getResourceAsStream(fnf)) {
                if (inp == null)
                    return false;
                tmp = File.createTempFile(fnf, null);
                tmp.deleteOnExit();
                Files.copy(inp, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            System.load(tmp.getAbsolutePath());
            return true;
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return false;
    }
}

