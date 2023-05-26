# Invocation

## Theory Of Operation

The theory of operation of UNA's invocation mechanism is based on a sort of "unified ABI theory".

This theory proposes that all commonly used C ABIs (except `stdcall` because it's not strictly C-complaint) can be folded into one super-ABI which is parameterized to the machine.

Further, this theory proposes that:

+ A single function can be written in reasonably portable C to make a dynamic call across this super-ABI for any series of integer arguments up to a given compile-time chosen limit, with linear size and time complexity, without encoding the specifics of any one ABI.
+ For any given ABI of the super-ABI, a single function can be written without using assembly to make a dynamic call for any series of integer or floating-point arguments up to a given compile-time chosen limit, with linear size and time complexity, but this needs to encode the specifics of that ABI.

### Unified ABI Theory

The Unified ABI theory is as thus:

To my knowledge, all reasonable C ABIs must ensure:

1. Any list of arguments given by the caller may be truncated on the right side by the callee and still operate perfectly.
   (This is required for var-arg support in the presence of deprecated, but still present features such as implicit declaration.)
   (This is violated by stdcall, of course.)
2. This implies that allocation of arguments must be from left to right.
   Later arguments never affect the allocation of earlier arguments.
3. The ABI must operate on consistent, measurable rules between different functions.
   The same argument list and return value must result in the same ABI.
   (Inlining and deleting a static function is not observable and thus doesn't count.)

To my knowledge, all *common and default* C ABIs follow the above rules, and also these:

1. A call's data can be absolutely split into the Integer Registers, the FP Registers, and the Stack.
   Some of these components do not always exist, but no further components exist in portable code.
   These three utterly define the call.
2. The stack always grows downwards.
   In theory, this is a throwback to when stack was in direct competition with main memory for physical address space.
   It has, however, remained a constant in all ABIs under discussion.
3. On stack, arguments are "pushed right to left".
   This seems unusual, but is better understood as that they are in memory left to right.
4. The stack and integer registers are measured in machine words (i.e. `void *`).
   FP registers are always measured in values (if the value is float or double does not alter FP register allocation).
   (This *has been tested* to apply on x86\_64, and can be seen to apply for AArch64 by reading `ARM Cortex-A Series Programmer's Guide for ARMv8-A / Floating-point register organization in AArch64`. This is also described to apply for RV32/RV64 with the `D` extension, which is part of `G` and thus expected.)
5. The misalignment of stack values longer than a word does not matter.
   The misalignment of stack values shorter than a word never happens because of the previous rule.
6. All calls with the same amount of arguments of the same sizes, but in different orders, use the same allocations of registers and stack.
   However, they will be accordingly reordered and shifted between different places.

A good look into this is the file `c/theory.c`.

Note, however, there is one critical difference: The amount of registers and how they are used varies.

* Linux-RV64 will use GP registers as "extra" floats. AArch64 and x86\_64 won't.
* RV64 has 8 GP argument registers. Windows-x86\_64 has only 4, while Linux-x86\_64 has 6.
* Some ABIs simply do not use registers, i.e. the default ABIs on 32-bit x86 systems.

Due to this, and possible variance in future ABIs, it's not possible for the super-ABI to consist solely of absolutes.

Instead, the super-ABI is simply a set of rules that allow for the creation of portable C code to create an interface able to prod the *real* ABI.

The results of the testing of that interface on a machine profiles the ABI to enough precision to be able to make calls.

## Invocation Itself

UNA invocation is a bit of an awkward process, owing to limiting the code to what can be considered reasonably standard C.

It supports a return type, 16 "machine word" arguments, and 8 "float/double" arguments.

To the target function, the machine words are supplied, then the float/double arguments.

The arguments must be arranged by an "ABI simulator" written in Java.

The return type and float/double selection is handled by a 10-bit field, the *variant*.

This is arranged as so: `01234567RR`

The lowest two bits `R` select the return type.

+ 0 `int32_t`
+ 1 `int64_t`
+ 2 `float`
+ 3 `double`

Note `void` is not a type, even for returns, as an undefined word return is always perfectly safe in every ABI known to the writer.

UNA calls are made using the `long UNAInvoke.call(...)` function.

**There is no API guarantee that this will remain consistent. Always use provided encoding functions unless you really have to.**

## stdcall

stdcall is a particularly annoying special case for 32-bit Windows.

Luckily, it's a completely stack-based convention and thus avoids most of the other issues.

Unluckily, it still has to be forced to fit into the general profile expected.

UNA stdcall calls are made using the `long UNAInvoke.stdcall(...)` function.

This function uses a different variant layout: the lower 2 bits are again the return type, while the upper bits are the number of arguments.

The amount of arguments can be from 0 to 16 words.

