/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Abstraction for RIFF metadata.
 * Created on 7th June 2022 as part of project VE2Bun
 */
public class RIFFInfoChunk {
    public HashMap<String, byte[]> keys = new HashMap<String, byte[]>();

    public String getAsString(String id, String def) {
        byte[] res = keys.get(id);
        if (res != null)
            return new String(res, StandardCharsets.UTF_8);
        return def;
    }
    
    public void putAsString(String id, String value) {
        keys.put(id, value.getBytes(StandardCharsets.UTF_8));
    }
    
    public void readInterior(@NonNull InputStream is) throws IOException {
        while (is.available() > 0) {
            RIFFInputStream ris = new RIFFInputStream(is);
            keys.put(ris.chunkId, ris.readToEnd());
            ris.close();
        }
    }

    public void write(@NonNull OutputStream os) throws IOException {
        RIFFOutputStream ros = new RIFFOutputStream(os, "LIST");
        ros.writeBytes("INFO");
        for (Entry<String, byte[]> entry : keys.entrySet())
            RIFFOutputStream.putChunk(ros, entry.getKey(), entry.getValue());
        ros.close();
    }
}
