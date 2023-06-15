/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.io.File;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import gabien.render.IWSIImage;
import gabien.text.IFixedSizeFont;

/**
 * This instance should be the entry point to the program.
 * It should call the static method gabienapp.Application.gabienmain(),
 * which runs the application.
 * On UI-thread oriented platforms,
 * please run this on a different thread
 * (All drawing primitives from platforms I've seen
 *  can be accessed from any thread,probably because games
 *  like to use their own schedulers)
 *
 * A second utility of IGaBIEn is to confirm that a caller "has engine access".
 * This is because Java packages can sometimes be limiting for access control.
 * So having an instance of IGaBIEn is a useful shorthand for "I am engine code".
 * This property is verifiable by using GaBIEn.verify(IGaBIEn).
 */
public interface IGaBIEn {
    // NOTE regarding the following two functions.
    // The raw API doesn't bother to do resource overlays - that's managed entirely in gabien-common.

    /**
     * Gets a resource from the application binary.
     * Failing that, returns null.
     */
    InputStream getResource(String resource);

    IRawAudioDriver getRawAudio(); // This is a singleton, but may be created when used.
    void hintShutdownRawAudio();

    void ensureQuit();

    /**
     * Decodes an image from an InputStream.
     * On error, this version of the function should return null.
     */
    @Nullable IWSIImage decodeWSIImage(@NonNull InputStream a);

    // Make a WSI image from a buffer.
    // Note that the colours are 0xAARRGGBB.
    IWSIImage.RW createWSIImage(@NonNull int[] colours, int width, int height);

    /**
     * Gets font overrides UILabel can use.
     * Note that IGrDriver is expected to honor UILabel font override if given.
     * The first font override is the default font (if drawText is called on a driver without an override)
     * Note that this implies at least one font will be listed.
     */
    String[] getFontOverrides();

    /**
     * Returns a native font by size and name, unless it does not exist (in which case returns null).
     * The GaBIEn version of this method is partially cached.
     */
    @Nullable IFixedSizeFont getNativeFont(int size, @NonNull String name);

    /**
     * Returns the default/fallback native font.
     * The GaBIEn version of this method is partially cached.
     */
    @NonNull IFixedSizeFont getDefaultNativeFont(int size);

    boolean tryStartTextEditor(String fpath);

    /**
     * Works out a location for a native with the given name.
     * This may create a temporary file that will be deleted on exit.
     * This may also not do that.
     * The default implementation delegates to the gabien.natives.Loader JavaSE handling,
     *  as this is almost always what you want, except on Android which is picky.
     */
    default File nativeDestinationSetup(String name) {
        return gabien.natives.Loader.destinationSetupJavaSE(name);
    }
}
