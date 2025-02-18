# `gabien-do` and `build-script`

`gabien-do` compiles and installs the engine build-script project, then compiles the current repository's build-script project.

The current repository's build-script project can then attach tools to the build assistant using a class at `buildscript.Index`.

_This build-script system is an entirely separate compile from the rest of the engine._ This is important because it allows tying the engine compile into whatever you're doing.
