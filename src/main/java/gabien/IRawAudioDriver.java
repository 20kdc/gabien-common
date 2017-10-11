/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

/**
 * Created on 09/12/15.
 */
public interface IRawAudioDriver {
    interface IRawAudioSource {
        // Pull 22050hz 16-bit stereo samples
        // short array should be new short[samples*2], as each sample has L and R channels
        // can be called from another thread!
        short[] pullData(int samples);
    }

    // Set a new audio source, replacing the previous.
    IRawAudioSource setRawAudioSource(IRawAudioSource src);
}
