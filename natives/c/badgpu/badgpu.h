/*
 * `gabien-common` - Cross-platform game and UI framework \
 * Written starting in 2016 by contributors (see `CREDITS.txt`) \
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty. \
 * A copy of the Unlicense should have been supplied as `COPYING.txt` in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * # BadGPU C Header And API Specification
 *
 * Version: `0.11.1`
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
 *  software in itself.
 *
 * This means essentially all non-comment changes are of _Minor_ severity or
 *  greater.
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
 * + Whenever possible, a single version of this specification must be
 *     resolvable to a single Git commit.
 * + If at all possible, version `1.0.0` will be the last version of
 *    this specification.
 *   + Failing this, it will be the last update to the _Major_ and _Minor_
 *      versions of the specification.
 *     + Failing this, it will be the last update to the _Major_ version of
 *        this specification. Any new drawing capabilities (such as, say, opt-in
 *        support for ES2 shaders) will be implemented as additional calls.
 *       + In the event of a catastrophic failure of future versions, at a bare
 *          minimum, all versions of this specification past `1.0.0` *must*
 *          remain compatible with the last patch version of the `1.0.x` series.
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
 *  at some performance cost (which I consider an acceptable loss given the
 *  reliability and portability benefits of reducing per-platform code), while
 *  still not losing as much performance as software rendering.
 *
 * For portability reasons, BadGPU is designed to target the subset of
 *  functionality common between three separate versions of OpenGL:
 *
 * + OpenGL 1.1 +
 *    `EXT_blend_subtract`, `EXT_blend_func_separate`,
 *    `EXT_framebuffer_object`
 * + OpenGL ES 1.0 Common +
 *    `OES_blend_subtract`, `OES_blend_func_separate`,
 *    `OES_framebuffer_object`, `OES_texture_npot`
 * + OpenGL ES 2.0 with shader compiler and `OES_texture_npot`
 *   + That the shader compiler can be omitted in a valid OpenGL ES 2.0
 *     implementation is an interesting trick, but also means that this
 *     specification can't guarantee the ability to implement, say, the alpha
 *     test (which isn't in baseline OpenGL ES 2.0).
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
 * In terms of API design, BadGPU owes some credit to Vulkan and WebGPU, mostly
 *  the latter, but avoids the heavy use of structs for binding reasons.
 * (The reference BadGPU implementation will have built-in JNI support.)
 *
 * Like WebGPU, to which the API design owes some credit, BadGPU does not
 *  support the reuse of serieses of commands.
 *
 * Unlike WebGPU, BadGPU is an immediate API; it is not possible to cancel a
 *  command, there's no buffer recording.
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
 * ## Object Management
 *
 * All BadGPU-generated handles are BadGPU objects, manipulatable with the
 *  `badgpuRef` and `badgpuUnref` functions.
 *
 * BadGPU objects start out with a single reference.
 * This reference is the one being returned from the function creating it.
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
 * Instances must only be used from one thread at a time.
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
 * Creates a new BadGPU instance.
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
 * ### `BADGPUTextureFlags`
 */
typedef enum BADGPUTextureFlags {
    // If, on the GPU, the texture has alpha.
    BADGPUTextureFlags_HasAlpha = 1,
    BADGPUTextureFlags_Force32 = 0x7FFFFFFF
} BADGPUTextureFlags;

/*
 * ### `BADGPUTextureFormat`
 *
 * Specifies a format of texture for writing to the GPU.
 *
 * Rationale: While BadGPU usually aliases these things to GL enums,
 *  this one's relevant to memory safety, as it changes the expected size of the
 *  data buffer. That in mind, this is something of a safety guard.
 */
typedef enum BADGPUTextureFormat {
    // A -> 111A
    BADGPUTextureFormat_Alpha = 0,
    // L -> LLL1
    BADGPUTextureFormat_Luma = 1,
    // LA -> LLLA
    BADGPUTextureFormat_LumaAlpha = 2,
    // RGB -> RGB1
    BADGPUTextureFormat_RGB = 3,
    // RGBA -> RGBA
    BADGPUTextureFormat_RGBA = 4,
    BADGPUTextureFormat_Force32 = 0x7FFFFFFF
} BADGPUTextureFormat;

/*
 * ### `badgpuNewTexture`
 *
 * Creates a texture, with possible initial data.
 *
 * Returns `NULL` on failure, otherwise the new texture.
 *
 * The flags are `BADGPUNewTextureFlags`.
 *
 * Mipmaps are not automatically created. This must be done using
 *  `badgpuGenerateMipmap` if the texture is to be used with mipmaps.
 *
 * Data can be supplied as a flat array of bytes.
 *
 * The size and layout of each pixel in the array of bytes is specified by
 *  the format.
 *
 * It's important to note that texture formats only specify the format of
 *  the data being provided. It does not alter the actual format on the GPU.
 *
 * Only the `HasAlpha` flag alters the actual format on the GPU.
 *
 * If `NULL` is passed as the data, then the texture contents are undefined.
 *
 * Rationale: `width` / `height` are `uint16_t` because that's usually as far as
 *  GL implementations go in the best case before giving up. The expected
 *  capacity of the data buffer is easy to check against the format.
 *
 * 1/2-component texture formats are stored on GPU as RGB / RGBA due to the
 *  requirements of `EXT_framebuffer_object` 4.4.4 Framebuffer Completeness,
 *  which does not define other formats as renderable.
 */
BADGPU_EXPORT BADGPUTexture badgpuNewTexture(BADGPUInstance instance,
    uint32_t flags, BADGPUTextureFormat format,
    uint16_t width, uint16_t height, const uint8_t * data);

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
    uint16_t width, uint16_t height);

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
 * Pixels are always read back as RGBA8888, as they would be supplied to
 *  `badgpuNewTexture`.
 *
 * Rationale: This is simply what `glReadPixels` provides for GLES 1.1.
 *
 * If `width` or `height` is 0, the function silently succeeds.
 *
 * Otherwise, fails if `data` or `texture` is `NULL`, or various other issues
 *  occur.
 */
BADGPU_EXPORT BADGPUBool badgpuReadPixels(BADGPUTexture texture,
    uint16_t x, uint16_t y, uint16_t width, uint16_t height, uint8_t * data);

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
    BADGPUDrawFlags_DepthTest = 16,
    BADGPUDrawFlags_Blend = 32,
    // Usually, the alpha test is `GEqual`. This inverts it to `Less`.
    BADGPUDrawFlags_AlphaTestInvert = 64,
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
 * ### `BADGPUBlendEquation`
 *
 * (Values deliberately match `glBlendEquationSeparate`.
 *  The implementation may make use of such.
 *  Users should not abuse this.)
 */
typedef enum BADGPUBlendEquation {
    // GL_FUNC_ADD: S + D
    BADGPUBlendEquation_Add = 0x8006,
    // GL_FUNC_SUBTRACT: S - D
    BADGPUBlendEquation_Sub = 0x800A,
    // GL_FUNC_REVERSE_SUBTRACT: D - S
    BADGPUBlendEquation_ReverseSub = 0x800B,
    BADGPUBlendEquation_Force32 = 0x7FFFFFFF
} BADGPUBlendEquation;

/*
 * ### `BADGPUBlendWeight`
 *
 * Values deliberately match glBlendFuncSeparate.
 * The implementation may make use of such.
 * Users should not abuse this.
 */
typedef enum BADGPUBlendWeight {
    // GL_ZERO: 0
    BADGPUBlendWeight_Zero = 0,
    // GL_ONE: 1
    BADGPUBlendWeight_One = 1,
    // GL_SRC_COLOR: Sc
    BADGPUBlendWeight_Src = 0x300,
    // GL_ONE_MINUS_SRC_COLOR: 1 - Sc
    BADGPUBlendWeight_InvertSrc = 0x301,
    // GL_DST_COLOR: Dc
    BADGPUBlendWeight_Dst = 0x306,
    // GL_ONE_MINUS_DST_COLOR: 1 - Dc
    BADGPUBlendWeight_InvertDst = 0x307,
    // GL_SRC_ALPHA: Sa
    BADGPUBlendWeight_SrcA = 0x0302,
    // GL_ONE_MINUS_SRC_ALPHA: 1 - Sa
    BADGPUBlendWeight_InvertSrcA = 0x303,
    // GL_DST_ALPHA: Da
    BADGPUBlendWeight_DstA = 0x304,
    // GL_ONE_MINUS_DST_ALPHA: 1 - Da
    BADGPUBlendWeight_InvertDstA = 0x305,
    // GL_SRC_ALPHA_SATURATE: min(Sa, 1 - Da)
    //  except for A output where it's just 1.
    BADGPUBlendWeight_SrcAlphaSaturate = 0x308,
    BADGPUBlendWeight_Force32 = 0x7FFFFFFF
} BADGPUBlendWeight;

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
 * For `vCol` and `vTC`, there are also "freeze flags". \
 * If used in conjunction with the arrays, only the first element of those
 *  arrays is used (allowing for "single-colour uniform" treatment). \
 * (This lookup ignores indices, and frozen arrays can be just that element.)
 *
 * `iStart` and `iCount` represent a range in the indices array, which in turn are
 *  indices into the vertex arrays. \
 * If `indices` is `NULL`, then it is essentially as if an array was passed with
 *  values 0 to 65535.
 *
 * `matrixA` and / or `matrixB` can be `NULL`. In this case, they are
 *  effectively identity. \
 * Otherwise, see `BADGPUMatrix`.
 *
 * `depthN`, `depthF` make up the depth range (0, 1 is a good default).
 *
 * `vX`, `vY`, `vW`, `vH` make up the viewport.
 *
 * `texture` is multiplied with the vertex colours. \
 * (If `NULL`, then the vertex colours are used as-is.) \
 * The texture coordinates are multiplied with the texture coordinate matrix.
 *
 * `poFactor`, `poUnits` make up the polygon offset.
 *
 * `alphaTestMin` specifies a minimum alpha. \
 * (Or a maximum, if `AlphaTestInvert` is set.)
 *
 * `stFunc`, `stRef`, and `stMask` are used for the stencil test. \
 * This `stMask` is for the test; the mask used for writing is the session's
 *  stencil mask. However, no writing occurs if the test isn't enabled in the
 *  draw flags.
 *
 * `stSF`, `stDF`, and `stDP` control what happens to the stencil buffer under
 *  various situations with the stencil test.
 *
 * Rationale: While this function is indeed absolutely massive, there are
 *  shorter wrappers such as badgpuDrawGeomNoDS. This is also arguably a natural
 *  cost of the avoidance of stack structs while also avoiding a stateful API.
 */
BADGPU_EXPORT BADGPUBool badgpuDrawGeom(
    BADGPU_SESSIONFLAGS,
    uint32_t flags,
    // Vertex Loader
    const BADGPUVector * vPos, const BADGPUVector * vCol, const BADGPUVector * vTC,
    BADGPUPrimitiveType pType, float plSize,
    uint32_t iStart, uint32_t iCount, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * matrixA, const BADGPUMatrix * matrixB,
    // DepthRange
    float depthN, float depthF,
    // Viewport
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    // Fragment Shader
    BADGPUTexture texture, const BADGPUMatrix * matrixT,
    // PolygonOffset
    float poFactor, float poUnits,
    // Alpha Test
    float alphaTestMin,
    // Stencil Test
    BADGPUCompare stFunc, uint8_t stRef, uint8_t stMask,
    BADGPUStencilOp stSF, BADGPUStencilOp stDF, BADGPUStencilOp stDP,
    // Depth Test
    BADGPUCompare dtFunc,
    // Blending
    BADGPUBlendWeight bwRGBS, BADGPUBlendWeight bwRGBD, BADGPUBlendEquation beRGB,
    BADGPUBlendWeight bwAS, BADGPUBlendWeight bwAD, BADGPUBlendEquation beA
);

/*
 * ### `badgpuDrawGeomNoDS`
 *
 * Alias for badgpuDrawGeom that removes depth / stencil parameters.
 *
 * Most removed values are 0 / `NULL`, except:
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
    const BADGPUVector * vPos, const BADGPUVector * vCol, const BADGPUVector * vTC,
    BADGPUPrimitiveType pType, float plSize,
    uint32_t iStart, uint32_t iCount, const uint16_t * indices,
    // Vertex Shader
    const BADGPUMatrix * matrixA, const BADGPUMatrix * matrixB,
    // Viewport
    int32_t vX, int32_t vY, int32_t vW, int32_t vH,
    // Fragment Shader
    BADGPUTexture texture, const BADGPUMatrix * matrixT,
    // Alpha Test
    float alphaTestMin,
    // Blending
    BADGPUBlendWeight bwRGBS, BADGPUBlendWeight bwRGBD, BADGPUBlendEquation beRGB,
    BADGPUBlendWeight bwAS, BADGPUBlendWeight bwAD, BADGPUBlendEquation beA
);

#undef BADGPU_SESSIONFLAGS

#endif

