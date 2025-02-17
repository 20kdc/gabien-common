# gabien-natives

Native libraries that are or will be used by gabien-common.

Because this project involves a stack of complicated tooling to reach a wide variety of target platforms, this project is treated as a separate library with binary releases of it that can be treated "external" to gabien.

However, it's still open-source.

These were going to have their own, stable version numbers using semantic versioning, but stuff got _complicated._ For now: They have codenames, and they've been _mostly_ stable since the first release, since the main source of changes is in BadGPU.

**Because of the above, this part of the repository is deliberately not included in the main build.**

## Goals

### BadGPU

Rough-edged subset of OpenGL presented in a WebGPU-like style with a very lightweight JNI wrapper to minimize marshalling cost. Memory safety is handled on the Java side.

Pre-multitexturing, post-FBOs (mainly because pbuffers are awful).

Hardware support ideology is "If you can run HL2, your hardware doesn't really have a reason not to be able to run this".

Flexibility ideology is to assume someone will have to write a GLES2 backend in future for compatibility reasons, and design accordingly.

(I expect this to be relevant roughly when Apple drops OpenGL and everybody has to scramble to shipping ANGLE.)

_API design is more or less complete, modulo two features which seem to be less reliable than originally intended (clip planes and texture matrices). These features may become more stable if a GLES2 backend is added._

### Codecs

gabien-natives contains MP3 and Vorbis decoders that have been stripped down to remove unnecessary code that can be handled in Java, such as Ogg sync. The idea here is that less code = less security surface.

## Build Instructions

1. Be running Linux or something that can imitate it, via say Docker. (The build process is already enough of a mess...)
2. You need `i686-w64-mingw32-gcc` and `x86_64-w64-mingw32-gcc`.
3. You need the Android NDK, versions `r9d` and `r12b`.
4. You need Zig `0.11.0-dev.2892+fd6200eda` ; other versions may work, no guarantee
5. You need to *symlink* the NDK and Zig into `thirdparty` as follows:

```
natives/thirdparty/android-ndk-r9d (to corresponding NDK)
natives/thirdparty/android-ndk-r12b (to corresponding NDK)
natives/thirdparty/zig (to Zig directory, NOT to Zig binary!)
```

The procedure is then to run `sdk-make`.

## Install Instructions

Run `./sdk-install`.

