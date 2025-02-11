/*
 * umvn - Hyperportable Java 8 build tool
 *
 * Written starting in 2025 by:
 *  20kdc
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * microMVN: "Not quite Maven" in a single class file.
 * Created February 10th, 2025.
 */
public final class umvn {
    // -- how-to-find this --
    public static final String URL = "https://github.com/20kdc/gabien-common/tree/master/micromvn";

    // -- switches --
    public static boolean LOG_HEADER_FOOTER = true;
    public static boolean LOG_DEBUG = false;
    public static boolean OFFLINE = false;

    // -- cache --
    /**
     * Maps absolute files to their POM objects.
     */
    public static HashMap<File, umvn> POM_BY_FILE = new HashMap<>();
    /**
     * Maps triples to their POM objects.
     */
    public static HashMap<String, umvn> POM_BY_TRIPLE = new HashMap<>();
    /**
     * Maps triples to their remote repositories.
     * This prevents redundant lookups.
     */
    public static HashMap<String, String> REPO_BY_TRIPLE = new HashMap<>();
    /**
     * Repository list. All repository URLs end in '/'.
     */
    public static final LinkedList<String> REPOSITORIES = new LinkedList<>();

    // -- POM --
    /**
     * pom.xml / .pom file contents
     */
    public final byte[] pomFileContent;

    /**
     * A "source" POM is a POM we should be compiling.
     * It must not be in the local repo.
     */
    public final File sourceDir;

    public final String groupId, artifactId, version, triple;

    public final umvn parent;

    public final LinkedList<umvn> modules = new LinkedList<>();
    public final HashMap<String, String> properties = new HashMap<>();

    public final LinkedList<String> depTriplesOptionalClasspath = new LinkedList<>();
    public final LinkedList<String> depTriplesOptionalPackaged = new LinkedList<>();
    public final LinkedList<String> depTriplesClasspath = new LinkedList<>();
    public final LinkedList<String> depTriplesPackaged = new LinkedList<>();

    public boolean isPOMPackaged;

    public String mainClass;

    // -- Loader --

    private umvn(byte[] pomFileContent, File pomFileAssoc, File sourceDir) {
        this.pomFileContent = pomFileContent;
        this.sourceDir = sourceDir;
        // do this immediately, even though we're invalid
        // this should help prevent infinite loops
        String debugInfo = "[POM in memory]";
        if (pomFileAssoc != null) {
            debugInfo = pomFileAssoc.toString();
            POM_BY_FILE.put(pomFileAssoc, this);
        }
        if (LOG_DEBUG)
            System.err.println(debugInfo);
        try {
            Document pomDoc = parseXML(pomFileContent);
            Element projectElement = findElement(pomDoc, "project", true);
            // -- Parent must happen first so that we resolve related modules early and so we know our triple --
            Element elm = findElement(projectElement, "parent", false);
            umvn theParent = null;
            String theGroupId = null;
            String theArtifactId = null;
            String theVersion = null;
            if (elm != null) {
                theParent = getPOMByTriple(getTriplePOMRef(elm, sourceDir));
                theGroupId = theParent.groupId;
                theArtifactId = theParent.artifactId;
                theVersion = theParent.version;
                properties.putAll(theParent.properties);
            }
            groupId = ensureSpecifierHelper(theGroupId, "groupId", projectElement);
            artifactId = ensureSpecifierHelper(theArtifactId, "artifactId", projectElement);
            version = ensureSpecifierHelper(theVersion, "version", projectElement);
            triple = getTriple(groupId, artifactId, version);
            parent = theParent;
            POM_BY_TRIPLE.put(triple, this);
            if (LOG_DEBUG)
                System.err.println(" = " + getTriple(groupId, artifactId, version));
            // -- <packaging> --
            String packaging = templateFindElement(projectElement, "packaging", "jar");
            if (packaging.equals("pom")) {
                isPOMPackaged = true;
            } else if (packaging.equals("jar")) {
                isPOMPackaged = false;
            } else {
                throw new RuntimeException("Unknown packaging type: " + packaging);
            }
            // -- <properties> --
            elm = findElement(projectElement, "properties", false);
            if (elm != null) {
                for (Node n : nodeChildrenArray(elm.getChildNodes())) {
                    if (n instanceof Element) {
                        // System.out.println(n.getNodeName() + "=" + n.getTextContent());
                        properties.put(n.getNodeName(), template(n.getTextContent()));
                    }
                }
            }
            // -- <repositories> --
            elm = findElement(projectElement, "repositories", false);
            if (elm != null) {
                for (Node n : nodeChildrenArray(elm.getChildNodes())) {
                    if (n instanceof Element && n.getNodeName().equals("repository")) {
                        String url = templateFindElement(n, "url", true);
                        installCorrectedRepoUrl(url);
                    }
                }
            }
            // -- <dependencies> --
            elm = findElement(projectElement, "dependencies", false);
            if (elm != null) {
                for (Node n : nodeChildrenArray(elm.getChildNodes())) {
                    if (n instanceof Element && n.getNodeName().equals("dependency")) {
                        Node optionalNode = findElement(n, "optional", false);
                        LinkedList<String> lstClasspath = depTriplesClasspath;
                        LinkedList<String> lstPackaged = depTriplesPackaged;
                        if (optionalNode != null && optionalNode.getTextContent().equals("true")) {
                            lstClasspath = depTriplesOptionalClasspath;
                            lstPackaged = depTriplesOptionalPackaged;
                        }
                        Node scopeNode = findElement(n, "scope", false);
                        String scope = "compile";
                        if (scopeNode != null)
                            scope = scopeNode.getTextContent();
                        if (scope.equals("compile")) {
                            String dep = getTriplePOMRef(n, sourceDir);
                            lstClasspath.add(dep);
                            lstPackaged.add(dep);
                        } else if (scope.equals("provided")) {
                            String dep = getTriplePOMRef(n, sourceDir);
                            lstClasspath.add(dep);
                        }
                        // other scopes = not relevant
                    }
                }
            }
            // -- <modules> --
            if (sourceDir != null) {
                elm = findElement(projectElement, "modules", false);
                if (elm != null) {
                    for (Node n : nodeChildrenArray(elm.getChildNodes())) {
                        if (n instanceof Element && n.getNodeName().equals("module")) {
                            String moduleName = template(n.getTextContent());
                            modules.add(loadPOM(new File(moduleName + "/pom.xml"), true));
                        }
                    }
                }
            }
            // -- <build> --
            Element buildElm = findElement(projectElement, "build", false);
            if (buildElm != null) {
                elm = findElement(buildElm, "plugins", false);
                if (elm != null) {
                    for (Node plugin : nodeChildrenArray(elm.getChildNodes())) {
                        if (plugin instanceof Element && plugin.getNodeName().equals("plugin")) {
                            String pluginId = templateFindElement(plugin, "artifactId", "unknown-dont-care");
                            if (LOG_DEBUG)
                                System.err.println(" plugin: " + pluginId);
                            if (pluginId.equals("maven-assembly-plugin")) {
                                Element a1 = findElementRecursive(plugin, "manifest", false);
                                if (a1 != null)
                                    this.mainClass = templateFindElement(a1, "mainClass", null);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("loading POM " + debugInfo, e);
        }
    }

    private static String ensureSpecifierHelper(String old, String attr, Node base) {
        Node theNode = findElement(base, attr, old == null);
        if (theNode != null)
            return theNode.getTextContent();
        return old;
    }

    // -- POM management --

    /**
     * Loads a POM from a file.
     */
    public static umvn loadPOM(File f, boolean isSource) {
        f = f.getAbsoluteFile();
        umvn res = POM_BY_FILE.get(f);
        if (res != null)
            return res;
        byte[] pomFileBytes;
        try {
            pomFileBytes = Files.readAllBytes(f.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + f, e);
        }
        return new umvn(pomFileBytes, f, isSource ? f.getParentFile() : null);
    }

    /**
     * Returns a triple.
     * Enthusiastically loads relative paths to ensure we have them discovered for later.
     */
    public String getTriplePOMRef(Node ref, File relativePathDir) {
        String theGroupId = templateFindElement(ref, "groupId", true);
        String theArtifactId = templateFindElement(ref, "artifactId", true);
        String theVersion = templateFindElement(ref, "version", true);
        String triple = getTriple(theGroupId, theArtifactId, theVersion);
        umvn possibleMatch = POM_BY_TRIPLE.get(triple);
        if (possibleMatch == null && relativePathDir != null) {
            String relPathNode = templateFindElement(ref, "relativePath", false);
            if (relPathNode != null)
                loadPOM(new File(relativePathDir, relPathNode), true);
        }
        return triple;
    }

    /**
     * Returns the POM from a triple.
     */
    public static umvn getPOMByTriple(String triple) {
        umvn trivial = POM_BY_TRIPLE.get(triple);
        if (trivial != null)
            return trivial;
        String[] parts = triple.split(":");
        if (parts.length != 3)
            throw new RuntimeException("Invalid triple: " + triple);
        return loadPOM(getOrDownloadArtifact(parts[0], parts[1], parts[2], ".pom"), false);
    }

    /**
     * Returns a colon-separated triple string.
     */
    public static String getTriple(String groupId, String artifactId, String version) {
        return groupId + ":" + artifactId + ":" + version;
    }

    // -- Classpath/Compile Management --

    /**
     * If this is a source POM, gets the source directory.
     */
    public File getSourceJavaDir() {
        return new File(sourceDir, "src/main/java");
    }

    /**
     * If this is a source POM, gets the source directory.
     */
    public File getSourceResourcesDir() {
        return new File(sourceDir, "src/main/resources");
    }

    /**
     * If this is a source POM, gets the target classes directory.
     */
    public File getSourceTargetDir() {
        return new File(sourceDir, "target");
    }

    /**
     * If this is a source POM, gets the target classes directory.
     */
    public File getSourceTargetClassesDir() {
        return new File(getSourceTargetDir(), "classes");
    }

    /**
     * If this is a source POM, gets the target JAR file.
     */
    public File getSourceTargetJARFile() {
        return new File(getSourceTargetDir(), artifactId + "-" + version + ".jar");
    }

    /**
     * If this is a source POM, gets the target packaged JAR file.
     */
    public File getSourceTargetPackagedJARFile() {
        return new File(getSourceTargetDir(), artifactId + "-" + version + "-jar-with-dependencies.jar");
    }

    /**
     * All POMs we want to compile when compiling this POM.
     * Won't do anything if this POM is already in the list.
     */
    public void addFullCompileSet(HashSet<umvn> compileThese) {
        if (compileThese.add(this))
            for (umvn s : modules)
                s.addFullCompileSet(compileThese);
    }

    /**
     * All POMs we want in the classpath/packaging when compiling this POM.
     * Won't do anything if this POM is already in the list.
     */
    public void addFullClasspathSet(HashSet<umvn> compileThese, boolean packaging, boolean root) {
        if (compileThese.add(this)) {
            if (root) {
                // Optional deps count for the root only.
                for (String s : (packaging ? depTriplesOptionalPackaged : depTriplesOptionalClasspath))
                    getPOMByTriple(s).addFullClasspathSet(compileThese, packaging, false);
            }
            for (String s : (packaging ? depTriplesPackaged : depTriplesClasspath))
                getPOMByTriple(s).addFullClasspathSet(compileThese, packaging, false);
        }
    }

    // -- Templating --

    public String getPropertyFull(String basis) {
        if (basis.equals("project.artifactId"))
            return artifactId;
        if (basis.equals("project.groupId"))
            return groupId;
        if (basis.equals("project.version"))
            return version;
        String pv = null;
        if (basis.startsWith("env."))
            pv = System.getenv(basis.substring(4));
        if (pv == null)
            pv = properties.get(basis);
        if (pv == null)
            pv = System.getProperty(basis);
        return pv;
    }

    public String getPropertyFull(String basis, String def) {
        String pv = getPropertyFull(basis);
        if (pv == null)
            pv = System.getProperty(basis, def);
        return pv;
    }

    /**
     * Templates property references. Needed because of hamcrest.
     */
    public String template(String text) {
        String res = "";
        int at = 0;
        while (true) {
            int idx = text.indexOf("${", at);
            if (idx == -1) {
                res += text.substring(at);
                return res;
            }
            if (at != idx)
                res += text.substring(at, idx);
            int idx2 = text.indexOf("}", idx);
            if (idx2 == -1)
                throw new RuntimeException("Unclosed template @ " + text);
            String prop = text.substring(idx + 2, idx2);
            String propVal = getPropertyFull(prop);
            if (propVal != null) {
                res += propVal;
            } else {
                // failed to template
                if (LOG_DEBUG)
                    System.err.println("Skipping template of property " + prop + "; doesn't exist");
                res += text.substring(at, idx2 + 1);
            }
            at = idx2 + 1;
        }
    }

    /**
     * Kind of shorthand for template(findElement().getTextContent()) but passes through null.
     */
    public String templateFindElement(Node pomDoc, String string, boolean required) {
        Node n = findElement(pomDoc, string, required);
        if (n == null)
            return null;
        return template(n.getTextContent());
    }

    /**
     * Finds an element. If it fails, returns the default. Otherwise, templates its text contents.
     */
    public String templateFindElement(Node pomDoc, String string, String def) {
        Node n = findElement(pomDoc, string, false);
        if (n == null)
            return def;
        return template(n.getTextContent());
    }

    // -- XML --

    public static Document parseXML(byte[] f) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(f));
        } catch (Exception ex) {
            throw new RuntimeException("parsing " + f, ex);
        }
    }

    public static Node[] nodeChildrenArray(NodeList list) {
        Node[] nodes = new Node[list.getLength()];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = list.item(i);
        return nodes;
    }

    public static Element findElementRecursive(Node pomDoc, String string, boolean required) {
        for (Node n : nodeChildrenArray(pomDoc.getChildNodes())) {
            if (n instanceof Element)
                if (n.getNodeName().equals(string))
                    return (Element) n;
            Element result = findElementRecursive(n, string, required);
            if (result != null)
                return result;
        }
        if (required)
            throw new RuntimeException("Unable to find element <" + string + ">");
        return null;
    }

    public static Element findElement(Node pomDoc, String string, boolean required) {
        for (Node n : nodeChildrenArray(pomDoc.getChildNodes())) {
            // System.out.println(n);
            if (n instanceof Element) {
                if (n.getNodeName().equals(string))
                    return (Element) n;
            }
        }
        if (required)
            throw new RuntimeException("Unable to find element <" + string + ">");
        return null;
    }

    // -- Build --

    /**
     * Cleans the project.
     */
    public void clean() {
        recursivelyDelete(getSourceTargetDir());
    }

    /**
     * Compiles the project. Returns false on compile error.
     */
    public boolean compile() throws Exception {
        if (isPOMPackaged)
            return true;
        File classes = getSourceTargetClassesDir();
        File java = getSourceJavaDir();
        File resources = getSourceResourcesDir();
        classes.mkdirs();
        // compile classes
        HashSet<String> copy = new HashSet<>();
        buildListOfRelativePaths(java, "", copy);
        LinkedList<Process> processes = new LinkedList<>();
        // batch into separate processes based on command line length because Windows borked?
        // uh, no, because javac also borked. uhhhh
        LinkedList<File> listForCurrentProcess = new LinkedList<>();
        for (String sourceFileName : copy) {
            if (!sourceFileName.endsWith(".java"))
                continue;
            listForCurrentProcess.add(new File(java, sourceFileName));
        }
        if (!listForCurrentProcess.isEmpty())
            processes.add(runJavac(listForCurrentProcess.toArray(new File[0])));
        // done!
        boolean ok = true;
        for (Process p : processes)
            ok &= p.waitFor() == 0;
        // copy resources
        copy.clear();
        buildListOfRelativePaths(resources, "", copy);
        for (String s : copy) {
            File dstFile = new File(classes, s);
            dstFile.getParentFile().mkdirs();
            Files.copy(new File(resources, s).toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return ok;
    }

    /**
     * Install this package.
     */
    public void install() throws Exception {
        File artifactPOM = getLocalRepoArtifact(".pom");
        artifactPOM.getParentFile().mkdirs();
        Files.write(artifactPOM.toPath(), pomFileContent);
        if (!isPOMPackaged) {
            Files.copy(getSourceTargetJARFile().toPath(), getLocalRepoArtifact(".jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(getSourceTargetPackagedJARFile().toPath(), getLocalRepoArtifact("-jar-with-dependencies.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Runs javac as configured for this project.
     */
    private Process runJavac(File[] s) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        LinkedList<String> command = new LinkedList<>();
        command.add(getPropertyFull("maven.compiler.executable"));
        command.add("-d");
        command.add(getSourceTargetClassesDir().toString());
        // build classpath/sourcepath
        StringBuilder classpath = new StringBuilder();
        StringBuilder sourcepath = new StringBuilder();
        HashSet<umvn> classpathSet = new HashSet<>();
        addFullClasspathSet(classpathSet, false, true);
        for (umvn classpathEntry : classpathSet) {
            if (classpathEntry.sourceDir != null) {
                if (sourcepath.length() != 0)
                    sourcepath.append(File.pathSeparatorChar);
                sourcepath.append(classpathEntry.getSourceJavaDir().toString());
            } else {
                if (classpath.length() != 0)
                    classpath.append(File.pathSeparatorChar);
                classpath.append(getOrDownloadArtifact(classpathEntry, ".jar").toString());
            }
        }
        if (sourcepath.length() != 0) {
            command.add("-sourcepath");
            command.add(sourcepath.toString());
        }
        if (classpath.length() != 0) {
            command.add("-classpath");
            command.add(classpath.toString());
        }
        // continue
        command.add("-encoding");
        command.add(getPropertyFull("project.build.sourceEncoding", "UTF-8"));
        String sourceVer = getPropertyFull("maven.compiler.source", null);
        if (sourceVer != null) {
            command.add("-source");
            command.add(sourceVer);
        }
        String targetVer = getPropertyFull("maven.compiler.target", null);
        if (targetVer != null) {
            command.add("-target");
            command.add(targetVer);
        }
        for (File f : s)
            command.add(f.toString());
        pb.command(command);
        return pb.start();
    }

    /**
     * Creates the base JAR.
     */
    public void packageJAR() {
        if (isPOMPackaged)
            return;
        HashMap<String, Consumer<OutputStream>> zip = new HashMap<>();
        integrateJAR(zip);
        zipBuild(getSourceTargetJARFile(), zip);
    }

    /**
     * Creates the base JAR 'in-memory', or integrates the existing base JAR for non-source POMs.
     */
    public void integrateJAR(HashMap<String, Consumer<OutputStream>> zip) {
        if (sourceDir != null) {
            // note that the manifest can (deliberately) be overwritten!
            zip.put("META-INF/MANIFEST.MF", zipMakeFile(createManifest(false)));
            zip.put("META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml", zipMakeFile(pomFileContent));
            HashSet<String> files = new HashSet<>();
            File classes = getSourceTargetClassesDir();
            buildListOfRelativePaths(classes, "", files);
            zipIntegrateRelativePaths(zip, "", classes, files);
        } else {
            if (isPOMPackaged)
                return;
            File myJAR = getOrDownloadArtifact(this, ".jar");
            try {
                ZipInputStream zis = new ZipInputStream(new FileInputStream(myJAR), StandardCharsets.UTF_8);
                while (true) {
                    ZipEntry ze = zis.getNextEntry();
                    if (ze == null)
                        break;
                    if (!ze.isDirectory())
                        zip.put(ze.getName(), zipMakeFileByBufferingInputStream(zis));
                    zis.closeEntry();
                }
                zis.close();
            } catch (Exception ex) {
                throw new RuntimeException("integrating zip " + myJAR, ex);
            }
        }
    }

    /**
     * Creates the assembly.
     */
    public void packageJARWithDependencies() {
        if (isPOMPackaged)
            return;
        HashMap<String, Consumer<OutputStream>> zip = new HashMap<>();
        HashSet<umvn> myDeps = new HashSet<umvn>();
        addFullClasspathSet(myDeps, true, true);
        myDeps.remove(this);
        for (umvn various : myDeps)
            various.integrateJAR(zip);
        // re-add self to ensure last
        integrateJAR(zip);
        zip.put("META-INF/MANIFEST.MF", zipMakeFile(createManifest(true)));
        // done
        zipBuild(getSourceTargetPackagedJARFile(), zip);
    }

    public String createManifest(boolean packaging) {
        String manifest = "Manifest-Version: 1.0\r\nCreated-By: micromvn\r\n";
        if (mainClass != null)
            manifest += "Main-Class: " + mainClass + "\r\n";
        return manifest;
    }

    // -- Repository Management --

    /**
     * Corrects and adds a repo URL.
     */
    public static void installCorrectedRepoUrl(String url) {
        if (!url.endsWith("/"))
            url += "/";
        if (!REPOSITORIES.contains(url))
            REPOSITORIES.add(url);
    }

    /**
     * Downloads this POM & JAR/etc.
     */
    public void completeDownload() {
        if (sourceDir != null)
            return;
        if (isPOMPackaged)
            return;
        getOrDownloadArtifact(this, ".jar");
    }

    /**
     * Gets or downloads a file into the local repo by artifact path.
     */
    public static File getOrDownloadArtifact(umvn pom, String suffix) {
        return getOrDownloadArtifact(pom.groupId, pom.artifactId, pom.version, suffix);
    }

    /**
     * Gets or downloads a file into the local repo by artifact.
     */
    public static File getOrDownloadArtifact(String groupId, String artifactId, String version, String suffix) {
        String artifactPath = getArtifactPath(groupId, artifactId, version, suffix);
        // look for it locally
        File localRepoFile = new File(getLocalRepo(), artifactPath);
        if (localRepoFile.exists())
            return localRepoFile;
        localRepoFile.getParentFile().mkdirs();
        if (!OFFLINE) {
            String triple = getTriple(groupId, artifactId, version);
            // this is the ugly part where we have to go find it on the server
            String cachedRepo = REPO_BY_TRIPLE.get(triple);
            if (cachedRepo != null)
                if (download(localRepoFile, cachedRepo + artifactPath))
                    return localRepoFile;
            // did not match cache
            for (String repo : REPOSITORIES) {
                if (cachedRepo != null && repo.equals(cachedRepo))
                    continue;
                if (download(localRepoFile, repo + artifactPath)) {
                    REPO_BY_TRIPLE.put(triple, repo);
                    return localRepoFile;
                }
            }
            throw new RuntimeException("Failed to retrieve " + artifactPath);
        } else {
            throw new RuntimeException(artifactPath + " not in local repo, and OFFLINE enabled.");
        }
    }

    public static boolean download(File target, String urlString) {
        try {
            java.net.URL url = new java.net.URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "micromvn");
            conn.connect();
            Files.copy(conn.getInputStream(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Thread.sleep(100);
            return true;
        } catch (Exception ex) {
            System.err.println("Failed download @ " + urlString + ": " + ex);
            return false;
        }
    }

    public static File getLocalRepo() {
        return new File(System.getProperty("maven.repo.local"));
    }

    public File getLocalRepoArtifact(String suffix) {
        return new File(getLocalRepo(), getArtifactPath(suffix));
    }

    public String getArtifactPath(String suffix) {
        return getArtifactPath(groupId, artifactId, version, suffix);
    }

    public static String getArtifactPath(String groupId, String artifactId, String version, String suffix) {
        return groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + suffix;
    }

    // -- IO Utilities --

    public static void buildListOfRelativePaths(File currentDir, String currentPrefix, HashSet<String> paths) {
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

    public static void recursivelyDelete(File sourceTargetDir) {
        try {
            sourceTargetDir = sourceTargetDir.getCanonicalFile();
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

    public static void zipIntegrateRelativePaths(HashMap<String, Consumer<OutputStream>> zip, String prefix, File dir, Collection<String> paths) {
        for (String p : paths)
            zip.put(prefix + p, zipMakeFileReader(new File(dir, p)));
    }

    public static Consumer<OutputStream> zipMakeFile(String text) {
        return zipMakeFile(text.getBytes(StandardCharsets.UTF_8));
    }

    public static Consumer<OutputStream> zipMakeFile(byte[] text) {
        return (os) -> {
            try {
                os.write(text);
            } catch (Exception ex) {
                throw new RuntimeException("Adding binary file", ex);
            }
        };
    }

    public static Consumer<OutputStream> zipMakeFileByBufferingInputStream(InputStream ins) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        while (true) {
            int len = ins.read(buffer);
            if (len <= 0)
                break;
            baos.write(buffer, 0, len);
        }
        return (os) -> {
            try {
                baos.writeTo(os);
            } catch (Exception ex) {
                throw new RuntimeException("Adding text file", ex);
            }
        };
    }

    public static Consumer<OutputStream> zipMakeFileReader(File inputFile) {
        return (os) -> {
            try {
                Files.copy(inputFile.toPath(), os);
            } catch (Exception ex) {
                throw new RuntimeException("Copying " + inputFile + " to ZIP", ex);
            }
        };
    }

    public static void zipBuild(File outputFile, HashMap<String, Consumer<OutputStream>> files) {
        try {
            outputFile.getParentFile().mkdirs();
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile), StandardCharsets.UTF_8);
            for (Map.Entry<String, Consumer<OutputStream>> ent : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(ent.getKey()));
                ent.getValue().accept(zos);
                zos.closeEntry();
            }
            zos.close();
        } catch (Exception e) {
            throw new RuntimeException("Writing ZIP: " + outputFile, e);
        }
    }

    // -- install-file --

    /**
     * Creates a dummy POM file for a file being installed by `install-file`.
     */
    public static void createDummyPOM(String groupId, String artifactId, String version, String packaging) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        sb.append("<modelVersion>4.0.0</modelVersion>\n");
        sb.append("<groupId>" + groupId + "</groupId>\n");
        sb.append("<artifactId>" + artifactId + "</artifactId>\n");
        sb.append("<version>" + version + "</version>\n");
        sb.append("<packaging>" + packaging + "</packaging>\n");
        sb.append("</project>\n");
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        Files.write(new File(getLocalRepo(), getArtifactPath(groupId, artifactId, version, ".pom")).toPath(), data);
    }

    // -- Main --

    public static void main(String[] args) throws Exception {
        // autodetect javac

        if (System.getProperty("maven.compiler.executable") == null) {
            String javac;
            String home = System.getenv("MICROMVN_JAVA_HOME");
            if (home == null)
                home = System.getenv("JAVA_HOME");
            String possiblyExe = System.getProperty("os.name", "unknown").toLowerCase().startsWith("windows") ? ".exe" : "";
            if (home != null) {
                if (home.endsWith(File.separator))
                    javac = home + "bin" + File.separator + "javac" + possiblyExe;
                else
                    javac = home + File.separator + "bin" + File.separator + "javac" + possiblyExe;
            } else {
                File f = new File(System.getProperty("java.home"));
                if (f.getName().equals("jre"))
                    f = f.getParentFile();
                File expectedTool = new File(f, "bin" + File.separator + "javac" + possiblyExe);
                // validate
                if (expectedTool.exists()) {
                    javac = expectedTool.toString();
                } else {
                    // we could have a problem here. fall back to PATH
                    javac = "javac";
                }
            }
            System.setProperty("maven.compiler.executable", javac);
        }

        // autodetect local repo

        if (System.getProperty("maven.repo.local") == null)
            System.setProperty("maven.repo.local", new File(System.getProperty("user.home"), ".m2/repository").toString());

        // arg parsing & init properties

        String goal = null;
        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            String s = args[argIndex];
            if (s.startsWith("-")) {
                if (s.startsWith("-D")) {
                    String info = s.substring(2);
                    if (info.length() == 0)
                        info = args[++argIndex];
                    int infoSplitterIndex = info.indexOf('=');
                    if (infoSplitterIndex == -1)
                        throw new RuntimeException("define " + info + " invalid, no =");
                    String k = info.substring(0, infoSplitterIndex);
                    String v = info.substring(infoSplitterIndex + 1);
                    System.setProperty(k, v);
                } else if (s.equals("--version") || s.equals("-v")) {
                    System.out.println(doVersionInfo());
                    return;
                } else if (s.equals("--help") || s.equals("-h")) {
                    doHelp();
                    return;
                } else if (s.equals("--quiet") || s.equals("-q")) {
                    LOG_HEADER_FOOTER = false;
                } else if (s.equals("--debug") || s.equals("-X")) {
                    LOG_DEBUG = true;
                } else if (s.equals("--offline") || s.equals("-o")) {
                    OFFLINE = true;
                } else {
                    throw new RuntimeException("Unknown option " + s);
                }
            } else {
                if (goal != null)
                    throw new RuntimeException("Can't specify multiple goals in micromvn.");
                int colon = s.lastIndexOf(':');
                if (colon == -1) {
                    goal = s;
                } else {
                    goal = s.substring(colon + 1);
                }
            }
        }
        if ((goal == null) || goal.equals("help")) {
            doHelp();
            return;
        }

        // handle properties

        String possibleRepoOverride = System.getProperty("repoUrl");
        if (possibleRepoOverride == null)
            possibleRepoOverride = "https://repo1.maven.org/maven2/";
        installCorrectedRepoUrl(possibleRepoOverride);

        // version

        if (LOG_HEADER_FOOTER)
            System.err.println(doVersionInfo());

        // goal

        if (goal.equals("clean")) {
            doBuild(0);
        } else if (goal.equals("compile")) {
            doBuild(1);
        } else if (goal.equals("package")) {
            doBuild(2);
        } else if (goal.equals("install")) {
            doBuild(3);
        } else if (goal.equals("get")) {
            String prop = System.getProperty("artifact");
            if (prop == null)
                throw new RuntimeException("get requires -Dartifact=...");
            getPOMByTriple(prop).completeDownload();
        } else if (goal.equals("install-file")) {
            String file = System.getProperty("artifact");
            String pomFile = System.getProperty("pomFile");
            String groupId = System.getProperty("groupId");
            String artifactId = System.getProperty("artifactId");
            String version = System.getProperty("version");
            String packaging = System.getProperty("packaging");
            if (file == null)
                throw new RuntimeException("install-file requires -Dfile=...");
            if (pomFile != null) {
                umvn resPom = loadPOM(new File(pomFile), false);
                groupId = resPom.groupId;
                artifactId = resPom.artifactId;
                version = resPom.version;
                packaging = resPom.isPOMPackaged ? "pom" : "jar";
            }
            if (groupId == null)
                throw new RuntimeException("install-file requires -DgroupId=... (or -DpomFile=...)");
            if (artifactId == null)
                throw new RuntimeException("install-file requires -DartifactId=... (or -DpomFile=...)");
            if (version == null)
                throw new RuntimeException("install-file requires -Dversion=... (or -DpomFile=...)");
            if (packaging == null)
                throw new RuntimeException("install-file requires -Dpackaging=... (or -DpomFile=...)");
            File outPOM = new File(getLocalRepo(), getArtifactPath(groupId, artifactId, version, ".pom"));
            File outJAR = new File(getLocalRepo(), getArtifactPath(groupId, artifactId, version, ".jar"));
            if (!packaging.equals("pom"))
                Files.copy(new File(file).toPath(), outJAR.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (pomFile != null) {
                Files.copy(new File(pomFile).toPath(), outPOM.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                createDummyPOM(groupId, artifactId, version, packaging);
            }
        } else {
            throw new RuntimeException("Unsupported goal/phase: " + goal);
        }
    }

    public static String doVersionInfo() {
        return "micromvn CommitIDHere\njava.version=" + System.getProperty("java.version") + " maven.compiler.executable=" + System.getProperty("maven.compiler.executable") + " maven.repo.local=" + System.getProperty("maven.repo.local");
    }

    public static void doHelp() {
        System.out.println("# micromvn: a Maven'-ish' builder in a single Java class");
        System.out.println("");
        System.out.println("micromvn is not Maven, it's not almost Maven, it's not a program which downloads Maven.\\");
        System.out.println("micromvn is a self-contained Java build tool small enough to be shipped with your projects, which acts enough like Maven for usecases that don't require full Maven, and doesn't add another installation step for new contributors.");
        System.out.println("");
        System.out.println("Usage: `java umvn [options] <goal> [options]`");
        System.out.println("");
        System.out.println("If a goal contains a colon, the last colon and everything before it is discarded.");
        System.out.println("");
        System.out.println("Goals are:");
        System.out.println("");
        System.out.println(" * `clean`\\");
        System.out.println("   Cleans all target projects (deletes target directory).");
        System.out.println(" * `compile`\\");
        System.out.println("   Cleans and compiles all target projects.");
        System.out.println(" * `package`\\");
        System.out.println("   Cleans, compiles, and packages all target projects to JAR files.");
        System.out.println("   This also includes an imitation of maven-assembly-plugin.");
        System.out.println(" * `install`\\");
        System.out.println("   Cleans, compiles, packages, and installs all target projects to the local Maven repo.");
        System.out.println(" * `dependency:get -Dartifact=<...>`\\");
        System.out.println("   Downloads a specific artifact to the local Maven repo.");
        System.out.println(" * `install:install-file -Dfile=<...> -DgroupId=<...> -DartifactId=<...> -Dversion=<...> -Dpackaging=<...>`\\");
        System.out.println("   Installs a JAR to the local Maven repo, creating a dummy POM for it.");
        System.out.println(" * `install:install-file -Dfile=<...> -DpomFile=<...>`\\");
        System.out.println("   Installs a JAR to the local Maven repo, importing an existing POM.");
        System.out.println(" * `help`\\");
        System.out.println("   Shows this text.");
        System.out.println("");
        System.out.println("## Options");
        System.out.println("");
        System.out.println(" * `-D<key>=<value>`\\");
        System.out.println("   Sets a Java System Property. These are inherited into the POM property space.");
        System.out.println(" * `--version` / `-v`\\");
        System.out.println("   Reports the version + some other info and exits.");
        System.out.println(" * `--help` / `-h`\\");
        System.out.println("   Shows this help text.");
        System.out.println(" * `--quiet` / `-q`\\");
        System.out.println("   Hides the header and footer.");
        System.out.println(" * `--debug` / `-X`\\");
        System.out.println("   Makes things loud for debugging.");
        System.out.println(" * `--offline` / `-o`\\");
        System.out.println("   Disables touching the network.");
        System.out.println("");
        System.out.println("## Environment Variables");
        System.out.println("");
        System.out.println(" * `MICROMVN_JAVA_HOME` / `JAVA_HOME`: JDK location for javac.\\");
        System.out.println("   If both are specified, `MICROMVN_JAVA_HOME` is preferred.\\");
        System.out.println("   If neither are specified, `java.home` will be used as a base.\\");
        System.out.println("   The `jre` directory will be stripped.\\");
        System.out.println("   If a tool cannot be found this way, it will be used from PATH.");
        System.out.println("");
        System.out.println("## Java System Properties");
        System.out.println("");
        System.out.println("* `user.home`: `.m2` directory is placed here.");
        System.out.println("* `maven.repo.local`: Maven repository is placed here (defaults to `${user.home}/.m2/repository`)");
        System.out.println("* `repoUrl`: Overrides the default remote repository.");
        System.out.println("");
        System.out.println("## Compiler Properties");
        System.out.println("");
        System.out.println("Compiler properties are inherited from Java properties and then overridden by POM.");
        System.out.println("");
        System.out.println(" * `project.build.sourceEncoding`\\");
        System.out.println("   Source file encoding (defaults to UTF-8)");
        System.out.println(" * `maven.compiler.source`\\");
        System.out.println("   Source version (`javac -source`)");
        System.out.println(" * `maven.compiler.target`\\");
        System.out.println("   Target version (`javac -target`)");
        System.out.println(" * `maven.compiler.executable`\\");
        System.out.println("   `javac` used for the build.\\");
        System.out.println("   This completely overrides the javac detected using `MICROMVN_JAVA_HOME` / `JAVA_HOME` / `java.home`.");
        System.out.println("");
        System.out.println("## POM Support");
        System.out.println("");
        System.out.println("The POM support here is pretty bare-bones. Inheritance support in particular is flakey.");
        System.out.println("");
        System.out.println("POM interpolation is supported, though the inheritance model isn't exact.\\");
        System.out.println("`env.` properties are supported, and the following *specific* `project.` properties:");
        System.out.println("");
        System.out.println("* `project.groupId`");
        System.out.println("* `project.artifactId`");
        System.out.println("* `project.version`");
        System.out.println("");
        System.out.println("Java System Properties are supported (but might have the wrong priority) and `<properties>` is supported.\\");
        System.out.println("No other properties are supported.");
        System.out.println("");
        System.out.println("To prevent breakage with non-critical parts of complex POMs, unknown properties aren't interpolated.");
        System.out.println("");
        System.out.println("micromvn makes a distinction between *source POMs* and *repo POMs.*\\");
        System.out.println("Source POMs are the root POM (where micromvn is run) or any POM findable via a `<module>` or `<relativePath>` chain.\\");
        System.out.println("Source POM code is *always* passed to javac via `-sourcepath`.\\");
        System.out.println("Repo POMs live in the local repo as usual and their JARs are passed to javac via `-classpath`.");
        System.out.println("");
        System.out.println("These exact POM elements are supported:");
        System.out.println("");
        System.out.println("* `project.groupId/artifactId/version`\\");
        System.out.println("  Project artifact coordinate.");
        System.out.println("* `project.parent.groupId/artifactId/version/relativePath`\\");
        System.out.println("  Parent project.");
        System.out.println("* `project.packaging`\\");
        System.out.println("  Sets the project's packaging type.");
        System.out.println("* `project.properties.*`\\");
        System.out.println("  Properties.");
        System.out.println("* `project.repositories.repository.url`\\");
        System.out.println("  Adds a custom repository.");
        System.out.println("* `project.dependencies.dependency.optional/scope/groupId/artifactId/version/relativePath`\\");
        System.out.println("  Dependency. `compile` and `provided` are supported (need to check if this acts correct w/ assembly).\\");
        System.out.println("  As per Maven docs, optional dependencies only 'count' when compiling the project directly depending on them.");
        System.out.println("* `project.modules.module`\\");
        System.out.println("  Adds a module that will be compiled with this project.");
        System.out.println("* `project.build.plugins.plugin.(...).manifest.mainClass` (where the plugin's `artifactId` is `maven-assembly-plugin`)\\");
        System.out.println("  Project's main class.");
        System.out.println("");
        System.out.println("## Quirks");
        System.out.println("");
        System.out.println("* The main hazard is a lack of real plugins or compile-time source generation.");
        System.out.println("* `maven-assembly-plugin` is very partially emulated and always runs during package.");
        System.out.println("* Manifest embedding support is weird. Single-JAR builds prioritize user-supplied manifests, while assembly builds always use a supplied manifest.");
        System.out.println("* All projects have a `jar-with-dependencies` build during the package phase.");
        System.out.println("* It is a known quirk/?feature? that it is possible to cause a POM to be referenced, but not built, and micromvn will attempt to package it.");
        System.out.println("* As far as micromvn is concerned, classifiers and the version/baseVersion distinction don't exist. A package is either POM-only or single-JAR.");
        System.out.println("");
        System.out.println("If any of these things are a problem, you probably should not use micromvn.");
    }

    public static void doBuild(int level) throws Exception {
        umvn rootPom = loadPOM(new File("pom.xml"), true);
        boolean doClean = level >= 0;
        boolean doCompile = level >= 1;
        boolean doPackage = level >= 2;
        boolean doInstall = level >= 3;

        // Set of all POMs to clean/compile.
        HashSet<umvn> compileThese = new HashSet<>();
        rootPom.addFullCompileSet(compileThese);

        if (doCompile || doPackage) {
            // This is used to make sure we have all necessary files for the rest of the build.
            HashSet<umvn> allCompile = new HashSet<>();
            HashSet<umvn> allPackage = new HashSet<>();
            for (umvn subPom : compileThese) {
                // System.out.println(subPom.triple);
                if (doCompile)
                    subPom.addFullClasspathSet(allCompile, false, true);
                if (doPackage)
                    subPom.addFullClasspathSet(allPackage, true, true);
            }

            // Join those together...
            HashSet<umvn> gatherStep = new HashSet<>();
            gatherStep.addAll(allCompile);
            gatherStep.addAll(allPackage);

            for (umvn u : gatherStep)
                u.completeDownload();
        }
        if (doClean) {
            for (umvn target : compileThese)
                target.clean();
        }
        if (doCompile) {
            boolean compileError = false;
            for (umvn target : compileThese)
                compileError |= !target.compile();
            if (compileError)
                System.exit(1);
        }
        if (doPackage) {
            for (umvn target : compileThese)
                target.packageJAR();
            for (umvn target : compileThese)
                target.packageJARWithDependencies();
        }
        if (doInstall) {
            for (umvn target : compileThese)
                target.install();
        }
        if (LOG_HEADER_FOOTER)
            System.err.println(compileThese.size() + " units processed.");
    }
}
