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

## Peek/Poke

The peek/poke API works in terms of Java types, due to the bulk operations being wrappers around JNI operations.

As such, the types are:

+ Boolean `Z`
+ Byte `B`
+ Short `S`
+ Int `I`
+ Long `J`
+ Float `F`
+ Double `D`
+ Pointer `Ptr` (not supported by bulk operations; Java-side, uses long, but C-side uses host word size)

There are `get` and `set` functions for all of the types except Boolean.

For all types except Pointer, there are bulk get/set functions, prefixed with `A`.

## Invocation

UNA invocation is a bit of an awkward process, owing to limiting the code to what can be considered reasonably standard C.

It supports a return type and up to 6 arguments.

Invocation has three types:

+ `int32_t` (`I` or 0)
+ `int64_t` (`L` or 1)
+ `float` (`F` or 2)

There are two "meta" types supported by the encoder:

+ `int32_t` (`V`) (void)
+ `void *` (`P`) (Pointer: `I` or `L` depending on hardware)

These types are fit into an integer which describes "variants" of a function (for types).

Variants are packed using `v = (v * typeCount) | t;` to add each type.

Note `void` is not a type, even for returns, as an undefined word return is always perfectly safe in every ABI known to the writer.

UNA calls are made using functions such as `long UNA.c2(long a0, long a1, long code, int variant)`.

This function takes two arguments, along with the code pointer and variant, and executes it, returning the result.

**There is no API guarantee that variant numbers will remain consistent. Always use `UNA.encodeV`.**

## Invoke Limitations

+ It's not possible to use doubles. (This would add too many combinations.)
+ Variants need to be chosen at runtime to ensure that pointers are properly changed. 
+ Calling conventions other than the system default aren't supported. This includes stdcall.

These usually have a workaround in the form of dynamic code generation, but that's system-specific.

## Special Invokes

There are some important functions from popular APIs (specifically OpenGL) which do not fit within the limits of the above.

As such, ways to invoke them are provided. See `UNA.LcIIIIIIIIP` for an example.

