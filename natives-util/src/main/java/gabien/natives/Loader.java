/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Function;

import gabien.uslx.licensing.LicenseComponent;
import gabien.uslx.licensing.LicenseManager;

/**
 * Loader for gabien-natives.
 * Created 25th May, 2023.
 */
public abstract class Loader {
    private Loader() {
    }

    public static final LicenseComponent LC_STB_VORBIS = new LicenseComponent("stb_vorbis", "https://github.com/nothings/stb/", "gabien/licensing/stb_vorbis/COPYING.txt", "gabien/licensing/stb_vorbis/CREDITS.txt");
    public static final LicenseComponent LC_MINIMP3 = new LicenseComponent("minimp3", "https://github.com/lieff/minimp3", "gabien/licensing/minimp3/LICENSE", null);

    static {
        LicenseManager.I.register(LC_STB_VORBIS);
        LicenseManager.I.register(LC_MINIMP3);
        LicenseManager.I.dependency(LicenseComponent.LC_GABIEN, LC_STB_VORBIS);
        LicenseManager.I.dependency(LicenseComponent.LC_GABIEN, LC_MINIMP3);
    }

    public static InputStream assetLookupJavaSE(String str) {
        return ClassLoader.getSystemClassLoader().getResourceAsStream("assets/" + str);
    }

    public static File destinationSetupJavaSE(String fnf) {
        File tmp;
        try {
            tmp = File.createTempFile(fnf, null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        tmp.deleteOnExit();
        return tmp;
    }

    public static boolean defaultLoaderJavaSE() {
        return defaultLoader(Loader::assetLookupJavaSE, Loader::destinationSetupJavaSE);
    }

    public static boolean defaultLoader(Function<String, InputStream> assetLookup, Function<String, File> destinationSetup) {
        StringWriter errors = new StringWriter();
        PrintWriter errorsP = new PrintWriter(errors);
        // all supported CPUs
        String[] cpu = {
            "x86_64",
            "aarch64",
            "riscv64",
            // OpenJDK will complain about disabling stack guard if this is loaded early
            "x86",
            "arm",
            "mipsel"
        };
        String[] os = {
            "linux-gnu",
            "windows-gnu",
            "macos",
            // so previously I said this shouldn't get extracted, I kiiinda lied
            "linux-android"
        };
        // would have been for Android but it doesn't work because Reasons
        try {
            System.loadLibrary("gabien-natives");
            return true;
        } catch (Throwable ex) {
            errorsP.append("gabien.natives.Loader: loadLibrary(gabien-natives): " + ex + "\n");
        }
        // try for anything obvious
        for (String o : os)
            for (String c : cpu)
                if (defaultLoaderConfig(c + "-" + o, errorsP))
                    return true;
        // get desperate
        String detectedCPU = detectCPU();
        for (String o : os)
            if (defaultLoaderConfigTmpWithBackpathCheck(detectedCPU + "-" + o, assetLookup, destinationSetup, errorsP))
                return true;
        // get REALLY desperate
        for (String o : os)
            for (String c : cpu)
                if (defaultLoaderConfigTmpWithBackpathCheck(c + "-" + o, assetLookup, destinationSetup, errorsP))
                    return true;
        // uhoh.
        System.err.println("gabien.natives.Loader: Failed, information:");
        System.err.print(errors.toString());
        return false;
    }
    private static String detectCPU() {
        String detectedCPU = System.getProperty("os.arch");
        if (detectedCPU.equalsIgnoreCase("amd64"))
            detectedCPU = "x86_64";
        else if (detectedCPU.equalsIgnoreCase("i686"))
            detectedCPU = "x86";
        else if (detectedCPU.contains("arm64"))
            detectedCPU = "aarch64";
        else if (detectedCPU.contains("arm"))
            detectedCPU = "arm";
        else if (detectedCPU.contains("mips"))
            detectedCPU = "mipsel";
        return detectedCPU;
    }
    private static boolean loadLibrary(String name, PrintWriter errorsP) {
        try {
            System.loadLibrary(name);
            return true;
        } catch (Throwable ex) {
            errorsP.append("gabien.natives.Loader: loadLibrary(" + name + "): " + ex + "\n");
        }
        return false;
    }
    private static boolean defaultLoaderConfig(String config, PrintWriter errorsP) {
        if (loadLibrary("gabien-natives-" + config, errorsP))
            return true;
        String fn = "natives." + config;
        try {
            System.load(new File(fn).getAbsolutePath());
            return true;
        } catch (Throwable ex) {
            errorsP.append("gabien.natives.Loader: load(" + fn + "): " + ex + "\n");
        }
        return false;
    }
    private static boolean defaultLoaderConfigTmpWithBackpathCheck(String config, Function<String, InputStream> assetLookup, Function<String, File> destinationSetup, PrintWriter errorsP) {
        // Backup mechanism laying around to run this on EGL even on systems that don't traditionally do that.
        if (System.getenv("BADGPU_EGL_LIBRARY") != null)
            if (loadWithTmpfile(config + ".CX_BackPath", assetLookup, destinationSetup, errorsP))
                return true;
        return loadWithTmpfile(config, assetLookup, destinationSetup, errorsP);
    }
    private static boolean loadWithTmpfile(String config, Function<String, InputStream> assetLookup, Function<String, File> destinationSetup, PrintWriter errorsP) {
        String fn = "natives." + config;
        String fnf = "gabien-natives/" + fn;
        try {
            // This can fail on Android, but that shouldn't run this anyway
            File tmp;
            try (InputStream inp = assetLookup.apply(fnf)) {
                if (inp == null) {
                    errorsP.append("gabien.natives.Loader: loadViaTmpfile(" + fnf + "): doesn't exist!\n");
                    return false;
                }
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[65536];
                while (true) {
                    int rd = inp.read(buf);
                    if (rd <= 0)
                        break;
                    bos.write(buf, 0, rd);
                }
                buf = bos.toByteArray();
                bos = null;
                // alright, we have all the data now for comparison/etc.
                tmp = destinationSetup.apply(fn);
                System.err.println("gabien.natives.Loader: " + fnf + " -> " + tmp);
                // Check to see if the data is already there
                boolean rewriteNecessary = tmp.length() != buf.length;
                if (!rewriteNecessary) {
                    // Absolutely confirm it's there
                    try (FileInputStream fis = new FileInputStream(tmp)) {
                        for (byte b : buf) {
                            int b1 = b & 0xFF;
                            int b2 = fis.read();
                            if (b1 != b2) {
                                rewriteNecessary = true;
                                break;
                            }
                        }
                        if (!rewriteNecessary)
                            if (fis.read() != -1)
                                rewriteNecessary = true;
                    } catch (Throwable ex2) {
                        rewriteNecessary = true;
                    }
                }
                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    fos.write(buf);
                } catch (Throwable ex2) {
                    // Continue; we might have *already* stored the file here on a previous run
                    // Worth a shot!
                    ex2.printStackTrace();
                }
            }
            System.load(tmp.getAbsolutePath());
            return true;
        } catch (Throwable ex) {
            errorsP.append("gabien.natives.Loader: loadViaTmpfile(" + fnf + "): " + ex + "\n");
        }
        return false;
    }

    public static native String getNativesVersion();
}

