/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.media.audio.fileio;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import gabien.media.audio.AudioIOSource;

/**
 * Created 20th October, 2023.
 */
public abstract class ReadAnySupportedAudioSource {
    private ReadAnySupportedAudioSource() {
    }

    public static AudioIOSource open(InputStream inp, boolean close) throws IOException {
        PushbackInputStream pb = new PushbackInputStream(inp);
        int b = pb.read();
        if (b == -1)
            throw new IOException("Empty file");
        pb.unread(b);
        if (b == 'O') {
            return OggVorbisSource.fromInputStream(pb, close);
        } else if (b == 'R') {
            return WavIO.readWAV(pb, close);
        } else if (b == 'M') {
            throw new IOException("MIDI not (yet?) supported");
        } else {
            // chances are high it's MP3
            throw new IOException("unknown (MP3?) - not (yet?) supported");
        }
    }
}
