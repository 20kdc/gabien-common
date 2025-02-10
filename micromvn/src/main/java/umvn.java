/*
 * umvn - Hyperportable Java 8 build tool
 *
 * Written starting in 2025 by:
 *  20kdc
 *
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

import java.io.File;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * microMVN: "Not quite Maven" in a single class file.
 * Created February 10th, 2025.
 */
public class umvn {
    // -- how-to-find this --
    public static final String URL = "https://github.com/20kdc/gabien-common/tree/master/micromvn";

    // -- switches --
    public static boolean LOG_DISCOVERY = false;

    // -- cache --
    public static HashMap<File, umvn> PARSED_POM_FILES = new HashMap<>();
    public static HashMap<String, umvn> PARSED_POM_MODULES = new HashMap<>();
    public static final LinkedList<String> REPOSITORIES = new LinkedList<>();

    // -- POM --
    /**
     * pom.xml / .pom file
     */
    public final File pomFile;

    /**
     * A "source" POM is a POM we should be compiling.
     * It must not be in the local repo.
     */
    public final boolean isSource;

    public final String groupId, artifactId, version, triple;

    public final umvn parent;

    public final LinkedList<umvn> modules = new LinkedList<>();
    public final HashMap<String, String> properties = new HashMap<>();

    public final LinkedList<String> depTriplesClasspath = new LinkedList<>();
    public final LinkedList<String> depTriplesPackaged = new LinkedList<>();

    public boolean isPOMPackaged;

    private umvn(File pom, boolean isSource) {
        this.isSource = isSource;
        if (LOG_DISCOVERY)
            System.err.print(pom);
        try {
            File relBase = isSource ? pom.getParentFile() : null;
            pomFile = pom;
            Document pomDoc = parseXML(pomFile);
            Element projectElement = findElement(pomDoc, "project", true);
            // -- Parent must happen first so that we resolve related modules early and so we know our triple --
            Element elm = findElement(projectElement, "parent", false);
            umvn theParent = null;
            String theGroupId = null;
            String theArtifactId = null;
            String theVersion = null;
            if (elm != null) {
                theParent = getPOMByTriple(getTriplePOMRef(elm, relBase));
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
            PARSED_POM_FILES.put(pom, this);
            PARSED_POM_MODULES.put(getTriple(groupId, artifactId, version), this);
            if (LOG_DISCOVERY)
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
                        if (!url.endsWith("/"))
                            url += "/";
                        if (!REPOSITORIES.contains(url))
                            REPOSITORIES.add(url);
                    }
                }
            }
            // -- <dependencies> --
            elm = findElement(projectElement, "dependencies", false);
            if (elm != null) {
                for (Node n : nodeChildrenArray(elm.getChildNodes())) {
                    if (n instanceof Element && n.getNodeName().equals("dependency")) {
                        Node optionalNode = findElement(n, "optional", false);
                        if (optionalNode != null && optionalNode.getTextContent().equals("true"))
                            continue;
                        Node scopeNode = findElement(n, "scope", false);
                        String scope = "compile";
                        if (scopeNode != null)
                            scope = scopeNode.getTextContent();
                        if (scope.equals("compile")) {
                            String dep = getTriplePOMRef(n, relBase);
                            depTriplesClasspath.add(dep);
                            depTriplesPackaged.add(dep);
                        } else if (scope.equals("provided")) {
                            String dep = getTriplePOMRef(n, relBase);
                            depTriplesClasspath.add(dep);
                        }
                        // other scopes = not relevant
                    }
                }
            }
            // -- <modules> --
            if (isSource) {
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
        } catch (Exception e) {
            throw new RuntimeException("loading POM " + pom, e);
        }
    }

    private static String ensureSpecifierHelper(String old, String attr, Node base) {
        Node theNode = findElement(base, attr, old == null);
        if (theNode != null)
            return theNode.getTextContent();
        return old;
    }

    // -- POM management --

    public static umvn loadPOM(File f, boolean mayUseRelativePath) {
        f = f.getAbsoluteFile();
        umvn res = PARSED_POM_FILES.get(f);
        if (res != null)
            return res;
        res = new umvn(f, mayUseRelativePath);
        return res;
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
        umvn possibleMatch = PARSED_POM_MODULES.get(triple);
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
        umvn trivial = PARSED_POM_MODULES.get(triple);
        if (trivial != null)
            return trivial;
        String[] parts = triple.split(":");
        if (parts.length != 3)
            throw new RuntimeException("Invalid triple: " + triple);
        return loadPOM(getOrDownloadArtifactPath(getArtifactPath(parts[0], parts[1], parts[2]) + ".pom"), false);
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
        return new File(pomFile.getParentFile(), "src/main/java");
    }

    /**
     * If this is a source POM, gets the source directory.
     */
    public File getSourceResourcesDir() {
        return new File(pomFile.getParentFile(), "src/main/resources");
    }

    /**
     * If this is a source POM, gets the target classes directory.
     */
    public File getSourceTargetDir() {
        return new File(pomFile.getParentFile(), "target");
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
    public void addFullClasspathSet(HashSet<umvn> compileThese, boolean packaging) {
        if (compileThese.add(this))
            for (String s : (packaging ? depTriplesPackaged : depTriplesClasspath))
                getPOMByTriple(s).addFullClasspathSet(compileThese, packaging);
    }

    // -- Templating --

    public String getPropertyFull(String basis) {
        String pv = properties.get(basis);
        if (pv == null)
            pv = System.getProperty(basis);
        if (pv == null)
            throw new RuntimeException("Unable to get property: " + basis);
        return pv;
    }

    public String getPropertyFull(String basis, String def) {
        String pv = properties.get(basis);
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
            res += getPropertyFull(prop);
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

    public static Document parseXML(File f) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            return dbf.newDocumentBuilder().parse(f);
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

    // -- Java --

    public static String getJavaTool(String tool) {
        String home = System.getenv("MICROMVN_JAVA_HOME");
        if (home == null)
            home = System.getenv("JAVA_HOME");
        String possiblyExe = System.getProperty("os.name", "unknown").toLowerCase().startsWith("windows") ? ".exe" : "";
        if (home != null)
            if (home.endsWith(File.separator))
                return home + "bin" + File.separator + tool + possiblyExe;
            else
                return home + File.separator + "bin" + File.separator + tool + possiblyExe;
        File f = new File(System.getProperty("java.home"));
        if (f.getName().equals("jre"))
            f = f.getParentFile();
        File expectedTool = new File(f, "bin" + File.separator + tool + possiblyExe);
        // validate
        if (expectedTool.exists())
            return expectedTool.toString();
        // we could have a problem here. fall back to PATH
        return tool;
    }

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
        Files.copy(pomFile.toPath(), new File(getLocalRepo(), getArtifactPath() + ".pom").toPath(), StandardCopyOption.REPLACE_EXISTING);
        if (!isPOMPackaged)
            Files.copy(getSourceTargetJARFile().toPath(), new File(getLocalRepo(), getArtifactPath() + ".jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
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
        command.add(getJavaTool("javac"));
        command.add("-d");
        command.add(getSourceTargetClassesDir().toString());
        // build classpath/sourcepath
        StringBuilder classpath = new StringBuilder();
        StringBuilder sourcepath = new StringBuilder();
        HashSet<umvn> classpathSet = new HashSet<>();
        addFullClasspathSet(classpathSet, false);
        for (umvn classpathEntry : classpathSet) {
            if (classpathEntry.isSource) {
                if (sourcepath.length() != 0)
                    sourcepath.append(File.pathSeparatorChar);
                sourcepath.append(classpathEntry.getSourceJavaDir().toString());
            } else {
                if (classpath.length() != 0)
                    classpath.append(File.pathSeparatorChar);
                String jarfile = getArtifactPath(classpathEntry.groupId, classpathEntry.artifactId, classpathEntry.version) + ".jar";
                classpath.append(getOrDownloadArtifactPath(jarfile).toString());
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

    // -- Repository Management --

    /**
     * Gets or downloads a file into the local repo by artifact path.
     */
    public static File getOrDownloadArtifactPath(String artifactPath) {
        // look for it locally
        File localRepoFile = new File(getLocalRepo(), artifactPath);
        if (localRepoFile.exists())
            return localRepoFile;
        localRepoFile.getParentFile().mkdirs();
        // this is the ugly part where we have to go find it on the server
        for (String repo : REPOSITORIES) {
            String urlString = repo + artifactPath;
            try {
                java.net.URL url = new java.net.URL(urlString);
                URLConnection conn = url.openConnection();
                conn.setRequestProperty("User-Agent", "micromvn");
                conn.connect();
                Files.copy(conn.getInputStream(), localRepoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Thread.sleep(100);
                return localRepoFile;
            } catch (Exception ex) {
                System.err.println("Failed download @ " + urlString + ": " + ex);
            }
        }
        throw new RuntimeException("Failed to retrieve " + artifactPath);
    }

    public static File getLocalRepo() {
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo == null)
            return new File(System.getProperty("user.home"), ".m2/repository");
        return new File(localRepo);
    }

    public String getArtifactPath() {
        return getArtifactPath(groupId, artifactId, version);
    }

    public static String getArtifactPath(String groupId, String artifactId, String version) {
        return groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version;
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

    // -- Main --

    public static void main(String[] args) throws Exception {
        REPOSITORIES.add("https://repo1.maven.org/maven2/");
        String versionInfo = "micromvn java.version=" + System.getProperty("java.version") + " javac=" + getJavaTool("javac") + " repo=" + getLocalRepo();
        LOG_DISCOVERY = System.getProperty("micromvn.logDiscovery", "false").equals("true");
        // continue
        LinkedList<String> goalsAndPhases = new LinkedList<>();
        for (String s : args) {
            if (s.startsWith("-")) {
                if (s.startsWith("-D")) {
                    String info = s.substring(2);
                    int infoSplitterIndex = info.indexOf('=');
                    if (infoSplitterIndex == -1)
                        throw new RuntimeException("define " + info + " invalid, no =");
                    String k = info.substring(0, infoSplitterIndex);
                    String v = info.substring(infoSplitterIndex + 1);
                    System.setProperty(k, v);
                } else if (s.equals("--version")) {
                    System.out.println(versionInfo);
                    return;
                } else {
                    throw new RuntimeException("Unknown option " + s);
                }
            } else {
                goalsAndPhases.add(s);
            }
        }
        System.err.println(versionInfo);
        umvn rootPom = loadPOM(new File("pom.xml"), true);
        boolean doGather = false;
        boolean doClean = false;
        boolean doCompile = false;
        boolean doPackage = false;
        boolean doInstall = false;
        for (String s : goalsAndPhases) {
            if (s.equals("clean")) {
                doClean = true;
            } else if (s.equals("compile")) {
                doGather = true;
                doClean = true;
                doCompile = true;
            } else if (s.equals("package")) {
                doGather = true;
                doClean = true;
                doCompile = true;
                doPackage = true;
            } else if (s.equals("install")) {
                doGather = true;
                doClean = true;
                doCompile = true;
                doPackage = true;
                doInstall = true;
            } else {
                throw new RuntimeException("Unsupported goal/phase: " + s);
            }
        }

        // Set of all POMs to clean/compile.
        HashSet<umvn> compileThese = new HashSet<>();
        rootPom.addFullCompileSet(compileThese);

        if (doGather) {
            // This is used to make sure we have all necessary files for the rest of the build.
            HashSet<umvn> allCompile = new HashSet<>();
            HashSet<umvn> allPackage = new HashSet<>();
            for (umvn subPom : compileThese) {
                // System.out.println(subPom.triple);
                if (doCompile)
                    subPom.addFullClasspathSet(allCompile, false);
                if (doPackage)
                    subPom.addFullClasspathSet(allPackage, true);
            }

            // Join those together...
            HashSet<umvn> gatherStep = new HashSet<>();
            gatherStep.addAll(allCompile);
            gatherStep.addAll(allPackage);

            for (umvn u : gatherStep)
                if (!(u.isSource || u.isPOMPackaged))
                    getOrDownloadArtifactPath(u.getArtifactPath() + ".jar");
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
            //
            System.out.println("NYI: Packaging");
        }
        if (doInstall) {
            for (umvn target : compileThese)
                target.install();
        }
    }
}
