/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package gabien;

/**
 * Offscreen buffers! (You should still be passing IGrInDriver everywhere except when using one of these to, IDK, cache map view stuff.)
 * Created on 04/06/17.
 */
public interface IOsbDriver extends IGrDriver, IGrInDriver.IImage {
}
