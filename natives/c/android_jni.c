/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

// Android makes everything here painful.

#ifdef ANDROID

#include <stddef.h>
#include <android/native_window_jni.h>
#include <EGL/egl.h>
#include <GLES/gl.h>

#include "badgpu/badgpu.h"

#define J_BADGPU(x) Java_gabien_natives_BadGPUUnsafe_ ## x

int64_t J_BADGPU(ANDcreateEGLSurface)(void * env, void * self, int64_t instance, jobject obj) {
    void * eglDisplay = badgpuGetEGLDisplay((void *) instance);
    void * eglConfig = badgpuGetEGLConfig((void *) instance);
    void * nwh = ANativeWindow_fromSurface(env, obj);
    int32_t oops = EGL_NONE;
    return (int64_t) (intptr_t) eglCreateWindowSurface(eglDisplay, eglConfig, nwh, &oops);
}

void J_BADGPU(ANDdestroyEGLSurface)(void * env, void * self, int64_t instance, int64_t surface) {
    eglDestroySurface(badgpuGetEGLDisplay((void *) instance), (void *) surface);
}

static const float vertexData[] = {
    -1, -1,
     1, -1,
     1,  1,
    -1, -1,
     1,  1,
    -1,  1
};
static const float textureData[] = {
    0, 1,
    1, 1,
    1, 0,
    0, 1,
    1, 0,
    0, 0
};

void J_BADGPU(ANDoverrideSurface)(void * env, void * self, int64_t instance, int64_t surface) {
    void * eglDisplay = badgpuGetEGLDisplay((void *) instance);
    void * eglContext = badgpuGetEGLContext((void *) instance);
    eglMakeCurrent(eglDisplay, (void *) surface, (void *) surface, eglContext);
}

void J_BADGPU(ANDblitToSurface)(void * env, void * self, int64_t instance, int64_t texture, int64_t surface, int32_t width, int32_t height) {
    void * eglDisplay = badgpuGetEGLDisplay((void *) instance);
    void * eglContext = badgpuGetEGLContext((void *) instance);

    badgpuResetGLState((void *) instance);
    glViewport(0, 0, width, height);

    glEnable(GL_TEXTURE_2D);
    glBindTexture(GL_TEXTURE_2D, badgpuGetGLTexture((void *) texture));
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(2, GL_FLOAT, 0, vertexData);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glTexCoordPointer(2, GL_FLOAT, 0, textureData);
    glDrawArrays(GL_TRIANGLES, 0, 6);

    eglSwapBuffers(eglDisplay, (void *) surface);
}

#endif

