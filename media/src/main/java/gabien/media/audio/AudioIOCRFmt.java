/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.audio;

/**
 * Channel/rate/format setup.
 * Created on 7th June 2022 as part of project VE2Bun
 */
public class AudioIOCRFmt extends AudioIOCRSet {
    public final AudioIOFormat format;

    public AudioIOCRFmt(AudioIOFormat fmt, AudioIOCRSet set) {
        super(set);
        format = fmt;
    }

    public AudioIOCRFmt(AudioIOFormat detect, int channels, int channelMask, int sampleRate) {
        super(channels, channelMask, sampleRate);
        format = detect;
    }
}
