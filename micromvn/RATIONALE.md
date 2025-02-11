# Rationale

## Why do this?

micromvn was originally created for three reasons:

 * Toolchain files are awkward to setup compared to one environment variable.
 * Apache Maven changes required JDK version every so often, causing added friction with programs that naively use the latest JDK.
 * Installing OpenJDK 21 broke my build due to a bug with Android D8.\
   While it has supposedly been fixed upstream, I'm on Android build-tools 34.0.0 and still get it.\
   At time of writing, that's the latest version. So this fix is only available if using AGP.\
   And since Gradle loves breaking the build (this is why I moved to Maven), AGP is *not an option.*

The thing to keep in mind is that I'm always going to be targetting Java 8 because of Android.\
That makes Java 8 effectively 'evergreen'.\
The ideal world here is that the GaBIEn and R48 release builds use only OpenJDK 8, forever.

A nicer solution to the toolchain problem in Maven does exist.\
I later found out about `maven.compiler.executable`, and support has been added to micromvn.\
But having a consistent 'evergreen JDK' is its own reward.

As for IDE integration, Eclipse is already great at compiling and running under the appropriate JVM.

## Isn't this something else the user has to install?

micromvn is intended to be shipped with your project. It's like the Gradle wrapper; but much more reliable.

* Gradle wrappers require an internet connection to download Gradle.
* Gradle wrappers break all the time due to the use of ObjectWeb ASM techniques that can't keep up with the local JDK.
* Gradle is very opaque, so it's awkward to get it to use a _specific_ `javac`; and Gradle's volatility causes most advice you might see on, say, Stack Overflow to be invalid by the time you need it.

micromvn requires only a Java 8 or newer JDK.

## Why does micromvn use POM files like Maven?

The idea is that the project is Maven as far as the IDE is concerned, so IDE integration is free.
