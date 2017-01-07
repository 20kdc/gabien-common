/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;


import gabien.IGrInDriver;
import gabien.IGrInDriver.IImage;

public class UIButton extends UIElement {
    public IImage buttonImage;
    public int imageX,imageY;
    public Runnable onClick;
    public double pressedTime=0;
    @Override
    public void updateAndRender(int ox, int oy,double DeltaTime,boolean selected, IGrInDriver igd) {
        if (pressedTime>0)
        {
            pressedTime-=DeltaTime;
        }
        igd.blitImage(imageX, imageY, elementBounds.width, elementBounds.height, ox, oy+(pressedTime>0?4:0), buttonImage);
    }

    @Override
    public void handleClick(int x, int y,int button) {
    	if (button==1)
    	{
        onClick.run();
        pressedTime=0.5;
    	}
    }
    
}
