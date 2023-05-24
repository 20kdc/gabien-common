# Unsafe Native Access (UNA)

Unlicense-licensed library to access native functions from Java.

Because this project involves a stack of complicated tooling, this project is treated as a separate library with binary releases of it that can be treated "external" to gabien.

These will have their own, stable version numbers using semantic versioning.

However, it's still open-source.

## Design Goals

+ Fast: UNA calls should not perform any "dynamic marshalling" steps.
  This does come at the cost of limiting UNA's maximum argument count.
  Code is moved as close to the start of the argument list as possible to reduce shuffling.
  UNA provides various methods of getting data in and out of native memory.
+ Portable: UNA relies on code generation, compiler optimizations, and ABI knowledge over assembly tricks.
  Porting UNA to a new platform should be as simple as having a working C compiler.
+ Small: UNA is not a platform abstraction library, nor does it try to make native access safe.
  `strdup` can just return 0. This is something you have to live with in C, and here too.
  UNA should contain:
  + Information about the host
  + Bindings for the dynamic linker
  + Bindings for string/memory-related C functions
  + Memory peek/poke
  + Bindings for useful JNI functionality
  + The actual invoke interface

## General Invoke Design

UNA invocation typically uses the default calling convention of the target, and supports up to 8 arguments.

UNA has five types:

+ i32 aka `I`
+ i64 aka `L`
+ f32 aka `F`
+ f64 aka `D`
+ Pointer aka `P`

Note `void` is not a type, even for returns, as an undefined `i32` return is always perfectly safe in every ABI known to the writer.

UNA calls are made using functions such as `int UNAC.cIII(int a0, int a1, long code)`.

This function takes two int arguments, along with the code pointer, and executes it, returning an int result.

## Limitations

+ Calling conventions other than the system default aren't supported. This includes stdcall.

These usually have a workaround in the form of dynamic code generation.

