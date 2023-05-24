# Unsafe Native Access (UNA)

# Under construction, specifications subject to change

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

## Invoke Core

UNA invocation is a bit of an awkward process, owing to the limitations.

It supports up to 8 arguments.

UNA has two types:

+ `int32_t` aka `I`
+ `float` aka `F`

Note `void` is not a type, even for returns, as an undefined word return is always perfectly safe in every ABI known to the writer.

UNA calls are made using functions such as `long UNAC.WcWW(long a0, long a1, long code)`.

This function takes two word arguments, along with the code pointer, and executes it, returning an int result.

## Invoke Limitations

+ It's not possible to use doubles. (This would add too many combinations to allow for 8 parameters.)
+ On 32-bit platforms, `long` must be manually broken into two using code such as `param, param >> 32`
+ Calling conventions other than the system default aren't supported. This includes stdcall.

These usually have a workaround in the form of dynamic code generation.

