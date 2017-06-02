/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabien;

import java.io.InputStream;
import java.io.OutputStream;

/*
 * This instance should be the entry point to the program.
 * It should call the static method gabienapp.Application.gabienmain(),
 * which runs the application.
 * On UI-thread oriented platforms,
 * please run this on a different thread
 * (All drawing primitives from platforms I've seen
 *  can be accessed from any thread,probably because games
 *  like to use their own schedulers)
 */

public interface IGaBIEn {
    public double getTime();

    public double timeDelta(boolean reset);

    // Gets a resource from the application binary.
    // Failing that, returns null.
    public InputStream getResource(String resource);

    // Runs getResource, then looks for a file in the data directory.
    // Failing that, returns null.
    public InputStream getFile(String resource);

    // Creates or overwrites a file in the data directory.
    // Failing that, returns null.
    public OutputStream getOutFile(String resource);

    //Means that only one IGrInDriver can be active at a time.
    //Typically,the last will be active,and all others will be ignored.
    public boolean singleWindowApp();

    public IRawAudioDriver getRawAudio(); // This is a singleton.

    public void ensureQuit();

    //On SingleWindowApp-style platforms,where windowing doesn't exist,ignore windowspecs.
    public IGrInDriver makeGrIn(String name, int w, int h, WindowSpecs windowspecs);

    public WindowSpecs defaultWindowSpecs(String name, int w, int h);

    // Get an image. See GaBIEn.getFile for how this works.
    // Notably, the image format supports ARGB.
    // The colour-keying is just because it's simpler to make assets that way.
    public IGrInDriver.IImage getImage(String a);
    // Get an image - and then cut out a given colour.
    public IGrInDriver.IImage getImageCK(String a, int r, int g, int b);

    // Make an image from a buffer.
    // Note that the colours are 0xAARRGGBB.
    public IGrInDriver.IImage createImage(int[] colours, int width, int height);

    public void hintFlushAllTheCaches();

    public int measureText(int i, String text);
}
