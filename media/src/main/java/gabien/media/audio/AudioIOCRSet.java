/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.audio;

/**
 * Channel/rate setup.
 * Created on 6th June 2022 as part of project VE2Bun
 */
public class AudioIOCRSet {
    public final int channels;
    public final int channelMask;
    public final int sampleRate;

    /**
     * Creates a new AudioIOCRSet with default channel mappings.
     * @param ch Channel count
     * @param sr Sample rate
     */
    public AudioIOCRSet(int ch, int sr) {
        channels = ch;
        channelMask = 0;
        sampleRate = sr;
    }

    /**
     * Creates a new AudioIOCRSet with a specific channel mask.
     * @param ch Channel count
     * @param ch Channel mask
     * @param sr Sample rate
     */
    public AudioIOCRSet(int ch, int chm, int sr) {
        channels = ch;
        channelMask = chm;
        sampleRate = sr;
    }

    /**
     * Creates a new AudioIOCRSet from another.
     * @param cr Source
     */
    public AudioIOCRSet(AudioIOCRSet cr) {
        channels = cr.channels;
        channelMask = cr.channelMask;
        sampleRate = cr.sampleRate;
    }
}
