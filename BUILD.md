# Build Guide

Ok, so, I'm not going to lie: _The GaBIEn build process is the way it is because every tool I used ran into compatibility issues with every other tool, so I decided to secede from most of the greater Java ecosystem._

The journey, and impetus, to get here involved a lot of pain that I hope you as a (new contributor / historian from the year 2125 / etc.) will never have to face. 

Unfortunately, that means you have to deal with this custom build system. \- 20kdc

## Setup

### Prerequisites

Here's the short summary:

* GaBIEn is intended to be developed and built using OpenJDK 8. **You are expected to have a Java 8 JDK located at the environment variable `JAVA_1_8_HOME`.**
* There are two external JARs that need to be installed explicitly. There's a command for this.
* **On Unices, `bash` or `zsh` are required.** (`bash` is used-in-production but `zsh` was tested with the activation script at time of writing.) _This is because implementing portable environment activation on other shells is much harder than it should be._

### Shell & First Compile (Unix)

The environment activation script is generally used on bash and at time of writing works on `zsh`.

You can run `. ./bin/activate` (similar to a Python venv)

This will setup the GaBIEn command-line environment, including putting the `JAVA_1_8_HOME` JDK and the tools in `bin/` into PATH.

You will see `GE ` at your prompt.

You can now run `gabien-do install` to install the two external JARs. If you are not targetting Android, it is possible to skip installing the Android platform JAR.

You can now run `gabien-do ready`.

You can also use `gabien-do check` to check for common installation issues.

### About `gabien-do install`

This command is required to install two packages.

#### `natives` (aka gabien-natives)

The `gabien-natives` package is complicated to compile. *For that reason,* it is shipped as binary releases on this repository.

If you _really_ want to compile it anyway, there are instructions in that directory. **Understand: You should not just jump straight into compiling gabien-natives unless you have a _really_ good reason.**

After extracting one of these packages, you will find a `sdk-install` script which will install the natives package to your local Maven repository.

#### `android-platform`

The `android-platform` package provides an Android platform JAR to D8. While D8 is installed via Maven, isn't.

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

Android is awkward. According to Android developer documentation, there are two methods of performing an Android build: The Gradle way and the manual way.

I have an axe to grind with Gradle

**Thanks to <https://github.com/REAndroid/ARSCLib>, I have now _almost_ completely removed any need to install the Android SDK from this repository.**

In any case, here's how you might set `ANDROID_JAR_D8`:

```
export ANDROID_JAR_D8=~/Android/Sdk/platforms/android-7/android.jar
```
