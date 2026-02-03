/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.audio;

/**
 * Interface to an audio output.
 * Created on 09/12/15.
 */
public interface IRawAudioDriver {
    /**
     * Interface to an audio source.
     */
    public interface IRawAudioSource {
        /**
         * Pull 22050hz 16-bit stereo samples.
         * Interleaved contains interleaved left/right samples.
         * Offset is an absolute, while frames is the number of frames (each frame being a left and right sample).
         * Can be called from another thread.
         */
        void pullData(short[] interleaved, int ofs, int frames);
    }

    /**
     * Set a new audio source, replacing the previous.
     */
    IRawAudioSource setRawAudioSource(IRawAudioSource src);
}
