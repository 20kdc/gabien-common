/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

public class Rect {
    public int x,y,width,height;
    public Rect(int i, int i0, int i1, int i2) {
        x=i;
        y=i0;
        width=i1;
        height=i2;
    }

    public boolean contains(int x, int y) {
        if (x>=this.x)
            if (y>=this.y)
                if (x<this.x+width)
                    if (y<this.y+height)
                        return true;
        return false;
    }
    private boolean lineintersects(int A,int AL,int B,int BL)
    {
        if (A>=B)
            if (A<B+BL)
                return true;
        if (B>=A)
            if (B<A+AL)
                return true;
        return false;
    }
    public boolean intersects(Rect rect) {
        if (lineintersects(rect.x,rect.width,x,width))
        {
        if (lineintersects(rect.y,rect.height,y,height))
        {
            return true;
        }
        }
        return false;
    }
}
