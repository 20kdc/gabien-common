/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien;

public class WindowSpecs {
	//Stop non-GaBIen classes from creating this object.
	//This way, I can extend it.
	protected WindowSpecs() {}
	public int scale = 1;
    public boolean resizable = false;
}
