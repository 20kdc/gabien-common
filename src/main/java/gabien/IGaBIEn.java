/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import gabien.ui.IConsumer;

import java.io.InputStream;
import java.io.OutputStream;

/**
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
    double getTime();

    double timeDelta(boolean reset);

    // NOTE regarding the following two functions.
    // The raw API doesn't bother to do resource overlays - that's managed entirely in gabien-common.

    // Gets a resource from the application binary.
    // Failing that, returns null.
    InputStream getResource(String resource);

    // Looks for a file in the data directory.
    // Failing that, returns null.
    InputStream getFile(String resource);

    // Creates or overwrites a file in the data directory.
    // Failing that, returns null.
    OutputStream getOutFile(String resource);

    //Means that only one IGrInDriver can be active at a time.
    //Typically,the last will be active,and all others will be ignored.
    boolean singleWindowApp();

    IRawAudioDriver getRawAudio(); // This is a singleton, but may be created when used.
    void hintShutdownRawAudio();

    void ensureQuit();

    //On SingleWindowApp-style platforms,where windowing doesn't exist,ignore windowspecs.
    IGrInDriver makeGrIn(String name, int w, int h, WindowSpecs windowspecs);
    IGrDriver makeOffscreenBuffer(int w, int h, boolean alpha);

    WindowSpecs defaultWindowSpecs(String name, int w, int h);

    // Get an image.
    // Notably, the image format supports ARGB.
    // The colour-keying is just because it's simpler to make assets that way.
    // 'res' should use GaBIEnImpl.getResource, otherwise GaBIEnImpl.getFile
    IImage getImage(String a, boolean res);
    // Get an image - and then cut out a given colour.
    IImage getImageCK(String a, boolean res, int r, int g, int b);

    // Make an image from a buffer.
    // Note that the colours are 0xAARRGGBB.
    IImage createImage(int[] colours, int width, int height);

    void hintFlushAllTheCaches();

    int measureText(int i, String text);

    // Gets font overrides UILabel can use.
    // Note that IGrDriver is expected to honor UILabel font override if given.
    // The first font override is the default font (if drawText is called on a driver without an override)
    // Note that this implies at least one font will be listed.
    String[] getFontOverrides();

    String[] listEntries(String s);

    void makeDirectories(String s);

    boolean fileOrDirExists(String s);
    boolean dirExists(String s);

    boolean tryStartTextEditor(String fpath);

    void rmFile(String s);

    // Sets the file browser directory path.
    // Same path format as usual.
    void setBrowserDirectory(String s);

    // Starts a file browser.
    // This is a replacement for UIFileBrowser, and uses native elements whenever possible.
    // Regarding the path, the only guarantee is that it'll be null or a valid file path.
    // It does not necessarily have to match the standard gabien path separator.
    void startFileBrowser(String text, boolean saving, String exts, IConsumer<String> result);
}
