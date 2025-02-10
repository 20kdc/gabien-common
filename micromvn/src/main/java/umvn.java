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
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class umvn {
    public static HashMap<File, umvn> PARSED_POM_FILES = new HashMap<>();
    public static HashMap<String, umvn> PARSED_POM_MODULES = new HashMap<>();
    public static LinkedList<String> REPOSITORIES = new LinkedList<>();

    /**
     * pom.xml / .pom file
     */
    public final File pomFile;

    public final String groupId, artifactId, version;

    public final umvn parent;

    public final LinkedList<umvn> modules = new LinkedList<>();
    public final HashMap<String, String> properties = new HashMap<>();

    public final LinkedList<umvn> dependenciesClasspath = new LinkedList<>();
    public final LinkedList<umvn> dependenciesPackaged = new LinkedList<>();

    public boolean isPOMPackaged;

    private umvn(File pom, boolean mayUseRelativePath) {
        System.err.print(pom);
        try {
            File relBase = mayUseRelativePath ? pom.getParentFile() : null;
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
                theParent = loadReferencePOM(elm, relBase);
                theGroupId = theParent.groupId;
                theArtifactId = theParent.artifactId;
                theVersion = theParent.version;
                properties.putAll(theParent.properties);
            }
            groupId = ensureSpecifierHelper(theGroupId, "groupId", projectElement);
            artifactId = ensureSpecifierHelper(theArtifactId, "artifactId", projectElement);
            version = ensureSpecifierHelper(theVersion, "version", projectElement);
            parent = theParent;
            PARSED_POM_FILES.put(pom, this);
            PARSED_POM_MODULES.put(getTriple(groupId, artifactId, version), this);
            System.err.println(" = " + getTriple(groupId, artifactId, version));
            // -- <packaging> --
            elm = findElement(projectElement, "packaging", false);
            if (elm != null) {
                String tc = elm.getTextContent();
                if (tc.equals("pom")) {
                    isPOMPackaged = true;
                } else if (tc.equals("jar")) {
                    isPOMPackaged = false;
                } else {
                    throw new RuntimeException("Unknown packaging type: " + tc);
                }
            }
            // -- <properties> --
            elm = findElement(projectElement, "properties", false);
            if (elm != null) {
                for (Node n : nodeChildrenArray(elm.getChildNodes())) {
                    if (n instanceof Element) {
                        // System.out.println(n.getNodeName() + "=" + n.getTextContent());
                        properties.put(n.getNodeName(), n.getTextContent());
                    }
                }
            }
            // -- <repositories> --
            elm = findElement(projectElement, "repositories", false);
            if (elm != null) {
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
                            umvn depPom = loadReferencePOM(n, relBase);
                            dependenciesClasspath.add(depPom);
                            dependenciesPackaged.add(depPom);
                        } else if (scope.equals("provided")) {
                            umvn depPom = loadReferencePOM(n, relBase);
                            dependenciesClasspath.add(depPom);
                        }
                        // other scopes = not relevant
                    }
                }
            }
            // -- <modules> --
            if (mayUseRelativePath) {
                elm = findElement(projectElement, "modules", false);
                if (elm != null) {
                    for (Node n : nodeChildrenArray(elm.getChildNodes())) {
                        if (n instanceof Element && n.getNodeName().equals("module")) {
                            String moduleName = n.getTextContent();
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

    public static umvn loadPOM(File f, boolean mayUseRelativePath) {
        f = f.getAbsoluteFile();
        umvn res = PARSED_POM_FILES.get(f);
        if (res != null)
            return res;
        res = new umvn(f, mayUseRelativePath);
        return res;
    }

    public String getPropertyFull(String basis) {
        String pv = properties.get(basis);
        if (pv == null)
            pv = System.getProperty(basis);
        if (pv == null)
            throw new RuntimeException("Unable to get property: " + basis);
        return pv;
    }

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

    public umvn loadReferencePOM(Node ref, File relativePathDir) {
        String theGroupId = template(findElement(ref, "groupId", true).getTextContent());
        String theArtifactId = template(findElement(ref, "artifactId", true).getTextContent());
        String theVersion = template(findElement(ref, "version", true).getTextContent());
        umvn possibleMatch = PARSED_POM_MODULES.get(getTriple(theGroupId, theArtifactId, theVersion));
        if (possibleMatch != null)
            return possibleMatch;
        if (relativePathDir != null) {
            Node relPathNode = findElement(ref, "relativePath", false);
            if (relPathNode != null)
                return loadPOM(relativePathDir, true);
        }
        // look for it locally
        File possiblePom = getLocalRepoArtifact(theGroupId, theArtifactId, theVersion, ".pom");
        if (possiblePom.exists())
            return loadPOM(possiblePom, false);
        // this is the ugly part where we'd have to go find it on the server
        throw new RuntimeException("NYI; wanted remote retrieve of " + getTriple(theGroupId, theArtifactId, theVersion));
    }

    public static String getTriple(String groupId, String artifactId, String version) {
        return groupId + "/" + artifactId + "/" + version;
    }

    public static void main(String[] args) {
        REPOSITORIES.add("https://repo1.maven.org/maven2/");
        String versionInfo = "micromvn java.version=" + System.getProperty("java.version") + " jdk=" + getJavaHome() + " repo=" + getLocalRepo();
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
        if (doGather) {
            //
        }
        if (doClean) {
            //
        }
        if (doCompile) {
            //
        }
        if (doPackage) {
            //
        }
        if (doInstall) {
            //
        }
    }

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

    public static File getJavaHome() {
        String home = System.getenv("JAVA_HOME");
        if (home != null)
            return new File(home);
        File f = new File(System.getProperty("java.home"));
        if (f.getName().equals("jre"))
            f = f.getParentFile();
        return f;
    }
    public static File getLocalRepo() {
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo == null)
            return new File(System.getProperty("user.home"), ".m2/repository");
        return new File(localRepo);
    }
    public static String getArtifactPath(String groupId, String artifactId, String version) {
        return groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version;
    }
    public static File getLocalRepoArtifact(String groupId, String artifactId, String version, String extension) {
        return new File(getLocalRepo(), getArtifactPath(groupId, artifactId, version) + extension);
    }
}
