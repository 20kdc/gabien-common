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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.reandroid.arsc.chunk.PackageBlock;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.value.Entry;

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

    private static final String NAME_installLocation = "installLocation";
    private static final int ID_installLocation = 0x010102b7;
    private static final String NAME_hardwareAccelerated = "hardwareAccelerated";
    private static final int ID_hardwareAccelerated = 0x010102d3;
    private static final String NAME_theme = "theme";
    private static final int ID_theme = 0x01010000;
    private static final int RES_ThemeLight = 0x0103000c;

    public static void androidBuild(CommandEnv env, String name, String pkg, String vName, int vCode, File appJar, File icon, String[] permissions, File apk) throws Exception {
        env = env.cd(new File(CommandEnv.GABIEN_HOME, "android"));
        Files.copy(icon.toPath(), new File(CommandEnv.GABIEN_HOME, "android/res/drawable/icon.png").toPath(), StandardCopyOption.REPLACE_EXISTING);

        // For debugging:
        //  aapt d xmltree workspace.apk AndroidManifest.xml
        //  aapt d resources workspace.apk resources.arsc
        //  gabien-do dump-arsc resources.arsc
        // -- build resources --
        TableBlock tableBlock = new TableBlock();
        PackageBlock packageBlock = tableBlock.getOrCreatePackage(0x7f, pkg);
        Entry iconEntry = packageBlock.getOrCreate("", "drawable", "icon");
        iconEntry.setValueAsString("res/drawable/icon.png");
        // -- build manifest --
        // Dear goodness this is so simple and easy to use THANK YOU
        AndroidManifestBlock manifestBlock = new AndroidManifestBlock();
        manifestBlock.getOrCreateElement("manifest").getOrCreateAndroidAttribute(NAME_installLocation, ID_installLocation).setValueAsDecimal(0);
        manifestBlock.setPackageName(pkg);
        manifestBlock.setVersionCode(vCode);
        manifestBlock.setVersionName(vName);
        manifestBlock.setMinSdkVersion(7);
        manifestBlock.setTargetSdkVersion(23);
        for (String p : permissions)
            manifestBlock.addUsesPermission(p);
        manifestBlock.setApplicationLabel(name);
        manifestBlock.getApplicationElement().getOrCreateAndroidAttribute(NAME_hardwareAccelerated, ID_hardwareAccelerated).setValueAsBoolean(true);
        // sadly, android:debuggable slows things down.
        // you have to go to JNI Tips, where it will happily THEN tell you that enabling this activates CheckJNI.
        // this note courtesy of past-me
        manifestBlock.setIconResourceId(iconEntry.getResourceId());
        // 0x0103000c: Theme_Light
        manifestBlock.getApplicationElement().getOrCreateAndroidAttribute(NAME_theme, ID_theme).setValueAsResourceId(RES_ThemeLight);
        ResXmlElement mainActivity = manifestBlock.getOrCreateMainActivity("gabien.MainActivity");
        mainActivity.getOrCreateAndroidAttribute("immersive", 0x010102c0).setValueAsBoolean(true);
        // -- done, refresh (autocorrects stuff) --
        packageBlock.refreshFull();
        // might be a bug? tableBlock refresh doesn't actually refresh packages
        tableBlock.refreshFull();
        manifestBlock.refreshFull();
        // -- the rest --

        File staging2 = new File(CommandEnv.GABIEN_HOME, "android/staging2").getAbsoluteFile();
        recursivelyDelete(staging2);
        staging2.mkdirs();
        TreeMap<String, byte[]> jarContents = new TreeMap<>();
        integrateZip(jarContents, new FileInputStream(appJar));
        // we need to remove things from the JAR that can't be accessed, keeping in mind that only assets/ files can be accessed
        // in practice this deletes the .class files and should reduce build sizes to before the build system changes
        for (String s : new LinkedList<>(jarContents.keySet())) {
            if (s.contains("/")) {
                if (s.startsWith("assets/") || s.startsWith("META-INF/"))
                    continue;
                // get rid of it
                jarContents.remove(s);
            }
        }
        jarContents.put("res/drawable/icon.png", Files.readAllBytes(icon.toPath()));
        // Merge in everything, run d8
        env.cd(CommandEnv.GABIEN_HOME).run(CommandEnv.INCEPT_COMMAND, "d8", "--release", "--lib", MavenRepository.getJARFile(Constants.COORDS_ANDROID_PLATFORM).getAbsolutePath(), "--output", staging2.getAbsolutePath(), appJar.getAbsolutePath());
        jarContents.put("classes.dex", Files.readAllBytes(new File(staging2, "classes.dex").toPath()));
        jarContents.put("resources.arsc", tableBlock.getBytes());
        jarContents.put("AndroidManifest.xml", manifestBlock.getBytes());
        Files.write(apk.toPath(), makeZip(jarContents));
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
