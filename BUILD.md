# Build Guide

Ok, so, I'm not going to lie: the GaBIEn build process is the way it is because Gradle kept breaking compatibility and ran into problems with JDK switching and generally made my ability to work on my projects as a developer impossible. As a result, I have instead created a spaghetti mess which **can always be made to work,** but **is not always pleasant to work with.**

I've at least started to 'comb the hair' of it, though!

I hope you understand. Let's push on. - 20kdc

## Setup

### Prerequisites

Here's the short summary:

* GaBIEn is intended to be developed and built using OpenJDK 8. **You are expected to have a Java 8 JDK located at the environment variable `JAVA_1_8_HOME`.**
* Due to issues with the Android Build Tools, **a later OpenJDK (any that Android D8 can run with) needs to be in PATH for Android builds to succeed. Plus there are a bunch of environment variables needed for that.**
* **gabien-natives needs to be installed (see next section).**
* **On Unices, `bash` or `zsh` are required.** (`bash` is used-in-production but `zsh` was tested at time of writing.) _This is because implementing portable environment activation on other shells is much harder than it should be._

### `gabien-natives`

The `gabien-natives` package is complicated to compile. *For that reason,* it is shipped as binary releases on this repository.

If you _really_ want to compile it anyway, there are instructions in that directory. **Understand: You should not just jump straight into compiling gabien-natives unless you have a _really_ good reason.**

After extracting one of these packages, you will find a `sdk-install` script which will install the natives package to your local Maven repository.

### Shell & First Compile (Unix)

The environment activation script is generally used on bash and at time of writing works on `zsh`.

You can run `. ./bin/activate` (similar to a Python venv)

This will setup the GaBIEn command-line environment, including putting the `JAVA_1_8_HOME` JDK and the tools in `bin/` into PATH.

You will see `GE ` at your prompt. You can now run `gabien-do ready`.

You can also use `gabien-do check` to check for common installation issues.

### Shell & First Compile (Windows)

`bin\activate` (or `call bin\activate` from scripts), similar to Unixes.

You can then run `gabien-do ready` (or `call gabien-do ready` from scripts), and also `gabien-do check` as usual.

## Eclipse IDE

Ensure Maven support is available. Use `File > Import...`.

Select `Maven > Existing Maven Projects`.

Select this repository directory.

When choosing which sub-projects to import, you may wish to deselect the parent project.

Also be sure to import the style file (`eclipse-style.xml`)!

### Eclipse warnings configuration

#### Null analysis

Most are set to Ignore, except:

* Null pointer access
* Violation of null specification
* Conflict between null annotations and null inference
* Problems detected by pessimistic analysis for free type variables
* Redundant null annotation
* NonNull parameter not annotated in overriding method

Use default annotations is on (provided by USLX) and inherit null annotations is also on.

## Android

Android is awkward. Officially, there are two methods of performing an Android build: The Gradle way and the manual way.

Since Gradle regularly breaks compatibility with each release and because JDK versioning makes holding to an old release untenable, I chose shell scripts.

These shell scripts... have not been converted for Windows. Personally, I'm hoping to replace them with Java sometime in the next 2 years (so she writes at the start of 2025...)

In any case, here's how you might set the important environment variables.

```
export ANDROID_BT=~/Android/Sdk/build-tools/34.0.0
export ANDROID_JAR_AAPT=~/Android/Sdk/platforms/android-25/android.jar
export ANDROID_JAR_D8=~/Android/Sdk/platforms/android-7/android.jar
```

_A **really big** problem is that D8 needs a later version of Java than Java 8._ It's for this reason that the original Java tools remain in PATH when the environment is activated.
