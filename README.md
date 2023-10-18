# gabien-common

## License

See COPYING, but TLDR: Unlicense except for thirdparty stuff.

All thirdparty dependencies must be under reasonably equivalent licenses.

However, their individual licenses will still be respected and reproduced.

Current list of thirdparty stuff:

* stb\_vorbis ( https://github.com/nothings/stb/ ) - *modified*, at `thirdparty/stb_vorbis_modified`

## Build Instructions (Command-Line)

Ensure Maven is installed and functional. At the present time, I'm using Apache Maven 3.8.4.

Run `mvn install` from this repository directory.

To avoid running unit tests, run `mvn install -DskipTests`.

All projects will be automatically compiled (and possibly unit-tested).

## Build Instructions (Eclipse IDE)

Ensure Maven support is available. Use `File > Import...`.

Select `Maven > Existing Maven Projects`.

Select this repository directory.

When choosing which sub-projects to import, you may wish to deselect the parent project.

*You may need to get Eclipse to ignore the toolchain plugin.*

Also be sure to import the style file!

## History

"gabien" (Graphics And Basic Input ENgine was at least one of the acronyms) was originally developed as a part of an unofficial map editor for an old freeware game.

Mainly, it served as a cross-platform rendering backend, running on both JavaSE and Android.

It was later refactored into a separate library to be used across my personal projects.

Parts of that map editor were eventually integrated into R48 (`gabien-app-r48`), but they were different projects.

Since then, it's had peaks and declines, the worst relating to build system issues.

As Java continues to decline, I consider the Godot 3.x series to be the best target for further development, but I have severe hardware compatibility concerns about Godot 4.

With this in mind, I intend to keep this around, just in case.

## Notes on Code Style

The code style isn't really documented as such, but it's basically the IDEA default with two changes.

Firstly, `new int[]{` or such is wrong, and should be `new int[] {`.

Secondly, annotation wrapping is a per-class matter of stylistic choice - some annotations are deliberately grouped together.

Pedantic, I know.

Otherwise, 4-spaces indentation, and do NOT use any "rearrange definitions" feature.

Sometimes there's a method to the definition order. (Sometimes there isn't, but still.)

## Sub-projects

* uslx: Universal Java utilities, NonNull/Nullable annotations, profiler.
* natives: This is where all the native code goes, including BadGPU, a library which is kind of like WebGPU but if it was targetting ancient phones and had to deal with JNI.
* natives-util: JNI wrappers for natives. These are in a separate package so that changes to the JNI wrapping don't have to be tightly bound to the compiled and separately released natives (in case of bugs/etc.)
* natives-examples: This ideally would be moved into tools, but this was the prototyping area where the more advanced capabilities of BadGPU were tested. Because the nature of the AWT WSI hadn't been figured out yet, this uses its own "not-gabien" WSI.
* datum: "mostly Scheme-compatible" S-expression format for easy to type and modify human-readable data with a minimalist specification.
* media: USLX, but for media formats! As such, requires natives-util.
* common: "Core engine APIs" - this is the "core engine" that android/javase primarily implement.
* ui: Utilities & UI framework, built around common. In general if an API isn't closely bound to the "core wrapper" it should probably go here. Backends need this for the emulated file browser (and ONLY for that, so it may get loosely bound in future)
* android: Android backend & Java 8 polyfill.
* javase: JavaSE (AWT) backend.
* tools: Tools for debugging gabien and messing around with stuff.

