/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package gabien.uslx.append;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * NOTE: Creating a RIFFOutputStream implicitly starts a RIFF chunk.
 * Closing one does NOT close the parent OutputStream.
 * Created on 6th June 2022 as part of project VE2Bun
 */
public class RIFFOutputStream extends XEDataOutputStream {
    private @NonNull final XEDataOutputStream targetDos;
    private @Nullable final ByteArrayOutputStream buffer;
    private final int plannedLength;

    /**
     * Opens a RIFF chunk with an unknown length.
     * The contents will be buffered in memory until closing.
     * @param base Target output stream to write to.
     * @param id RIFF chunk ID.
     * @throws IOException
     */
    public RIFFOutputStream(@NonNull OutputStream base, @NonNull String id) throws IOException {
        this(new XEDataOutputStream(base), id);
    }

    /**
     * Opens a RIFF chunk with an unknown length.
     * The contents will be buffered in memory until closing.
     * @param base Target output stream to write to.
     * @param id RIFF chunk ID.
     * @throws IOException
     */
    public RIFFOutputStream(@NonNull XEDataOutputStream base, @NonNull String id) throws IOException {
        super(new ByteArrayOutputStream());
        targetDos = new XEDataOutputStream(base);
        buffer = (ByteArrayOutputStream) out;
        if (id.length() != 4)
            throw new IOException("RIFF chunk ID must be 4 characters.");
        targetDos.writeBytes(id);
        plannedLength = -1;
    }

    public static int getInteriorChunkSize(int len) {
        return 8 + len + (len & 1);
    }

    /**
     * Opens a RIFF chunk with a known length.
     * @param base Target output stream to write to.
     * @param id RIFF chunk ID.
     * @param length Length of chunk.
     * @throws IOException
     */
    public RIFFOutputStream(@NonNull OutputStream base, @NonNull String id, int length) throws IOException {
        super(base);
        targetDos = this;
        buffer = null;
        if (id.length() != 4)
            throw new IOException("RIFF chunk ID must be 4 characters.");
        writeBytes(id);
        writeInt(length);
        written = 0;
        plannedLength = length;
    }

    /**
     * Writes a RIFF chunk with known contents.
     * @param os Target output stream to write to.
     * @param id RIFF chunk ID.
     * @param content Contents.
     * @throws IOException
     */
    public static void putChunk(OutputStream os, String id, byte[] content) throws IOException {
        putChunk(new XEDataOutputStream(os), id, content);
    }

    /**
     * Writes a RIFF chunk with known contents.
     * @param os Target output stream to write to.
     * @param id RIFF chunk ID.
     * @param content Contents.
     * @throws IOException
     */
    public static void putChunk(XEDataOutputStream os, String id, byte[] content) throws IOException {
        if (id.length() != 4)
            throw new IOException("RIFF chunk ID must be 4 characters.");
        os.writeBytes(id);
        os.writeInt(content.length);
        os.write(content);
        if ((content.length & 1) != 0)
            os.write(0);
    }

    @Override
    public void close() throws IOException {
        if (buffer != null) {
            int sz = buffer.size();
            targetDos.writeInt(sz);
            buffer.writeTo(targetDos);
        } else {
            if (written != plannedLength)
                throw new IOException("Fixed-size RIFFOutputStream was told to write " + plannedLength + " but got " + written + ".");
            if ((plannedLength & 1) != 0)
                out.write(0);
        }
        // This prevents further writing.
        out = null;
    }
}
