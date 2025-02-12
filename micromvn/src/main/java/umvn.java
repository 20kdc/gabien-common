/*
 * See doCopying function for disclaimer of copyright.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    public static final String VERSION = "CommitIDHere";
    public static final String URL = "https://github.com/20kdc/gabien-common/tree/master/micromvn";

    // -- switches --
    public static boolean LOG_HEADER_FOOTER = true;
    public static boolean LOG_DOWNLOAD = true;
    public static boolean LOG_ACTIVITY = true;
    public static boolean LOG_DEBUG = false;
    public static boolean OFFLINE = false;

    /**
     * So this works kind of as a semaphore in regards to maximum independent javac processes.
     */
    public static AtomicInteger JAVAC_PROCESSES = new AtomicInteger(Runtime.getRuntime().availableProcessors());

    /**
     * Command-line properties take precedence over everything else.
     */
    public static HashMap<String, String> CMDLINE_PROPS = new HashMap<>();

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

    public final String groupId, artifactId, version, coordsGA, triple;

    public final umvn parent;

    public final LinkedList<umvn> modules = new LinkedList<>();
    public final HashMap<String, String> properties = new HashMap<>();

    public static final int DEPSET_ROOT_MAIN_COMPILE = 0;
    public static final int DEPSET_ROOT_MAIN_PACKAGE = 1;
    public static final int DEPSET_ROOT_MAIN_RUNTIME = 2;
    public static final int DEPSET_TDEP_MAIN_COMPILE = 3;
    public static final int DEPSET_TDEP_MAIN_PACKAGE = 4;
    public static final int DEPSET_TDEP_MAIN_RUNTIME = 5;
    // test = (main-compile | main-runtime)
    public static final int DEPSET_ROOT_TEST = 6;
    // test-root = (test | scope[test])
    public static final int DEPSET_TDEP_TEST = 7;
    public static final int DEPSET_COUNT = 8;
    public static final int[] DEPSET_TRANSITIVE = {
            // root-main-compile
            DEPSET_TDEP_MAIN_COMPILE,
            // root-main-package
            DEPSET_TDEP_MAIN_PACKAGE,
            // root-main-runtime
            DEPSET_TDEP_MAIN_RUNTIME,
            // tdep-main-compile
            DEPSET_TDEP_MAIN_COMPILE,
            // tdep-main-package
            DEPSET_TDEP_MAIN_PACKAGE,
            // tdep-main-runtime
            DEPSET_TDEP_MAIN_RUNTIME,
            // test
            DEPSET_TDEP_TEST,
            // test-root
            DEPSET_TDEP_TEST
    };

    // Each of these sets (HashSet<String>) can be used by itself (i.e. it is never necessary to OR the sets).
    public final Object[] depSets = new Object[DEPSET_COUNT];

    // Maps a coordsGA to a triple.
    // This represents the entirety of this package's contribution to version-space.
    public final HashMap<String, String> coordsGAToTriples = new HashMap<>();

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
            // -- <properties> (even version can be derived from this & it doesn't template anything, so it must be first) --
            Element elm = findElement(projectElement, "properties", false);
            if (elm != null) {
                for (Node n : nodeChildrenArray(elm.getChildNodes())) {
                    if (n instanceof Element) {
                        properties.put(n.getNodeName(), n.getTextContent());
                    }
                }
            }
            // -- Parent must happen next so that we resolve related modules early and so we know our triple --
            elm = findElement(projectElement, "parent", false);
            umvn theParent = null;
            String theGroupId = null;
            String theArtifactId = null;
            String theVersion = null;
            if (elm != null) {
                String parentGA = getGAPOMRef(elm, sourceDir);
                String parentTriple = coordsGAToTriples.get(parentGA);
                if (parentTriple == null)
                    throw new RuntimeException("Parent project " + parentGA + " with no version (can't bootstrap from nothing)");
                theParent = getPOMByTriple(parentTriple);
                theGroupId = theParent.groupId;
                theArtifactId = theParent.artifactId;
                theVersion = theParent.version;
                for (Map.Entry<String, String> prop : theParent.properties.entrySet())
                    properties.putIfAbsent(prop.getKey(), prop.getValue());
                coordsGAToTriples.putAll(theParent.coordsGAToTriples);
            }
            groupId = ensureSpecifierHelper(theGroupId, "groupId", projectElement);
            artifactId = ensureSpecifierHelper(theArtifactId, "artifactId", projectElement);
            version = ensureSpecifierHelper(theVersion, "version", projectElement);
            coordsGA = groupId + ":" + artifactId;
            triple = getTriple(groupId, artifactId, version);
            parent = theParent;
            POM_BY_TRIPLE.put(triple, this);
            if (LOG_DEBUG)
                System.err.println(" = " + getTriple(groupId, artifactId, version));
            // -- <packaging> --
            String packaging = templateFindElement(projectElement, "packaging", "jar");
            if (packaging.equals("pom")) {
                isPOMPackaged = true;
            } else {
                // extended packaging types seem to be a Thing ("bundle")
                // let's assume anything not "pom" is "jar"
                isPOMPackaged = false;
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
            // -- depsets --
            for (int i = 0; i < DEPSET_COUNT; i++)
                depSets[i] = new HashSet<String>();
            // -- <dependencies> --
            elm = findElement(projectElement, "dependencies", false);
            if (elm != null) {
                for (Node n : nodeChildrenArray(elm.getChildNodes())) {
                    if (n instanceof Element && n.getNodeName().equals("dependency")) {
                        Node optionalNode = findElement(n, "optional", false);
                        boolean optional = false;
                        if (optionalNode != null && optionalNode.getTextContent().equals("true")) {
                            optional = true;
                        }
                        Node scopeNode = findElement(n, "scope", false);
                        String scope = "compile";
                        if (scopeNode != null)
                            scope = scopeNode.getTextContent();
                        String dep = getGAPOMRef(n, sourceDir);
                        if (scope.equals("compile")) {
                            getDepSet(DEPSET_ROOT_MAIN_COMPILE).add(dep);
                            getDepSet(DEPSET_ROOT_MAIN_PACKAGE).add(dep);
                            getDepSet(DEPSET_ROOT_MAIN_RUNTIME).add(dep);
                            getDepSet(DEPSET_ROOT_TEST).add(dep);
                            if (!optional) {
                                getDepSet(DEPSET_TDEP_MAIN_COMPILE).add(dep);
                                getDepSet(DEPSET_TDEP_MAIN_PACKAGE).add(dep);
                                getDepSet(DEPSET_TDEP_MAIN_RUNTIME).add(dep);
                                getDepSet(DEPSET_TDEP_TEST).add(dep);
                            }
                        } else if (scope.equals("provided")) {
                            getDepSet(DEPSET_ROOT_MAIN_COMPILE).add(dep);
                            getDepSet(DEPSET_ROOT_MAIN_PACKAGE).add(dep);
                            getDepSet(DEPSET_ROOT_TEST).add(dep);
                            if (!optional) {
                                getDepSet(DEPSET_TDEP_MAIN_COMPILE).add(dep);
                                getDepSet(DEPSET_TDEP_MAIN_PACKAGE).add(dep);
                                getDepSet(DEPSET_TDEP_TEST).add(dep);
                            }
                        } else if (scope.equals("runtime")) {
                            getDepSet(DEPSET_ROOT_MAIN_PACKAGE).add(dep);
                            getDepSet(DEPSET_ROOT_MAIN_RUNTIME).add(dep);
                            getDepSet(DEPSET_ROOT_TEST).add(dep);
                            if (!optional) {
                                getDepSet(DEPSET_TDEP_MAIN_PACKAGE).add(dep);
                                getDepSet(DEPSET_TDEP_MAIN_RUNTIME).add(dep);
                                getDepSet(DEPSET_TDEP_TEST).add(dep);
                            }
                        } else if (scope.equals("test")) {
                            getDepSet(DEPSET_ROOT_TEST).add(dep);
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
                            String moduleName = template(this, n.getTextContent());
                            modules.add(loadPOM(new File(sourceDir, moduleName), true));
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
            // -- warnings --
            if (sourceDir != null && properties.containsKey("maven.compiler.executable") && !properties.containsKey("maven.compiler.fork")) {
                System.err.println("WARN: In " + triple + ", maven.compiler.executable specified without maven.compiler.fork!");
                System.err.println("      This is a footgun and may lead to incompatibility with Apache Maven.");
                System.err.println("      Apart from this warning, micromvn ignores maven.compiler.fork!");
                System.err.println("      To silence this warning, specify maven.compiler.fork (recommended: true)");
            }
        } catch (Exception e) {
            throw new RuntimeException("loading POM " + debugInfo, e);
        }
    }

    private String ensureSpecifierHelper(String old, String attr, Node base) {
        Node theNode = findElement(base, attr, old == null);
        if (theNode != null)
            return template(this, theNode.getTextContent());
        return old;
    }

    // -- POM management --

    /**
     * Loads a POM from a file.
     */
    public static umvn loadPOM(File f, boolean isSource) {
        byte[] pomFileBytes;
        try {
            f = f.getCanonicalFile();
            if (f.isDirectory())
                f = new File(f, "pom.xml");
            umvn res = POM_BY_FILE.get(f);
            if (res != null)
                return res;
            pomFileBytes = Files.readAllBytes(f.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + f, e);
        }
        return new umvn(pomFileBytes, f, isSource ? f.getParentFile() : null);
    }

    /**
     * Returns GA.
     * Enthusiastically loads relative paths to ensure we have them discovered for later.
     * Also contributes to this project's coordsGAToTriples map (which you should use to get a triple if you need it)
     */
    public String getGAPOMRef(Node ref, File relativePathDir) {
        String theGroupId = templateFindElement(ref, "groupId", true);
        String theArtifactId = templateFindElement(ref, "artifactId", true);
        String theVersion = templateFindElement(ref, "version", false);
        String coordsGA = theGroupId + ":" + theArtifactId;
        if (theVersion == null)
            return coordsGA;
        String triple = getTriple(theGroupId, theArtifactId, theVersion);
        coordsGAToTriples.put(coordsGA, triple);
        umvn possibleMatch = POM_BY_TRIPLE.get(triple);
        if (possibleMatch == null && relativePathDir != null) {
            String relPathNode = templateFindElement(ref, "relativePath", false);
            if (relPathNode != null)
                loadPOM(new File(relativePathDir, relPathNode), true);
        }
        return coordsGA;
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

    public static final int SRCGROUP_MAIN = 0;
    public static final int SRCGROUP_TEST = 1;

    /**
     * If this is a source POM, gets the source directory.
     */
    public File getSourceJavaDir(int group) {
        if (group == SRCGROUP_MAIN)
            return new File(sourceDir, "src/main/java");
        if (group == SRCGROUP_TEST)
            return new File(sourceDir, "src/test/java");
        throw new RuntimeException("unknown srcgroup");
    }

    /**
     * If this is a source POM, gets the source directory.
     */
    public File getSourceResourcesDir(int group) {
        if (group == SRCGROUP_MAIN)
            return new File(sourceDir, "src/main/resources");
        if (group == SRCGROUP_TEST)
            return new File(sourceDir, "src/test/resources");
        throw new RuntimeException("unknown srcgroup");
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
    public File getSourceTargetClassesDir(int group) {
        if (group == SRCGROUP_MAIN)
            return new File(getSourceTargetDir(), "classes");
        if (group == SRCGROUP_TEST)
            return new File(getSourceTargetDir(), "test-classes");
        throw new RuntimeException("unknown srcgroup");
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
    public void aggregateSet(HashSet<umvn> compileThese) {
        if (compileThese.add(this))
            for (umvn s : modules)
                s.aggregateSet(compileThese);
    }

    /**
     * Gets a direct dependency set.
     */
    @SuppressWarnings("unchecked")
    public HashSet<String> getDepSet(int set) {
        return (HashSet<String>) depSets[set];
    }

    /**
     * Returns a fully resolved dependency set from coordGA to POM objects.
     * You should typically pass DEPSET_ROOT_* here.
     */
    public Map<String, umvn> resolveAllDependencies(int set) {
        // This algorithm exists because of protobuf and protobuf-parent.
        // This alerted me to the fact that micromvn didn't actually handle version resolution at all.
        // In order to fix things and provide the most compatible experience possible,
        //  this implements a queue-based breadth-first traversal.
        // However, if a dependency version is not directly specified nearby, it may sometimes be necessary to peek at another project.
        // That's where deferral comes in.
        // The algorithm is as follows:
        // There is an integration queue and the GA pool.
        // Everything is handled pass-by-pass.
        // Assuming all POMs are immediately available when they are integrated:
        //  The integration queue & splitting into passes ensures a breadth-first ordering.
        //  The GA pool is immediately cleared with each iteration.
        // Where this isn't possible, whatever can be resolved is and the rest is left for later.
        // The hope is that a version reference will be discovered in another part of the tree.
        // If the loop stalls, dependency resolution has failed.

        if (LOG_DEBUG)
            System.err.println("Resolving dependencies for " + triple + " {");

        // coordsGAToTriples tables are pulled into here.
        HashMap<String, String> resTableSeen = new HashMap<>();
        // The umvn objects actually referenced go here.
        // All objects here must also have entered queue.
        HashMap<String, umvn> resTableConfirmed = new HashMap<>();

        LinkedList<umvn> intQueue = new LinkedList<>();

        LinkedList<String> gaPool = new LinkedList<>();
        HashSet<String> enteredGAPool = new HashSet<>();

        intQueue.add(this);
        resTableConfirmed.put(coordsGA, this);

        while (true) {
            // Indicates forward progress.
            boolean activity = false;
            // integrate versions & pool GAs
            while (!intQueue.isEmpty()) {
                umvn queueEntry = intQueue.pop();
                for (Map.Entry<String, String> entries : queueEntry.coordsGAToTriples.entrySet()) {
                    if (resTableSeen.putIfAbsent(entries.getKey(), entries.getValue()) == null) {
                        if (LOG_DEBUG)
                            System.err.println(" " + entries.getKey() + " = " + entries.getValue());
                        activity = true;
                    }
                    int setAdjusted = queueEntry == this ? set : DEPSET_TRANSITIVE[set];
                    for (String dep : queueEntry.getDepSet(setAdjusted)) {
                        if (enteredGAPool.add(dep)) {
                            gaPool.add(dep);
                            activity = true;
                        }
                    }
                }
            }
            // GA pool
            for (String k : gaPool) {
                if (resTableConfirmed.containsKey(k))
                    continue;
                String triple = resTableSeen.get(k);
                if (triple == null)
                    continue;
                // we have a triple, add it and queue for integration
                umvn output = getPOMByTriple(triple);
                intQueue.add(output);
                resTableConfirmed.put(k, output);
                activity = true;
            }
            activity |= gaPool.removeIf((k) -> resTableConfirmed.containsKey(k));
            // if both the GA pool and the integration queue are empty, we're done
            if (gaPool.isEmpty() && intQueue.isEmpty())
                break;
            // if there's no activity and we can't leave then we've stalled
            if (!activity)
                throw new RuntimeException("could not resolve deps: " + gaPool);
            if (LOG_DEBUG)
                System.err.println("---");
        }
        if (LOG_DEBUG)
            System.err.println("}");
        return resTableConfirmed;
    }

    // -- Templating --

    /**
     * Gets a project/system/etc. property.
     */
    public static String getPropertyFull(umvn context, String basis) {
        /*
         * The execution order doesn't seem to be properly documented, so here's the gist:
         *
         * "-D" properties to Maven take precedence over project properties:
         *  `mvn clean ; mvn compile -Dmaven.compiler.executable=nope -Dmaven.compiler.fork=true`
         *  result: expected failure (nope is not a javac)
         *
         * Project properties take precedence over java system properties and environment variables:
         *  `PROPTEST=no mvn clean`
         *  result: `micromvn (proptest: Proptest ProptestEnv)`
         *  Proptest comes from a java.version override in the project
         *  ProptestEnv comes from a env.PROPTEST override in the project
         *  It seems reasonably clear from Maven's documentation about forced uppercase that Maven patches these into the system props
         *
         * Properties from the command-line are eagerly templated and non-existent properties result in the empty string:
         *  `mvn clean "-DversionProperty2=${versionProperty}XYZ"`
         *  result: XYZ
         *  `mvn clean "-DversionProperty2=${example}XYZ" -Dexample=moo`
         *  result: XYZ
         *
         */
        // hardcoded
        String pv;
        if (context != null) {
            if (basis.equals("project.artifactId"))
                return context.artifactId;
            if (basis.equals("project.groupId"))
                return context.groupId;
            if (basis.equals("project.version"))
                return context.version;
            pv = CMDLINE_PROPS.get(basis);
            if (pv != null)
                return pv;
            pv = context.properties.get(basis);
            if (pv != null)
                return template(context, pv);
        }
        pv = System.getProperty(basis);
        if (pv != null)
            return pv;
        return "";
    }

    /**
     * Templates property references. Needed because of hamcrest.
     */
    public static String template(umvn context, String text) {
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
            String propVal = getPropertyFull(context, prop);
            if (propVal != null) {
                res += propVal;
            } else {
                // failed to template
                if (context != null) {
                    System.err.println("WARN: " + context.sourceDir + " : Property " + prop + " doesn't exist");
                } else if (LOG_DEBUG) {
                    System.err.println("WARN: Property " + prop + " doesn't exist");
                }
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
        return template(this, n.getTextContent());
    }

    /**
     * Finds an element. If it fails, returns the default. Otherwise, templates its text contents.
     */
    public String templateFindElement(Node pomDoc, String string, String def) {
        Node n = findElement(pomDoc, string, false);
        if (n == null)
            return def;
        return template(this, n.getTextContent());
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
     * Begins compiling the project.
     * To parallelize the compile, all project compiles are started at once.
     * The completion code is stored in a Runnable.
     */
    public void beginCompile(int group, LinkedList<Runnable> queue, AtomicBoolean errorSignal) throws Exception {
        if (isPOMPackaged)
            return;

        String groupName;
        int depSet;
        if (group == SRCGROUP_MAIN) {
            groupName = "main";
            depSet = DEPSET_ROOT_MAIN_COMPILE;
        } else if (group == SRCGROUP_TEST) {
            groupName = "test";
            depSet = DEPSET_ROOT_TEST;
        } else {
            groupName = "unk" + group;
            depSet = DEPSET_ROOT_MAIN_COMPILE;
        }

        File classes = getSourceTargetClassesDir(group);
        File java = getSourceJavaDir(group);
        File resources = getSourceResourcesDir(group);
        classes.mkdirs();
        // compile classes
        HashSet<String> copy = new HashSet<>();
        buildListOfRelativePaths(java, "", copy);

        HashSet<File> classpath = new HashSet<>();
        HashSet<File> sourcepath = new HashSet<>();
        HashSet<umvn> classpathSet = new HashSet<>();

        classpathSet.addAll(resolveAllDependencies(depSet).values());
        for (umvn classpathEntry : classpathSet) {
            if (classpathEntry.sourceDir != null) {
                sourcepath.add(classpathEntry.getSourceJavaDir(SRCGROUP_MAIN));
            } else {
                classpath.add(getOrDownloadArtifact(classpathEntry, ".jar"));
            }
        }

        // explicitly re-add this; the test compile won't include us
        sourcepath.add(java);

        LinkedList<File> listForCurrentProcess = new LinkedList<>();
        for (String sourceFileName : copy) {
            if (!sourceFileName.endsWith(".java"))
                continue;
            listForCurrentProcess.add(new File(java, sourceFileName));
        }
        if (!listForCurrentProcess.isEmpty()) {
            ensureProcessSlotFree(queue);
            if (LOG_ACTIVITY)
                System.err.println("[compile start] " + triple + " " + groupName);
            runJavac(classes, listForCurrentProcess.toArray(new File[0]), classpath, sourcepath, queue, (v) -> {
                if (v != 0)
                    errorSignal.set(true);
                if (LOG_DEBUG)
                    System.err.println("[compile end  ] " + triple + " " + groupName);
            });
        } else if (LOG_DEBUG) {
            System.err.println("[compile empty] " + triple + " " + groupName);
        }

        queue.add(() -> {
            try {
                // copy resources
                copy.clear();
                buildListOfRelativePaths(resources, "", copy);
                for (String s : copy) {
                    File dstFile = new File(classes, s);
                    dstFile.getParentFile().mkdirs();
                    Files.copy(new File(resources, s).toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ex) {
                throw new RuntimeException("at tail of " + triple + " compile", ex);
            }
        });
    }

    /**
     * Begins testing the project.
     * Same idea as with compiles.
     */
    public void beginTest(LinkedList<Runnable> queue, AtomicBoolean errorSignal) throws Exception {
        if (isPOMPackaged)
            return;

        File classes = getSourceTargetClassesDir(SRCGROUP_TEST);

        // listclasses
        HashSet<String> copy = new HashSet<>();
        buildListOfRelativePaths(classes, "", copy);

        HashSet<String> listForCurrentProcess = new HashSet<>();
        byte[] expected = ("Lorg/junit/Test;").getBytes(StandardCharsets.UTF_8);
        for (String sourceFileName : copy) {
            if (!sourceFileName.endsWith(".class"))
                continue;
            if (sourceFileName.contains("$"))
                continue;
            // check if we can reasonably believe the class is a test
            byte[] data = Files.readAllBytes(new File(classes, sourceFileName).toPath());
            boolean classIsATest = false;
            for (int i = 0; i <= data.length - expected.length; i++) {
                boolean match = true;
                for (int j = 0; j < expected.length; j++) {
                    if (expected[j] != data[i + j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    classIsATest = true;
                    break;
                }
            }
            if (!classIsATest)
                continue;
            if (LOG_DEBUG)
                System.err.println(triple + " " + sourceFileName + " is a test");
            // alright, it looks like it
            listForCurrentProcess.add(sourceFileName.substring(0, sourceFileName.indexOf('.')).replace(File.separatorChar, '.'));
        }
        if (!listForCurrentProcess.isEmpty()) {
            ensureProcessSlotFree(queue);
            if (LOG_DEBUG)
                System.err.println("[test start] " + triple);
            LinkedList<String> argsFinal = new LinkedList<>();
            argsFinal.add("org.junit.runner.JUnitCore");
            argsFinal.addAll(listForCurrentProcess);
            runJava(argsFinal, queue, (v) -> {
                if (v != 0)
                    errorSignal.set(true);
                if (LOG_DEBUG)
                    System.err.println("[test end  ] " + triple);
            });
        }
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
    public Process runJavac(File dest, File[] s, HashSet<File> classpath, HashSet<File> sourcepath, LinkedList<Runnable> queue, Consumer<Integer> onEnd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        File responseFile = File.createTempFile("javac", ".rsp");
        PrintStream ps = new PrintStream(responseFile, "UTF-8");
        ps.println("-d");
        ps.println(dest.toString());
        ps.println("-implicit:none");
        // build classpath/sourcepath
        if (!sourcepath.isEmpty()) {
            ps.println("-sourcepath");
            ps.println(assembleClasspath(sourcepath));
        }
        if (!classpath.isEmpty()) {
            ps.println("-classpath");
            ps.println(assembleClasspath(classpath));
        }
        // continue
        ps.println("-encoding");
        ps.println(getPropertyFull(this, "project.build.sourceEncoding"));
        String sourceVer = getPropertyFull(this, "maven.compiler.source");
        if (!sourceVer.equals("")) {
            ps.println("-source");
            ps.println(sourceVer);
        }
        String targetVer = getPropertyFull(this, "maven.compiler.target");
        if (!targetVer.equals("")) {
            ps.println("-target");
            ps.println(targetVer);
        }
        for (File f : s)
            ps.println(f.toString());
        ps.close();
        responseFile.deleteOnExit();
        LinkedList<String> command = new LinkedList<>();
        command.add(getPropertyFull(this, "maven.compiler.executable"));
        command.add("@" + responseFile.getAbsolutePath());
        pb.command(command);
        return startProcess(pb, queue, onEnd);
    }

    /**
     * Gets the test runtime classpath as a string.
     */
    public String getTestRuntimeClasspath() {
        HashSet<File> classpath = new HashSet<>();

        for (umvn classpathEntry : resolveAllDependencies(DEPSET_ROOT_TEST).values()) {
            if (classpathEntry.sourceDir != null) {
                classpath.add(classpathEntry.getSourceTargetClassesDir(SRCGROUP_MAIN));
                classpath.add(classpathEntry.getSourceTargetClassesDir(SRCGROUP_TEST));
            } else {
                classpath.add(getOrDownloadArtifact(classpathEntry, ".jar"));
            }
        }

        return assembleClasspath(classpath);
    }

    /**
     * Runs java as configured for testing this project.
     */
    public Process runJava(Collection<String> args, LinkedList<Runnable> queue, Consumer<Integer> onEnd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(sourceDir);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        LinkedList<String> command = new LinkedList<>();
        command.add(getPropertyFull(this, "micromvn.java"));

        String classpath = getTestRuntimeClasspath();

        if (!classpath.isEmpty()) {
            command.add("-classpath");
            command.add(classpath);
        }

        command.addAll(args);
        pb.command(command);
        return startProcess(pb, queue, onEnd);
    }

    /**
     * Assembles a classpath/sourcepath from a set of File objects.
     */
    public static String assembleClasspath(Collection<File> files) {
        StringBuilder classpath = new StringBuilder();
        for (File f : files) {
            if (classpath.length() != 0)
                classpath.append(File.pathSeparatorChar);
            classpath.append(f);
        }
        return classpath.toString();
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
            File classes = getSourceTargetClassesDir(SRCGROUP_MAIN);
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
        myDeps.addAll(resolveAllDependencies(DEPSET_ROOT_MAIN_PACKAGE).values());
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
        String manifest = "Manifest-Version: 1.0\r\nCreated-By: microMVN " + VERSION + "\r\n";
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
            if (LOG_DOWNLOAD)
                System.err.println("download: " + urlString);
            java.net.URL url = new java.net.URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "microMVN " + VERSION);
            conn.connect();
            Files.copy(conn.getInputStream(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Thread.sleep(100);
            return true;
        } catch (Exception ex) {
            if (LOG_DOWNLOAD)
                System.err.println("download failed: " + urlString + " : " + ex);
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

    // -- Staging --

    /**
     * Executes all runnables in a queue.
     * Queues are used to try and provide 'good enough' parallelization for compiles/etc while keeping the code simple.
     * Notably, queues start executing earlier than runQueue!
     * This is because startProcessAndEnqueueWait may be out of process slots, so it needs the queue to catch up.
     */
    public static void runQueue(LinkedList<Runnable> list) {
        while (!list.isEmpty()) {
            Runnable first = list.pop();
            first.run();
        }
    }

    /**
     * Before announcing a process has been started, it is useful to ensure process end announcements have been seen.
     */
    public static void ensureProcessSlotFree(LinkedList<Runnable> queue) {
        while (JAVAC_PROCESSES.get() == 0) {
            queue.pop().run();
        }
    }

    /**
     * Starts a process.
     * Also enqueues a wait (to ensure that there will eventually be a process slot for another javac)
     */
    public static Process startProcess(ProcessBuilder pb, LinkedList<Runnable> queue, Consumer<Integer> onEnd) {
        ensureProcessSlotFree(queue);
        Process process;
        try {
            process = pb.start();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        JAVAC_PROCESSES.decrementAndGet();
        queue.add(() -> {
            try {
                int v = process.waitFor();
                JAVAC_PROCESSES.incrementAndGet();
                onEnd.accept(v);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        return process;
    }

    // -- Main --

    public static void main(String[] args) throws Exception {
        // patch in environment as if from JSP
        for (Map.Entry<String, String> env : System.getenv().entrySet())
            System.setProperty("env." + env.getKey().toUpperCase(Locale.ROOT), env.getValue());
        // autodetect javac
        if (System.getProperty("maven.compiler.executable") == null) {
            String java;
            String javac;
            String home = System.getenv("MICROMVN_JAVA_HOME");
            if (home == null)
                home = System.getenv("JAVA_HOME");
            String possiblyExe = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT).startsWith("windows") ? ".exe" : "";
            if (home != null) {
                if (home.endsWith(File.separator)) {
                    javac = home + "bin" + File.separator + "javac" + possiblyExe;
                    java = home + "bin" + File.separator + "java" + possiblyExe;
                } else {
                    javac = home + File.separator + "bin" + File.separator + "javac" + possiblyExe;
                    java = home + File.separator + "bin" + File.separator + "java" + possiblyExe;
                }
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
                expectedTool = new File(f, "bin" + File.separator + "java" + possiblyExe);
                // validate
                if (expectedTool.exists()) {
                    java = expectedTool.toString();
                } else {
                    // we could have a problem here. fall back to PATH
                    java = "java";
                }
            }
            // need to find a better property for this, but what?
            System.setProperty("micromvn.java", java);
            // this is expected at least
            System.setProperty("maven.compiler.executable", javac);
        }

        // autodetect local repo
        if (System.getProperty("maven.repo.local") == null)
            System.setProperty("maven.repo.local", new File(System.getProperty("user.home"), ".m2/repository").toString());

        // other defaults
        if (System.getProperty("project.build.sourceEncoding") == null)
            System.setProperty("project.build.sourceEncoding", "UTF-8");

        // arg parsing & init properties
        String goal = null;
        LinkedList<String> extraArgs = new LinkedList<>();

        File rootPOMFile = new File("pom.xml");

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
                    CMDLINE_PROPS.put(k, template(null, v));
                } else if (s.startsWith("-T")) {
                    String info = s.substring(2);
                    if (info.length() == 0)
                        info = args[++argIndex];
                    JAVAC_PROCESSES.set(Integer.parseInt(info));
                } else if (s.equals("--threads")) {
                    String info = args[++argIndex];
                    JAVAC_PROCESSES.set(Integer.parseInt(info));
                } else if (s.startsWith("-f")) {
                    String info = s.substring(2);
                    if (info.length() == 0)
                        info = args[++argIndex];
                    rootPOMFile = new File(info);
                } else if (s.equals("--file")) {
                    String info = args[++argIndex];
                    rootPOMFile = new File(info);
                } else if (s.equals("--version") || s.equals("-v")) {
                    System.out.println(doVersionInfo());
                    System.out.println("");
                    doCopying((line) -> System.out.println(line));
                    return;
                } else if (s.equals("--help") || s.equals("-h")) {
                    doHelp();
                    return;
                } else if (s.equals("--quiet") || s.equals("-q")) {
                    LOG_HEADER_FOOTER = false;
                    LOG_DOWNLOAD = false;
                    LOG_ACTIVITY = false;
                } else if (s.equals("--debug") || s.equals("-X")) {
                    LOG_DEBUG = true;
                } else if (s.equals("--offline") || s.equals("-o")) {
                    OFFLINE = true;
                } else {
                    throw new RuntimeException("Unknown option " + s);
                }
            } else {
                if (goal != null)
                    throw new RuntimeException("Can't specify multiple goals in microMVN.");
                int colon = s.lastIndexOf(':');
                if (colon == -1) {
                    goal = s;
                } else {
                    goal = s.substring(colon + 1);
                }
                if (goal.equals("umvn-run")) {
                    argIndex++;
                    while (argIndex < args.length)
                        extraArgs.add(args[argIndex++]);
                }
            }
        }
        if ((goal == null) || goal.equals("help")) {
            doHelp();
            return;
        }

        // handle properties

        installCorrectedRepoUrl(CMDLINE_PROPS.getOrDefault("repoUrl", "https://repo1.maven.org/maven2/"));

        // version

        if (LOG_HEADER_FOOTER)
            System.err.println(doVersionInfo());

        // goal

        if (goal.equals("clean")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doClean(buildAggregate);
            doFinalStatusOK(buildAggregate.size() + " projects cleaned.");
        } else if (goal.equals("compile")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doClean(buildAggregate);
            doGather(buildAggregate, true, false, false, false);
            doCompile(buildAggregate, false);
            doFinalStatusOK(buildAggregate.size() + " projects compiled.");
        } else if (goal.equals("test-compile")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doClean(buildAggregate);
            doGather(buildAggregate, true, false, true, false);
            doCompile(buildAggregate, true);
            doFinalStatusOK(buildAggregate.size() + " projects compiled with tests.");
        } else if (goal.equals("test")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doClean(buildAggregate);
            doGather(buildAggregate, true, false, true, true);
            doCompile(buildAggregate, true);
            doTest(buildAggregate);
            doFinalStatusOK(buildAggregate.size() + " projects tested.");
        } else if (goal.equals("test-only")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doGather(buildAggregate, false, false, false, true);
            doTest(buildAggregate);
            doFinalStatusOK(buildAggregate.size() + " projects tested.");
        } else if (goal.equals("package")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doClean(buildAggregate);
            doGather(buildAggregate, true, true, false, false);
            doCompile(buildAggregate, false);
            doPackageAndInstall(buildAggregate, true, false);
            doFinalStatusOK(buildAggregate.size() + " projects packaged.");
        } else if (goal.equals("package-only")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doGather(buildAggregate, false, true, false, false);
            doPackageAndInstall(buildAggregate, true, false);
            doFinalStatusOK(buildAggregate.size() + " projects packaged.");
        } else if (goal.equals("install")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doClean(buildAggregate);
            doGather(buildAggregate, true, true, false, false);
            doCompile(buildAggregate, false);
            doPackageAndInstall(buildAggregate, true, true);
            doFinalStatusOK(buildAggregate.size() + " projects installed to local repo.");
        } else if (goal.equals("install-only")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doGather(buildAggregate, false, true, false, false);
            doPackageAndInstall(buildAggregate, false, true);
            doFinalStatusOK(buildAggregate.size() + " projects installed to local repo.");
        } else if (goal.equals("test-install")) {
            HashSet<umvn> buildAggregate = doAggregate(rootPOMFile);
            doClean(buildAggregate);
            doGather(buildAggregate, true, true, true, true);
            doCompile(buildAggregate, true);
            doTest(buildAggregate);
            doPackageAndInstall(buildAggregate, true, true);
            doFinalStatusOK(buildAggregate.size() + " projects installed to local repo.");
        } else if (goal.equals("get")) {
            String prop = System.getProperty("artifact");
            if (prop == null)
                throw new RuntimeException("get requires -Dartifact=...");
            getPOMByTriple(prop).completeDownload();
            doFinalStatusOK("Installed.");
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
            doFinalStatusOK("Installed.");
        } else if (goal.equals("umvn-test-classpath")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            HashSet<umvn> onePom = new HashSet<umvn>();
            onePom.add(rootPom);
            doGather(onePom, false, false, false, true);
            System.out.println(rootPom.getTestRuntimeClasspath());
        } else if (goal.equals("umvn-run")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            HashSet<umvn> onePom = new HashSet<umvn>();
            onePom.add(rootPom);
            doGather(onePom, false, false, false, true);
            extraArgs.addFirst(rootPom.getTestRuntimeClasspath());
            extraArgs.addFirst("-classpath");
            extraArgs.addFirst(System.getProperty("micromvn.java"));

            ProcessBuilder pb = new ProcessBuilder();
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            pb.command(extraArgs);
            System.exit(pb.start().waitFor());
        } else if (goal.equals("umvn-make-scripts")) {
            File unixFile = new File("umvn");
            File windowsFile = new File("umvn.cmd");

            PrintStream unix = new PrintStream(unixFile, "UTF-8");
            unix.print("#!/bin/sh\n\n");
            doCopying((line) -> unix.print(("# " + line).trim() + "\n"));
            unix.print("\n");
            unix.print("SCRIPT_PATH=\"`readlink -e $0`\"\n");
            unix.print("SCRIPT_DIR=\"`dirname \"$SCRIPT_PATH\"`\"\n");
            unix.print("java -cp \"$SCRIPT_DIR\" umvn \"$@\"\n");
            unix.close();

            PrintStream windows = new PrintStream(windowsFile, "UTF-8");
            windows.print("@echo off\r\n\r\n");
            doCopying((line) -> windows.print(("rem " + line.replace("<", "").replace(">", "")).trim() + "\r\n"));
            windows.print("\r\n");
            windows.print("java -cp \"%~dp0\\\" umvn %*\r\n");
            windows.close();

            unixFile.setExecutable(true);
        } else {
            doFinalStatusError("Unsupported goal/phase: " + goal);
        }
    }

    public static String doVersionInfo() {
        return "microMVN " + VERSION + " " + URL + "\njava.version=" + System.getProperty("java.version") + " maven.compiler.executable=" + System.getProperty("maven.compiler.executable") + " maven.repo.local=" + System.getProperty("maven.repo.local");
    }

    public static void doHelp() {
        System.out.println("# microMVN: a Maven'-ish' builder in a single Java class");
        System.out.println("");
        System.out.println("microMVN is not Maven, it's not almost Maven, it's not a program which downloads Maven.\\");
        System.out.println("microMVN is a self-contained Java build tool small enough to be shipped with your projects, which acts enough like Maven for usecases that don't require full Maven, and doesn't add another installation step for new contributors.");
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
        System.out.println(" * `test-compile`\\");
        System.out.println("   Cleans and compiles all target projects along with their tests.");
        System.out.println(" * `test[-only]`\\");
        System.out.println("   Runs the JUnit 4 console runner, `org.junit.runner.JUnitCore`, on all tests in all target projects.\\");
        System.out.println("   Tests are assumed to be non-inner classes in the test source tree that appear to refer to `org.junit.Test`.\\");
        System.out.println("   `-only` suffix skips clean/compile.");
        System.out.println(" * `package[-only]`\\");
        System.out.println("   Cleans, compiles, and packages all target projects to JAR files.\\");
        System.out.println("   This also includes an imitation of maven-assembly-plugin.\\");
        System.out.println("   `-only` suffix skips clean/compile.");
        System.out.println(" * `install[-only]`\\");
        System.out.println("   Cleans, compiles, packages, and installs all target projects to the local Maven repo.\\");
        System.out.println("   `-only` suffix skips clean/compile/package.");
        System.out.println(" * `test-install`\\");
        System.out.println("   Cleans, compiles, tests, packages, and installs all target projects to the local Maven repo.");
        System.out.println(" * `dependency:get -Dartifact=<...>`\\");
        System.out.println("   Downloads a specific artifact to the local Maven repo.");
        System.out.println(" * `install:install-file -Dfile=<...> -DgroupId=<...> -DartifactId=<...> -Dversion=<...> -Dpackaging=<...>`\\");
        System.out.println("   Installs a JAR to the local Maven repo, creating a dummy POM for it.");
        System.out.println(" * `install:install-file -Dfile=<...> -DpomFile=<...>`\\");
        System.out.println("   Installs a JAR to the local Maven repo, importing an existing POM.");
        System.out.println(" * `help`\\");
        System.out.println("   Shows this text.");
        System.out.println(" * `umvn-test-classpath`\\");
        System.out.println("   Dumps the test classpath to standard output.");
        System.out.println(" * `umvn-run <...>`\\");
        System.out.println("   This goal causes all options after it to be instead passed to `java`.\\");
        System.out.println("   It runs `java`, similarly to how `test` works, setting up the test classpath for you.\\");
        System.out.println("   *It does not automatically run a clean/compile.*");
        System.out.println(" * `umvn-make-scripts <...>`\\");
        System.out.println("   Extracts scripts `umvn` and `umvn.class` to run the `umvn.class` file.");
        System.out.println("");
        System.out.println("## Options");
        System.out.println("");
        System.out.println(" * `-D <key>=<value>`\\");
        System.out.println("   Overrides a POM property. This is absolute and applies globally.");
        System.out.println(" * `-T <num>` / `--threads <num>`\\");
        System.out.println("   Sets the maximum number of `javac` processes to run at any given time.");
        System.out.println(" * `-f <pom>` / `--file <pom>`\\");
        System.out.println("   Sets the root POM file.");
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
        System.out.println("* `maven.repo.local`: Maven repository is placed here (defaults to `${user.home}/.m2/repository`)\\");
        System.out.println("  `-D` switches don't override this.");
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
        System.out.println(" * `micromvn.java`\\");
        System.out.println("   `java` used for executing tests/etc.\\");
        System.out.println("   This completely overrides the java detected using `MICROMVN_JAVA_HOME` / `JAVA_HOME` / `java.home`.");
        System.out.println("");
        System.out.println("## POM Support");
        System.out.println("");
        System.out.println("The POM support here is pretty bare-bones. Inheritance support in particular is flakey.");
        System.out.println("");
        System.out.println("POM interpolation is supported, though inheritance may be shaky.\\");
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
        System.out.println("microMVN makes a distinction between *source POMs* and *repo POMs.*\\");
        System.out.println("Source POMs are the root POM (where it's run) or any POM findable via a `<module>` or `<relativePath>` chain.\\");
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
        System.out.println("  Sets the project's packaging type.\\");
        System.out.println("  `pom` and `jar` are supported; unknown values resolve to `jar` (for compat. with, say, `bundle`).");
        System.out.println("* `project.properties.*`\\");
        System.out.println("  Properties.");
        System.out.println("* `project.repositories.repository.url`\\");
        System.out.println("  Adds a custom repository.");
        System.out.println("* `project.dependencies.dependency.optional/scope/groupId/artifactId/version/relativePath`\\");
        System.out.println("  Dependency. `compile`, `provided`, `runtime` and `test` are supported (need to check if this acts correct w/ assembly).\\");
        System.out.println("  As per Maven docs, optional dependencies only 'count' when compiling the project directly depending on them.");
        System.out.println("* `project.modules.module`\\");
        System.out.println("  Adds a module that will be compiled with this project.");
        System.out.println("* `project.build.plugins.plugin.(...).manifest.mainClass` (where the plugin's `artifactId` is `maven-assembly-plugin`)\\");
        System.out.println("  Project's main class.");
        System.out.println("");
        System.out.println("## Quirks");
        System.out.println("");
        System.out.println("* Maven dependency version resolution is messy. I *hope* I've gotten something in place that works now.");
        System.out.println("* The main hazard is a lack of real plugins or compile-time source generation.");
        System.out.println("* `maven-assembly-plugin` is very partially emulated and always runs during package.");
        System.out.println("* Manifest embedding support is weird. Single-JAR builds prioritize user-supplied manifests, while assembly builds always use a supplied manifest.");
        System.out.println("* All projects have a `jar-with-dependencies` build during the package phase.");
        System.out.println("* It is a known quirk/?feature? that it is possible to cause a POM to be referenced, but not built, and microMVN will attempt to package it.");
        System.out.println("* As far as microMVN is concerned, classifiers and the version/baseVersion distinction don't exist. A package is either POM-only or single-JAR.");
        System.out.println("* Testing only supports JUnit 4 and the way the test code discovers tests is awful.\\");
        System.out.println("  `umvn-test-classpath` and `umvn-run` exist as a 'good enough' workaround to attach your own runner.");
        System.out.println("* You don't need to explicitly skip tests. (This is an intentional difference.)");
        System.out.println("* Builds are *always* clean builds.");
        System.out.println("* Property precedence is hardcoded > command-line > POM > parent POM > env > Java System Properties > defaults. This is an attempt to match Maven behaviour.");
        System.out.println("");
        System.out.println("If any of these things are a problem, you probably should not use microMVN.");
    }

    public static void doCopying(Consumer<String> res) {
        res.accept("microMVN - Hyperportable Java 8 build tool");
        res.accept("");
        res.accept("Written starting in 2025 by:");
        res.accept(" 20kdc");
        res.accept("");
        res.accept("This is free and unencumbered software released into the public domain.");
        res.accept("");
        res.accept("Anyone is free to copy, modify, publish, use, compile, sell, or");
        res.accept("distribute this software, either in source code form or as a compiled");
        res.accept("binary, for any purpose, commercial or non-commercial, and by any");
        res.accept("means.");
        res.accept("");
        res.accept("In jurisdictions that recognize copyright laws, the author or authors");
        res.accept("of this software dedicate any and all copyright interest in the");
        res.accept("software to the public domain. We make this dedication for the benefit");
        res.accept("of the public at large and to the detriment of our heirs and");
        res.accept("successors. We intend this dedication to be an overt act of");
        res.accept("relinquishment in perpetuity of all present and future rights to this");
        res.accept("software under copyright law.");
        res.accept("");
        res.accept("THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND,");
        res.accept("EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF");
        res.accept("MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.");
        res.accept("IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR");
        res.accept("OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,");
        res.accept("ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR");
        res.accept("OTHER DEALINGS IN THE SOFTWARE.");
        res.accept("");
        res.accept("For more information, please refer to <http://unlicense.org>");
    }

    public static HashSet<umvn> doAggregate(File f) {
        umvn rootPom = loadPOM(f, true);
        HashSet<umvn> compileThese = new HashSet<>();
        rootPom.aggregateSet(compileThese);
        return compileThese;
    }

    public static void doGather(HashSet<umvn> packages, boolean doCompile, boolean doPackage, boolean doTestCompile, boolean doTestRuntime) {
        // This is used to make sure we have all necessary files for the rest of the build.
        HashSet<umvn> allCompile = new HashSet<>();
        HashSet<umvn> allPackage = new HashSet<>();
        HashSet<umvn> allTestCompile = new HashSet<>();
        HashSet<umvn> allTestRuntime = new HashSet<>();
        for (umvn subPom : packages) {
            // System.out.println(subPom.triple);
            if (doCompile)
                allCompile.addAll(subPom.resolveAllDependencies(DEPSET_ROOT_MAIN_COMPILE).values());
            if (doPackage)
                allCompile.addAll(subPom.resolveAllDependencies(DEPSET_ROOT_MAIN_PACKAGE).values());
            if (doTestCompile)
                allCompile.addAll(subPom.resolveAllDependencies(DEPSET_ROOT_TEST).values());
            if (doTestRuntime)
                allCompile.addAll(subPom.resolveAllDependencies(DEPSET_ROOT_TEST).values());
        }

        // Join those together...
        HashSet<umvn> gatherStep = new HashSet<>();
        gatherStep.addAll(allCompile);
        gatherStep.addAll(allPackage);
        gatherStep.addAll(allTestCompile);
        gatherStep.addAll(allTestRuntime);

        for (umvn u : gatherStep)
            u.completeDownload();
    }

    public static void doClean(HashSet<umvn> packages) {
        for (umvn target : packages)
            target.clean();
    }

    public static void doCompile(HashSet<umvn> packages, boolean tests) throws Exception {
        AtomicBoolean compileError = new AtomicBoolean(false);
        LinkedList<Runnable> completing = new LinkedList<>();
        for (umvn target : packages) {
            target.beginCompile(SRCGROUP_MAIN, completing, compileError);
            if (tests)
                target.beginCompile(SRCGROUP_TEST, completing, compileError);
        }
        runQueue(completing);
        if (compileError.get()) {
            doFinalStatusError("Compile failed");
            System.exit(1);
        }
    }

    public static void doTest(HashSet<umvn> packages) throws Exception {
        AtomicBoolean testError = new AtomicBoolean(false);
        LinkedList<Runnable> completing = new LinkedList<>();
        for (umvn target : packages) {
            target.beginTest(completing, testError);
        }
        runQueue(completing);
        if (testError.get()) {
            doFinalStatusError("Tests failed");
            System.exit(1);
        }
    }

    public static void doPackageAndInstall(HashSet<umvn> packages, boolean doPackage, boolean doInstall) throws Exception {
        if (doPackage) {
            for (umvn target : packages)
                target.packageJAR();
            for (umvn target : packages)
                target.packageJARWithDependencies();
        }
        if (doInstall) {
            for (umvn target : packages)
                target.install();
        }
        if (LOG_HEADER_FOOTER)
            System.err.println(packages.size() + " units processed.");
    }

    public static void doFinalStatusError(String err) {
        if (LOG_HEADER_FOOTER)
            System.err.println("[ERR] " + err);
        System.exit(1);
    }

    public static void doFinalStatusOK(String err) {
        if (LOG_HEADER_FOOTER)
            System.err.println("[OK] " + err);
        System.exit(0);
    }
}
