# Unsafe Native Access (UNA)

# Under construction, specifications subject to change

Unlicense-licensed library to access native functions from Java.

Because this project involves a stack of complicated tooling, this project is treated as a separate library with binary releases of it that can be treated "external" to gabien.

These will have their own, stable version numbers using semantic versioning.

However, it's still open-source.

*Because of the above, this part of the repository is deliberately not included in the `mvn install` process.*

## Design Goals

+ Fast(ish): UNA avoids any memory allocation during native calls after an initial setup.
  Unfortunately some other kinds of marshalling and so forth can't be avoided due to ABI issues.
+ Portable(ish): UNA does not rely on any assembly. However, the invocation component is architecture/ABI-specific,
  and so requires some poking to customize for a specific target. See `INVOKE_INTERNALS.md` if you're curious.
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

## Invoke

## Invoke Limitations

+ Parameters need to be "encoded". While not the most complex operation, it's not strictly trivial either.
+ Variants need to be chosen at runtime to ensure that pointers are properly changed.
+ Calling conventions other than the system default aren't supported. This includes stdcall.

These usually have a workaround in the form of dynamic code generation, but that's system-specific.

