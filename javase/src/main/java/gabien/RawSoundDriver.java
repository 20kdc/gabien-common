/*
 * gabien-javase - gabien backend for desktop Java
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.eclipse.jdt.annotation.NonNull;

import gabien.audio.IRawAudioDriver;
import gabien.backend.NullAudioSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This has been being revised since 2014 and it's still a mess.
 * Arguably even more of a mess than it was before.
 * Just so future people understand:
 * DO NOT BUILD AUDIO APIs BASED ON WRITING DATA TO STREAMS
 * JUST DON'T DO IT. PERIOD. IT IS A TERRIBLE IDEA.
 * Instead:
 * 1. Negotiate the size of buffers/packets/whatever you want to call them during stream opening.
 *    This gives the application a chance to allocate appropriate buffers that never need to be reallocated. 
 * 2. Always use callbacks. The assumption should be that the callback has X time to complete.
 *    X is the buffer size translated into audio time.
 * This comment written 18th June, 2023.
 */
final class RawSoundDriver implements IRawAudioDriver, Runnable {
    SourceDataLine sdl;

    public RawSoundDriver() throws LineUnavailableException {
        AudioFormat af = new AudioFormat(22050, 16, 2, true, false);
        sdl = AudioSystem.getSourceDataLine(af);
        sdl.open(af);
        sdl.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent event) {
                if (event.getType() == Type.STOP) {
                    try {
                        sdl.start();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        sdl.start();
        soundThread.setName("Sound");
        soundThread.setPriority(Thread.MAX_PRIORITY);
        soundThread.start();
    }

    private Thread soundThread = new Thread(this);

    private AtomicReference<IRawAudioSource> source = new AtomicReference<IRawAudioSource>(new NullAudioSource());

    public void createData(ShortBuffer dataB, short[] dataIL) {
        source.get().pullData(dataIL, 0, dataIL.length / 2);
        dataB.position(0);
        dataB.put(dataIL);
    }

    private boolean alive = true;

    @Override
    public void run() {
        int frames = 2048;

        // "ring-ish" buffer from which data is transferred
        byte[] dataB = new byte[frames * 4];
        ByteBuffer bb = ByteBuffer.wrap(dataB);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer fb = bb.asShortBuffer();
        // working buffers
        short[] dataIL = new short[frames * 2];
        while (alive) {
            createData(fb, dataIL);
            sdl.write(dataB, 0, dataB.length);
        }
    }

    @Override
    public @NonNull IRawAudioSource setRawAudioSource(@NonNull IRawAudioSource src) {
        return source.getAndSet(src);
    }

    public void shutdown() {
        alive = false;
    }
}
