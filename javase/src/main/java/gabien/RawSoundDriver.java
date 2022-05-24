/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package gabien;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Note that it may be a good idea to move part of this into GaBIEn-Common, and
 * make ISoundDriver take raw data.
 * (Later on, that happened)
 */
final class RawSoundDriver implements IRawAudioDriver, Runnable {
    SourceDataLine sdl;

    public RawSoundDriver() throws LineUnavailableException {
        AudioFormat af = new AudioFormat(22050, 16, 2, true, true);
        sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af, 22050);
        soundthread.setName("GaBIEn.Sound");
        soundthread.setPriority(Thread.MAX_PRIORITY);
        soundthread.start();
    }

    private Thread soundthread = new Thread(this);
    private AtomicReference<IRawAudioSource> source = new AtomicReference<IRawAudioSource>(new IRawAudioSource() {
        @Override
        public short[] pullData(int samples) {
            return new short[samples * 2];
        }
    });

    public byte[] createData(int amount) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            short[] data = source.get().pullData(amount);
            int pt = 0;
            for (int px = 0; px < amount * 2; px++)
                dos.writeShort(data[pt++]);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean alive = true;

    @Override
    public void run() {
        while (alive) {
            // a is in sample frames now.
            // So a/2205 == amount of 10-millisecond blocks you can sleep in.
            int a = (sdl.available() / 4);
            if (a > 0) {
                byte[] bytes = createData(a);
                sdl.write(bytes, 0, bytes.length);
            } else if (!sdl.isRunning()) {
                System.err.println("SOUND:needed restart...");
                sdl.start();
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
            
        }
    }

    @Override
    public IRawAudioSource setRawAudioSource(IRawAudioSource src) {
        return source.getAndSet(src);
    }

    public void shutdown() {
        alive = false;
    }
}
