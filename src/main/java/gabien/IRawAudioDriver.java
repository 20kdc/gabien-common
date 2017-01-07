/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
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
    void setRawAudioSource(IRawAudioSource src);
}
