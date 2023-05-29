/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

/*
 * BadGPU C Header And API Specification
 * API Specification Version: 0.07
 */

#ifndef BADGPU_H_
#define BADGPU_H_

#include <stdint.h>

#ifndef BADGPU_EXPORT
#define BADGPU_EXPORT
#endif

/*
 * Abstract
 *
 * The BadGPU API is a cross-platform offscreen rendering API.
 *
 * It is designed with adaptability in mind, not efficiency.
 *
 * It can be considered something of a relative ideologically to the OSMesa API,
 *  but OSMesa has the issue that it never takes advantage of hardware.
 *
 * BadGPU takes advantage of hardware while avoiding the complex WSI issues by
 *  simply not supporting WSI.
 *
 * This allows BadGPU to be used seamlessly with any windowing/drawing framework
 *  at some performance cost (which I consider an acceptable loss given the
 *  reliability and portability benefits of reducing per-platform code), while
 *  still not losing as much performance as software rendering.
 *
 * For portability reasons, BadGPU is designed to target the subset of
 *  functionality common between three separate versions of OpenGL:
 * 1. OpenGL 1.1 +
 *     EXT_blend_subtract,
 *     EXT_blend_func_separate, EXT_framebuffer_object
 * 2. OpenGL ES 1.0 Common +
 *     OES_blend_subtract, OES_framebuffer_object, OES_stencil8
 * 3. OpenGL ES 2.0 + shader compiler & NPOT textures
 *
 * Rationale: Complex blend functions are the reason this mess even started.
 *  And the stencil buffer, along with the ability to share the stencil buffer
 *  between target images, is useful in "imitation multitexturing". In this way,
 *  a multitextured object may be composited and then transferred.
 *
 * Features which would require complex code generation to implement in OpenGL
 *  ES 2.0 are not supported.
 *
 * As such, the pipeline looks like this, based off of the OpenGL ES 2 pipeline:
 * 1. Vertex Loading / Shader:
 *     The shader is constant.
 *     Vertices always have the following format:
 *      attribute vec4 vertex;
 *      (Rationale: Software TnL, possible interesting effects.)
 *      attribute vec4 colour;
 *      attribute vec4 texCoord;
 *      (Rationale: Interesting possibilities for texCoord-accelerated anim.)
 *     This format is split into multiple arrays for flexibility.
 *     The vertex is transformed by two mat4 uniforms.
 *     These are modelview and projection.
 * 2. Primitive Assembly:
 *     POINTS, LINES, LINE_STRIP, LINE_LOOP,
 *     TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN
 *      PointSize / LineWidth (as one value)
 *      DrawElements as UNSIGNED_SHORT
 * 3. Rasterization:
 *     All attributes are smoothed.
 *     Viewport
 *     SCISSOR_TEST
 *      Scissor
 *     Multisampling/linesmooth is removed by ES2, and thus not present.
 *     FrontFace
 *     CULL_FACE
 *      CullFace
 *     POLYGON_OFFSET_FILL
 *      PolygonOffset
 * 4. Fragment Shader:
 *     There are two shaders, for if the texture is enabled or disabled.
 *     TEXTURE_2D
 *      GenTextures, DeleteTextures, BindTexture
 *      TexImage2D, TexSubImage2D, CopyTexSubImage2D
 *      GenerateMipmap
 *      The texture is multiplied with the colour.
 * 5. Blend/Alpha/Stencil:
 *     ALPHA_TEST
 *      AlphaFunc "kind of"
 *      (Rationale:
 *       Because ES2 removes the alpha test and complicating ES2 makes things
 *       Very Hard, the alpha test is always enabled with a provided threshold,
 *       and with an invert bit.
 *       This can be implemented with a multiply-add sequence on ES2.)
 *     STENCIL_TEST
 *      StencilFunc
 *      StencilOp
 *     DEPTH_TEST
 *      DepthFunc
 *     BLEND
 *      BlendFuncSeparate
 *      BlendEquationSeparate
 * 6. Masks
 *     ColorMask
 *     StencilMask
 *     DepthMask
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
 *  assurance.
 */

/*
 * Types
 *
 * The big types in BadGPU are object handles.
 * These all have a unified API for creation and destruction.
 * (See badgpuRef/badgpuUnref.)
 */

// Generic object handle.
typedef struct BADGPUObject * BADGPUObject;
typedef BADGPUObject BADGPUInstance;
typedef BADGPUObject BADGPUTexture;
typedef BADGPUObject BADGPUDSBuffer;

typedef unsigned char BADGPUBool;

/*
 * 4D float vector. Used for matrices and also vertex data.
 */
typedef struct BADGPUVector {
    float x, y, z, w;
} BADGPUVector;

/*
 * A matrix is a "transposed" matrices as per GL LoadMatrix.
 * They are represented as a set of basis vectors.
 * Each basis vector is multiplied by the corresponding input vector float.
 * The multiplied vectors are then added together.
 */
typedef struct BADGPUMatrix {
    BADGPUVector x, y, z, w;
} BADGPUMatrix;

/*
 * NewInstance Flags
 */
typedef enum BADGPUNewInstanceFlags {
    BADGPUNewInstanceFlags_Debug = 1,
    BADGPUNewInstanceFlags_Force32 = 0x7FFFFFFF
} BADGPUNewInstanceFlags;

/*
 * Metainfo Type
 * Values deliberately match glGetString.
 * The implementation may make use of such.
 * Users should not abuse this.
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
 * Texture Formats/Flags
 *
 * BADGPU doesn't support any HDR textures.
 * It also doesn't consider stencil/depth a kind of texture.
 * It's important to note that texture formats also specify the format of
 *  texture data being provided to, say, badgpuNewTexture.
 */
typedef enum BADGPUTextureFlags {
    // If minifying the texture uses linear filtering.
    BADGPUTextureFlags_MinLinear = 1,
    // If magnifying the texture uses linear filtering.
    BADGPUTextureFlags_MagLinear = 2,
    // If mipmapping is used.
    BADGPUTextureFlags_Mipmap = 4,
    // If accesses beyond the edges of the texture repeat (rather than clamping)
    BADGPUTextureFlags_WrapS = 16,
    BADGPUTextureFlags_WrapT = 32,
    BADGPUTextureFlags_Force32 = 0x7FFFFFFF
} BADGPUTextureFlags;

typedef enum BADGPUTextureFormat {
    // A -> 111A
    BADGPUTextureFormat_Alpha = 1,
    // L -> LLL1
    BADGPUTextureFormat_Luma = 2,
    // LA -> LLLA
    BADGPUTextureFormat_LumaAlpha = 3,
    // RGB -> RGB1
    BADGPUTextureFormat_RGB = 4,
    // RGBA -> RGBA
    BADGPUTextureFormat_RGBA = 5,
    BADGPUTextureFormat_Force32 = 0x7FFFFFFF
} BADGPUTextureFormat;

/*
 * Session Flags
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
 * Draw Flags
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
    // Usually, the alpha test is >=. This inverts it to <.
    BADGPUDrawFlags_AlphaTestInvert = 64,
    // Colour array only has to be one element long, and that's the only colour.
    BADGPUDrawFlags_FreezeColor = 128,
    BADGPUDrawFlags_FreezeColour = 128,
    // TC array only has to be one element long, and that's the only TC.
    BADGPUDrawFlags_FreezeTC = 256,
    BADGPUDrawFlags_Force32 = 0x7FFFFFFF
} BADGPUDrawFlags;

/*
 * Primitive Type
 * Values deliberately match glBegin etc.
 * The implementation may make use of such.
 * Users should not abuse this.
 * (Rationale for not including loop/strip/fan primitive types:
 *  These only really work properly with primitive restart, and DrawElements
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
 * Comparison Function
 * Values deliberately match glDepthFunc etc.
 * The implementation may make use of such.
 * Users should not abuse this.
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
 * Stencil Op
 * Values deliberately match glStencilOp.
 * The implementation may make use of such.
 * Users should not abuse this.
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
 * Blend Equation
 * Values deliberately match glBlendEquationSeparate.
 * The implementation may make use of such.
 * Users should not abuse this.
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
 * Blend Weight
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
    // GL_SRC_ALPHA_SATURATE: min(Sa, 1 - Da) ; except for A output where it's just 1.
    BADGPUBlendWeight_SrcAlphaSaturate = 0x308,
    BADGPUBlendWeight_Force32 = 0x7FFFFFFF
} BADGPUBlendWeight;

/*
 * Object Management
 *
 * All BadGPU-generated handles are BadGPU objects, manipulatable with the
 *  badgpuRef and badgpuUnref functions.
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
 * References a BadGPU object.
 * Returns what it was given.
 */
BADGPU_EXPORT BADGPUObject badgpuRef(BADGPUObject obj);

/*
 * Unreferences a BadGPU object.
 * Returns non-zero if the object was completely removed.
 * Otherwise, hanging references presumably still exist.
 */
BADGPU_EXPORT BADGPUBool badgpuUnref(BADGPUObject obj);

/*
 * Instance Initialization
 *
 * BadGPU is split into instances.
 * There is no inter-instance resource sharing.
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

/*
 * Creates a new BadGPU instance.
 *
 * This will allocate resources such as an EGLDisplay or HWND, so the instance
 *  should be unreferenced when done with.
 *
 * On failure, NULL is returned, and if the error pointer is provided, a
 *  constant C string pointer is placed there.
 * (On success, the error pointer is not updated.)
 * This constant C string, being a constant, is thread-safe, and cannot be
 *  invalidated.
 *
 * The flags are BADGPUNewInstanceFlags.
 * The debug flag indicates that a performance hit is acceptable for better
 *  debugging information to be provided in an implementation-dependent manner.
 *
 * Rationale: Due to cross-compilation limitations, the current reference
 *  implementation of GL error tracking outputs to standard output.
 *
 * For some applications, this is unacceptable behaviour.
 *
 * In addition, GL error tracking arguably adds unnecessary overhead. This isn't
 *  as big of a concern to BadGPU, but given there's nowhere to put the errors,
 *  it's relatively okay.
 */
BADGPU_EXPORT BADGPUInstance badgpuNewInstance(uint32_t flags, char ** error);

/*
 * Returns a string describing an aspect of the instance, or NULL on error.
 *
 * This string's lifetime is only certain until the next call involving the
 *  instance or any related object.
 */
BADGPU_EXPORT const char * badgpuGetMetaInfo(BADGPUInstance instance,
    BADGPUMetaInfoType mi);

/*
 * Texture/2D Buffer Management
 */

/*
 * Creates a texture, with possible initial data.
 * The flags are BADGPUNewTextureFlags.
 * Mipmaps are automatically created if the texture is mipmapped.
 * Data can be supplied as a flat array of bytes.
 * The size and layout of each pixel in the array of bytes is specified by
 *  the format.
 * If NULL is passed as the data, then the texture contents are undefined.
 */
BADGPU_EXPORT BADGPUTexture badgpuNewTexture(BADGPUInstance instance,
    uint32_t flags, BADGPUTextureFormat format,
    uint16_t width, uint16_t height, const uint8_t * data);

/*
 * Creates a depth/stencil buffer.
 * These are used for drawing... and that's about it.
 */
BADGPU_EXPORT BADGPUDSBuffer badgpuNewDSBuffer(BADGPUInstance instance,
    uint16_t width, uint16_t height);

/*
 * Generates mipmaps for a texture.
 * This must be done explicitly if the texture is rendered to and then expected
 *  to have consistent mipmaps.
 * It should not be done if mipmaps are not used by the texture.
 */
BADGPU_EXPORT void badgpuGenerateMipmap(BADGPUTexture texture);

/*
 * Reads pixels back from a texture.
 * Pixels are always read back as RGBA8888, as they would be supplied to
 *  badgpuNewTexture.
 */
BADGPU_EXPORT void badgpuReadPixels(BADGPUTexture texture,
    uint16_t x, uint16_t y, uint16_t width, uint16_t height, uint8_t * data);

/*
 * Drawing Commands
 *
 * The following rule applies to all functions in this section:
 * The texture and depth/stencil buffers, if both present,
 *  must be the same size and instance.
 *
 * Drawing commands have the same first parameters, known as the session
 *  parameters, prefixed with 's'.
 * They are controlled by sFlags, which are BADGPUSessionFlags.
 * (Rationale: These parameters are all those documented as common between
 *  clear and drawing commands. See ES 2.0: 4.2.3 Clearing the Buffers,
 *  same place in ES 1.1)
 *
 * It is important to keep in mind that the default state of the masks is
 *  NOT to render things. Normal operation might use MaskAll or MaskRGBAD.
 */

#define BADGPU_SESSIONFLAGS \
    BADGPUTexture sTexture, BADGPUDSBuffer sDSBuffer, \
    uint32_t sFlags, \
    int32_t sScX, int32_t sScY, int32_t sScWidth, int32_t sScHeight

/*
 * Performs a clear.
 * The session flag masks control the clear.
 * If flags are enabled for a buffer, that buffer must be present.
 * However, otherwise, buffers may not be present.
 */
BADGPU_EXPORT void badgpuDrawClear(
    BADGPU_SESSIONFLAGS,
    uint8_t cR, uint8_t cG, uint8_t cB, uint8_t cA, float depth, uint8_t stencil
);

/*
 * Performs a drawing command.
 * The flags provided are BADGPUDrawFlags.
 *
 * vPos/vCol/vTC are the vertex data arrays.
 * vCol and vTC can be null to leave that feature at a default.
 * vPos CANNOT be null.
 * For vCol and vTC, there are also "freeze flags".
 * If used in conjunction with the arrays, only the first element of those
 *  arrays is used (allowing for "single-colour uniform" treatment).
 * (This lookup ignores indices, and frozen arrays can be just that element.)
 *
 * iStart and iCount represent a range in the indices array, which in turn are
 *  indices into the vertex arrays.
 * If indices is null, then it is essentially as if an array was passed with
 *  values 0 to 65535.
 * matrix* can be null, in which case they are effectively identity.
 * Otherwise, see BADGPUMatrix.
 * depthN, depthF make up the depth range (0, 1 is a good default).
 * vX, vY, vW, vH make up the viewport.
 *
 * texture is multiplied with the vertex colours.
 * (If null, then the vertex colours are used as-is.)
 * The texture coordinates are multiplied with the texture coordinate matrix.
 * poFactor, poUnits make up the polygon offset.
 * alphaTestMin specifies a minimum alpha.
 * (Or a maximum, if AlphaTestInvert is set.)
 *
 * stFunc, stRef, and stMask are used for the stencil test.
 * This stMask is for the test; the mask used for writing is the session's
 *  stencil mask. However, no writing occurs if the test isn't enabled in the
 *  draw flags.
 * stSF, stDF, and stDP control what happens to the stencil buffer under various
 *  situations with the stencil test.
 */
BADGPU_EXPORT void badgpuDrawGeom(
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
 * Alias for badgpuDrawGeom that removes parameters only useful with a DSBuffer.
 * This is solely a convenience wrapper which provides zeros.
 * It is not an optimization.
 */
BADGPU_EXPORT void badgpuDrawGeomNoDS(
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

