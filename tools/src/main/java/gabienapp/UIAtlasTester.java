/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabienapp;

import java.util.Random;

import gabien.GaBIEn;
import gabien.atlas.AllAtlasStrategies;
import gabien.atlas.IAtlasStrategy;
import gabien.render.AtlasPage;
import gabien.render.IGrDriver;
import gabien.ui.UIElement;
import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;
import gabien.wsi.IDesktopPeripherals;
import gabien.wsi.IGrInDriver;
import gabien.wsi.IPeripherals;

/**
 * Created 18th July, 2023.
 */
public class UIAtlasTester extends UIElement {

    private int mode = 0;
    private int st = 0;
    private Random random = new Random();
    private Rect[] atlas;
    private AtlasPage ap;

    public UIAtlasTester() {
        super(512, 512 + 32);
        ap = GaBIEn.makeAtlasPage(512, 512);
    }

    public Size[] createTestCase() {
        Size[] test = new Size[64];
        if (mode == 0) {
            for (int i = 0; i < test.length; i++)
                test[i] = new Size(random.nextInt(48) + 16, random.nextInt(48) + 16);
        } else {
            for (int i = 0; i < test.length; i++)
                test[i] = new Size(random.nextBoolean() ? 64 : 32, random.nextBoolean() ? 64 : 32);
        }
        return test;
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        IDesktopPeripherals dp = (IDesktopPeripherals) peripherals;
        boolean planRegen = false;
        if (dp.isKeyJustPressed(IGrInDriver.VK_A)) {
            mode++;
            mode %= 2;
            planRegen = true;
        } else if (dp.isKeyJustPressed(IGrInDriver.VK_S)) {
            st++;
            st %= AllAtlasStrategies.strategies.length;
            planRegen = true;
        } else if (dp.isKeyJustPressed(IGrInDriver.VK_D)) {
            planRegen = true;
        } else if (dp.isKeyJustPressed(IGrInDriver.VK_F)) {
            for (int i = 0; i < 128; i++) {
                atlas = calculate(AllAtlasStrategies.strategies[st], new Size(512, 512), createTestCase());
                boolean done = false;
                for (int j = 0; j < atlas.length; j++)
                    if (atlas[j] == null)
                        done = true;
                if (done)
                    break;
            }
        }
        if (planRegen)
            atlas = calculate(AllAtlasStrategies.strategies[st], new Size(512, 512), createTestCase());
    }

    private Rect[] calculate(IAtlasStrategy iAtlasStrategy, Size size, Size[] createTestCase) {
        IAtlasStrategy.Instance instance = iAtlasStrategy.instance(size);
        Rect[] rects = new Rect[createTestCase.length];
        for (int i = 0; i < rects.length; i++)
            rects[i] = instance.add(createTestCase[i]);
        return rects;
    }

    @Override
    protected void render(IGrDriver igd) {
        if (atlas == null)
            return;
        int amount = 0;
        for (Rect r : atlas) {
            if (r != null) {
                igd.clearRect(255, 255, 255, r.x, r.y, r.width, r.height);
                amount++;
            }
        }
        for (Rect r : atlas)
            if (r != null)
                igd.fillRect(0, 0, 0, 128, r.x + 1, r.y + 1, r.width - 2, r.height - 2);
        GaBIEn.engineFonts.f16.drawLine(igd, 0, 512, "fulfillment: " + amount + "/" + atlas.length, false);
        ap.copyFrom(0, 0, 512, 512, 0, 0, igd);
        ap.copyFrom(0, 512, 128, 16, 16, 16, igd);
        igd.blitImage(0, 512 + 16, ap);
    }
}
