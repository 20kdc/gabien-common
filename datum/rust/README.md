# `datum-rs`

Datum is an S-expression format meant for quick implementation in various languages.

It has a specification, available at <https://github.com/20kdc/gabien-common/blob/master/datum/specification.md>.

It was originally developed for use in some of my Java programs for the purpose of fulfilling the role of 'terse data language,' with some key distinctions:

* As free-form a syntax as reasonably possible.
* Avoids the problems that YAML has.
* Concise implementation.

It's intended to be reasonably readable by R6RS readers, but not a strict subset. (However, it has been used in a Java project to implement a "Javaified" Scheme dialect.)

`datum-rs` is a library for reading and writing Datum values in Rust.

In order to allow use in diverse environments, it attempts to follow some key rules:

* `#![no_std]`
* `#![forbid(unsafe_code)]`
* completely public-domain
* no external dependencies, but without trying to reinvent the wheel _too_ hard

## TODO

* Serde integration! That makes everything more popular! But need to standardize a mapping
* Example program (a toy LISP perhaps?)
* Shore up all the documentation
* Figure out how to make doctests that rely on alloc while not being for alloc functions
* If Datum is going to be a serious project I should probably move it out of the `gabien-common` umbrella. But also, that's probably going to mean the Java implementation has to be either left behind or things are going to get a little ugly. Basically, mass reorganization incoming.

## License

```
This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org>
```
