/*
 * gabien-android - gabien backend for Android
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class RawAudioDriver implements IRawAudioDriver {
    public boolean keepAlive = true;

    public RawAudioDriver() {
        Thread t = new Thread() {
            @Override
            public void run() {
                AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 22050, AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT, 1024,
                        AudioTrack.MODE_STREAM);
                while (keepAlive) {
                    at.write(ras.pullData(512), 0, 1024);
                    if (at.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
                        at.play();
                }
            }
        };
        t.start();
    }

    private IRawAudioSource ras = new IRawAudioSource() {
        @Override
        public short[] pullData(int samples) {
            return new short[samples * 2];
        }
    };

    @Override
    public IRawAudioSource setRawAudioSource(IRawAudioSource src) {
        IRawAudioSource last = ras;
        ras = src;
        return last;
    }
}
