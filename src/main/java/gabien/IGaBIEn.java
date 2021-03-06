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

    IRawAudioDriver getRawAudio(); // This is a singleton, but may be created when used.
    void hintShutdownRawAudio();

    void ensureQuit();

    IGrDriver makeOffscreenBuffer(int w, int h, boolean alpha);

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
}
