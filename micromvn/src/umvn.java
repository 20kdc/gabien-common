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
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
public final class umvn implements Comparable<umvn> {
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

    /**
     * Used for maven.build.timestamp!
     */
    public static final Date REFERENCE_BUILD_START_DATE = new Date();

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
     * Semi-internal to getOrDownloadArtifact.
     */
    public static HashMap<String, String> ARTIFACT_TO_REPO_CACHE = new HashMap<>();
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
    public final File sourceDir, sourceTargetDir;

    public final String groupId, artifactId, version, coordsGA, triple;

    public final umvn parent;

    /**
     * Total aggregate module set.
     */
    public final LinkedList<umvn> aggregate = new LinkedList<>();

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

    /**
     * Each of these sets can be used by itself (i.e. it is never necessary to OR the sets).
     * However, these are direct dependencies only. Each value is a coordGA. TreeSet is used for reproducibility.
     */
    @SuppressWarnings("unchecked")
    public final SortedSet<String>[] depSets = new SortedSet[DEPSET_COUNT];

    // Maps a coordsGA to a resolve function.
    // This represents the entirety of this package's contribution to version-space.
    public final HashMap<String, Supplier<umvn>> coordsGAResolvers = new HashMap<>();

    public boolean isPOMPackaged;

    public String mainClass;

    public final LinkedList<String> compilerArgs = new LinkedList<>();

    /**
     * These become "project.*" properties.
     */
    public static final String[] BOUND_PROPERTIES = {
            "groupId",
            "artifactId",
            "version",
            "packaging",
            "build.sourceEncoding",
            "build.sourceDirectory",
            "build.testSourceDirectory",
            "build.resources.resource.directory",
            "build.testResources.testResource.directory"
    };

    // -- Loader --

    private umvn(byte[] pomFileContent, File pomFileAssoc, File sourceDir) {
        this.pomFileContent = pomFileContent;
        this.sourceDir = sourceDir;
        this.sourceTargetDir = sourceDir != null ? new File(sourceDir, "target") : null;
        // do this immediately, even though we're invalid
        // this should help prevent infinite loops
        String debugInfo = pomFileAssoc != null ? pomFileAssoc.toString() : "[POM in memory]";
        if (pomFileAssoc != null)
            POM_BY_FILE.put(pomFileAssoc, this);
        if (LOG_DEBUG)
            System.err.println(debugInfo);
        try {
            Document pomDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(pomFileContent));
            Element projectElement = findElement(pomDoc, "project", true);
            // -- uninheritable defaults --
            properties.put("project.packaging", "jar");
            // -- <properties> (even version can be derived from this & it doesn't template anything, so it must be first) --
            Element elm = findElement(projectElement, "properties", false);
            if (elm != null)
                for (Node n : nodeListArray(elm.getChildNodes()))
                    if (n instanceof Element)
                        properties.put(n.getNodeName(), n.getTextContent());
            // -- POM binding --
            for (String property : BOUND_PROPERTIES)
                autoAttachProperty(projectElement, "project", property);
            // -- Parent must happen next so that we resolve related modules early and so we know our triple --
            elm = findElement(projectElement, "parent", false);
            if (elm != null) {
                String parentGA = getGAPOMRef(elm, sourceDir);
                Supplier<umvn> parentResolver = coordsGAResolvers.get(parentGA);
                if (parentResolver == null)
                    throw new RuntimeException("Parent project " + parentGA + " with no version (can't bootstrap from nothing)");
                parent = parentResolver.get();
                foldMaps(properties, parent.properties, (k) -> !k.equals("project.artifactId"));
                foldMaps(coordsGAResolvers, parent.coordsGAResolvers, (k) -> true);
            } else {
                parent = null;
            }
            // -- Inheritable defaults --
            properties.putIfAbsent("project.build.sourceEncoding", "UTF-8");
            properties.putIfAbsent("project.build.sourceDirectory", "${basedir}/src/main/java");
            properties.putIfAbsent("project.build.testSourceDirectory", "${basedir}/src/test/java");
            properties.putIfAbsent("project.build.resources.resource.directory", "${basedir}/src/main/resources");
            properties.putIfAbsent("project.build.testResources.testResource.directory", "${basedir}/src/test/resources");
            // -- Triple setup --
            groupId = getPropertyFull(this, "project.groupId");
            artifactId = getPropertyFull(this, "project.artifactId");
            version = getPropertyFull(this, "project.version");
            coordsGA = groupId + ":" + artifactId;
            triple = getTriple(groupId, artifactId, version);
            if (groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty())
                throw new RuntimeException("Missing key property: " + triple);
            isPOMPackaged = getPropertyFull(this, "project.packaging").equals("pom");
            POM_BY_TRIPLE.put(triple, this);
            if (LOG_DEBUG)
                System.err.println(" = " + triple);
            // -- <repositories> --
            elm = findElement(projectElement, "repositories", false);
            if (elm != null) {
                for (Node n : nodeListArray(elm.getChildNodes())) {
                    if (n instanceof Element && n.getNodeName().equals("repository")) {
                        String url = templateFindElement(n, "url", null);
                        if (url != null)
                            installCorrectedRepoUrl(url);
                    }
                }
            }
            // -- depsets --
            for (int i = 0; i < DEPSET_COUNT; i++)
                depSets[i] = new TreeSet<String>();
            // -- <dependencies> --
            elm = findElement(projectElement, "dependencies", false);
            if (elm != null) {
                for (Node n : nodeListArray(elm.getChildNodes())) {
                    if (n instanceof Element && n.getNodeName().equals("dependency")) {
                        boolean optional = templateFindElement(n, "optional", "false").equals("true");
                        String scope = templateFindElement(n, "scope", "compile");
                        String dep = getGAPOMRef(n, sourceDir);
                        if (scope.equals("compile")) {
                            depSets[DEPSET_ROOT_MAIN_COMPILE].add(dep);
                            depSets[DEPSET_ROOT_MAIN_PACKAGE].add(dep);
                            depSets[DEPSET_ROOT_MAIN_RUNTIME].add(dep);
                            depSets[DEPSET_ROOT_TEST].add(dep);
                            if (!optional) {
                                depSets[DEPSET_TDEP_MAIN_COMPILE].add(dep);
                                depSets[DEPSET_TDEP_MAIN_PACKAGE].add(dep);
                                depSets[DEPSET_TDEP_MAIN_RUNTIME].add(dep);
                                depSets[DEPSET_TDEP_TEST].add(dep);
                            }
                        } else if (scope.equals("provided")) {
                            depSets[DEPSET_ROOT_MAIN_COMPILE].add(dep);
                            // provided is NOT packaged (ref: gabien-android)
                            depSets[DEPSET_ROOT_TEST].add(dep);
                            if (!optional) {
                                depSets[DEPSET_TDEP_MAIN_COMPILE].add(dep);
                                depSets[DEPSET_TDEP_TEST].add(dep);
                            }
                        } else if (scope.equals("runtime")) {
                            depSets[DEPSET_ROOT_MAIN_PACKAGE].add(dep);
                            depSets[DEPSET_ROOT_MAIN_RUNTIME].add(dep);
                            depSets[DEPSET_ROOT_TEST].add(dep);
                            if (!optional) {
                                depSets[DEPSET_TDEP_MAIN_PACKAGE].add(dep);
                                depSets[DEPSET_TDEP_MAIN_RUNTIME].add(dep);
                                depSets[DEPSET_TDEP_TEST].add(dep);
                            }
                        } else if (scope.equals("test")) {
                            depSets[DEPSET_ROOT_TEST].add(dep);
                        } else if (scope.equals("import")) {
                            Supplier<umvn> importResolver = coordsGAResolvers.get(dep);
                            if (importResolver == null)
                                throw new RuntimeException("Dep import project " + dep + " with no version");
                            umvn importPOM = importResolver.get();
                            for (int i = 0 ; i < DEPSET_COUNT; i++)
                                depSets[i].addAll(importPOM.depSets[i]);
                            foldMaps(coordsGAResolvers, importPOM.coordsGAResolvers, (k) -> true);
                        }
                        // other scopes = not relevant
                    }
                }
            }
            // -- <modules> --
            aggregate.add(this);
            elm = findElement(projectElement, "modules", false);
            if (sourceDir != null && elm != null)
                for (Node n : nodeListArray(elm.getElementsByTagName("module")))
                    for (umvn module : loadPOM(new File(sourceDir, template(this, n.getTextContent())), true).aggregate)
                        if (!aggregate.contains(module))
                            aggregate.add(module);
            // -- <build> --
            Element buildElm = findElement(projectElement, "build", false);
            if (buildElm != null) {
                elm = findElement(buildElm, "plugins", false);
                if (elm != null) {
                    for (Node plugin : nodeListArray(elm.getChildNodes())) {
                        if (plugin instanceof Element && plugin.getNodeName().equals("plugin")) {
                            String pluginId = templateFindElement(plugin, "artifactId", "unknown-dont-care");
                            if (LOG_DEBUG)
                                System.err.println(" plugin: " + pluginId);
                            if (pluginId.equals("maven-assembly-plugin") || pluginId.equals("maven-jar-plugin")) {
                                Element a1 = findElementRecursive(plugin, "manifest", false);
                                if (a1 != null)
                                    this.mainClass = templateFindElement(a1, "mainClass", null);
                            } else if (pluginId.equals("maven-compiler-plugin")) {
                                Element ca = findElementRecursive(plugin, "compilerArgs", false);
                                if (ca != null)
                                    for (Node c2 : nodeListArray(ca.getElementsByTagName("arg")))
                                        compilerArgs.add(template(this, c2.getTextContent()));
                            }
                        }
                    }
                }
            }
            // -- warnings --
            if (sourceDir != null && properties.containsKey("maven.compiler.executable") && !properties.containsKey("maven.compiler.fork")) {
                System.err.println("WARN: In " + triple + ", maven.compiler.executable specified without maven.compiler.fork!");
                System.err.println("      This is a footgun and may lead to incompatibility with Apache Maven, as it ignores maven.compiler.executable when maven.compiler.fork is false.");
            }
        } catch (Exception e) {
            throw new RuntimeException("loading POM " + debugInfo, e);
        }
    }

    public static <K, V> boolean foldMaps(Map<K, V> to, Map<K, V> from, Predicate<K> keyFilter) {
        boolean activity = false;
        for (Map.Entry<K, V> entry : from.entrySet())
            activity |= keyFilter.test(entry.getKey()) && to.putIfAbsent(entry.getKey(), entry.getValue()) == null;
        return activity;
    }

    /**
     * Implicitly auto-handles inheritance.
     */
    public void autoAttachProperty(Node base, String propPrefix, String path) {
        int dot = path.indexOf('.');
        String component;
        if (dot == -1) {
            component = path;
        } else {
            component = path.substring(0, dot);
            path = path.substring(dot + 1);
        }
        String propFull = propPrefix + "." + component;
        for (Node n : nodeListArray(base.getChildNodes())) {
            if (n instanceof Element && n.getNodeName().equals(component)) {
                if (dot == -1) {
                    properties.put(propFull, n.getTextContent());
                } else {
                    autoAttachProperty(n, propFull, path);
                }
            }
        }
    }

    /**
     * Returns GA.
     * Enthusiastically loads relative paths to ensure we have them discovered for later.
     * Also contributes to this project's coordsGAToTriples map (which you should use to get a triple if you need it)
     */
    public String getGAPOMRef(Node ref, File relativePathDir) {
        String theGroupId = templateFindRequiredElement(ref, "groupId");
        String theArtifactId = templateFindRequiredElement(ref, "artifactId");
        String theVersion = templateFindElement(ref, "version", null);
        String theRelativePath = templateFindElement(ref, "relativePath", null);
        String coordsGA = theGroupId + ":" + theArtifactId;
        if (theVersion == null || theVersion.equals("LATEST") || theVersion.equals("RELEASE"))
            return coordsGA;
        String theVersionFixed = theVersion.replace("[", "").replace("]", "").replace("(", "").replace(")", "").split(",")[0];
        coordsGAResolvers.put(coordsGA, () -> pomByCoordinates(theGroupId, theArtifactId, theVersionFixed, true));
        if ((pomByCoordinates(theGroupId, theArtifactId, theVersionFixed, false) == null) && relativePathDir != null && theRelativePath != null)
            loadPOM(new File(relativePathDir, theRelativePath), true);
        return coordsGA;
    }

    /**
     * Finds an element. If it fails, returns the default. Otherwise, templates its text contents.
     */
    public String templateFindElement(Node pomDoc, String string, String def) {
        Node n = findElement(pomDoc, string, false);
        return n == null ? def : template(this, n.getTextContent());
    }

    /**
     * templateFindElement but erroring on lack of element.
     */
    public String templateFindRequiredElement(Node pomDoc, String string) {
        return template(this, findElement(pomDoc, string, true).getTextContent());
    }

    public static Node[] nodeListArray(NodeList list) {
        Node[] nodes = new Node[list.getLength()];
        for (int i = 0; i < nodes.length; i++)
            nodes[i] = list.item(i);
        return nodes;
    }

    public static Element findElementRecursive(Node pomDoc, String string, boolean required) {
        for (Node n : nodeListArray(pomDoc.getChildNodes())) {
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
        for (Node n : nodeListArray(pomDoc.getChildNodes()))
            if (n instanceof Element)
                if (n.getNodeName().equals(string))
                    return (Element) n;
        if (required)
            throw new RuntimeException("Unable to find element <" + string + ">");
        return null;
    }

    // -- Sort/Compare/String --

    @Override
    public int hashCode() {
        return triple.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof umvn && triple.equals(((umvn) obj).triple);
    }

    @Override
    public int compareTo(umvn o) {
        return triple.compareTo(o.triple);
    }

    @Override
    public String toString() {
        return triple;
    }

    // -- POM management --

    public static final String SUFFIX_POM = ".pom";
    public static final String SUFFIX_JAR = ".jar";
    public static final String SUFFIX_ASM = "-jar-with-dependencies.jar";

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
     * Returns the POM from coordinates.
     */
    public static umvn pomByCoordinates(String groupId, String artifactId, String version, boolean load) {
        umvn trivial = POM_BY_TRIPLE.get(getTriple(groupId, artifactId, version));
        if (trivial != null || !load)
            return trivial;
        return loadPOM(getOrDownloadArtifact(groupId, artifactId, version, SUFFIX_POM), false);
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
    public static final String[] SRCGROUP_NAMES = {"main", "test"};
    public static final int[] SRCGROUP_COMPILE_DEPSETS = {DEPSET_ROOT_MAIN_COMPILE, DEPSET_ROOT_TEST};
    public static final String[] SRCGROUP_PROP_JAVA = {"project.build.sourceDirectory", "project.build.testSourceDirectory"};
    public static final String[] SRCGROUP_PROP_RES = {"project.build.resources.resource.directory", "project.build.testResources.testResource.directory"};
    public static final String[] SRCGROUP_CLASSDIR = {"classes", "test-classes"};

    public File getSourceRelativeOrAbsolutePath(String path) {
        File f = new File(path);
        if (f.isAbsolute())
            return f;
        return new File(sourceDir, path);
    }

    /**
     * If this is a source POM, gets the target classes directory.
     */
    public File getSourceTargetClassesDir(int group) {
        return new File(sourceTargetDir, SRCGROUP_CLASSDIR[group]);
    }

    /**
     * If this is a source POM, gets a target artifact.
     */
    public File getSourceTargetArtifact(String suffix) {
        return new File(sourceTargetDir, artifactId + "-" + version + suffix);
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
            System.err.println("Resolving dependencies for " + this + " {");

        // coordsGAResolvers tables are pulled into here.
        HashMap<String, Supplier<umvn>> resTableSeen = new HashMap<>();
        // The umvn objects actually referenced go here.
        // All objects here must also have entered queue.
        // This is a TreeMap for reproducibility.
        TreeMap<String, umvn> resTableConfirmed = new TreeMap<>();

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
                activity |= foldMaps(resTableSeen, queueEntry.coordsGAResolvers, (k) -> true);
                int setAdjusted = queueEntry == this ? set : DEPSET_TRANSITIVE[set];
                for (String dep : queueEntry.depSets[setAdjusted]) {
                    if (enteredGAPool.add(dep)) {
                        gaPool.add(dep);
                        activity = true;
                    }
                }
            }
            // GA pool
            for (String k : gaPool) {
                if (resTableConfirmed.containsKey(k))
                    continue;
                Supplier<umvn> resolver = resTableSeen.get(k);
                if (resolver == null)
                    continue;
                // we have a triple, add it and queue for integration
                umvn output = resolver.get();
                if (LOG_DEBUG)
                    System.err.println(" " + output);
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
         * https://maven.apache.org/ref/3-LATEST/maven-model-builder/index.html
         *
         * Some tests also:
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
         */
        String pv;
        if (basis.equals("build.timestamp") || basis.equals("maven.build.timestamp"))
            return new SimpleDateFormat(getPropertyFull(context, "maven.build.timestamp.format")).format(REFERENCE_BUILD_START_DATE);
        if (context != null) {
            // hardcoded
            if ((basis.equals("project.basedir") || basis.equals("basedir")) && context.sourceDir != null)
                return context.sourceDir.toString();
            // POM properties
            if (basis.startsWith("project.")) {
                pv = context.properties.get(basis);
                return pv != null ? template(context, pv) : "";
            }
        }
        // -D
        pv = CMDLINE_PROPS.get(basis);
        if (pv != null)
            return pv;
        pv = context != null ? context.properties.get(basis) : null;
        if (pv != null)
            return template(context, pv);
        pv = System.getProperty(basis);
        return pv != null ? pv : "";
    }

    /**
     * Templates property references. Needed because of hamcrest.
     */
    public static String template(umvn context, String text) {
        StringBuilder res = new StringBuilder();
        int at = 0;
        while (true) {
            int idx = text.indexOf("${", at);
            if (idx == -1)
                return res + text.substring(at);
            if (at != idx)
                res.append(text.substring(at, idx));
            int idx2 = text.indexOf('}', idx);
            if (idx2 == -1)
                throw new RuntimeException("Unclosed template @ " + text);
            res.append(getPropertyFull(context, text.substring(idx + 2, idx2)));
            at = idx2 + 1;
        }
    }

    // -- Build --

    /**
     * Begins compiling the project.
     * To parallelize the compile, all project compiles are started at once.
     * The completion code is stored in a Runnable.
     */
    public void beginCompile(int group, LinkedList<Runnable> queue, AtomicBoolean errorSignal) throws Exception {
        if (isPOMPackaged)
            return;

        File classes = getSourceTargetClassesDir(group);
        File java = getSourceRelativeOrAbsolutePath(getPropertyFull(this, SRCGROUP_PROP_JAVA[group]));
        File resources = getSourceRelativeOrAbsolutePath(getPropertyFull(this, SRCGROUP_PROP_RES[group]));
        classes.mkdirs();
        // compile classes
        TreeSet<String> copy = new TreeSet<>();
        buildListOfRelativePaths(java, "", copy);

        // Must be TreeSet for reproducibility
        TreeSet<File> classpath = new TreeSet<>();
        TreeSet<File> sourcepath = new TreeSet<>();

        for (umvn classpathEntry : resolveAllDependencies(SRCGROUP_COMPILE_DEPSETS[group]).values()) {
            if (classpathEntry.isPOMPackaged)
                continue;
            if (classpathEntry.sourceDir != null) {
                sourcepath.add(classpathEntry.getSourceRelativeOrAbsolutePath(getPropertyFull(classpathEntry, SRCGROUP_PROP_JAVA[SRCGROUP_MAIN])));
            } else {
                classpath.add(getOrDownloadArtifact(classpathEntry, SUFFIX_JAR));
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
        String groupName = SRCGROUP_NAMES[group];
        if (!listForCurrentProcess.isEmpty()) {
            runJavac(classes, listForCurrentProcess, classpath, sourcepath, queue, (v) -> {
                if (v != 0)
                    errorSignal.set(true);
                if (LOG_DEBUG)
                    System.err.println("[compile end  ] " + this + " " + groupName);
            });
            if (LOG_ACTIVITY)
                System.err.println("[compile start] " + this + " " + groupName);
        } else if (LOG_DEBUG) {
            System.err.println("[compile empty] " + this + " " + groupName);
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
                throw new RuntimeException("at tail of " + this + " compile", ex);
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
        TreeSet<String> copy = new TreeSet<>();
        buildListOfRelativePaths(classes, "", copy);

        TreeSet<String> listForCurrentProcess = new TreeSet<>();
        byte[] expected = getPropertyFull(this, "micromvn.testMarker").getBytes(StandardCharsets.UTF_8);
        byte[] buffer = new byte[expected.length];
        for (String sourceFileName : copy) {
            if (sourceFileName.contains("$") || !sourceFileName.endsWith(".class"))
                continue;
            // find test marker
            classIsTestLabel: try (FileInputStream fis = new FileInputStream(new File(classes, sourceFileName))) {
                fis.read(buffer, 0, buffer.length - 1);
                while (fis.read(buffer, buffer.length - 1, 1) > 0) {
                    testEqualityFail: {
                        for (int i = 0; i < buffer.length; i++)
                            if (buffer[i] != expected[i])
                                break testEqualityFail;
                        break classIsTestLabel;
                    }
                    System.arraycopy(buffer, 1, buffer, 0, buffer.length - 1);
                }
                continue;
            }
            if (LOG_DEBUG)
                System.err.println(this + " " + sourceFileName + " is a test");
            // alright, it looks like it
            listForCurrentProcess.add(sourceFileName.substring(0, sourceFileName.indexOf('.')).replace('/', '.'));
        }
        if (!listForCurrentProcess.isEmpty()) {
            LinkedList<String> argsFinal = new LinkedList<>();
            argsFinal.add(getPropertyFull(this, "micromvn.testMainClass"));
            argsFinal.addAll(listForCurrentProcess);
            runJava(argsFinal, queue, (v) -> {
                errorSignal.compareAndSet(false, v != 0);
                if (LOG_DEBUG)
                    System.err.println("[test end  ] " + this);
            });
            if (LOG_DEBUG)
                System.err.println("[test start] " + this);
        }
    }

    /**
     * Install this project.
     */
    public void install() throws Exception {
        installArtifact(SUFFIX_POM);
        if (!isPOMPackaged) {
            installArtifact(SUFFIX_JAR);
            installArtifact(SUFFIX_ASM);
        }
    }

    /**
     * Installs a specific artifact of this project.
     */
    public void installArtifact(String suffix) throws Exception {
        File inLocalRepo = new File(getLocalRepo(), getArtifactPath(groupId, artifactId, version, suffix));
        inLocalRepo.getParentFile().mkdirs();
        if (suffix.equals(SUFFIX_POM)) {
            Files.write(inLocalRepo.toPath(), pomFileContent);
        } else {
            Files.copy(getSourceTargetArtifact(suffix).toPath(), inLocalRepo.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Runs javac as configured for this project.
     */
    public Process runJavac(File dest, Iterable<File> s, Collection<File> classpath, Collection<File> sourcepath, LinkedList<Runnable> queue, Consumer<Integer> onEnd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        File responseFile = File.createTempFile("javac", ".rsp");
        LinkedList<String> args = new LinkedList<>();
        args.add("-d");
        args.add(dest.toString());
        args.add("-implicit:none");
        // build classpath/sourcepath
        if (!sourcepath.isEmpty()) {
            args.add("-sourcepath");
            args.add(assembleClasspath(sourcepath));
        }
        if (!classpath.isEmpty()) {
            args.add("-classpath");
            args.add(assembleClasspath(classpath));
        }
        // continue
        mirrorJavacArg(args, "-encoding", "project.build.sourceEncoding");
        mirrorJavacArg(args, "-source", "maven.compiler.source");
        mirrorJavacArg(args, "-target", "maven.compiler.target");
        mirrorJavacArg(args, "-release", "maven.compiler.release");
        mirrorJavacSwitch(args, null, "-nowarn", "maven.compiler.showWarnings");
        mirrorJavacSwitch(args, "-g", "-g:none", "maven.compiler.debug");
        mirrorJavacSwitch(args, "-parameters", null, "maven.compiler.parameters");
        mirrorJavacSwitch(args, "-verbose", null, "maven.compiler.verbose");
        mirrorJavacSwitch(args, "-deprecation", null, "maven.compiler.showDeprecation");
        args.addAll(compilerArgs);
        s.forEach((f) -> args.add(f.toString()));
        try (PrintStream ps = new PrintStream(responseFile, "UTF-8")) {
            args.forEach(ps::println);
        }
        responseFile.deleteOnExit();
        pb.command(getPropertyFull(this, "maven.compiler.executable"), "@" + responseFile.getAbsolutePath());
        return startProcess(pb, queue, onEnd);
    }

    private void mirrorJavacArg(LinkedList<String> ps, String arg, String prop) {
        String targetVer = getPropertyFull(this, prop);
        if (!targetVer.isEmpty()) {
            ps.add(arg);
            ps.add(targetVer);
        }
    }

    private void mirrorJavacSwitch(LinkedList<String> ps, String on, String off, String prop) {
        String selection = getPropertyFull(this, prop).equals("true") ? on : off;
        if (selection != null)
            ps.add(selection);
    }

    /**
     * Gets the test runtime classpath as a string.
     */
    public String getTestRuntimeClasspath() {
        TreeSet<File> classpath = new TreeSet<>();
        for (umvn classpathEntry : resolveAllDependencies(DEPSET_ROOT_TEST).values()) {
            if (classpathEntry.isPOMPackaged)
                continue;
            if (classpathEntry.sourceDir != null) {
                classpath.add(classpathEntry.getSourceTargetClassesDir(SRCGROUP_MAIN));
                classpath.add(classpathEntry.getSourceTargetClassesDir(SRCGROUP_TEST));
            } else {
                classpath.add(getOrDownloadArtifact(classpathEntry, SUFFIX_JAR));
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
        pb.inheritIO();
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
        return String.join(File.pathSeparator, files.stream().map(File::toString).collect(Collectors.toList()));
    }

    /**
     * Creates the base JAR.
     */
    public void packageJAR() {
        if (isPOMPackaged)
            return;
        TreeMap<String, Consumer<OutputStream>> zip = new TreeMap<>();
        integrateJAR(zip);
        zipBuild(getSourceTargetArtifact(SUFFIX_JAR), zip);
    }

    /**
     * Creates the base JAR 'in-memory', or integrates the existing base JAR for non-source POMs.
     * SortedMap for reproducibility.
     */
    public void integrateJAR(SortedMap<String, Consumer<OutputStream>> zip) {
        if (isPOMPackaged)
            return;
        if (sourceDir != null) {
            // note that the manifest can (deliberately) be overwritten!
            zip.put("META-INF/MANIFEST.MF", zipMakeFile(createManifest(false)));
            zip.put("META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml", zipMakeFile(pomFileContent));
            zip.put("META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties", zipMakeFile("groupId=" + groupId + "\nartifactId=" + artifactId + "\nversion=" + version + "\n"));
            TreeSet<String> files = new TreeSet<>();
            File classes = getSourceTargetClassesDir(SRCGROUP_MAIN);
            buildListOfRelativePaths(classes, "", files);
            zipIntegrateRelativePaths(zip, "", classes, files);
        } else {
            File myJAR = getOrDownloadArtifact(this, SUFFIX_JAR);
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
        TreeMap<String, Consumer<OutputStream>> zip = new TreeMap<>();
        TreeSet<umvn> myDeps = new TreeSet<>();
        myDeps.addAll(resolveAllDependencies(DEPSET_ROOT_MAIN_PACKAGE).values());
        myDeps.remove(this);
        for (umvn various : myDeps)
            various.integrateJAR(zip);
        // re-add self to ensure last
        integrateJAR(zip);
        zip.put("META-INF/MANIFEST.MF", zipMakeFile(createManifest(true)));
        // done
        zipBuild(getSourceTargetArtifact(SUFFIX_ASM), zip);
    }

    public String createManifest(boolean packaging) {
        String manifest = "Manifest-Version: 1.0\r\nCreated-By: microMVN " + VERSION + "\r\n";
        if (!packaging) {
            // for thin JARs, create a classpath which includes all 
            String classPath = "";
            for (umvn dep : resolveAllDependencies(DEPSET_ROOT_MAIN_RUNTIME).values())
                if (dep != this)
                    classPath += " " + dep.artifactId + "-" + dep.version + ".jar";
            if (!classPath.isEmpty())
                manifest += "Class-Path:" + classPath + "\r\n";
        }
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
        if (isPOMPackaged || sourceDir != null)
            return;
        getOrDownloadArtifact(this, SUFFIX_JAR);
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
            String triple = groupId + ":" + artifactId + ":" + version;
            // this is the ugly part where we have to go find it on the server
            String cachedRepo = ARTIFACT_TO_REPO_CACHE.get(triple);
            if (cachedRepo != null)
                if (download(localRepoFile, cachedRepo + artifactPath))
                    return localRepoFile;
            // did not match cache
            for (String repo : REPOSITORIES) {
                if (cachedRepo != null && repo.equals(cachedRepo))
                    continue;
                if (download(localRepoFile, repo + artifactPath)) {
                    ARTIFACT_TO_REPO_CACHE.put(triple, repo);
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
        return new File(getPropertyFull(null, "maven.repo.local"));
    }

    public static String getArtifactPath(String groupId, String artifactId, String version, String suffix) {
        return groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + suffix;
    }

    // -- IO Utilities --

    /**
     * Builds a list of relative paths.
     * For reproducibility, insists upon a SortedSet recipient.
     * Also always uses forward slashes (important for ZIP!)
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

    public static void zipIntegrateRelativePaths(Map<String, Consumer<OutputStream>> zip, String prefix, File dir, Collection<String> paths) {
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

    public static void zipBuild(File outputFile, TreeMap<String, Consumer<OutputStream>> files) {
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
     * Creates a dummy POM file for a file being installed by `install-file`, or for a new project.
     */
    public static void createDummyPOM(File target, String groupId, String artifactId, String version, String packaging, boolean newProject) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        sb.append("\t<modelVersion>4.0.0</modelVersion>\n");
        sb.append("\t<groupId>" + groupId + "</groupId>\n");
        sb.append("\t<artifactId>" + artifactId + "</artifactId>\n");
        sb.append("\t<version>" + version + "</version>\n");
        sb.append("\t<packaging>" + packaging + "</packaging>\n");
        if (newProject) {
            // Provide some semi-opinionated defaults.
            // This covers what I might use on a day-to-day basis.
            sb.append("\t<!-- <parent><groupId></groupId><artifactId></artifactId><version></version><scope></scope><relativePath></relativePath></parent> -->\n");
            sb.append("\t<properties>\n");
            sb.append("\t\t<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n");
            sb.append("\t\t<maven.compiler.source>" + getPropertyFull(null, "maven.compiler.source") + "</maven.compiler.source>\n");
            sb.append("\t\t<maven.compiler.target>" + getPropertyFull(null, "maven.compiler.target") + "</maven.compiler.target>\n");
            sb.append("\t\t<!-- <maven.compiler.executable>${env.JAVA_1_8_HOME}/bin/javac</maven.compiler.executable> -->\n");
            sb.append("\t\t<maven.compiler.fork>true</maven.compiler.fork>\n");
            sb.append("\t</properties>\n");
            if (packaging.equals("pom")) {
                sb.append("\t<modules>\n");
                sb.append("\t\t<!-- <module></module> -->\n");
                sb.append("\t</modules>\n");
            } else {
                sb.append("\t<dependencies>\n");
                sb.append("\t\t<!-- <dependency><groupId></groupId><artifactId></artifactId><version></version><scope></scope></dependency> -->\n");
                sb.append("\t</dependencies>\n");
                sb.append("\t<!-- JAR -->\n");
                sb.append("\t<build>\n");
                sb.append("\t\t<plugins>\n");
                sb.append("\t\t\t<plugin>\n");
                sb.append("\t\t\t\t<artifactId>maven-assembly-plugin</artifactId>\n");
                sb.append("\t\t\t\t<executions><execution>\n");
                sb.append("\t\t\t\t\t<phase>package</phase>\n");
                sb.append("\t\t\t\t\t<goals><goal>single</goal></goals>\n");
                sb.append("\t\t\t\t\t<configuration>\n");
                sb.append("\t\t\t\t\t\t<archive><manifest><mainClass>com.example.Main</mainClass></manifest></archive>\n");
                sb.append("\t\t\t\t\t\t<descriptorRefs><descriptorRef>jar-with-dependencies</descriptorRef></descriptorRefs>\n");
                sb.append("\t\t\t\t\t</configuration>\n");
                sb.append("\t\t\t\t</execution></executions>\n");
                sb.append("\t\t\t</plugin>\n");
                sb.append("\t\t</plugins>\n");
                sb.append("\t</build>\n");
            }
        }
        sb.append("</project>\n");
        Files.write(target.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    // -- Staging --

    /**
     * Executes all runnables in a queue.
     * Queues are used to try and provide 'good enough' parallelization for compiles/etc while keeping the code simple.
     * Notably, queues start executing earlier than runQueue!
     * This is because startProcessAndEnqueueWait may be out of process slots, so it needs the queue to catch up.
     */
    public static void runQueue(LinkedList<Runnable> list) {
        while (!list.isEmpty())
            list.pop().run();
    }

    /**
     * Starts a process.
     * Also enqueues a wait (to ensure that there will eventually be a process slot for another javac)
     */
    public static Process startProcess(ProcessBuilder pb, LinkedList<Runnable> queue, Consumer<Integer> onEnd) throws Exception {
        while (JAVAC_PROCESSES.get() == 0)
            queue.pop().run();
        Process process = pb.start();
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
            systemSetPropertyDef("env." + env.getKey().toUpperCase(Locale.ROOT), env.getValue());
        // autodetect javac
        String java;
        String javac;
        String home = System.getProperty("env.MICROMVN_JAVA_HOME", "");
        if (home.equals(""))
            home = System.getProperty("env.JAVA_HOME", "");
        String possiblyExe = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT).startsWith("windows") ? ".exe" : "";
        if (!home.equals("")) {
            if (!home.endsWith(File.separator))
                home += File.separator;
            javac = home + "bin" + File.separator + "javac" + possiblyExe;
            java = home + "bin" + File.separator + "java" + possiblyExe;
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
        systemSetPropertyDef("micromvn.java", java);
        systemSetPropertyDef("maven.compiler.executable", javac);

        // Default system properties
        systemSetPropertyDef("maven.repo.local", new File(System.getProperty("user.home"), ".m2/repository").toString());
        systemSetPropertyDef("maven.build.timestamp.format", "yyyy-MM-dd'T'HH:mm:ss'Z'");
        // https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html
        systemSetPropertyDef("maven.compiler.source", "1.8");
        systemSetPropertyDef("maven.compiler.target", "1.8");
        systemSetPropertyDef("maven.compiler.showWarnings", "true");
        systemSetPropertyDef("maven.compiler.debug", "true");
        systemSetPropertyDef("maven.compiler.parameters", "false");
        systemSetPropertyDef("maven.compiler.verbose", "false");
        systemSetPropertyDef("maven.compiler.showDeprecation", "false");
        systemSetPropertyDef("micromvn.testMainClass", "org.junit.runner.JUnitCore");
        systemSetPropertyDef("micromvn.testMarker", "Lorg/junit/Test;");

        // arg parsing & init properties
        String goal = null;
        LinkedList<String> extraArgs = new LinkedList<>();

        File rootPOMFile = new File("pom.xml");

        for (int argIndex = 0; argIndex < args.length; argIndex++) {
            String s = args[argIndex];
            if (s.startsWith("-")) {
                if (s.length() != 2 && !s.startsWith("--")) {
                    args[argIndex--] = s.substring(2);
                    s = s.substring(0, 2); // Try mvn -X--version
                }
                if (s.equals("-D") || s.equals("--define")) {
                    String info = args[++argIndex];
                    int infoSplitterIndex = info.indexOf('=');
                    if (infoSplitterIndex == -1) {
                        CMDLINE_PROPS.put(info, "true"); // mvn -Djava.version -v
                    } else {
                        CMDLINE_PROPS.put(info.substring(0, infoSplitterIndex), template(null, info.substring(infoSplitterIndex + 1)));
                    }
                } else if (s.equals("-T") || s.equals("--threads")) {
                    JAVAC_PROCESSES.set(Integer.parseInt(args[++argIndex]));
                } else if (s.startsWith("-f") || s.equals("--file")) {
                    rootPOMFile = new File(args[++argIndex]);
                } else if (s.equals("--version") || s.equals("-v")) {
                    System.out.println(doVersionInfo());
                    System.out.println("");
                    doCopying((line) -> System.out.println(line));
                    return;
                } else if (s.equals("--show-version") || s.equals("-V")) {
                    System.err.println(doVersionInfo());
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

        // goal

        if (goal.equals("clean")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doClean(rootPom.aggregate);
            doFinalStatusOK(rootPom.aggregate.size() + " projects cleaned.");
        } else if (goal.equals("compile")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doClean(rootPom.aggregate);
            doGather(rootPom.aggregate, DEPSET_ROOT_MAIN_COMPILE);
            doCompile(rootPom.aggregate, false);
            doFinalStatusOK(rootPom.aggregate.size() + " projects compiled.");
        } else if (goal.equals("test-compile")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doClean(rootPom.aggregate);
            doGather(rootPom.aggregate, DEPSET_ROOT_MAIN_COMPILE, DEPSET_ROOT_TEST);
            doCompile(rootPom.aggregate, true);
            doFinalStatusOK(rootPom.aggregate.size() + " projects compiled with tests.");
        } else if (goal.equals("test")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doClean(rootPom.aggregate);
            doGather(rootPom.aggregate, DEPSET_ROOT_MAIN_COMPILE, DEPSET_ROOT_TEST);
            doCompile(rootPom.aggregate, true);
            doTest(rootPom.aggregate);
            doFinalStatusOK(rootPom.aggregate.size() + " projects tested.");
        } else if (goal.equals("test-only")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doGather(rootPom.aggregate, DEPSET_ROOT_TEST);
            doTest(rootPom.aggregate);
            doFinalStatusOK(rootPom.aggregate.size() + " projects tested.");
        } else if (goal.equals("package")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doClean(rootPom.aggregate);
            doGather(rootPom.aggregate, DEPSET_ROOT_MAIN_COMPILE, DEPSET_ROOT_MAIN_PACKAGE);
            doCompile(rootPom.aggregate, false);
            doPackageAndInstall(rootPom.aggregate, true, false);
            doFinalStatusOK(rootPom.aggregate.size() + " projects packaged.");
        } else if (goal.equals("package-only")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doGather(rootPom.aggregate, DEPSET_ROOT_MAIN_PACKAGE);
            doPackageAndInstall(rootPom.aggregate, true, false);
            doFinalStatusOK(rootPom.aggregate.size() + " projects packaged.");
        } else if (goal.equals("install")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doClean(rootPom.aggregate);
            doGather(rootPom.aggregate, DEPSET_ROOT_MAIN_COMPILE, DEPSET_ROOT_MAIN_PACKAGE);
            doCompile(rootPom.aggregate, false);
            doPackageAndInstall(rootPom.aggregate, true, true);
            doFinalStatusOK(rootPom.aggregate.size() + " projects installed to local repo.");
        } else if (goal.equals("install-only")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doGather(rootPom.aggregate, DEPSET_ROOT_MAIN_PACKAGE);
            doPackageAndInstall(rootPom.aggregate, false, true);
            doFinalStatusOK(rootPom.aggregate.size() + " projects installed to local repo.");
        } else if (goal.equals("test-install")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            doClean(rootPom.aggregate);
            doGather(rootPom.aggregate, DEPSET_ROOT_MAIN_COMPILE, DEPSET_ROOT_MAIN_PACKAGE, DEPSET_ROOT_TEST);
            doCompile(rootPom.aggregate, true);
            doTest(rootPom.aggregate);
            doPackageAndInstall(rootPom.aggregate, true, true);
            doFinalStatusOK(rootPom.aggregate.size() + " projects installed to local repo.");
        } else if (goal.equals("get")) {
            String prop = getPropertyFull(null, "artifact");
            if (prop.isEmpty())
                throw new RuntimeException("get requires -Dartifact=...");
            String[] parts = prop.split(":");
            if (parts.length != 3)
                throw new RuntimeException("Invalid triple: " + prop);
            pomByCoordinates(parts[0], parts[1], parts[2], true).completeDownload();
            doFinalStatusOK("Installed.");
        } else if (goal.equals("install-file")) {
            String file = getPropertyFull(null, "file");
            String pomFile = getPropertyFull(null, "pomFile");
            String groupId = getPropertyFull(null, "groupId");
            String artifactId = getPropertyFull(null, "artifactId");
            String version = getPropertyFull(null, "version");
            String packaging = getPropertyFull(null, "packaging");
            if (file.isEmpty())
                throw new RuntimeException("install-file requires -Dfile=...");
            if (!pomFile.isEmpty()) {
                umvn resPom = loadPOM(new File(pomFile), false);
                groupId = resPom.groupId;
                artifactId = resPom.artifactId;
                version = resPom.version;
                packaging = resPom.isPOMPackaged ? "pom" : "jar";
            }
            if (groupId.isEmpty())
                throw new RuntimeException("install-file requires -DgroupId=... (or -DpomFile=...)");
            if (artifactId.isEmpty())
                throw new RuntimeException("install-file requires -DartifactId=... (or -DpomFile=...)");
            if (version.isEmpty())
                throw new RuntimeException("install-file requires -Dversion=... (or -DpomFile=...)");
            if (packaging.isEmpty())
                throw new RuntimeException("install-file requires -Dpackaging=... (or -DpomFile=...)");
            File outPOM = new File(getLocalRepo(), getArtifactPath(groupId, artifactId, version, SUFFIX_POM));
            File outJAR = new File(getLocalRepo(), getArtifactPath(groupId, artifactId, version, SUFFIX_JAR));
            if (!packaging.equals("pom"))
                Files.copy(new File(file).toPath(), outJAR.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (!pomFile.isEmpty()) {
                Files.copy(new File(pomFile).toPath(), outPOM.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                createDummyPOM(outPOM, groupId, artifactId, version, packaging, false);
            }
            doFinalStatusOK("Installed.");
        } else if (goal.equals("umvn-test-classpath")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            HashSet<umvn> onePom = new HashSet<umvn>();
            onePom.add(rootPom);
            doGather(onePom, DEPSET_ROOT_TEST);
            System.out.println(rootPom.getTestRuntimeClasspath());
        } else if (goal.equals("umvn-run")) {
            umvn rootPom = loadPOM(rootPOMFile, true);
            HashSet<umvn> onePom = new HashSet<umvn>();
            onePom.add(rootPom);
            doGather(onePom, DEPSET_ROOT_TEST);
            extraArgs.addFirst(rootPom.getTestRuntimeClasspath());
            extraArgs.addFirst("-classpath");
            extraArgs.addFirst(getPropertyFull(null, "micromvn.java"));

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

            doFinalStatusOK("Created scripts.");
        } else if (goal.equals("umvn-new-project")) {
            File f = new File("pom.xml");
            if (f.exists())
                doFinalStatusError("pom.xml already exists.");
            createDummyPOM(f, "com.example", "new-project", "1.0.0-SNAPSHOT", "jar", true);
            new File("src/main/java").mkdirs();
            new File("src/main/resources").mkdirs();
            doFinalStatusOK("Created new project POM file and source directories.");
        } else {
            doFinalStatusError("Unsupported goal/phase: " + goal);
        }
    }

    public static void systemSetPropertyDef(String prop, String value) {
        if (System.getProperty(prop) == null)
            System.setProperty(prop, value);
    }

    public static String doVersionInfo() {
        return "microMVN " + VERSION + " " + URL + "\njava.version=" + getPropertyFull(null, "java.version") + " maven.compiler.executable=" + getPropertyFull(null, "maven.compiler.executable") + " maven.repo.local=" + getPropertyFull(null, "maven.repo.local");
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
        System.out.println("* `clean`\\");
        System.out.println("  Cleans all target projects (deletes target directory).");
        System.out.println("* `compile`\\");
        System.out.println("  Cleans and compiles all target projects.");
        System.out.println("* `test-compile`\\");
        System.out.println("  Cleans and compiles all target projects along with their tests.");
        System.out.println("* `test[-only]`\\");
        System.out.println("  Runs tests in all target projects.\\");
        System.out.println("  Tests are assumed to be non-inner classes in the test source tree containing the value of `micromvn.testMarker`.\\");
        System.out.println("  Tests are run in each project using the value of `micromvn.testMainClass` for that project.\\");
        System.out.println("  `-only` suffix skips clean/compile.");
        System.out.println("* `package[-only]`\\");
        System.out.println("  Cleans, compiles, and packages all target projects to JAR files.\\");
        System.out.println("  This also includes an imitation of maven-assembly-plugin.\\");
        System.out.println("  `-only` suffix skips clean/compile.");
        System.out.println("* `install[-only]`\\");
        System.out.println("  Cleans, compiles, packages, and installs all target projects to the local Maven repo.\\");
        System.out.println("  `-only` suffix skips clean/compile/package.");
        System.out.println("* `test-install`\\");
        System.out.println("  Cleans, compiles, tests, packages, and installs all target projects to the local Maven repo.");
        System.out.println("* `dependency:get -Dartifact=<...>`\\");
        System.out.println("  Downloads a specific artifact to the local Maven repo.");
        System.out.println("* `install:install-file -Dfile=<...> -DgroupId=<...> -DartifactId=<...> -Dversion=<...> -Dpackaging=<...>`\\");
        System.out.println("  Installs a JAR to the local Maven repo, creating a dummy POM for it.");
        System.out.println("* `install:install-file -Dfile=<...> -DpomFile=<...>`\\");
        System.out.println("  Installs a JAR to the local Maven repo, importing an existing POM.");
        System.out.println("* `help`\\");
        System.out.println("  Shows this text.");
        System.out.println("* `umvn-test-classpath`\\");
        System.out.println("  Dumps the test classpath to standard output.");
        System.out.println("* `umvn-run <...>`\\");
        System.out.println("  This goal causes all options after it to be instead passed to `java`.\\");
        System.out.println("  It runs `java`, similarly to how `test` works, setting up the test classpath for you.\\");
        System.out.println("  *It does not automatically run a clean/compile.*");
        System.out.println("* `umvn-make-scripts <...>`\\");
        System.out.println("  Extracts scripts `umvn` and `umvn.class` to run the `umvn.class` file.");
        System.out.println("* `umvn-new-project`\\");
        System.out.println("  Creates a new pom.xml file if it does not already exist.");
        System.out.println("");
        System.out.println("## Options");
        System.out.println("");
        System.out.println("* `-D <key>=<value>` / `--define <key>=<value>`\\");
        System.out.println("  Overrides a POM property. This is absolute and applies globally.");
        System.out.println("* `-T <num>` / `--threads <num>`\\");
        System.out.println("  Sets the maximum number of `javac` processes to run at any given time.");
        System.out.println("* `-f <pom>` / `--file <pom>`\\");
        System.out.println("  Sets the root POM file.");
        System.out.println("* `--version` / `-v`\\");
        System.out.println("  Reports the version + some other info and exits.");
        System.out.println("* `--show-version` / `-V`\\");
        System.out.println("  Reports the version + some other info, continues.");
        System.out.println("* `--help` / `-h`\\");
        System.out.println("  Shows this help text.");
        System.out.println("* `--quiet` / `-q`\\");
        System.out.println("  Hides the header and footer.");
        System.out.println("* `--debug` / `-X`\\");
        System.out.println("  Makes things loud for debugging.");
        System.out.println("* `--offline` / `-o`\\");
        System.out.println("  Disables touching the network.");
        System.out.println("");
        System.out.println("## Environment Variables");
        System.out.println("");
        System.out.println("* `MICROMVN_JAVA_HOME` / `JAVA_HOME`: JDK location for javac.\\");
        System.out.println("  If both are specified, `MICROMVN_JAVA_HOME` is preferred.\\");
        System.out.println("  If neither are specified, `java.home` will be used as a base.\\");
        System.out.println("  The `jre` directory will be stripped.\\");
        System.out.println("  If a tool cannot be found this way, it will be used from PATH.");
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
        System.out.println("Compiler properties are inherited from Java properties (except `project.*`) and then overridden by POM or command-line.");
        System.out.println("");
        System.out.println("* `project.build.sourceEncoding`\\");
        System.out.println("  Source file encoding (defaults to UTF-8)");
        System.out.println("* `project.build.sourceDirectory` / `project.build.testSourceDirectory` / `build.resources.resource.directory` / `build.testResources.testResource.directory`\\");
        System.out.println("  Various source code directories.");
        System.out.println("* `maven.compiler.source` / `maven.compiler.target` / `maven.compiler.release`\\");
        System.out.println("  Source/Target/Release versions (`javac` `-source`/`-target`/`-release`) ; source & target default to 1.8");
        System.out.println("* `maven.compiler.showWarnings` / `maven.compiler.debug` / `maven.compiler.parameters` / `maven.compiler.verbose` / `maven.compiler.showDeprecation`\\");
        System.out.println("  `javac` `-nowarn` (inverted), `-g`, `-parameters`, `-verbose`, `-deprecation`.");
        System.out.println("* `maven.compiler.executable`\\");
        System.out.println("  `javac` used for the build.\\");
        System.out.println("  This completely overrides the javac detected using `MICROMVN_JAVA_HOME` / `JAVA_HOME` / `java.home`.");
        System.out.println("* `micromvn.java`\\");
        System.out.println("  `java` used for executing tests/etc.\\");
        System.out.println("  This completely overrides the java detected using `MICROMVN_JAVA_HOME` / `JAVA_HOME` / `java.home`.");
        System.out.println("* `micromvn.testMainClass` / `micromvn.testMarker`\\");
        System.out.println("  These default to `org.junit.runner.JUnitCore` (JUnit 4 console runner) and `Lorg/junit/Test;`. Changing them can be used to adapt micromvn's test logic to another test framework.\\");
        System.out.println("  If `micromvn.testMarker` is found in a class file in the test classes directory, that file is recognized as a test and is passed as an argument.");
        System.out.println("");
        System.out.println("## POM Support");
        System.out.println("");
        System.out.println("The POM support here is pretty bare-bones. Inheritance support in particular is flakey.");
        System.out.println("");
        System.out.println("POM interpolation is supported, though inheritance may be shaky.\\");
        System.out.println("The supported sources of properties are (in evaluation order):");
        System.out.println("");
        System.out.println("1. `project.*`, `basedir`, or `maven.build.timestamp`\\");
        System.out.println("   Only `project.basedir` and Compiler ");
        System.out.println("2. Command-line properties");
        System.out.println("3. Properties in `<properties>`");
        System.out.println("4. Java System properties");
        System.out.println("5. `env.*`");
        System.out.println("6. Fixed defaults for various properties");
        System.out.println("");
        System.out.println("Java System Properties are supported and `<properties>` is supported.\\");
        System.out.println("");
        System.out.println("No other properties are supported.");
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
        System.out.println("  Dependency. `compile`, `provided`, `runtime`, `test` and `import` are supported.\\");
        System.out.println("  As per Maven docs, optional dependencies only 'count' when compiling the project directly depending on them.");
        System.out.println("* `project.modules.module`\\");
        System.out.println("  Adds a module that will be compiled with this project.");
        System.out.println("* `project.build.plugins.plugin.(...).manifest.mainClass` (where the plugin's `artifactId` is `maven-assembly-plugin` / `maven-jar-plugin`)\\");
        System.out.println("  Project's main class.");
        System.out.println("* `project.build.plugins.plugin.(...).compilerArgs.arg` (where the plugin's `artifactId` is `maven-compiler-plugin`)\\");
        System.out.println("  Adds an arg to the compiler command-line. Can be useful to toggle lints. Not inherited right now.");
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
        System.out.println("* Testing is weird. See `micromvn.testMainClass`, `micromvn.testMarker`, `umvn-test-classpath` and `umvn-run`.");
        System.out.println("* You don't need to explicitly skip tests. (This is an intentional difference.)");
        System.out.println("* Compilation itself is always clean and never incremental.");
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

    public static void doGather(Collection<umvn> packages, int... depSets) {
        // This is used to make sure we have all necessary files for the rest of the build.
        HashSet<umvn> allGather = new HashSet<>();
        for (int depSet : depSets)
            for (umvn subPom : packages)
                allGather.addAll(subPom.resolveAllDependencies(depSet).values());
        for (umvn u : allGather)
            u.completeDownload();
    }

    public static void doClean(Collection<umvn> packages) {
        for (umvn target : packages)
            recursivelyDelete(target.sourceTargetDir);
    }

    public static void doCompile(Collection<umvn> packages, boolean tests) throws Exception {
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

    public static void doTest(Collection<umvn> packages) throws Exception {
        AtomicBoolean testError = new AtomicBoolean(false);
        LinkedList<Runnable> completing = new LinkedList<>();
        for (umvn target : packages)
            target.beginTest(completing, testError);
        runQueue(completing);
        if (testError.get()) {
            doFinalStatusError("Tests failed");
            System.exit(1);
        }
    }

    public static void doPackageAndInstall(Collection<umvn> packages, boolean doPackage, boolean doInstall) throws Exception {
        if (doPackage) {
            for (umvn target : packages)
                target.packageJAR();
            for (umvn target : packages)
                target.packageJARWithDependencies();
        }
        if (doInstall)
            for (umvn target : packages)
                target.install();
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
