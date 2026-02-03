/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.media.midi.newsynth;

import java.util.Arrays;
import java.util.LinkedList;

import org.eclipse.jdt.annotation.Nullable;

import datum.DatumInvalidVisitor;
import datum.DatumSrcLoc;
import datum.DatumVisitor;
import datum.DatumWriter;
import gabien.datum.DatumKVDVisitor;
import gabien.datum.DatumTreeCallbackVisitor;
import gabien.media.midi.MIDISynthesizer;
import gabien.media.midi.MIDISynthesizer.Channel;

/**
 * The New Synth's palette
 * 3rd July, 2025
 */
public class NSPalette implements MIDISynthesizer.Palette {
    // Every program in the palette (including unused ones...)
    public final LinkedList<NSPatch> patches = new LinkedList<>();
    // 128 melodic programs followed by 128 percussive programs
    public final NSPatch[] programList = new NSPatch[256];

    @Override
    public @Nullable Channel create(MIDISynthesizer parent, int bank, int program, int note, int velocity) {
        int baseProgram = 0;
        if (bank >= 128) {
            baseProgram = 128;
            program = note + 128;
        }
        NSPatch resultingPatch = programList[program];
        if (resultingPatch == null)
            resultingPatch = programList[baseProgram];
        if (resultingPatch == null)
            return null;
        return new NSChannel(resultingPatch);
    }

    public void writeToDatum(DatumWriter writer) {
        for (NSPatch patch : patches) {
            writer.visitId("patch", DatumSrcLoc.NONE);
            patch.writeToDatum(writer);
            writer.visitNewline();
            for (int i = 0; i < programList.length; i++) {
                if (programList[i] == patch) {
                    writer.visitId("assign", DatumSrcLoc.NONE);
                    writer.visitInt(i, DatumSrcLoc.NONE);
                    writer.visitNewline();
                }
            }
        }
    }

    public DatumVisitor createDatumReadVisitor() {
        patches.clear();
        Arrays.fill(programList, null);
        return new DatumKVDVisitor() {
            @Nullable NSPatch lastPatch;
            @SuppressWarnings("null")
            @Override
            public DatumVisitor handle(String key, DatumSrcLoc loc) {
                if (key.equals("patch")) {
                    NSPatch newPatch = new NSPatch();
                    lastPatch = newPatch;
                    patches.add(newPatch);
                    return newPatch.createDatumReadVisitor();
                }
                if (key.equals("assign"))
                    return new DatumTreeCallbackVisitor<Long>((obj) -> {
                        programList[(int) (long) obj] = lastPatch;
                    });
                return DatumInvalidVisitor.INSTANCE;
            }
        };
    }
}
