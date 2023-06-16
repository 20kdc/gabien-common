/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import org.eclipse.jdt.annotation.NonNull;

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
         * Short array should be new short[samples*2], as each sample has L and R channels.
         * Can be called from another thread!
         */
        @NonNull short[] pullData(int samples);
    }

    /**
     * Set a new audio source, replacing the previous.
     */
    @NonNull IRawAudioSource setRawAudioSource(@NonNull IRawAudioSource src);
}
