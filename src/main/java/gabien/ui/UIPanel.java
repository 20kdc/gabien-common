/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import java.util.LinkedList;
import gabien.IGrInDriver;
import gabien.IGrInDriver.IImage;

/**
 * UIPanel allows multiple elements to be grouped together.
 * It can also display images.
 */
public class UIPanel extends UIElement {
    public IImage baseImage;
    public int imageX,imageY;
    public UIElement selectedElement;
    public LinkedList<UIElement> allElements = new LinkedList<UIElement>();

    //ox,oy specify where this element is being drawn.the bounds are used in calculating this.
    //the root panel gets a ox,oy of 0,0,for obvious reasons.
    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean select, IGrInDriver igd) {
        if (baseImage!=null)
            igd.blitImage(imageX, imageY, elementBounds.width, elementBounds.height, ox, oy, baseImage);
        for (UIElement uie : allElements) {
            Rect r = uie.getBounds();
            uie.updateAndRender(ox + r.x, oy + r.y, deltaTime, (selectedElement == uie) && select, igd);
        }
    }
    //The click is within the (0,0,my width-1,my textHeight-1) range.
    @Override
    public void handleClick(int x, int y,int button) {
        selectedElement=null;
        for (UIElement uie : allElements) {
            Rect r = uie.getBounds();
            if (r.contains(x, y)) {
                selectedElement = uie;
                uie.handleClick(x - r.x, y - r.y, button);
                return;
            }
        }
    }
    @Override
    public void handleDrag(int x, int y) {
        for (UIElement uie : allElements) {
            Rect r = uie.getBounds();
            if (r.contains(x, y)) {
                uie.handleDrag(x - r.x, y - r.y);
                return;
            }
        }
    }

}
