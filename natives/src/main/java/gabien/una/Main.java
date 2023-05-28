/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.ToLongFunction;

import javax.imageio.ImageIO;

import gabien.una.UNAABIFinder.Convention;

import static gabien.una.UNAPoke.*;

public class Main {
    private static final int T_WIDTH = 320;
    private static final int T_HEIGHT = 200;

    public static void main(String[] args) throws Exception {
        System.out.println("UNA Self-Test");
        System.out.println("Loaded? " + UNALoader.defaultLoader());
        UNA.setup();
        System.out.println("isWin32: " + UNA.isWin32);
        System.out.println("isBigEndian: " + UNA.isBigEndian);
        System.out.println("is32Bit: " + UNA.is32Bit);

        System.out.println("Architecture/OS: " + UNA.getArchOS());
        System.out.println("Size of pointers: " + UNA.getSizeofPtr());
        long purpose = UNA.getTestStringRaw();
        long strlen = UNAC.strlen(purpose);

        ByteBuffer obj = UNA.newDirectByteBuffer(purpose, strlen);
        byte[] data = new byte[(int) strlen];
        obj.get(data);
        System.out.println("UTF-8 ByteBuffer retrieval test: " + new String(data, StandardCharsets.UTF_8));

        byte[] data2 = new byte[(int) strlen];
        peekAB(purpose, strlen, data2, 0);
        System.out.println("UTF-8 GetRegion retrieval test: " + new String(data2, StandardCharsets.UTF_8));

        System.out.println("Finding ABI...");
        UNAABI khronos = UNAABIFinder.getABI(Convention.StdcallOnWindows);
        UNAABI winapi = UNAABIFinder.getABI(Convention.Stdcall);

        System.out.println("Trying to find EGL...");
        long eglFound = UNAC.dlopen("libEGL.so.1");
        System.out.println("EGL: " + eglFound);
        if (eglFound != 0) {
            ToLongFunction<String> egl = UNAC.dlsymLambda(eglFound);
            // First function lookup, so make a big deal of it
            UNAFn egd = dlsym(egl, "eglGetDisplay", khronos, "p(p)");
            // Run it, get display
            long dsp = egd.call(0);
            System.out.println("Display: " + dsp);
            // Allocate 1KB of memory for various uses (yes this leaks i don't care)
            long exmem = UNAC.mallocChk(1024);
            // init EGL
            egd = dlsym(egl, "eglInitialize", khronos, "i(ppp)");
            System.out.println(" = " + egd.call(dsp, exmem, exmem + 4));
            System.out.println("EGL Version: " + peekI(exmem) + "." + peekI(exmem + 4));
            // Need to get configs...
            egd = dlsym(egl, "eglGetConfigs", khronos, "i(ppip)");
            System.out.println(" = " + egd.call(dsp, exmem + 8, 1, exmem));
            System.out.println("Number of configs: " + peekI(exmem));
            long config = peekPtr(exmem + 8);
            System.out.println("1st config: " + config);
            // Surface attributes
            pokeAI(exmem,
                    0x3057, T_WIDTH, // EGL_WIDTH
                    0x3056, T_HEIGHT, // EGL_HEIGHT
                    0x3038 // EGL_NONE
            );
            egd = dlsym(egl, "eglCreatePbufferSurface", khronos, "p(ppp)");
            long sfc = egd.call(dsp, config, exmem);
            System.out.println(" = " + sfc);
            // Ok, we have a surface. Now we need GL access.
            // Context attributes
            pokeAI(exmem,
                    0x3038 // EGL_NONE
            );
            egd = dlsym(egl, "eglCreateContext", khronos, "p(pppp)");
            long ctx = egd.call(dsp, config, 0, exmem);
            System.out.println(" = " + ctx);
            // And now, finally, access the GL context.
            egd = dlsym(egl, "eglMakeCurrent", khronos, "i(pppp)");
            System.out.println(" = " + egd.call(dsp, sfc, sfc, ctx));
            // -- Stage 2: OpenGL --
            // Loader
            final UNAFn eglGetProcAddress = dlsym(egl, "eglGetProcAddress", khronos, "p(p)");
            ToLongFunction<String> gl = gpa(eglGetProcAddress);
            opengl(gl, khronos);
        }
        long u3Found = UNAC.dlopen("user32.dll");
        System.out.println("user32.dll: " + u3Found);
        long g3Found = UNAC.dlopen("gdi32.dll");
        System.out.println("gdi32.dll: " + g3Found);
        long owFound = UNAC.dlopen("opengl32.dll");
        System.out.println("opengl32.dll: " + owFound);
        if (u3Found != 0 && g3Found != 0 && owFound != 0) {
            // ToLongFunction<String> u3 = UNAC.dlsymLambda(u3Found);
            // ToLongFunction<String> g3 = UNAC.dlsymLambda(g3Found);
            ToLongFunction<String> ow = UNAC.dlsymLambda(owFound);
            long hdc = UNA.wCreateInvisibleGLWindowHDC();
            int pixFmt = UNA.wChooseAndSetSanePixelFormatHDC(hdc);
            System.out.println("HDC init: " + hdc + ", " + pixFmt);
            UNAFn wglCreateContext = dlsym(ow, "wglCreateContext", winapi, "p(p)");
            long initialContext = wglCreateContext.call(hdc);
            System.out.println(" = " + initialContext);
            System.out.println(" GetLastError: " + UNA.wGetLastError());
            dlsym(ow, "wglMakeCurrent", winapi, "v(pp)").call(hdc, initialContext);
            // Loader
            final UNAFn wglGetProcAddress = dlsym(ow, "wglGetProcAddress", winapi, "p(p)");
            ToLongFunction<String> wgl = gpa(wglGetProcAddress);
            // Allocate 1KB of memory for various uses (yes this leaks i don't care)
            long exmem = UNAC.mallocChk(1024);
            pokeI(exmem, 0);
            long extensions = dlsym(wgl, "wglGetExtensionsStringARB", winapi, "p(p)").call(hdc);
            System.out.println(" = " + UNA.newStringUTF(extensions));
            long sfc = dlsym(wgl, "wglCreatePbufferARB", winapi, "p(piiip)").call(hdc, pixFmt, T_WIDTH, T_HEIGHT, exmem);
            System.out.println(" = " + sfc);
            long sfcHdc = dlsym(wgl, "wglGetPbufferDCARB", winapi, "p(p)").call(sfc);
            System.out.println(" = " + sfcHdc);
            long ctx = wglCreateContext.call(sfcHdc);
            System.out.println(" = " + ctx);
        }
    }
    private static void opengl(ToLongFunction<String> gl, UNAABI khronos) throws Exception {
        // Allocate 1MB of memory for various uses (yes this leaks i don't care)
        long exmem = UNAC.mallocChk(1024 * 1024);
        // Draw triangles
        rct(gl, khronos,
                -1, 0,
                1, 0,
                1, 1,
                -1, 1,
                0.129f, 0.694f, 1
        );
        rct(gl, khronos,
                -1, -1,
                1, -1,
                1, 0,
                -1, 0,
                1, 0.129f, 0.549f
        );
        float m = 0f;
        rct(gl, khronos,
                -1, -m,
                1, -m,
                1, m,
                -1, m,
                1, 0.847f, 0
        );
        // Read pixels
        UNAFn egd = dlsym(gl, "glReadPixels", khronos, "v(iiiiiip)");
        // GL_RGB, GL_UNSIGNED_BYTE
        egd.call(0, 0, T_WIDTH, T_HEIGHT, 0x1907, 0x1401, exmem);
        // Now output this to an image!
        byte[] imgData = peekAB(exmem, T_WIDTH * T_HEIGHT * 3);
        BufferedImage bi = new BufferedImage(T_WIDTH, T_HEIGHT, BufferedImage.TYPE_INT_RGB);
        int[] imgRGB = new int[T_WIDTH * T_HEIGHT];
        int dataPtr = 0;
        for (int i = 0; i < T_WIDTH * T_HEIGHT; i++) {
            byte r = imgData[dataPtr++];
            byte g = imgData[dataPtr++];
            byte b = imgData[dataPtr++];
            imgRGB[i] = ((r << 16) & 0xFF0000) | ((g << 8) & 0xFF00) | ((b << 0) & 0xFF);
        }
        bi.setRGB(0, 0, T_WIDTH, T_HEIGHT, imgRGB, 0, T_WIDTH);
        ImageIO.write(bi, "PNG", new File("out.png"));
    }
    private static void rct(ToLongFunction<String> gl, UNAABI khronos, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float r, float g, float b) {
        tri(gl, khronos, x1, y1, x2, y2, x4, y4, r, g, b);
        tri(gl, khronos, x2, y2, x3, y3, x4, y4, r, g, b);
    }
    private static void tri(ToLongFunction<String> gl, UNAABI khronos, float x1, float y1, float x2, float y2, float x3, float y3, float r, float g, float b) {
        // Setup vertices
        UNAFn egd = dlsym(gl, "glColor4f", khronos, "v(ffff)");
        egd.call(Float.floatToRawIntBits(r), Float.floatToRawIntBits(g), Float.floatToRawIntBits(b), Float.floatToRawIntBits(1));
        long exmem = allocAF(new float[] {
                x1, y1,
                x2, y2,
                x3, y3
        });
        egd = dlsym(gl, "glVertexPointer", khronos, "v(iiip)");
        // GL_FLOAT
        egd.call(2, 0x1406, 8, exmem);
        egd = dlsym(gl, "glEnableClientState", khronos, "v(i)");
        // GL_VERTEX_ARRAY
        egd.call(0x8074);
        // Actual draw call
        egd = dlsym(gl, "glDrawArrays", khronos, "v(iii)");
        // GL_TRIANGLES
        egd.call(0x0004, 0, 3);
        UNAC.free(exmem);
    }
    private static ToLongFunction<String> gpa(UNAFn egp) {
        return str -> {
            long tmp = UNAC.strdup(str);
            long res = egp.call(tmp);
            UNAC.free(tmp);
            return res;
        };
    }
    private static UNAFn dlsym(ToLongFunction<String> dl, String fn, UNAABI abi, String sig) {
        UNAFn tmp = UNAC.dlsym(dl, fn, abi, sig);
        System.out.println(fn + ": " + tmp);
        return tmp;
    }
}
