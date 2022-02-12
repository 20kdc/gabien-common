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
        sdl.open(af, 8820);
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
            int[] L = new int[amount];
            int[] R = new int[amount];
            short[] data = source.get().pullData(amount);
            for (int px = 0; px < amount; px++) {
                L[px] += data[(px * 2)];
                R[px] += data[(px * 2) + 1];
            }
            for (int px = 0; px < amount; px++) {
                if (L[px] > 32767)
                    L[px] = 32767;
                if (L[px] < -32768)
                    L[px] = -32768;
                if (R[px] > 32767)
                    R[px] = 32767;
                if (R[px] < -32768)
                    R[px] = -32768;
                dos.writeShort(L[px]);
                dos.writeShort(R[px]);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean alive = true;

    @Override
    public void run() {
        int buf = 800; // Increase this to decrease CPU usage, but at a cost of latency
        while (alive) {
            int a = (sdl.available() / 4);
            // a is in sample frames now.
            // So a/2205 == amount of 10-millisecond blocks you can sleep in.
            while (a < buf) {
                try {
                    Thread.sleep((2205 - a) / 221);
                } catch (InterruptedException e) {
                }
                a = (sdl.available() / 4);
            }
            a = (sdl.available() / 4);
            if (a <= 0) {
                continue;
            }
            byte[] bytes = createData(a);

            sdl.write(bytes, 0, bytes.length);
            if (!sdl.isRunning()) {
                System.err.println("SOUND:needed restart...");
                sdl.start();
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
