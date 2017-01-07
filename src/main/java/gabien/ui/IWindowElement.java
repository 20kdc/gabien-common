/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien.ui;

/**
 * Should be used for UIElement subclasses that might be windows.
 * The methods are only called by things that act as windowing interfaces.
 * Created on 12/30/16.
 */
public interface IWindowElement {
    boolean wantsSelfClose();
    // The window will not be processed after this occurs.
    void windowClosed();
}
