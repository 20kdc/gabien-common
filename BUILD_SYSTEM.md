# How The New Build System Hangs Together

## The Train Of Thought

So, let's say you've got a problem. What kinda problem?

Well, you want your project to be buildable for essentially as long as possible. You want your bus number to be infinite. You have a project that has to target Android, so your maximum runtime version is 'Java SE 8-' (with emphasis on the minus) for code you ship.

You don't want to have to uninstall a JDK just to get your own build to work because nobody knows what `JAVA_HOME` is. _You won't be there to adjust your buildscripts to fit the latest Gradle breaking change of the month._

How do you do it?

Well, first, you ditch third-party dependencies that might up their requirements at any time. OpenJDK 8 is eternal; this is a blessing in disguise.

`micromvn` is the first step in this, solving a ton of problems for contributor onboarding simply by existing. If there are ever issues with having its binary embedded in-repo, it's entirely possible to work a compilation into the activation script.

`micromvn` provides a common language between GaBIEn infrastructure and IDE infrastructure for the 'Java project', and provides a reliable, eternally Java 8 tool. It's the primitive that everything else needs. That's all it does and that's all it has to do.

The problem is that `micromvn` is not a portable batch script language. And pretending to use some kind of plugin system as a batch script language is often more trouble than its worth. _If you want a system of scripts, you need a system of scripts._

But that's fine, because having it around does give you the ability to compile Java projects which you can edit properly in your IDE. A Java project is perfectly capable of _acting_ as a portable batch script, and now you can guarantee there's a tool to compile and run such a project from the command-line available to the user.

So thus, the GaBIEn virtual environment, adding `umvn` for low-level operations and `gabien-do` for high-level tasks.

## How It Works

The "GaBIEn virtual environment" is really a pair of tools that get stapled into your PATH, namely these two:

* `umvn`: micromvn.
* `gabien-do`: Much more difficult to explain.
* `gabien-incept`: `gabien-do` without the compilation.

The main thing about `gabien-do` is that it uses `umvn` to first recompile and install the `gabien-buildscript` project, followed by recompiling the repository's `build-script` package and executing it. When run in the `gabien-common` repository, this results in a double recompile, but that's okay.

`gabien-incept` is used when `gabien-do` wants to fork a JVM.

`gabien-buildscript` contains the actual CLI frontend of `gabien-do`, along with common classes and utilities expected to be used by buildscripts.

Note that `umvn` is connected only by command-line invocation; a `umvn` API was considered, but I ultimately rejected the idea because `umvn`'s minimalism causes it to be highly unstable outside of a single-invocation context, and its internal structure changes at a whim. Keeping its API at command-line-level helps keep it stable.

## How You Use It

If you're compiling a project, you just activate the GaBIEn virtual environment, much like if you were activating a Python `venv`.

You can then see commands in any given project using `gabien-do` from that project's root directory (the directory in which the `build-script` directory lives).

If you're making a project, the main quirk is that you need to create a `build-script` project. This project must not depend on any projects that you intend to compile while the script is running. In practice it should probably only depend on `gabien-buildscript`.

This project can be empty, but you can create a `buildscript.Index` class implementing `gabien.builder.api.ToolModule` -- this class lets you register tools to be invoked from the command-line.

Tools follow a reasonably consistent structure designed to reduce the amount of unnecessary legwork for command-line args etc. in the build-script; in particular, inheritance is possible, and parameter sets can be embedded into tools.
