/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

#include "una.h"

// Core

int64_t UNA(getSizeofPtr)(void * env, void * self) {
    return (int64_t) sizeof(void *);
}

static int64_t endianCheck = 1;

// This is "Windows" as opposed to "everything else".
// This is because Windows is the only OS that does a lot of stupid things.
#define SYSFLAG_W32 1
#define SYSFLAG_BE 2
#define SYSFLAG_32 4

int64_t UNA(getSysFlags)(void * env, void * self) {
    int64_t flags = 0;
#ifdef WIN32
    flags |= SYSFLAG_W32;
#endif
    if ((*(int32_t*)&endianCheck) != 1)
        flags |= SYSFLAG_BE;
    if (sizeof(void *) == 4)
        flags |= SYSFLAG_32;
    return flags;
}

void * UNA(getArchOS)(void * env, void * self) {
    return JNI_NewStringUTF(env, UNA_ARCHOS);
}

int64_t UNA(getTestStringRaw)(void * env, void * self) {
    return J_PTR("This is a test string to be retrieved.");
}

static int32_t sanityTester(int32_t a0, float a1, int32_t a2, float a3, int32_t a4, float a5, int32_t a6, float a7, int32_t a8, float a9, int32_t aA, float aB, int32_t aC, float aD, int32_t aE, float aF) {
    if (a0 != 1)
        return 0;
    if (a1 != 2)
        return 0;
    if (a2 != 3)
        return 0;
    if (a3 != 4)
        return 0;
    if (a4 != 5)
        return 0;
    if (a5 != 6)
        return 0;
    if (a6 != 7)
        return 0;
    if (a7 != 8)
        return 0;
    if (a8 != 9)
        return 0;
    if (a9 != 10)
        return 0;
    if (aA != 11)
        return 0;
    if (aB != 12)
        return 0;
    if (aC != 13)
        return 0;
    if (aD != 14)
        return 0;
    if (aE != 15)
        return 0;
    if (aF != 16)
        return 0;
    return 1;
}

int64_t UNA(getSanityTester)(void * env, void * self) {
    return J_PTR(sanityTester);
}

// just because

#ifdef WIN32
void printf(const char * str, ...);
int64_t UNA(wCreateInvisibleGLWindowHDC)(void * env, void * self) {
    WNDCLASS wc = {
        .lpfnWndProc = DefWindowProcA,
        .hInstance = GetModuleHandleA(NULL),
        .hbrBackground = (HBRUSH) (COLOR_BACKGROUND),
        .lpszClassName = "gabien_una_gl_window",
        .style = CS_OWNDC
    };
    RegisterClass(&wc);
    HWND hwnd = CreateWindowA("gabien_una_gl_window", "una", WS_OVERLAPPEDWINDOW | WS_VISIBLE, 0, 0, 256, 256, 0, 0, GetModuleHandleA(NULL), 0);
    HDC hdc = GetDC(hwnd);
    printf("hwnd %lli\n", (long long) hwnd);
    printf("hdc %lli\n", (long long) hdc);
    return J_PTR(hdc);
}
int32_t UNA(wChooseAndSetSanePixelFormatHDC)(void * env, void * self, int64_t hdcl) {
    HDC hdc = (HDC) C_PTR(hdcl);
    PIXELFORMATDESCRIPTOR pfd = {
        .nSize = sizeof(PIXELFORMATDESCRIPTOR),
        .nVersion = 1,
        .dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL,
        .iPixelType = PFD_TYPE_RGBA,
        .cColorBits = 32,
        .cDepthBits = 24,
        .cStencilBits = 8,
        .iLayerType = PFD_MAIN_PLANE
    };
    int pixFmt = ChoosePixelFormat(hdc, &pfd);
    SetPixelFormat(hdc, pixFmt, &pfd);
    printf("hdc %lli\n", (long long) hdc);
    return (int32_t) pixFmt;
}
#endif

