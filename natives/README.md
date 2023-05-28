# gabien-natives

Native libraries that are or will be used by gabien-common.

Because this project involves a stack of complicated tooling to reach all intended targets, this project is treated as a separate library with binary releases of it that can be treated "external" to gabien.

These will have their own, stable version numbers using semantic versioning.

However, it's still open-source.

*Because of the above, this part of the repository is deliberately not included in the `mvn install` process.*

## Goals

### BadGPU

Rough-edged subset of OpenGL presented in a WebGPU-like style and (will be) JNI-wrapped for good measure.
Pre-multitexturing, post-FBOs (mainly because pbuffers are awful).

Hardware support ideology is "If you can run HL2, your hardware doesn't really have a reason not to be able to run this".

Flexibility ideology is to assume someone will have to write a GLES2 backend in future for compatibility reasons, and design accordingly.

(I expect this to be relevant roughly when Apple drops OpenGL and everybody has to scramble to shipping ANGLE.)

_API design is under revision, though the reference implementation is complete enough that things should stabilize as other parts come into play._
