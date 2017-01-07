/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

import gabien.IGrInDriver;

public abstract class UIElement {
    protected Rect elementBounds = new Rect(0,0,1,1);
    public void setBounds(Rect r) {
        elementBounds=new Rect(r.x,r.y,r.width,r.height);
    }
    public Rect getBounds() {
        Rect r=new Rect(elementBounds.x,elementBounds.y,elementBounds.width,elementBounds.height);
        return r;
    }
    public abstract void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd);
    //
    public abstract void handleClick(int x, int y, int button);
    public void handleDrag(int x, int y) {

    }
}
