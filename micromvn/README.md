# micromvn: a Maven'-ish' builder in a single Java class

micromvn is not Maven.\
micromvn is not almost Maven.\
micromvn is not a program which downloads Maven.\
micromvn is a self-contained Java build tool small enough to be shipped with your projects, which acts enough like Maven for usecases that don't require full Maven, and doesn't add another installation step for new contributors.

micromvn is intended to be shipped with your project, kind of like the Gradle wrapper; but much more reliable.\
Gradle wrappers require an internet connection to download Gradle and break all the time due to libraries that can't keep up with the JDK.\
micromvn requires only a Java 8 or newer JDK.\
I believe it supports enough to compile *reasonable* projects.\
The idea is that the project is Maven as far as the IDE is concerned and umvn as far as final build is concerned.

Usage: `java umvn [options] <goal> [options]`

If a goal contains a colon, the last colon and everything before it is discarded.

Goals are:

 * `clean`\
   Cleans all target projects (deletes target directory).
 * `compile`\
   Cleans and compiles all target projects.
 * `package`\
   Cleans, compiles, and packages all target projects to JAR files.
   This also includes an imitation of maven-assembly-plugin.
 * `install`\
   Cleans, compiles, packages, and installs all target projects to the local Maven repo.
 * `dependency:get -Dartifact=<...>`\
   Downloads a specific artifact to the local Maven repo.
 * `install:install-file -Dfile=<...> -DgroupId=<...> -DartifactId=<...> -Dversion=<...> -Dpackaging=<...>`\
   Installs a JAR to the local Maven repo, creating a dummy POM for it.
 * `install:install-file -Dfile=<...> -DpomFile=<...>`\
   Installs a JAR to the local Maven repo, importing an existing POM.
 * `help`\
   Shows this text.

## Options

 * `-D<key>=<value>`\
   Sets a Java System Property. These are inherited into the POM property space.
 * `--version` / `-v`\
   Reports the version + some other info and exits.
 * `--help` / `-h`\
   Shows this help text.
 * `--quiet` / `-q`\
   Hides the header and footer.
 * `--debug` / `-X`\
   Makes things loud for debugging.
 * `--offline` / `-o`\
   Disables touching the network.

## Environment Variables

 * `MICROMVN_JAVA_HOME` / `JAVA_HOME`: JDK location for javac.\
   If both are specified, `MICROMVN_JAVA_HOME` is preferred.\
   If neither are specified, `java.home` will be used as a base.\
   The `jre` directory will be stripped.\
   If a tool cannot be found this way, it will be used from PATH.

## Java System Properties

* `user.home`: `.m2` directory is placed here.
* `maven.repo.local`: Maven repository is placed here (defaults to `${user.home}/.m2/repository`)
* `repoUrl`: Overrides the default remote repository.

## Compiler Properties

Compiler properties are inherited from Java properties and then overridden by POM.

 * `project.build.sourceEncoding`\
   Source file encoding (defaults to UTF-8)
 * `maven.compiler.source`\
   Source version (`javac -source`)
 * `maven.compiler.target`\
   Target version (`javac -target`)
 * `maven.compiler.executable`\
   `javac` used for the build.\
   This completely overrides the javac detected using `MICROMVN_JAVA_HOME` / `JAVA_HOME` / `java.home`.

## POM Support

The POM support here is pretty bare-bones. Inheritance support in particular is flakey.

POM interpolation is supported, though the inheritance model isn't exact.\
`env.` properties are supported, and the following *specific* `project.` properties:

* `project.groupId`
* `project.artifactId`
* `project.version`

Java System Properties are supported (but might have the wrong priority) and `<properties>` is supported.\
No other properties are supported.

To prevent breakage with non-critical parts of complex POMs, unknown properties aren't interpolated.

micromvn makes a distinction between *source POMs* and *repo POMs.*\
Source POMs are the root POM (where micromvn is run) or any POM findable via a `<module>` or `<relativePath>` chain.\
Source POM code is *always* passed to javac via `-sourcepath`.\
Repo POMs live in the local repo as usual and their JARs are passed to javac via `-classpath`.

These exact POM elements are supported:

* `project.groupId/artifactId/version`\
  Project artifact coordinate.
* `project.parent.groupId/artifactId/version/relativePath`\
  Parent project.
* `project.packaging`\
  Sets the project's packaging type.
* `project.properties.*`\
  Properties.
* `project.repositories.repository.url`\
  Adds a custom repository.
* `project.dependencies.dependency.optional/scope/groupId/artifactId/version/relativePath`\
  Dependency. `compile` and `provided` are supported (need to check if this acts correct w/ assembly).\
  As per Maven docs, optional dependencies only 'count' when compiling the project directly depending on them.
* `project.modules.module`\
  Adds a module that will be compiled with this project.
* `project.build.plugins.plugin.(...).manifest.mainClass` (where the plugin's `artifactId` is `maven-assembly-plugin`)\
  Project's main class.

## Quirks

* The main hazard is a lack of real plugins or compile-time source generation.
* `maven-assembly-plugin` is very partially emulated and always runs during package.
* Manifest embedding support is weird. Single-JAR builds prioritize user-supplied manifests, while assembly builds always use a supplied manifest.
* All projects have a `jar-with-dependencies` build during the package phase.
* It is a known quirk/?feature? that it is possible to cause a POM to be referenced, but not built, and micromvn will attempt to package it.
* As far as micromvn is concerned, classifiers and the version/baseVersion distinction don't exist. A package is either POM-only or single-JAR.

If any of these things are a problem, you probably should not use micromvn.

## Toolchains

micromvn was originally created because Maven toolchain files are awkward to setup compared to one environment variable.

For the R48 project to continue to support legacy devices, it needs to be compiled on OpenJDK 8.\
However, for the R48 project to continue to be maintained, it needs to be easy to setup a development environment.\
Compiling in a manner supported by Android D8 requires an increasingly complicated series of workarounds.\
When compiling for Java 8, OpenJDK 21 creates class files not compatible with Android D8.\
But latest versions of Maven do not run on Java 8!
