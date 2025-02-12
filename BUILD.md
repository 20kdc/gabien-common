# Build Guide

Ok, so, I'm not going to lie: the way R48's release infrastructure is automated is heavily reliant on Unix-specific shell scripts.

Regardless, let's push on.

## Setup

### JDK

GaBIEn is intended to be developed and built using OpenJDK 8. OpenJDK 17 is likely to work; OpenJDK 21 is less reliable due to a buggy interaction with Android D8 in the Android build-tools 34.0.0.

Predicted things that will break on later JDKs:

* Android legacy version APK signing requirements: APKs must be signed using SHA-1 to support older Android versions. Newer versions of `jarsigner` do not like this because it is an outdated algorithm.
* Outright dropping of `-target 8`: OpenJDK 21 threatens an upcoming lack of support for building Java 8 classfiles.

For this reason: You are expected to have a Java 8 JDK located at the environment variable `JAVA_1_8_HOME`.

### `gabien-natives`

The `gabien-natives` package is complicated to compile. *For that reason,* it is shipped as binary releases on this repository.

After extracting one of these packages, you will find a `sdk-install` script which will install the natives package to your local Maven repository.

### First Compile

The `umvn` command is a 'Maven-like' Java builder/tester. It's supplied in a manner similar to the Gradle Wrapper.

Running one of these commands from the `gabien-common` will confirm it is installed correctly:

* `./umvn test`
* Windows: `umvn.cmd test`
* Maven installed: `mvn test`

Assuming the tests pass, you can then run `umvn install` / `mvn install -DskipTests` and start using GaBIEn like any other Maven-supplied framework.

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
