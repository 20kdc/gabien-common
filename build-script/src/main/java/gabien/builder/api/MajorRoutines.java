/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.builder.api;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created February 18th, 2025.
 */
public class MajorRoutines {
    private MajorRoutines() {
    }

    public static void ready(CommandEnv diag) {
        CommandEnv inHome = diag.cd(CommandEnv.GABIEN_HOME);
        inHome.info("Readying engine...");
        inHome.run(CommandEnv.UMVN_COMMAND, "test", "-q");
        inHome.run(CommandEnv.UMVN_COMMAND, "package-only", "-q");
        inHome.run(CommandEnv.UMVN_COMMAND, "install-only", "-q");
    }

    public static void androidBuild(CommandEnv env, String name, String pkg, String vName, int vCode, File appJar, File icon, String[] permissions, File apk) throws Exception {
        env = env.cd(new File(CommandEnv.GABIEN_HOME, "android"));
        Files.copy(icon.toPath(), new File(CommandEnv.GABIEN_HOME, "android/res/drawable/icon.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
        try (PrintStream ps = new PrintStream(new File(CommandEnv.GABIEN_HOME, "android/AndroidManifest.xml"), "UTF-8")) {
            ps.print("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"" + pkg + "\"\n");
            ps.print(" android:installLocation=\"auto\"\n");
            ps.print(" android:versionCode=\"" + vCode + "\"\n");
            ps.print(" android:versionName=\"" + vName + "\">\n");
            ps.print(" <uses-sdk android:minSdkVersion=\"7\" android:targetSdkVersion=\"23\" />\n");
            for (String p : permissions)
                ps.print(" <uses-permission android:name=\"" + p + "\"/>");
            ps.print(" <application\n");
            ps.print("  android:hardwareAccelerated=\"true\"\n");
            // sadly, android:debuggable slows things down.
            // you have to go to JNI Tips, where it will happily THEN tell you that enabling this activates CheckJNI.
            // this note courtesy of past-me
            //ps.print("  android:debuggable=\"true\"\n");
            ps.print("  android:icon=\"@drawable/icon\"\n");
            ps.print("  android:label=\"@string/app_name\"\n");
            ps.print("  android:theme=\"@style/AppTheme\">\n");
            ps.print("  <activity android:name=\"gabien.MainActivity\" android:immersive=\"true\">\n");
            ps.print("   <intent-filter>\n");
            ps.print("    <action android:name=\"android.intent.action.MAIN\"/>\n");
            ps.print("    <category android:name=\"android.intent.category.LAUNCHER\"/>\n");
            ps.print("   </intent-filter>\n");
            ps.print("  </activity>\n");
            ps.print(" </application>\n");
            ps.print("</manifest>\n");
        }
        try (PrintStream ps = new PrintStream(new File(CommandEnv.GABIEN_HOME, "android/res/values/strings.xml"), "UTF-8")) {
            ps.print("<resources><string name=\"app_name\">" + name + "</string></resources>\n");
        }
        File staging = new File(CommandEnv.GABIEN_HOME, "android/staging").getAbsoluteFile();
        File staging2 = new File(CommandEnv.GABIEN_HOME, "android/staging2").getAbsoluteFile();
        recursivelyDelete(staging);
        recursivelyDelete(staging2);
        staging.mkdirs();
        staging2.mkdirs();
        TreeMap<String, byte[]> jarContents = new TreeMap<>();
        integrateZip(jarContents, new FileInputStream(appJar));
        // Extract JAR contents to staging directory
        extractZip(staging, jarContents);
        // Merge in everything, run d8
        env.cd(CommandEnv.GABIEN_HOME).run(CommandEnv.INCEPT_COMMAND, "d8", "--release", "--lib", System.getenv("ANDROID_JAR_D8"), "--output", staging2.getAbsolutePath(), appJar.getAbsolutePath());
        env.run(CommandEnv.AAPT_COMMAND, "p", "-f", "-I", System.getenv("ANDROID_JAR_AAPT"), "-M", "AndroidManifest.xml", "-S", "res", "-A", "staging/assets", "-F", apk.getAbsolutePath());
        env.cd(staging2).run(CommandEnv.AAPT_COMMAND, "a", apk.getAbsolutePath(), "classes.dex");
        // Obviously, I'll move this stuff into a config file or something if I ever release to the real Play Store - and will change my keystore
        // For making debug keys that'll probably live longer than me:
        // keytool -genkeypair -keyalg RSA -validity 36500
        // Need to override jarsigner breaking things for no reason
        env = env.env("JAVA_TOOL_OPTIONS", "-Djava.security.properties=../java.security");
        env.run(CommandEnv.JARSIGNER_COMMAND, "-sigalg", "SHA1withRSA", "-digestalg", "SHA1", "-storepass", "android", "-sigFile", "CERT", apk.getAbsolutePath(), "mykey");
    }

    /**
     * Be CAREFUL with this! (copied from umvn)
     */
    public static void recursivelyDelete(File sourceTargetDir) {
        try {
            sourceTargetDir = sourceTargetDir.getCanonicalFile();
            if (sourceTargetDir.getParentFile() == null)
                throw new RuntimeException("The requested operation would destroy an entire drive, which is generally considered a bad move.");
            Path p = sourceTargetDir.toPath();
            if (Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                for (File sub : sourceTargetDir.listFiles()) {
                    if (!sub.getCanonicalFile().getParentFile().equals(sourceTargetDir))
                        throw new RuntimeException("Will not recursively delete, weird structure");
                    recursivelyDelete(sub);
                }
            }
            sourceTargetDir.delete();
        } catch (Exception ex) {
            throw new RuntimeException("during recursive delete @ " + sourceTargetDir);
        }
    }

    /**
     * Builds a list of relative paths.
     * For reproducibility, insists upon a SortedSet recipient.
     * Also always uses forward slashes (important for ZIP!)
     * Copied from umvn
     */
    public static void buildListOfRelativePaths(File currentDir, String currentPrefix, SortedSet<String> paths) {
        if (!currentDir.isDirectory())
            return;
        for (File f : currentDir.listFiles()) {
            if (f.isDirectory()) {
                buildListOfRelativePaths(f, currentPrefix + f.getName() + "/", paths);
            } else {
                paths.add(currentPrefix + f.getName());
            }
        }
    }

    /**
     * Reads all bytes in an input stream.
     * Kind-of copied from umvn (specifically zipMakeFileByBufferingInputStream)
     */
    public static byte[] readAllBytes(InputStream ins) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (true) {
            int len = ins.read(buffer);
            if (len <= 0)
                break;
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    /**
     * Integrates a ZIP into a map of a future zip.
     * Copied from umvn w/ type changed
     */
    public static void integrateZip(SortedMap<String, byte[]> map, InputStream in) throws IOException {
        ZipInputStream zis = new ZipInputStream(in, StandardCharsets.UTF_8);
        while (true) {
            ZipEntry ze = zis.getNextEntry();
            if (ze == null)
                break;
            if (!ze.isDirectory())
                map.put(ze.getName(), readAllBytes(zis));
            zis.closeEntry();
        }
        zis.close();
    }

    /**
     * Extracts a ZIP into a directory.
     */
    public static void extractZip(File base, SortedMap<String, byte[]> data) throws IOException {
        base.mkdirs();
        for (Map.Entry<String, byte[]> s : data.entrySet()) {
            File f = new File(base, s.getKey());
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), s.getValue());
        }
    }

    /**
     * Makes a ZIP file from its contents.
     * Copied from umvn w/ type changed
     */
    public static byte[] makeZip(SortedMap<String, byte[]> files) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8);
            for (Map.Entry<String, byte[]> ent : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(ent.getKey()));
                zos.write(ent.getValue());
                zos.closeEntry();
            }
            zos.close();
        } catch (Exception e) {
            throw new RuntimeException("Writing ZIP", e);
        }
        return baos.toByteArray();
    }
}
