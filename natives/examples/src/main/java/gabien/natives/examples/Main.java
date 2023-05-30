/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.natives.examples;

import java.awt.Canvas;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import gabien.natives.BadGPU;
import gabien.natives.BadGPUEnum.MetaInfoType;

/**
 * Yes, this deliberately uses AWT.
 * Created 30th May, 2023.
 */
public class Main {
    public volatile int currentCanvasWidth, currentCanvasHeight;
    public final BadGPU.Instance instance;
    public final BadGPU.Texture screen;
    public Timer t;
    public Main() {
        instance = BadGPU.newInstance(BadGPU.NewInstanceFlags.BackendCheck | BadGPU.NewInstanceFlags.CanPrintf);
        System.out.println("init: vendor: " + instance.getMetaInfo(MetaInfoType.Vendor));
        System.out.println("init: renderer: " + instance.getMetaInfo(MetaInfoType.Renderer));
        System.out.println("init: version: " + instance.getMetaInfo(MetaInfoType.Version));
        screen = instance.newTexture(0, 512, 512, null, 0);
        System.out.println(BadGPU.drawClear(screen, null, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0, 1, 0, 1, 1, 0, 0));
        System.out.println(BadGPU.drawClear(screen, null, BadGPU.SessionFlags.MaskAll | BadGPU.SessionFlags.Scissor, 0, 0, 512, 512, 1, 1, 0, 1, 0, 0));
        System.out.println(BadGPU.drawClear(screen, null, BadGPU.SessionFlags.MaskAll | BadGPU.SessionFlags.Scissor, 0, 0, 256, 256, 0, 1, 1, 1, 0, 0));
        System.out.println("---");
    }
    public static void main(String[] args) {
        System.setProperty("sun.awt.noerasebackground", "true");
        System.setProperty("sun.awt.erasebackgroundonresize", "true");
        gabien.natives.Loader.defaultLoader();
        final Main m = new Main();
        final Frame w = new Frame("gabien-natives examples");
        w.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
            }
            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
        @SuppressWarnings("serial")
        final Canvas c = new Canvas() {
            @Override
            public void paint(Graphics g) {
            }
        };
        c.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                m.currentCanvasWidth = c.getWidth();
                m.currentCanvasHeight = c.getHeight();
            }
        });
        w.add(c);
        w.setSize(800, 600);
        w.setVisible(true);
        w.addWindowListener(new WindowAdapter() {
           @Override
           public void windowClosing(WindowEvent e) {
               w.dispose();
               System.exit(1);
           }
        });
        m.t = new Timer();
        m.t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Graphics gr = c.getGraphics();
                // this is awful code, and needs revising, but right now I just want to confirm stuff works
                BufferedImage tmp = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
                int[] data = new int[512 * 512];
                ByteBuffer dataSrc = ByteBuffer.allocateDirect(data.length * 4);
                System.out.println("rpx" + m.screen.readPixels(0, 0, 512, 512, dataSrc, 0));
                int ptr = 0;
                for (int i = 0; i < data.length; i++) {
                    int r = dataSrc.get(ptr++) & 0xFF;
                    int g = dataSrc.get(ptr++) & 0xFF;
                    int b = dataSrc.get(ptr++) & 0xFF;
                    int a = dataSrc.get(ptr++) & 0xFF;
                    data[i] = (r << 16) | (g << 8) | b | (a << 24);
                }
                tmp.setRGB(0, 0, 512, 512, data, 0, 512);
                gr.drawImage(tmp, 0, 0, m.currentCanvasWidth, m.currentCanvasHeight, null);
            }
        }, 0, 100);
    }
}
