/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.natives.examples.stencilshadows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import gabien.natives.BadGPU;
import gabien.natives.BadGPU.Texture;
import gabien.natives.examples.IMain;
import gabien.natives.examples.State;
import gabien.natives.examples.U;

/**
 * Bah, you people with your depth-texture nonsense.
 * Created 11th June, 2023.
 */
public class StateStencilShadows extends State {
    public static final boolean DEBUGBASICS = false;
    final STHCameraSetup cam;
    final LinkedList<STHTriangle> tris = new LinkedList<>();
    float rX = 0;
    float rY = 0;
    public StateStencilShadows(IMain m) {
        super(m);
        cam = new STHCameraSetup(m.getInstance());
        if (!DEBUGBASICS) {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(U.class.getResourceAsStream("shadowing.obj"), StandardCharsets.UTF_8));
                LinkedList<Float> vX = new LinkedList<>();
                LinkedList<Float> vY = new LinkedList<>();
                LinkedList<Float> vZ = new LinkedList<>();
                while (br.ready()) {
                    String ln = br.readLine();
                    if (ln.startsWith("v ")) {
                        String[] ents = ln.split(" ");
                        vX.add(Float.parseFloat(ents[1]));
                        vY.add(Float.parseFloat(ents[2]));
                        vZ.add(Float.parseFloat(ents[3]));
                    } else if (ln.startsWith("f ")) {
                        String[] ents = ln.split(" ");
                        STHTriangle tri = new STHTriangle();
                        for (int i = 0; i < 3; i++) {
                            int vid = Integer.parseInt(ents[i + 1]) - 1;
                            tri.setPos(2 - i, vX.get(vid), -vY.get(vid), 1 - vZ.get(vid));
                        }
                        tris.add(tri);
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            STHTriangle tri = new STHTriangle();
            tri.setPos(0, -8, -8, 8);
            tri.setPos(1,  8, -8, 8);
            tri.setPos(2, -8,  8, 8);
            tris.add(tri);
            tri = new STHTriangle();
            tri.setPos(0,  8, -8, 8);
            tri.setPos(1,  8,  8, 8);
            tri.setPos(2, -8,  8, 8);
            tris.add(tri);
            tri = new STHTriangle();
            tri.setPos(0, -1, -1, 2);
            tri.setPos(1,  1, -1, 2);
            tri.setPos(2, -1,  1, 2);
            tris.add(tri);
        }
    }

    @Override
    public void frame(Texture screen, int w, int h) {
        cam.doSetup(screen, w, h);
        BadGPU.drawClear(screen, cam.dsBuffer, BadGPU.SessionFlags.MaskAll, 0, 0, 0, 0, 0, 0, 0, 1, 1, 128);
        for (STHTriangle tri : tris) {
            tri.calcD();
            tri.drawDepth(cam);
        }
        float d = main.getDeltaTime();
        if (main.getKey(IMain.KEY_W))
            rY -= d;
        if (main.getKey(IMain.KEY_A))
            rX -= d;
        if (main.getKey(IMain.KEY_S))
            rY += d;
        if (main.getKey(IMain.KEY_D))
            rX += d;
        drawLight(rX, rY, 1, 0.5f, 0.25f, 0.25f);
        drawLight( 1,  1, -1, 0.5f, 1f, 0.5f);
        drawLight( 0, -1, -1, 0.5f, 0.5f, 1f);
    }
    private void drawLight(float x, float y, float z, float r, float g, float b) {
        BadGPU.drawClear(cam.backBuffer, cam.dsBuffer, BadGPU.SessionFlags.StencilAll, 0, 0, 0, 0, 0, 0, 0, 0, 0, 128);
        for (STHTriangle tri : tris)
            tri.drawShadow(cam, x, y, z);
        for (STHTriangle tri : tris)
            tri.drawLight(cam, x, y, z, r, g, b);
    }
}
