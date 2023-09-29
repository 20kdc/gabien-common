/*
 * `gabien-common` - Cross-platform game and UI framework \
 * Written starting in 2016 by contributors (see `CREDITS.txt`) \
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty. \
 * A copy of the Unlicense should have been supplied as `COPYING.txt` in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * # BadGPU C Header And API Specification
 *
 * Version: `1.0.1`
 *
 * ## Formatting Policy
 *
 * + 80-column lines in most cases.
 *   This rule can be ignored for parameters if organization is worsened, or for
 *    the license block. This rule can't be ignored for comments in general.
 * + In the body of the document, care *should* be taken to use RFC 2119-style
 *    keywords, such as the former, in ways that are appropriate.
 * + Lines beginning with space followed by asterisk will be pushed into the
 *    markdown file as commentary such as this. \
 *   Plan accordingly; in particular, writing backslash before the end of a
 *    line will make it into a hard line break, important for paragraphs inside
 *    lists such as this one.
 * + If a decision is worth explanation, explain it in a Rationale section. \
 *   Rationale: Rationale sections help to clarify why a decision was made,
 *    and suggest example patterns of usage.
 * + Enum value references where the enum can be resolved shouldn't include
 *    the enum name.
 *
 * ## Versioning Policy
 *
 * Semantic versioning 2.0.0 is in use here.
 *
 * However, this is a specification with a C header, and thus not a piece of
 *  software in itself, so essentially all non-comment changes are of _Minor_
 *  severity or greater.
 *
 * + When the C content (not comments) of the specification changes, a _Minor_
 *    or _Major_ version increment _must_ be made as appropriate.
 * + Other changes to this specification _should_ make _Patch_ increments unless
 *    the change, despite not changing the C content, makes a meaningful change
 *    to the ABI (for example, making a parameter optional). \
 *   The rule of thumb here is if the reference implementation was changed to
 *    implement a spec change, make a _Minor_ or _Major_ increment. \
 *   Otherwise, make a _Patch_ increment. \
 *   (Rationale: While the specification is intended to be the ultimate source
 *    of truth on BadGPU, the reference implementation is the only one that
 *    exists at this time. It is therefore more useful to version according to
 *    the behaviour of the reference implementation, and not cause undue version
 *    drama for any hypothetical working applications over a spec change with
 *    zero effective impact on said applications.)
 * + Whenever possible, a single version of this specification should be
 *     resolvable to a single Git commit.
 * + If at all possible, version `1.0.0` will be the last update to the _Major_
 *    and _Minor_ versions of the specification.
 *   + Failing this, it will be the last update to the _Major_ version of
 *      this specification. Any new drawing capabilities (such as, say, opt-in
 *      support for ES2 shaders) will be implemented as additional calls.
 *     + In the event of a catastrophic failure of future versions, at a bare
 *        minimum, all versions of this specification past `1.0.0` *must*
 *        remain compatible with the last patch version of the `1.0.x` series.
 *
 * ## Design Policy
 *
 * + Allowed types are:
 *   + `(u)?int(8|16|32)_t`
 *   + `unsigned char` (as `BadGPUBool` only! This is chosen to match JNI.)
 *   + `float`
 *   + Structs and enums, including undeclared (i.e. opaque)
 *     + Structs should not be used as a replacement for parameters unless they
 *        have some level of dynamic layout, like arrays or different types.
 *       *However,* adding a struct is better than adding a handle.
 *       Rationale: Structs tend to add unnecessary overhead in JNI. None of the
 *        solutions are good. At best you lose what you gained.
 *   + Pointers of various shapes and sizes
 *     + Handles in particular should only be used when BadGPU is holding an
 *        underlying resource of some kind. This is why the only three handle
 *        types at time of writing are `Instance`, `Texture`, and `DSBuffer`.
 *       If BadGPU isn't holding a resource, it's added overhead to create
 *        get/set functions to prod what is essentially a remote struct.
 * + Function pointers are not okay. Do anything else. \
 *   Rationale: BadGPU is not an asynchronous API, so does not stand to gain
 *    from using function pointers, except for implementation simplicity. \
 *   Due to how BadGPU manages resources, a situation can occur where BadGPU is
 *    holding onto a pointer for eternity if code is badly written. This is
 *    formally considered an application of the "leaking memory is safe"
 *    principle, but with function pointers it becomes somewhat dangerous. \
 *   Even if a function pointer is used only temporarily, this is a mess that
 *    wrappers inevitably have to clean up, and when it's temporary it's even
 *    more likely that there was a simpler and better way.
 * + Avoid making things stateful unless they hold resources at another level,
 *    like say the GL. Even then, if state can be avoided by prodding the GL a
 *    small amount when the resource is in use, try to avoid that state. \
 *   Rationale: If I actually liked the amount of state in OpenGL 1.1, this
 *    would just have been a context creation library. The amount of functions
 *    in OpenGL that amount to "get state, set state" is absurd, as a reading
 *    of the reference implementation will show. Because BadGPU sticks to a
 *    very limited pipeline, this is actually practical.
 *   + In particular, never add a mutable variable that simply acts as a
 *      parameter to a function call. Immutable values can make sense under
 *      appropriate conditions.
 * + Never add a function just so BadGPU can retrieve a value the user gave in
 *    the first place.
 *   Rationale: OpenGL only did this so that libraries could still function in
 *    the immense swaths of state OpenGL has. \
 *   It is a very related fact that `glPushAttrib` and `glPopAttrib` exist. \
 *   Since BadGPU avoids immense swaths of state, and particularly mutable state
 *    that needs saving and restoring, it follows that getters/setters for this
 *    state are not necessary.
 *
 * ### The Custom WSI API Is A Special Exception
 *
 * The custom WSI API only exists because it's the only reasonable way to allow
 *  an application to use BadGPU efficiently with SDL2 context creation.
 *
 * As such, it's designed to work well specifically for C code that is
 *  responsible for connecting BadGPU and SDL2 along with some GL glue code.
 *
 * It is not designed to work well for anything else, and even exposing WSI
 *  details in general is a concession to performing Android WSI. This both was
 *  practical to implement and also absolutely necessary to implement.
 *
 * ## Implementation Policy
 *
 * If a detail of rendering is unclear, the implementation may choose any of:
 *
 * + OpenGL (any specification between 1.1 and 2.1 inclusive)
 * + OpenGL ES 1.1 (& Extension Pack, & documents referenced thereby)
 * + OpenGL extensions referenced by this specification
 * + OpenGL ES 2.0
 *
 * In the name of not overtly constraining implementations, invariance rules are
 *  assumed to not apply, since otherwise the particular set of GL state
 *  manipulations performed by the implementation may interfere.
 */

#ifndef BADGPU_H_
#define BADGPU_H_

#include <stdint.h>

#ifndef BADGPU_EXPORT
#define BADGPU_EXPORT
#endif

/*
 * ## Abstract
 *
 * The BadGPU API is a cross-platform off-screen rendering API.
 *
 * It is designed with adaptability in mind, not efficiency.
 *
 * It can be considered something of a relative ideologically to the OSMesa API,
 *  but OSMesa has the issue that it never takes advantage of acceleration. \
 * (To be clear, it is perfectly valid to implement BadGPU using OSMesa. It is
 *  simply not the preferred outcome.)
 *
 * BadGPU takes advantage of acceleration, but avoids complex WSI issues by
 *  simply not supporting WSI. BadGPU can't directly draw onto the screen.
 *
 * This allows BadGPU to be used seamlessly with any windowing/drawing framework
 *  at some performance cost, while still not losing as much performance as
 *  software rendering.
 *
 * Rationale:
 *
 * Apple makes WSI... bad. To get as far as I did required digging up the old
 *  tome known as the *CGL Reference*. Luckily, that part doesn't require the
 *  horror known as Objective-C. Lowest common denominator logic says that
 *  no WSI for Apple means no WSI for anyone. However, the integration functions
 *  exist so that some WSI can be 'added on' for Android, where performance is
 *  otherwise unusably low.
 *
 * In terms of API design, BadGPU owes some credit to WebGPU, but avoids the
 *  heavy use of structs for binding reasons, and removes explicit render pass
 *  objects.
 *
 * BadGPU isn't thread-safe per-se, outside of instances being isolated.
 *
 * Unlike WebGPU, BadGPU does not try to provide absolute memory safety
 *  assurance, but does at least make a good-faith effort to prevent crashes in
 *  favour of failure.
 *
 * In particular, if an implementation vendor _knows_ an operation will crash,
 *  the implementation _must not_ perform the operation, instead returning
 *  whatever failure indicator is appropriate.
 *
 * ### Functionality
 *
 * For portability reasons, BadGPU is designed to target the subset of
 *  functionality common between three separate versions of OpenGL:
 *
 * + OpenGL 1.1 +
 *    `EXT_blend_subtract`, `EXT_blend_func_separate`,
 *    `EXT_framebuffer_object`
 * + OpenGL ES 1.1 Common +
 *    `OES_blend_subtract`, `OES_blend_func_separate`,
 *    `OES_framebuffer_object`, `OES_texture_npot`
 * + OpenGL ES 2.0 with shader compiler and `OES_texture_npot`
 *   + That the shader compiler can be omitted in a valid OpenGL ES 2.0
 *     implementation is an interesting trick, but also means that this
 *     specification can't guarantee to implement certain features.
 *
 * Rationale: Complex blend functions are the reason this mess even started. \
 *  And the stencil buffer, along with the ability to share the stencil buffer
 *  between target images, is useful in "imitation multitexturing". In this way,
 *  a multitextured object may be composited and then transferred.
 *
 * Features which would require complex code generation to implement in OpenGL
 *  ES 2.0 are not supported.
 *
 * As such, the pipeline is essentially that of OpenGL ES 2, but with a lot of
 *  the options cut out and with fixed-function parts inserted where necessary.
 *
 * I considered PBuffers, but they're an excellent way to force you to deal with
 *  heavy WSI. Don't PBuffer.
 *
 * In terms of API design, BadGPU owes some credit to Vulkan and WebGPU, mostly
 *  the latter, but avoids the heavy use of structs for binding reasons.
 * (The reference BadGPU implementation will have built-in JNI support.)
 *
 * Like WebGPU, to which the API design owes some credit, BadGPU does not
 *  support the reuse of serieses of commands, but unlike it, BadGPU is an
 *  immediate API; it is not possible to cancel a command, there's no buffer
 *  recording.
 *
 * Unlike OpenGL, BadGPU tries to be as stateless as possible.
 *
 * BadGPU is thread-safe across different instances, but not across one.
 *
 * Unlike WebGPU, BadGPU does not try to provide absolute memory safety
 *  assurance, but does at least make a good-faith effort to prevent crashes in
 *  favour of failure.
 *
 * In particular, if an implementation vendor _knows_ an operation will crash,
 *  the implementation _must not_ perform the operation, instead returning
 *  whatever failure indicator is appropriate.
 */

/*
 * ## Data Types
 *
 * The big types in BadGPU are object handles.
 * These all have a unified API for creation and destruction.
 * (See Object Management below.)
 *
 * There are also a few simple data types, listed below.
 */

/*
 * ### `BADGPUBool`
 *
 * Chosen for JNI compatibility.
 *
 * 1 is true and 0 is false.
 *
 * With the exception of `badgpuUnref`, 1 is success, 0 is failure.
 */
typedef unsigned char BADGPUBool;

/*
 * ### `BADGPUVector`
 *
 * A 4-dimensional float vector, used for matrices and vertex data.
 */
typedef struct BADGPUVector {
    float x, y, z, w;
} BADGPUVector;

/*
 * ### `BADGPUMatrix`
 *
 * A "transposed" matrix as per `glLoadMatrix`.
 *
 * They are represented as a set of basis vectors.
 *
 * Each basis vector is multiplied by the corresponding input vector float.
 *
 * The multiplied vectors are then added together.
 */
typedef struct BADGPUMatrix {
    BADGPUVector x, y, z, w;
} BADGPUMatrix;

/*
 * ## Coordinate System, Rasterization, Etc.
 *
 * All BadGPU rules are reflections of underlying OpenGL rules. \
 * However, there are some elements worth noting to prevent confusion:
 *
 * 1. OpenGL textures, and therefore BadGPU textures, are "officially" submitted
 *     bottom-to-top. However, by the same metric, texture coordinate T=0 is
 *     the "bottom" of the texture. In practice, sending textures top-to-bottom
 *     results in T=0 being the top and T=1 being the bottom, which is easier to
 *     understand from a 2D developer's perspective. The orientation of your
 *     textures is therefore a matter of personal opinion.
 * 2. OpenGL window coordinates work the same way, and they map to texture
 *     coordinates via the expression `(w + 1) / 2`.
 * 3. An interesting property of this is that your application can choose Y
 *     orientation, as long as it is consistent about it and doesn't have to
 *     present any of the unflipped results to external WSI.
 */

/*
 * ## Object Management
 *
 * All BadGPU-generated handles are BadGPU objects, manipulatable with the
 *  `badgpuRef` and `badgpuUnref` functions.
 *
 * BadGPU objects start out with a single reference.
 * This reference is the one being returned from the function creating it.
 *
 * All BadGPU objects have an _owning instance_. The owning instance of an
 *  instance is itself. The owning instance must be bound when performing any
 *  interaction with an object, including referencing or unreferencing.
 *
 * Rationale: Providing a single point of reference management for BadGPU
 *  objects makes the job of wrappers and so forth easier. In addition, this
 *  also adds memory safety.
 *
 * BadGPU objects hold references to things they depend on (the instance), so
 *  instances will not be deleted until their resources are deleted.
 *
 * This ensures handles can't end up in a "stuck" state where they can't be
 *  deleted because their instance no longer exists, which could cause
 *  unexpected segmentation faults in wrappers that manage resources using GCs.
 *
 * The requirement that the instance must be bound is a reflection of
 *  limitations in underlying graphics APIs.
 */

/*
 * ### `BADGPUObject`
 *
 * Generic object handle.
 */
typedef struct BADGPUObject * BADGPUObject;

/*
 * ### `badgpuRef`
 *
 * References a BadGPU object.
 *
 * Does nothing if given `NULL`.
 *
 * Returns what it was given.
 */
BADGPU_EXPORT BADGPUObject badgpuRef(BADGPUObject obj);

/*
 * ### `badgpuUnref`
 *
 * Unreferences a BadGPU object.
 *
 * Returns 1 if the object was completely removed, otherwise 0.
 *
 * Otherwise, hanging references presumably still exist.
 *
 * Rationale: Indicating when an object is completely removed is of use for
 *  debugging purposes. It should not be used by wrappers for memory safety, as
 *  objects may be unreferenced during the destruction of other objects, and
 *  these will go unreported.
 */
BADGPU_EXPORT BADGPUBool badgpuUnref(BADGPUObject obj);

/*
 * ## Instances
 *
 * BadGPU is split into instances.
 *
 * There is no inter-instance resource sharing.
 *
 * Instances must only be used from one thread at a time, and they must be
 *  bound and unbound to a thread by the user using the `badgpuBindInstance` and
 *  `badgpuUnbindInstance` functions.
 *
 * _A word of warning: Moving instances between threads is tempting driver
 *   bugs._
 *
 * Rationale: Cross-compilation to many platforms,
 *  particularly macOS and Windows, makes ensuring a ready supply of things
 *  like threading primitives hard.
 *
 * As BadGPU doesn't have any support for external surfaces anyway, relying on
 *  host memory to carry final framebuffers to their destinations, it seems
 *  appropriate to simply provide a guarantee of inter-instance isolation.
 *
 * If CPU work expended on BadGPU and GL processing becomes a severe issue, or
 *  simply for convenience or safety purposes, users may choose to layer a
 *  multi-threading model on top of BadGPU, i.e. one based around queues.
 *
 * If two tasks are sufficiently isolated that surface transfers are rare, then
 *  a model can be created where a texture is copied through host memory to
 *  provide it on another instance. This functionality is not directly provided
 *  by BadGPU, because it is more efficient to implement it on a queue system
 *  as described earlier.
 *
 * Another problem is that APIs such as EGL do not allow a context to be bound
 *  to more than one thread at a time, and a further problem is that all of the
 *  necessary poking to bind contexts incurs overhead and potentially could
 *  upset other libraries using these APIs.
 */

// Instance of BadGPU. The root object, in a sense.
typedef BADGPUObject BADGPUInstance;

/*
 * ### `BADGPUNewInstanceFlags`
 *
 * Rationale: Due to cross-compilation limitations, the current reference
 *  implementation of BadGPU outputs to standard output, and for some
 *  applications, this will be unacceptable behaviour.
 *
 * In addition, GL error tracking arguably adds unnecessary overhead. This isn't
 *  as big of a concern to BadGPU, but given there's nowhere to put the errors,
 *  it's relatively okay to disable it.
 *
 * Finally, aggressive checking of errors runs the risk of errors derailing an
 *  otherwise functioning application.
 */
typedef enum BADGPUNewInstanceFlags {
    // If true, BADGPU is allowed to use printf to report errors and so forth.
    // (Errors that leave the function with no instance won't be reported.)
    // Note that printf is substituted with Android logging functions there.
    BADGPUNewInstanceFlags_CanPrintf = 1,
    // Check the backend (glGetError, etc.) after most operations.
    // Use with CanPrintf or BackendCheckAggressive or both.
    BADGPUNewInstanceFlags_BackendCheck = 2,
    // Allows BackendCheck to outright halt operations.
    BADGPUNewInstanceFlags_BackendCheckAggressive = 4,
    BADGPUNewInstanceFlags_Force32 = 0x7FFFFFFF
} BADGPUNewInstanceFlags;

/*
 * ### `badgpuNewInstance`
 *
 * Creates a new BadGPU instance and binds it to the current thread.
 * (See `badgpuBindInstance` and `badgpuUnbindInstance`.)
 *
 * This will allocate resources such as an `EGLDisplay` or `HWND`, so the
 *  instance should be unreferenced when done with.
 *
 * On failure, `NULL` is returned, and if the error pointer is provided, a
 *  constant C string pointer is placed there.
 * (On success, the error pointer is not updated.)
 * This constant C string, being a constant, is thread-safe, and cannot be
 *  invalidated.
 *
 * The flags are `BADGPUNewInstanceFlags`.
 *
 * Notably, there is an alternate version of this function in Integration.
 * The alternate version allows specifying a custom WSI backend.
 */
BADGPU_EXPORT BADGPUInstance badgpuNewInstance(uint32_t flags, const char ** error);

/*
 * ### `BADGPUMetaInfoType`
 *
 * (Values deliberately match `glGetString`.
 * The implementation may make use of such.
 * Users should not abuse this.)
 */
typedef enum BADGPUMetaInfoType {
    // GL_VENDOR
    BADGPUMetaInfoType_Vendor = 0x1F00,
    // GL_RENDERER
    BADGPUMetaInfoType_Renderer = 0x1F01,
    // GL_VERSION
    BADGPUMetaInfoType_Version = 0x1F02,
    BADGPUMetaInfoType_Force32 = 0x7FFFFFFF
} BADGPUMetaInfoType;

/*
 * ### `badgpuGetMetaInfo`
 *
 * Returns a string describing an aspect of the instance, or `NULL` on error.
 *
 * This string's lifetime is only certain until the next call involving the
 *  instance or any related object.
 */
BADGPU_EXPORT const char * badgpuGetMetaInfo(BADGPUInstance instance,
    BADGPUMetaInfoType mi);

/*
 * ### `badgpuBindInstance`
 *
 * Binds a BadGPU instance to the current thread.
 *
 * Returns 1 on success, 0 on failure.
 *
 * The instance must not be bound to more than one thread at a time.
 *
 * Failure is returned if the instance is detected to already be bound
 *  somewhere, including on the current thread.
 *
 * This detection is not absolute and should not be treated as a replacement for
 *  proper thread synchronization.
 *
 * Rationale: Some graphics APIs ensure thread safety by preventing the binding
 *  of a context in more than one thread. Therefore, explicit unbinding is
 *  essentially mandatory, and therefore explicit binding is also mandatory.
 */
BADGPU_EXPORT BADGPUBool badgpuBindInstance(BADGPUInstance instance);

/*
 * ### `badgpuUnbindInstance`
 *
 * Unbinds a BadGPU instance from the current thread.
 *
 * Note that an instance shouldn't be unbound before being finally unreferenced.
 *
 * In fact, the instance being bound when being unreferenced is required.
 */
BADGPU_EXPORT void badgpuUnbindInstance(BADGPUInstance instance);

/*
 * ### `badgpuFlushInstance`
 *
 * Ensures the GPU is executing sent draw commands.
 *
 * There is never a situation where you must run this; it is only a performance
 *  optimization tool.
 *
 * Rationale: GL drivers usually avoid stalling by displaying frames with a
 *  delay. However, BadGPU requires the user to implement their own WSI via
 *  CPU memory. This means pixels must be retrieved, and that causes stalling.
 *
 * A way to ensure commands are being executed on the GPU is to perform a
 *  `glFlush`, and then come back around later after spending some time to
 *  handle CPU-side tasks. This exposes that functionality.
 */
BADGPU_EXPORT void badgpuFlushInstance(BADGPUInstance instance);

/*
 * ### `badgpuFinishInstance`
 *
 * Exactly equivalent to `glFinish`.
 *
 * Rationale: This is useful for timing debugging.
 */
BADGPU_EXPORT void badgpuFinishInstance(BADGPUInstance instance);

/*
 * ## Texture Conversion Engine
 */

/*
 * ### `BADGPUTextureLoadFormat`
 *
 * This is just the format used for uploading/downloading the texture.
 * BADGPU will internally convert these as necessary.
 * With that in mind, only two formats are blessed with "no conversion",
 *  depending on circumstance.
 */
typedef enum BADGPUTextureLoadFormat {
    // RGBA as individual bytes. No conversion for textures with alpha.
    BADGPUTextureLoadFormat_RGBA8888 = 0,
    // RGB as individual bytes. No conversion for textures without alpha.
    BADGPUTextureLoadFormat_RGB888 = 1,
    // ARGB as a 32-bit integer.
    BADGPUTextureLoadFormat_ARGBI32 = 2,
    BADGPUTextureLoadFormat_Force32 = 0x7FFFFFFF
} BADGPUTextureLoadFormat;

/*
 * ### `badgpuPixelsConvert`
 *
 * Convert pixels between texture load formats.
 *
 * Specifically from `fD` in format `fF` to `tD` in format `tF`.
 *
 * Any conversion pair is possible, though some are more efficient than others.
 *
 * (In particular, specifying the same source/destination formats is silly.)
 *
 * This function cannot convert in-place, so don't try it.
 *
 * The main advantage of this functionality is that the compiler may be able
 *  to vectorize better than one written in a JIT'd language.
 *
 * This is a library function and thus does not need an instance.
 */
BADGPU_EXPORT void badgpuPixelsConvert(BADGPUTextureLoadFormat fF,
    BADGPUTextureLoadFormat tF, int16_t width, int16_t height, const void * fD,
    void * tD);

/*
 * ### `badgpuPixelsConvertRGBA8888ToARGBI32InPlace`
 *
 * A dedicated function to convert `RGBA8888` to `ARGBI32` in-place.
 *
 * This has particular optimization effort put towards it that is not evident
 *  in the general conversion functions.
 *
 * This function is used internally to accelerate `badgpuReadPixels` when this
 *  format is selected.
 *
 * This is a library function and thus does not need an instance.
 */
BADGPU_EXPORT void badgpuPixelsConvertRGBA8888ToARGBI32InPlace(int16_t width,
    int16_t height, void * data);

/*
 * ### `badgpuPixelsSize`
 *
 * Get the size of a texture in a given `BADGPUTextureLoadFormat` in bytes.
 *
 * Returns 0 on error.
 *
 * This is a library function and thus does not need an instance.
 *
 * Rationale: The relation between `width`, `height` and the return value is
 *  such that an overflow is impossible on any hardware. By comparison, using
 *  a "standard" type like `size_t` would lead to overflows on 32-bit hardware.
 */
BADGPU_EXPORT uint32_t badgpuPixelsSize(BADGPUTextureLoadFormat format,
    int16_t width, int16_t height);


/*
 * ## Texture/2D Buffer Management
 *
 * BadGPU has two kinds of image.
 *
 * Textures are fixed-size, always read/write images.
 *
 * After creation, they can be written to by using them as framebuffers, and
 *  drawing to them.
 *
 * `DSBuffer`s are depth / stencil buffers. They cannot be directly read or
 *  written, but they can be used in drawing commands.
 *
 * Rationale: `OES_depth_texture` isn't ubiquitous.
 *
 * In fact, even on Vulkan-class hardware, I can't get it on Mesa in
 *  GLES-CM 1.1 mode. (Can from GLES2, though.)
 *
 * This, and `OES_packed_depth_stencil`, in mind, bundling depth / stencil into
 *  a single object that is treated as something of a black box simplifies the
 *  API for no real downside.
 *
 * All defined texture types in OpenGL ES 1.1 are renderable.
 *
 * This in mind, insisting all textures can be used as framebuffers allows for
 *  a very general, easy to use API, rather than trying to specify which
 *  specific sub-type of texture you can or can't render to.
 */

/*
 * ### `BADGPUTexture`
 *
 * A 2D image with possible mipmaps.
 */
typedef BADGPUObject BADGPUTexture;

/*
 * ### `badgpuNewTexture`
 *
 * Creates a texture, with possible initial data.
 *
 * Returns `NULL` on failure, otherwise the new texture.
 *
 * The `width` and `height` must both be above 0. Unfortunately, it is not
 *  practical to specify one exact maximum limit that will always work.
 *
 * Mipmaps are not automatically created. This must be done using
 *  `badgpuGenerateMipmap` if the texture is to be used with mipmaps.
 *
 * Data can be supplied in various formats; see `fmt`. \
 * Notably, this format value only affects the format you supply to load it. \
 * It does not affect the format the GPU stores the texture in. \
 * (This means that `fmt` is ignored if `data` is NULL.)
 *
 * All textures are RGBA8888. This is because RGB888 is unoptimized in practice.
 *
 * If `NULL` is passed as the data, then the texture contents are undefined.
 *
 * Rationale: `width` / `height` are `int16_t` to ensure that only valid inputs
 *  can be given, and to ensure that overflows cannot occur when calculating the
 *  required buffer size. Besides, GL implementations don't even reach these
 *  limits before giving up.
 *
 * 1/2-component texture formats are not allowed due to the
 *  requirements of `EXT_framebuffer_object` 4.4.4 Framebuffer Completeness,
 *  which does not define other formats as renderable.
 *
 * Testing also revealed that uploading at least `GL_LUMA` as RGB / RGBA did
 *  not work. Consider this something of a cut for deadline kind of deal...
 *
 * Further testing also revealed that you get free performance by NOT, repeat,
 *  NOT allocating RGB888 buffers. As sad as that is, this is why they've been
 *  removed.
 */
BADGPU_EXPORT BADGPUTexture badgpuNewTexture(BADGPUInstance instance,
    int16_t width, int16_t height, BADGPUTextureLoadFormat fmt,
    const void * data);

/*
 * ### `BADGPUDSBuffer`
 *
 * A depth / stencil buffer.
 */
typedef BADGPUObject BADGPUDSBuffer;

/*
 * ### `badgpuNewDSBuffer`
 *
 * Creates a depth / stencil buffer. \
 * These are used for drawing... and that's about it.
 *
 * Returns `NULL` on failure, otherwise the new `DSBuffer`.
 *
 * The `width` and `height` must both be above 0. Unfortunately, it is not
 *  practical to specify one exact maximum limit that will always work.
 *
 * Rationale: While `OES_packed_depth_stencil` seems essentially ubiquitous,
 *  it may not turn out so. Furthermore, `OES_depth_texture` is _not_
 *  ubiquitous.
 *
 * In fact, even on Vulkan-class hardware, I can't get it on Mesa in
 *  GLES-CM 1.1 mode. (Can from GLES2, though.)
 *
 * That in mind, there are no benefits to treating these as textures.
 *
 * At the same time, creating a dedicated renderbuffer API seems wasteful;
 *  BadGPU does not have any use for an RGBA renderbuffer.
 *
 * Abstracting the depth / stencil buffer into a single object that is managed
 *  by BadGPU isolates applications from changes to the reference
 *  implementation, and keeps the API simple. `OES_packed_depth_stencil` may be
 *  used to implement this or may not.
 */
BADGPU_EXPORT BADGPUDSBuffer badgpuNewDSBuffer(BADGPUInstance instance,
    int16_t width, int16_t height);

/*
 * ### `badgpuGenerateMipmap`
 *
 * Returns 1 on success, 0 on failure.
 *
 * Regenerates mipmaps for a texture. \
 * This must be done explicitly after a texture is rendered to, or when textures
 *  are created with data, if that texture will be used with mipmapping. \
 * It should not be done if mipmaps are not used by the texture.
 *
 * Rationale: The `EXT_framebuffer_object` document goes into great detail about
 *  the rationale on the GL end. On BadGPU's end, deferring mipmap generation
 *  until the moment of use sounds bad for performance. The application knows
 *  when (or if!) it wants mipmaps to be regenerated.
 *
 * The ability to specify mipmaps manually was not included, as it
 *  disincentivizes use of formats that are properly compressed.
 */
BADGPU_EXPORT BADGPUBool badgpuGenerateMipmap(BADGPUTexture texture);

/*
 * ### `badgpuReadPixels`
 *
 * Reads pixels back from a texture.
 *
 * Returns 1 on success, 0 on failure.
 *
 * Pixels are read back in the format set by `fmt` (CPU-side conversion is done
 *  if necessary).
 *
 * Rationale: RGBA8888 is all `glReadPixels` provides for GLES 1.1, but now we
 *  have conversion functions so that, say, Java APIs can be made happier.
 *
 * If `width` or `height` is 0, the function succeeds (doing nothing) regardless
 *  of any other parameters, while if they are below 0, the function fails.
 *
 * Otherwise, fails if `data` or `texture` is `NULL`, or various other issues
 *  occur.
 */
BADGPU_EXPORT BADGPUBool badgpuReadPixels(BADGPUTexture texture,
    uint16_t x, uint16_t y, int16_t width, int16_t height,
    BADGPUTextureLoadFormat fmt, void * data);

/*
 * ## Drawing Commands
 *
 * The following rule applies to all functions in this section:
 * The texture and depth / stencil buffers, if both present,
 *  must be the same size and instance.
 *
 * Drawing commands have the same first parameters, known as the session
 *  parameters, prefixed with 's'.
 * They are controlled by `sFlags`, which are `BADGPUSessionFlags`.
 *
 * It is important to keep in mind that the default state of the masks is
 *  NOT to render things. Normal operation might use `MaskAll` or `MaskRGBAD`.
 *
 * Rationale: These parameters are all those documented as common between
 *  clear and drawing commands. See ES 2.0: 4.2.3 Clearing the Buffers,
 *  same place in ES 1.1.
 *
 * A struct tends to lead to difficulties for wrappers, which must
 *  choose if they want to preserve the struct (performance hazard), or
 *  decompose it themselves. A handle would be inappropriate for data that is
 *  this dynamic. As such, keeping these as arguments is the best choice.
 *
 * FBOs are removed as a concept because they are awkward, and not really
 *  meaningful except as a performance crutch for API validation by OpenGL.
 *
 * FBO caching could be transparently implemented if necessary.
 */

#define BADGPU_SESSIONFLAGS \
    BADGPUTexture sTexture, BADGPUDSBuffer sDSBuffer, \
    uint32_t sFlags, \
    int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight

/*
 * ### `BADGPUSessionFlags`
 */
typedef enum BADGPUSessionFlags {
    // Masks
    // Importantly, these are needed to enable any meaningful rendering!
    // Stencil. These flags are deliberately at the bottom.
    // This allows ORing in an 8-bit stencil mask directly,
    //  and retrieving as such.
    BADGPUSessionFlags_StencilAll = 0x00FF,
    BADGPUSessionFlags_Stencil0 = 0x0001,
    BADGPUSessionFlags_Stencil1 = 0x0002,
    BADGPUSessionFlags_Stencil2 = 0x0004,
    BADGPUSessionFlags_Stencil3 = 0x0008,
    BADGPUSessionFlags_Stencil4 = 0x0010,
    BADGPUSessionFlags_Stencil5 = 0x0020,
    BADGPUSessionFlags_Stencil6 = 0x0040,
    BADGPUSessionFlags_Stencil7 = 0x0080,
    BADGPUSessionFlags_MaskR = 0x0100,
    BADGPUSessionFlags_MaskG = 0x0200,
    BADGPUSessionFlags_MaskB = 0x0400,
    BADGPUSessionFlags_MaskA = 0x0800,
    BADGPUSessionFlags_MaskDepth = 0x1000,
    BADGPUSessionFlags_MaskRGBA = 0x0F00,
    BADGPUSessionFlags_MaskRGBAD = 0x1F00,
    BADGPUSessionFlags_MaskAll = 0x1FFF,
    // Scissor enable
    BADGPUSessionFlags_Scissor = 0x2000,
    BADGPUSessionFlags_Force32 = 0x7FFFFFFF
} BADGPUSessionFlags;

/*
 * ### `badgpuDrawClear`
 *
 * Performs a clear.
 *
 * Returns 1 on success, 0 on failure.
 *
 * The session flag masks control the clear. \
 * Either buffer may or may not be present, but at least one buffer should be. \
 * (Failure to provide any buffers at all is a wasteful NOP.)
 *
 * Rationale: Separate clear flags aren't necessary when the masks exist. \
 *  The reference implementation chooses to still translate these into buffer \
 *  bits in case of potential GL-level optimizations.
 */
BADGPU_EXPORT BADGPUBool badgpuDrawClear(
    BADGPU_SESSIONFLAGS,
    float cR, float cG, float cB, float cA, float depth, uint8_t stencil
);

/*
 * ### `BADGPUDrawFlags`
 */
typedef enum BADGPUDrawFlags {
    // Changes the definition of a front face to be in reverse.
    BADGPUDrawFlags_FrontFaceCW = 1,
    // Enables face culling.
    BADGPUDrawFlags_CullFace = 2,
    // Changes culling to cull the front face rather than the back.
    BADGPUDrawFlags_CullFaceFront = 4,
    BADGPUDrawFlags_StencilTest = 8,
    // Flag 16 unused
    BADGPUDrawFlags_Blend = 32,
    // Flag 64 still unused
    // Colour array only has to be one element long, and that's the only colour.
    BADGPUDrawFlags_FreezeColor = 128,
    BADGPUDrawFlags_FreezeColour = 128,
    // TC array only has to be one element long, and that's the only TC.
    BADGPUDrawFlags_FreezeTC = 256,
    // If minifying the texture uses linear filtering.
    BADGPUDrawFlags_MinLinear = 512,
    // If magnifying the texture uses linear filtering.
    BADGPUDrawFlags_MagLinear = 1024,
    // If mipmapping is used.
    BADGPUDrawFlags_Mipmap = 2048,
    // If accesses beyond the edges of the texture repeat (rather than clamping)
    BADGPUDrawFlags_WrapS = 4096,
    BADGPUDrawFlags_WrapT = 8192,
    BADGPUDrawFlags_Force32 = 0x7FFFFFFF
} BADGPUDrawFlags;

/*
 * ### `BADGPUPrimitiveType`
 *
 * (Values deliberately match `glBegin` etc.
 *  The implementation may make use of such.
 *  Users should not abuse this.)
 *
 * (Rationale for not including loop / strip / fan primitive types:
 *  These only really work properly with primitive restart, and `glDrawElements`
 *   makes them pretty close to obsolete anyway. Between that and the 65536
 *   vertex index limit, there's really no good reason to use these.)
 */
typedef enum BADGPUPrimitiveType {
    // GL_POINTS
    BADGPUPrimitiveType_Points = 0x0000,
    // GL_LINES
    BADGPUPrimitiveType_Lines = 0x0001,
    // GL_TRIANGLES
    BADGPUPrimitiveType_Triangles = 0x0004,
    BADGPUPrimitiveType_Force32 = 0x7FFFFFFF
} BADGPUPrimitiveType;

/*
 * ### `BADGPUCompare`
 *
 * (Values deliberately match `glDepthFunc` etc.
 *  The implementation may make use of such.
 *  Users should not abuse this.)
 */
typedef enum BADGPUCompare {
    BADGPUCompare_Never = 0x0200,
    BADGPUCompare_Always = 0x0207,
    BADGPUCompare_Less = 0x0201,
    BADGPUCompare_LEqual = 0x0203,
    BADGPUCompare_Equal = 0x0202,
    BADGPUCompare_Greater = 0x0204,
    BADGPUCompare_GEqual = 0x0206,
    BADGPUCompare_NotEqual = 0x0205,
    BADGPUCompare_Force32 = 0x7FFFFFFF
} BADGPUCompare;

/*
 * ### `BADGPUStencilOp`
 *
 * (Values deliberately match `glStencilOp`.
 *  The implementation may make use of such.
 *  Users should not abuse this.)
 */
typedef enum BADGPUStencilOp {
    // GL_KEEP
    BADGPUStencilOp_Keep = 0x1E00,
    // GL_ZERO
    BADGPUStencilOp_Zero = 0,
    // GL_REPLACE
    BADGPUStencilOp_Replace = 0x1E01,
    // GL_INCR
    BADGPUStencilOp_Inc = 0x1E02,
    // GL_DECR
    BADGPUStencilOp_Dec = 0x1E03,
    // GL_INVERT
    BADGPUStencilOp_Invert = 0x150A,
    BADGPUStencilOp_Force32 = 0x7FFFFFFF
} BADGPUStencilOp;

/*
 * ### `BADGPUBlendOp`
 *
 * These are 3-bit. See `BADGPU_BLEND_PROGRAM`.
 */
typedef enum BADGPUBlendOp {
    // GL_FUNC_ADD: S + D
    BADGPUBlendOp_Add = 0,
    // GL_FUNC_SUBTRACT: S - D
    BADGPUBlendOp_Sub = 1,
    // GL_FUNC_REVERSE_SUBTRACT: D - S
    BADGPUBlendOp_ReverseSub = 2,
    BADGPUBlendOp_Force32 = 0x7FFFFFFF
} BADGPUBlendOp;

/*
 * ### `BADGPUBlendWeight`
 *
 * These are 6-bit. See `BADGPU_BLEND_PROGRAM`.
 *
 * Rationale: The first digit specifies the source.
 *
 * + 0: Special
 * + 3: Destination
 * + 5: Source
 *
 * The second digit specifies the operation:
 *
 * + 0: Channel
 * + 1: Invert Channel
 * + 2: Alpha
 * + 3: Invert Alpha
 *
 * Note however that these can't be mixed and matched arbitrarily.
 */
typedef enum BADGPUBlendWeight {
    // GL_ZERO: 0
    BADGPUBlendWeight_Zero =       000,
    // GL_ONE: 1
    BADGPUBlendWeight_One =        001,
    // GL_SRC_ALPHA_SATURATE: min(Sa, 1 - Da)
    //  except for A output where it's just 1.
    BADGPUBlendWeight_SrcAlphaSaturate = 002,
    // GL_DST_COLOR: Dc
    BADGPUBlendWeight_Dst =        030,
    // GL_ONE_MINUS_DST_COLOR: 1 - Dc
    BADGPUBlendWeight_InvertDst =  031,
    // GL_DST_ALPHA: Da
    BADGPUBlendWeight_DstA =       032,
    // GL_ONE_MINUS_DST_ALPHA: 1 - Da
    BADGPUBlendWeight_InvertDstA = 033,
    // GL_SRC_COLOR: Sc
    BADGPUBlendWeight_Src =        050,
    // GL_ONE_MINUS_SRC_COLOR: 1 - Sc
    BADGPUBlendWeight_InvertSrc =  051,
    // GL_SRC_ALPHA: Sa
    BADGPUBlendWeight_SrcA =       052,
    // GL_ONE_MINUS_SRC_ALPHA: 1 - Sa
    BADGPUBlendWeight_InvertSrcA = 053,
    BADGPUBlendWeight_Force32 = 0x7FFFFFFF
} BADGPUBlendWeight;

/*
 * ### `BADGPU_BLEND_EQUATION`
 *
 * When blending is enabled, blend equations are run for each channel. \
 * These have two inputs: The source pixel colour (after texturing etc.), \
 *  and the destination pixel colour (i.e. what's on the framebuffer).
 *
 * Blend equations mostly treat all channels as isolated, *except* that the
 *  alpha channel can be read in particular. As such, when the "colour channel"
 *  is referred to, that refers to the specific channel being processed. This
 *  does mean that for the alpha channel, the alpha/non-alpha weights are
 *  equivalent.
 *
 * The results of all blend equations are then (atomically, as far as the
 *  blending unit is concerned) written to the framebuffer.
 *
 * Blend equations are made up of three components, two blend weights
 *  (multiplied with the source and destination respectively) and one blend
 *  operation (which combines the two multiplied values).
 *
 * `bwS` and `bwD` are the source and destination `BADGPUBlendWeight` values,
 *  and `be` is the `BADGPUBlendOp` value.
 *
 * In order to reduce argument count, BadGPU represents a whole equation as a
 *  single integer, and can similarly represent the pair of equations required
 *  to program the blending unit as a single integer.
 *
 * For example, oct. `52530` is a decent equation if dstA isn't an issue.
 *
 * Fans of premultiplied alpha might prefer a different equation, but beware:
 *  vertex colours need to be appropriately adjusted to use premultiplied alpha
 *  in BadGPU.
 *
 * An equation such as oct. `01530` may be of use for that.
 */
#define BADGPU_BLEND_EQUATION(bwS, bwD, be) (((bwS) << 9) | ((bwD) << 3) | (be))

/*
 * ### `BADGPU_BLEND_PROGRAM`
 *
 * Given two blend equations (see `BADGPU_BLEND_EQUATION`), creates a program.
 *
 * `eqRGB` is the blend equation used for the R, G, and B channels.
 * `eqA` is the blend equation used for the A channel.
 *
 * This embeds the entire configuration of the blending unit into one integer.
 *
 * Rationale: This format is designed to allow embedding blend programs into
 *  source code, reading them (as octal), and reducing argument count.
 *
 * In particular, blend programs can be calculated at compile or load time,
 *  and then simply passed as a single argument.
 *
 * This is similar to if they were objects, but weighing less, still allowing
 *  for dynamic use, and still allowing runtime poking.
 */
#define BADGPU_BLEND_PROGRAM(eqRGB, eqA) (((equRGB) << 15) | (eqA))

/*
 * ### `badgpuDrawGeom`
 *
 * Performs a drawing command.
 *
 * Returns 1 on success, 0 on failure.
 *
 * The flags provided are `BADGPUDrawFlags`.
 *
 * `vPos` / `vCol` / `vTC` are the vertex data arrays. \
 * `vCol` and `vTC` can be `NULL` to leave that feature at a default. \
 * `vPos` *must not* be `NULL`, or the function will error.
 *
 * `vPosD` and `vTCD` must be between 2 and 4 inclusive, or the function will
 *  error.
 *
 * `vPos`, `vCol` and `vTC` are arrays of float vectors, but specifically:
 *
 * + `vPos` is 2 to 4-dimensional (X, Y, Z, W; defaults for Z, W are 0 and 1). \
 *   This is controlled by `vPosD`.
 * + `vCol` is 4-dimensional (R, G, B, A).
 * + `vTC` is 2 to 4-dimensional (same as vPos). \
 *   This is controlled by `vTCD`.
 *
 * For `vCol` and `vTC`, there are also "freeze flags". \
 * If used in conjunction with the arrays, only the first element of those
 *  arrays is used (allowing for "single-colour uniform" treatment). \
 * (This lookup ignores indices, and frozen arrays can be just that element.)
 *
 * `iStart` and `iCount` represent a range in the indices array, which in turn
 *  are indices into the vertex arrays. \
 * If `indices` is `NULL`, then it is essentially as if an array was passed with
 *  values 0 to 65535. \
 * `iCount` must not be below 0 or above 65536.
 *
 * `mvMatrix` can be `NULL`. In this case, it is effectively identity. \
 * Otherwise, see `BADGPUMatrix`. \
 * It is worth noting that this is formally the `GL_MODELVIEW` matrix by OpenGL
 *  rules, which changes how it interacts with other features; mainly it ensures
 *  that this matrix counts as a modification to the input vertices.
 *
 * `vX`, `vY`, `vW`, `vH` make up the viewport.
 *
 * `texture` is multiplied with the vertex colours. \
 * (If `NULL`, then the vertex colours are used as-is.) \
 * The texture coordinates are multiplied with the texture coordinate matrix.
 *
 * `clipPlane` specifies a clip plane, or can be `NULL` to disable it. \
 * Clip planes are in the space after transformation by the matrix, but before
 *  the perspective divide.
 *
 * `atFunc` and `atRef` specify the alpha test.
 *
 * `stFunc`, `stRef`, and `stMask` are used for the stencil test. \
 * This `stMask` is for the test; the mask used for writing is the session's
 *  stencil mask. However, no writing occurs if the test isn't enabled in the
 *  draw flags.
 *
 * `stSF`, `stDF`, and `stDP` control what happens to the stencil buffer under
 *  various situations with the stencil test.
 *
 * `dtFunc` is used for the depth test. \
 * `depthN`, `depthF` make up the depth range (0, 1 is a good default). \
 * `poFactor`, `poUnits` make up the polygon offset.
 *
 * `blendProgram` is as prepared by `BADGPU_BLEND_PROGRAM`.
 *
 * Rationale: While this function is indeed absolutely massive, there are
 *  shorter wrappers such as badgpuDrawGeomNoDS. This is also arguably a natural
 *  cost of the avoidance of stack structs while also avoiding a stateful API.
 *
 * Some specific included functionality, and why:
 *
 * + 4D vertex support had to be included for software TnL.
 * + The ability to change vertex and TC dimension counts is important because
 *    the amount of memory used by batches can get rather high, and associated
 *    vertex loading costs can also get rather high.
 *
 * Some specific missing functionality, and why:
 *
 * + Lighting isn't a thing for several reasons:
 *   + It's annoying for ES2.
 *   + It's per-vertex, so you don't really get anything out of it. \
 *     If you really want this, do it on the CPU or something.
 *   + Trusting mobile GPU vendors to implement that convoluted mess correctly
 *      is certainly a test of faith... Not one I'd take.
 * + Specifying integer vertex/TCs is more trouble than it is worth.
 *   There may be merit to specifying colours as RGBA bytes, but it would make
 *    some pretty useful stuff have to go onto a slowpath if actually used.
 * + The projection matrix was removed because the driver simply does the work
 *    on-CPU these days anyway. It doesn't even bother to ask if the work really
 *    has to be done on that HW, it's all done in the state tracker. \
 *   The modelview matrix was chosen as the surviving one, as it's a natural
 *    extension of the vertices, and the clip plane would act weird otherwise.
 * + Flat-shading makes no sense without lighting.
 * + The `MULTISAMPLE` and `POINT_SPRITE_OES` enables are a mess.
 * + Logic ops aren't in ES2.
 * + Dithering is left at whatever state the GL leaves it in, which should be
 *    enabled according to specification. There's no reason anyone would ever
 *    want to disable it, especially given hardware and driver variance exists.
 */
BADGPU_EXPORT BADGPUBool badgpuDrawGeom(
    BADGPU_SESSIONFLAGS,
    uint32_t flags,
    // Vertex Loader
    int32_t vPosD, const float * vPos,
    const float * vCol,
    int32_t vTCD, const float * vTC,
    BADGPUPrimitiveType pType, float plSize,
    uint32_t iStart, uint32_t iCount, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * mvMatrix,
    // Viewport
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    // Fragment Shader
    BADGPUTexture texture, const BADGPUMatrix * matrixT,
    const float * clipPlane, BADGPUCompare atFunc, float atRef,
    // Stencil Test
    BADGPUCompare stFunc, uint8_t stRef, uint8_t stMask,
    BADGPUStencilOp stSF, BADGPUStencilOp stDF, BADGPUStencilOp stDP,
    // Depth Test / DepthRange / PolygonOffset
    BADGPUCompare dtFunc, float depthN, float depthF, float poFactor, float poUnits,
    // Blending
    uint32_t blendProgram
);

/*
 * ### `badgpuDrawGeomNoDS`
 *
 * Alias for badgpuDrawGeom that removes depth / stencil parameters.
 *
 * Most removed values are 0 / `NULL`, except:
 *
 * + All stencil ops are `Keep`.
 * + `stFunc` / `dtFunc` are both `Always`.
 *
 * Rationale: The depth / stencil buffer is naturally linked with 3D drawing, or
 *  at least advanced drawing. Some applications mostly consist of 2D drawing,
 *  or otherwise don't need the buffer due to Z-sorting/etc.
 *
 * In these cases, some overhead can be shaved, particularly in wrappers, by
 *  using a version of the function that strips out arguments that are not
 *  relevant to the `DSBuffer`.
 *
 * The default values are set such that the relevant units are kept disabled,
 *  even if the flags are set to enable those units, to prevent accidents.
 *
 * In practice, ES1.1 4.1.5 Stencil Test and 4.1.6 Depth Buffer Test specify
 *  that the lack of a stencil and depth buffer make those tests always pass,
 *  and disables stencil modification. With that in mind, sDSBuffer being `NULL`
 *  is enough to optimize away the state wrangling.
 */
BADGPU_EXPORT BADGPUBool badgpuDrawGeomNoDS(
    BADGPUTexture sTexture,
    uint32_t sFlags,
    int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight,
    uint32_t flags,
    // Vertex Loader
    int32_t vPosD, const float * vPos,
    const float * vCol,
    int32_t vTCD, const float * vTC,
    BADGPUPrimitiveType pType, float plSize,
    uint32_t iStart, uint32_t iCount, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * mvMatrix,
    // Viewport
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    // Fragment Shader
    BADGPUTexture texture, const BADGPUMatrix * matrixT,
    const float * clipPlane, BADGPUCompare atFunc, float atRef,
    // Blending
    uint32_t blendProgram
);

#undef BADGPU_SESSIONFLAGS

/*
 * ## Integration
 *
 * BadGPU avoids WSI. \
 * It tends to be hard to port, particularly to Mac. \
 * However, in the case of Android, the performance problems of not having it
 *  got so bad that something had to be done.
 *
 * That in mind, the following functions allow access to certain values in
 *  BadGPU. Obviously, this leads to platform-specific code.
 *
 * This allows just enough flexibility to build a WSI layer... if you have to.
 */

/*
 * ### `badgpuResetGLState`
 *
 * Resets GL state to try and keep interacting code consistent.
 *
 * This acts like a drawing command, but only shuffles around internal state.
 *
 * As such it returns 1 on success and 0 on failure.
 */
BADGPU_EXPORT BADGPUBool badgpuResetGLState(BADGPUInstance instance);

/*
 * ### `BADGPUWSIType`
 *
 * Rationale: The WSI layer has different implementations depending on platform.
 */
typedef enum BADGPUWSIType {
    BADGPUWSIType_Custom =  0x00000000,
    BADGPUWSIType_EGL =     0x00000001,
    BADGPUWSIType_CGL =     0x00000002,
    BADGPUWSIType_WGL =     0x00000003,

    BADGPUWSIType_Force32 = 0x7FFFFFFF
} BADGPUWSIType;

/*
 * ### `BADGPUContextType`
 *
 * Specifies a type of OpenGL context. \
 * This is useful if using BadGPU in conjunction with another source of OpenGL
 *  contexts.
 */
typedef enum BADGPUContextType {
    // OpenGL ES 1(.1+extensions)
    BADGPUContextType_GLESv1 =  0x00000000,
    // OpenGL 1.x or 2.x (with appropriate extensions)
    BADGPUContextType_GL =      0x00000001,
    // OpenGL ES 2 (Not supported in reference implementation)
    BADGPUContextType_GLESv2 =  0x00000002,
    // OpenGL 3.x Core 
    // If at all possible, return BADGPUContextType_GL instead!
    // Even if a device "supports" GL 3.x, it may not really support it.
    // See Intel and to a lesser extent AMD.
    // Not supported in reference implementation anyway.
    BADGPUContextType_GL3Core = 0x00000003,

    BADGPUContextType_Force32 = 0x7FFFFFFF
} BADGPUContextType;

/*
 * ### `BADGPUWSIQuery`
 *
 * Rationale: The WSI layer has various variables that can be retrieved.
 * Sometimes it's necessary to access those for direct poking (i.e. to implement
 *  proper WSI).
 */
typedef enum BADGPUWSIQuery {
    // BADGPUWSIType as void*
    BADGPUWSIQuery_WSIType =         0x00000000,
    // Native handle of libGL or equivalent.
    // Note that this will not necessarily access extension functions.
    BADGPUWSIQuery_LibGL =           0x00000001,
    // BADGPUContextType as void*
    BADGPUWSIQuery_ContextType =     0x00000002,
    // BADGPUWSIContext (for getProcAddress/etc.)
    BADGPUWSIQuery_ContextWrapper =  0x00000003,

    // EGLDisplay
    BADGPUWSIQuery_EGLDisplay =      0x00000100,
    // EGLContext
    BADGPUWSIQuery_EGLContext =      0x00000101,
    // EGLConfig
    BADGPUWSIQuery_EGLConfig =       0x00000102,
    // EGLSurface (of 'false draw surface')
    // Note this isn't equivalent to the EGL calls to get the current surface.
    // This always returns the false draw surface created by the WSI layer.
    BADGPUWSIQuery_EGLSurface =      0x00000103,
    // libEGL native handle
    BADGPUWSIQuery_EGLLibEGL =       0x00000104,

    // Pixel format
    BADGPUWSIQuery_CGLPixFmt =       0x00000200,
    // Context
    BADGPUWSIQuery_CGLContext =      0x00000201,

    // WGL window
    BADGPUWSIQuery_WGLHWND =         0x00000300,
    // WGL HDC
    BADGPUWSIQuery_WGLHDC =          0x00000301,
    // WGL HGLRC
    BADGPUWSIQuery_WGLHGLRC =        0x00000302,

    BADGPUWSIQuery_Force32 =         0x7FFFFFFF
} BADGPUWSIQuery;

/*
 * ### `badgpuGetWSIValue`
 *
 * Returns the requested value, or `NULL` if none.
 */
BADGPU_EXPORT void * badgpuGetWSIValue(BADGPUInstance instance, BADGPUWSIQuery query);

/*
 * ### `badgpuGetGLTexture`
 *
 * Returns the texture ID. Does not have a proper invalid value, so will return
 *  0 if not relevant. Will always be valid if this is a GL-based context.
 */
BADGPU_EXPORT uint32_t badgpuGetGLTexture(BADGPUTexture texture);

/*
 * ### `badgpuNewTextureFromGL`
 *
 * Provides an existing GL texture to BadGPU.
 *
 * This texture will not be deleted by BadGPU.
 *
 * Returns `NULL` on failure, otherwise the new texture.
 */
BADGPU_EXPORT BADGPUTexture badgpuNewTextureFromGL(BADGPUInstance instance, uint32_t tex);

/*
 * ### `BADGPUWSIContext`
 *
 * Structure for a custom WSI handler.
 * This is useful if using BadGPU in conjunction with another source of OpenGL
 *  contexts.
 */
typedef struct BADGPUWSIContext {
    // This field will never be written or read by BadGPU on a provided context.
    void * userdata;
    // Makes this context current. Returns false on failure.
    // (Should a context not require this, always returns true.)
    BADGPUBool (*makeCurrent)(struct BADGPUWSIContext * self);
    // Makes this context not current.
    // (Should a context not require this, this is a NOP.)
    void (*stopCurrent)(struct BADGPUWSIContext * self);
    // Gets a function address. The context must be current!
    // Importantly, this must work for core and extension functions.
    // This matters in the case of, say, WGL.
    void * (*getProcAddress)(struct BADGPUWSIContext * self, const char * proc);
    // Equivalent to badgpuGetWSIValue.
    // Note that BadGPU will call this for any fields which are not specific to
    //  a WSI type but are relevant to the context type, and also to retrieve
    //  the context type itself.
    void * (*getValue)(struct BADGPUWSIContext * self, BADGPUWSIQuery query);
    // Called on the destruction of the instance using the WSI.
    // Failure to create the instance also counts as destruction.
    // The instance will never access this context past this point.
    void (*close)(struct BADGPUWSIContext * self);
} * BADGPUWSIContext;

/*
 * ### `badgpuNewInstanceWithWSI`
 *
 * See `badgpuNewInstance`. \
 * However, a BADGPUWSIContext pointer is passed. \
 * The context (and pointer) must live as long as the instance does.
 *
 * Any failure or the instance's destruction will call `wsi->close`. \
 * Past this call, the context (and pointer) are no longer in use.
 */
BADGPU_EXPORT BADGPUInstance badgpuNewInstanceWithWSI(uint32_t flags, const char ** error, BADGPUWSIContext wsi);

#endif

