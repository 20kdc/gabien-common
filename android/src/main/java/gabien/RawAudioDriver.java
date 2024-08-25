/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNull;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import gabien.audio.IRawAudioDriver;
import gabien.backend.NullAudioSource;

public class RawAudioDriver implements IRawAudioDriver {
    public boolean keepAlive = true;

    public RawAudioDriver() {
        Thread t = new Thread() {
            @Override
            public void run() {
                System.err.println("gabien.RawAudioDriver: Audio start...");
                while (keepAlive) {
                    AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 22050, AudioFormat.CHANNEL_OUT_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT, 1024,
                            AudioTrack.MODE_STREAM);
                    if (at.getState() != AudioTrack.STATE_INITIALIZED) {
                        System.err.println("gabien.RawAudioDriver: Audio track failed to initialize!!!");
                    }
                    short[] dataIL = new short[1024];
                    while (keepAlive) {
                        source.get().pullData(dataIL, 0, dataIL.length / 2);
                        // ERROR_DEAD_OBJECT
                        if (at.write(dataIL, 0, dataIL.length) < 0) {
                            System.err.println("gabien.RawAudioDriver: Audio track returned write error. Assuming dead");
                            break;
                        }
                        if (at.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                            System.err.println("gabien.RawAudioDriver: Audio getPlayState rerun");
                            at.play();
                        }
                    }
                    at.release();
                }
                System.err.println("gabien.RawAudioDriver: Audio stop...");
            }
        };
        t.start();
    }

    private AtomicReference<IRawAudioSource> source = new AtomicReference<IRawAudioSource>(new NullAudioSource());

    @Override
    public @NonNull IRawAudioSource setRawAudioSource(@NonNull IRawAudioSource src) {
        return source.getAndSet(src);
    }
}
