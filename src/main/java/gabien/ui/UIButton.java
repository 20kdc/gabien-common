/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien.ui;


import gabien.IGrInDriver;
import gabien.IImage;

public class UIButton extends UIElement {
    public IImage buttonImage;
    public int imageX, imageY;
    public Runnable onClick;
    public double pressedTime = 0;

    @Override
    public void updateAndRender(int ox, int oy, double DeltaTime, boolean selected, IGrInDriver igd) {
        if (pressedTime > 0)
            pressedTime -= DeltaTime;
        Rect bounds = getBounds();
        igd.blitImage(imageX, imageY, bounds.width, bounds.height, ox, oy + (pressedTime > 0 ? 4 : 0), buttonImage);
    }

    @Override
    public void handleClick(int x, int y, int button) {
        if (button == 1) {
            onClick.run();
            pressedTime = 0.5;
        }
    }

}
