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
            // Allocate 1MB of memory for various uses
            long exmem = UNAC.mallocChk(1024 * 1024);
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
            final int width = 320;
            final int height = 200;
            pokeAI(exmem,
                    0x3057, width, // EGL_WIDTH
                    0x3056, height, // EGL_HEIGHT
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
            ToLongFunction<String> gl = str -> {
                long tmp = UNAC.strdup(str);
                long res = eglGetProcAddress.call(tmp);
                UNAC.free(tmp);
                return res;
            };
            // Setup vertices
            pokeAF(exmem,
                    -1, -1,
                    -1, 1,
                    1, 1
            );
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
            // Read pixels
            egd = dlsym(gl, "glReadPixels", khronos, "v(iiiiiip)");
            // GL_RGB, GL_UNSIGNED_BYTE
            egd.call(0, 0, width, height, 0x1907, 0x1401, exmem);
            // Now output this to an image!
            byte[] imgData = peekAB(exmem, width * height * 3);
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int[] imgRGB = new int[width * height];
            int dataPtr = 0;
            for (int i = 0; i < width * height; i++) {
                byte r = imgData[dataPtr++];
                byte g = imgData[dataPtr++];
                byte b = imgData[dataPtr++];
                imgRGB[i] = ((r << 16) & 0xFF0000) | ((g << 8) & 0xFF00) | ((b << 0) & 0xFF);
            }
            bi.setRGB(0, 0, width, height, imgRGB, 0, width);
            ImageIO.write(bi, "PNG", new File("out.png"));
        }
    }
    private static UNAFn dlsym(ToLongFunction<String> dl, String fn, UNAABI abi, String sig) {
        UNAFn tmp = UNAC.dlsym(dl, fn, abi, sig);
        System.out.println(fn + ": " + tmp);
        return tmp;
    }
}
