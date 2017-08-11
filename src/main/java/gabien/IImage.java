package gabien;

/**
 * An image. All IImages that are not IGrDrivers must be immutable.
 * Created on 11/08/17.
 */
public interface IImage {
    int getWidth();

    int getHeight();

    // 0xAARRGGBB. The buffer is safe to edit, but changes do not propagate back.
    int[] getPixels();

    // Creates a PNG file.
    byte[] createPNG();
}
