/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.IGrInDriver;
import gabien.IImage;
import gabien.ScissorGrInDriver;

import java.util.LinkedList;

/**
 * UIPanel allows multiple elements to be grouped together.
 * It can also display images.
 */
public class UIPanel extends UIElement {
    protected boolean useScissoring = false;

    public IImage baseImage; // I forgot this existed. Whoops.
    public int imageX, imageY;
    public UIElement selectedElement;
    public LinkedList<UIElement> allElements = new LinkedList<UIElement>();

    //ox,oy specify where this element is being drawn.the bounds are used in calculating this.
    //the root panel gets a ox,oy of 0,0,for obvious reasons.
    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean select, IGrInDriver igd) {
        if (baseImage != null)
            igd.blitImage(imageX, imageY, elementBounds.width, elementBounds.height, ox, oy, baseImage);
        if (useScissoring) {
            ScissorGrInDriver sgi = new ScissorGrInDriver();
            Rect b = getBounds();
            sgi.workLeft = ox;
            sgi.workTop = oy;
            sgi.workRight = sgi.workLeft + b.width;
            sgi.workBottom = sgi.workTop + b.height;
            sgi.inner = igd;
            igd = sgi;
        }
        for (UIElement uie : allElements) {
            Rect r = uie.getBounds();
            uie.updateAndRender(ox + r.x, oy + r.y, deltaTime, (selectedElement == uie) && select, igd);
        }
    }

    //The click is within the (0,0,my width-1,my textHeight-1) range.
    @Override
    public void handleClick(MouseAction ma) {
        selectedElement = null;
        for (UIElement uie : allElements) {
            Rect r = uie.getBounds();
            if (r.contains(ma.x, ma.y)) {
                selectedElement = uie;
                uie.handleClick(ma.transform(r.x, r.y));
                return;
            }
        }
    }

    @Override
    public void handleDrag(int x, int y) {
        if (selectedElement != null) {
            Rect r = selectedElement.getBounds();
            selectedElement.handleDrag(x - r.x, y - r.y);
        }
    }

    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        for (UIElement uie : allElements) {
            Rect r = uie.getBounds();
            if (r.contains(x, y))
                uie.handleMousewheel(x - r.x, y - r.y, north);
        }
    }
}
