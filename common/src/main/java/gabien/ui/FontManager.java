/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui;

import gabien.GaBIEn;
import gabien.text.IFixedSizeFont;
import gabien.text.TextTools;

import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Just get this out of UILabel so I can continue doing meaningful stuff.
 * Created on 16th February 2018. Heavily gutted as of 23rd June, 2023.
 */
public final class FontManager {
    // Font override name.
    public final String fontOverride;
    public final boolean fontOverrideUE8;

    private ReentrantLock formatLock = new ReentrantLock();
    // Key format is a weird mess, check the relevant function
    private WeakHashMap<String, String> formatData = new WeakHashMap<String, String>();

    public FontManager(String fo, boolean ue8) {
        fontOverride = fo;
        fontOverrideUE8 = ue8;
    }

    private boolean useSystemFont(String text, int height) {
        if (fontOverride != null) {
            if (height > 8)
                return true;
            // <= 8 - still use system?
            if (fontOverrideUE8)
                return true;
        }
        // Does the font exist?
        if (height != 16)
            if (height != 8)
                if (height != 6)
                    return true;
        // Finally resolved to use internal if possible - is this allowed?
        int l = text.length();
        for (int p = 0; p < l; p++)
            if (text.charAt(p) >= 128)
                return true;
        return false;
    }

    /**
     * FontManager's font lookup function, finally accessible (mainly as a way to otherwise get rid of FontManager).
     */
    public IFixedSizeFont getFontForText(String text, int height) {
        if (useSystemFont(text, height))
            return GaBIEn.getNativeFont(height, fontOverride, true);
        return GaBIEn.engineFonts.derive(height);
    }

    // NOTE: This assumes the results are for the final content block.
    //       So it doesn't include the padding at the bottom.
    public Size getTextSize(String text, int textHeight) {
        int w = 0;
        IFixedSizeFont font = getFontForText(text, textHeight);
        int lineContentHeight = font.getContentHeight();
        int h = lineContentHeight;
        while (text.length() > 0) {
            int nlI = text.indexOf('\n');
            String tLine = text;
            if (nlI != -1) {
                tLine = tLine.substring(0, nlI);
                text = text.substring(nlI + 1);
                // Another line incoming, add pre-emptively.
                h += textHeight;
            } else {
                text = "";
            }
            w = Math.max(w, font.measureLine(tLine, false));
        }
        return new Size(w, h);
    }

    public int getFontSizeGeneralContentHeight(int textHeight) {
        return getFontForText("", textHeight).getContentHeight();
    }

    /**
     * Be careful with this function: It can be a performance hazard if used repeatedly on the same "block" of text.
     * Use getFontForText(...).measureLine(...) if you're going to do that.
     * In the two places this is used, it's used for good reason (though that may change as TextTools matures).
     */
    public int getLineLength(String text, int height) {
        return getFontForText(text, height).measureLine(text, false);
    }

    public String formatTextFor(String text, int textHeight, int width) {
        // This is a bunch of worst-case scenarios that should be ignored anyway
        if (width <= 0)
            return "";
        String key = width + ";" + textHeight + ";" + text;
        String res;
        // This takes a while, and is a critical path, particularly on Android.
        // So *cache it*.
        formatLock.lock();
        res = formatData.get(key);
        formatLock.unlock();
        if (res != null)
            return res;
        // Actually do the thing
        IFixedSizeFont font = getFontForText(text, textHeight);
        res = TextTools.formatTextFor(text, font, width);
        formatLock.lock();
        formatData.put(key, res);
        formatLock.unlock();
        return res;
    }
}
