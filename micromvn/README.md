# microMVN: a Maven'-ish' builder in a single Java class

microMVN is not Maven, it's not almost Maven, it's not a program which downloads Maven.\
microMVN is a self-contained Java build tool small enough to be shipped with your projects, which acts enough like Maven for usecases that don't require full Maven, and doesn't add another installation step for new contributors.

Usage: `java umvn [options] <goal> [options]`

If a goal contains a colon, the last colon and everything before it is discarded.

Goals are:

* `clean`\
  Cleans all target projects (deletes target directory).
* `compile`\
  Cleans and compiles all target projects.
* `test-compile`\
  Cleans and compiles all target projects along with their tests.
* `test[-only]`\
  Runs tests in all target projects.\
  Tests are assumed to be non-inner classes in the test source tree containing the value of `micromvn.testMarker`.\
  Tests are run in each project using the value of `micromvn.testMainClass` for that project.\
  `-only` suffix skips clean/compile.
* `package[-only]`\
  Cleans, compiles, and packages all target projects to JAR files.\
  This also includes an imitation of maven-assembly-plugin.\
  `-only` suffix skips clean/compile.
* `install[-only]`\
  Cleans, compiles, packages, and installs all target projects to the local Maven repo.\
  `-only` suffix skips clean/compile/package.
* `test-install`\
  Cleans, compiles, tests, packages, and installs all target projects to the local Maven repo.
* `dependency:get -Dartifact=<...>`\
  Downloads a specific artifact to the local Maven repo.
* `install:install-file -Dfile=<...>/-Durl=<...> -DgroupId=<...> -DartifactId=<...> -Dversion=<...> -Dpackaging=<...>`\
  Installs a JAR to the local Maven repo, creating a dummy POM for it. The JAR may be downloaded.
* `install:install-file -Dfile=<...>/-Durl=<...> -DpomFile=<...>`\
  Installs a JAR to the local Maven repo, importing an existing POM. The JAR may be downloaded.
* `help`\
  Shows this text.
* `umvn-test-classpath`\
  Dumps the test classpath to standard output.
* `umvn-run <...>`\
  This goal causes all options after it to be instead passed to `java`.\
  It runs `java`, similarly to how `test` works, setting up the test classpath for you.\
  *It does not automatically run a clean/compile.*
* `umvn-make-scripts <...>`\
  Extracts scripts `umvn` and `umvn.class` to run the `umvn.class` file.
* `umvn-new-project`\
  Creates a new pom.xml file if it does not already exist.

## Options

* `-D <key>=<value>` / `--define <key>=<value>`\
  Overrides a POM property. This is absolute and applies globally.
* `-T <num>` / `--threads <num>`\
  Sets the maximum number of `javac` processes to run at any given time.
* `-f <pom>` / `--file <pom>`\
  Sets the root POM file.
* `--version` / `-v`\
  Reports the version + some other info and exits.
* `--show-version` / `-V`\
  Reports the version + some other info, continues.
* `--help` / `-h`\
  Shows this help text.
* `--quiet` / `-q`\
  Hides the header and footer.
* `--debug` / `-X`\
  Makes things loud for debugging. javac response files are not cleaned up.
* `--offline` / `-o`\
  Disables touching the network.

## Environment Variables

* `MICROMVN_JAVA_HOME` / `JAVA_HOME`: JDK location for javac.\
  If both are specified, `MICROMVN_JAVA_HOME` is preferred.\
  If neither are specified, `java.home` will be used as a base.\
  The `jre` directory will be stripped.\
  If a tool cannot be found this way, it will be used from PATH.
* `MICROMVN_DEBUG` is another way to set `--debug`.
* `MICROMVN_OFFLINE` is another way to set `--offline`.

## Java System Properties

* `user.home`: `.m2` directory is placed here.
* `maven.repo.local`: Maven repository is placed here (defaults to `${user.home}/.m2/repository`)\
  `-D` switches don't override this.
* `repoUrl`: Overrides the default remote repository.

## Compiler Properties

Compiler properties are inherited from Java properties (except `project.*`) and then overridden by POM or command-line.

* `project.build.sourceEncoding`\
  Source file encoding (defaults to UTF-8)
* `project.build.sourceDirectory` / `project.build.testSourceDirectory` / `build.resources.resource.directory` / `build.testResources.testResource.directory`\
  Various source code directories.
* `maven.compiler.source` / `maven.compiler.target` / `maven.compiler.release`\
  Source/Target/Release versions (`javac` `-source`/`-target`/`-release`) ; source & target default to 1.8
* `maven.compiler.showWarnings` / `maven.compiler.debug` / `maven.compiler.parameters` / `maven.compiler.verbose` / `maven.compiler.showDeprecation`\
  `javac` `-nowarn` (inverted), `-g`, `-parameters`, `-verbose`, `-deprecation`.
* `maven.compiler.executable`\
  `javac` used for the build.\
  This completely overrides the javac detected using `MICROMVN_JAVA_HOME` / `JAVA_HOME` / `java.home`.
* `micromvn.java`\
  `java` used for executing tests/etc.\
  This completely overrides the java detected using `MICROMVN_JAVA_HOME` / `JAVA_HOME` / `java.home`.
* `micromvn.testMainClass` / `micromvn.testMarker`\
  These default to `org.junit.runner.JUnitCore` (JUnit 4 console runner) and `Lorg/junit/Test;`. Changing them can be used to adapt micromvn's test logic to another test framework.\
  If `micromvn.testMarker` is found in a class file in the test classes directory, that file is recognized as a test and is passed as an argument.

## POM Support

The POM support here is pretty bare-bones. Inheritance support in particular is flakey.

POM interpolation is supported, though inheritance may be shaky.\
The supported sources of properties are (in evaluation order):

1. `project.*`, `basedir`, or `maven.build.timestamp`\
   Only `project.basedir` and Compiler 
2. Command-line properties
3. Properties in `<properties>`
4. Java System properties
5. `env.*`
6. Fixed defaults for various properties

Java System Properties are supported and `<properties>` is supported.\

No other properties are supported.

microMVN makes a distinction between *source POMs* and *repo POMs.*\
Source POMs are the root POM (where it's run) or any POM findable via a `<module>` or `<relativePath>` chain.\
Source POM code is *always* passed to javac via `-sourcepath`.\
Repo POMs live in the local repo as usual and their JARs are passed to javac via `-classpath`.

These exact POM elements are supported:

* `project.groupId/artifactId/version`\
  Project artifact coordinate.
* `project.parent.groupId/artifactId/version/relativePath`\
  Parent project.
* `project.packaging`\
  Sets the project's packaging type.\
  `pom` and `jar` are supported; unknown values resolve to `jar` (for compat. with, say, `bundle`).
* `project.properties.*`\
  Properties.
* `project.repositories.repository.url`\
  Adds a custom repository.
* `project.dependencies.dependency.optional/scope/groupId/artifactId/version/relativePath`\
  Dependency. `compile`, `provided`, `runtime`, `test` and `import` are supported.\
  As per Maven docs, optional dependencies only 'count' when compiling the project directly depending on them.
* `project.modules.module`\
  Adds a module that will be compiled with this project.
* `project.build.plugins.plugin.(...).manifest.mainClass` (where the plugin's `artifactId` is `maven-assembly-plugin` / `maven-jar-plugin`)\
  Project's main class.
* `project.build.plugins.plugin.(...).compilerArgs.arg` (where the plugin's `artifactId` is `maven-compiler-plugin`)\
  Adds an arg to the compiler command-line. Can be useful to toggle lints. Not inherited right now.

## Quirks

* Maven dependency version resolution is messy. I *hope* I've gotten something in place that works now.
* The main hazard is a lack of real plugins or compile-time source generation.
* `maven-assembly-plugin` is very partially emulated and always runs during package.
* Manifest embedding support is weird. Single-JAR builds prioritize user-supplied manifests, while assembly builds always use a supplied manifest.
* All projects have a `jar-with-dependencies` build during the package phase.
* It is a known quirk/?feature? that it is possible to cause a POM to be referenced, but not built, and microMVN will attempt to package it.
* As far as microMVN is concerned, classifiers and the version/baseVersion distinction don't exist. A package is either POM-only or single-JAR.
* Testing is weird. See `micromvn.testMainClass`, `micromvn.testMarker`, `umvn-test-classpath` and `umvn-run`.
* You don't need to explicitly skip tests. (This is an intentional difference.)
* Compilation itself is always clean and never incremental.

If any of these things are a problem, you probably should not use microMVN.
