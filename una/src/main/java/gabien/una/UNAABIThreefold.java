/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.util.LinkedList;

/**
 * Class for an ABI.
 * Created 26th May, 2023.
 */
public final class UNAABIThreefold extends UNAABI {
    private final UNAInvoke.Mode base;
    private final boolean is32;
    private final int pullDownWordIndex;
    private final int gpStart, gpEnd;
    private final int fpStart, fpEnd;
    private final int stackStart, stackEnd;
    private final boolean fpMigratesToGP;

    UNAABIThreefold(UNAInvoke.Mode b, UNASysTypeInfo ti, boolean b32, boolean be, int gpc, int fpc, boolean fpm) {
        super(ti);
        base = b;
        is32 = b32;
        pullDownWordIndex = be ? 0 : 1;

        gpStart = UNAInvoke.BASE_A;
        gpEnd = UNAInvoke.BASE_A + gpc;

        stackStart = UNAInvoke.BASE_A + gpc;
        stackEnd = UNAInvoke.BASE_A + UNAInvoke.SIZE_A;

        fpStart = UNAInvoke.BASE_F;
        fpEnd = UNAInvoke.BASE_F + fpc;

        fpMigratesToGP = fpm;
    }

    /**
     * Emulates the compiler's argument GP/FP allocator to produce an invoker.
     */
    @Override
    public IUNAFnType of(UNAProto proto) {
        LinkedList<UNAInvoke.Command> llc = new LinkedList<>();
        // Allocators
        int nextGP = gpStart;
        int nextFP = fpStart;
        int nextStack = stackStart;
        // Main arg loop
        for (int i = 0; i < proto.args.length; i++) {
            int inputIdx = i;
            UNAType arg = proto.args[i];
            boolean canGPAlloc = true;
            // FP
            if (arg.isFP) {
                if (nextFP != fpEnd) {
                    // Allocated to FP file
                    llc.add(new UNAInvoke.Command(inputIdx, 0, -1L, 0, nextFP++));
                    continue;
                }
                canGPAlloc = fpMigratesToGP;
            }
            boolean arg2W = get2Word(arg);
            int words = arg2W ? 2 : 1;
            for (int w = 0; w < words; w++) {
                boolean pullDown = arg2W && w == pullDownWordIndex;
                // GP
                if (canGPAlloc)
                    if (nextGP != gpEnd) {
                        llc.add(new UNAInvoke.Command(inputIdx, pullDown ? 32 : 0, -1L, 0, nextGP++));
                        continue;
                    }
                // well, that failed! what about stack?
                if (nextStack == stackEnd)
                    throw new RuntimeException("The provided arguments exceeded the capacity of UNA's invoking function.");
                llc.add(new UNAInvoke.Command(inputIdx, 0, -1L, 0, nextStack++));
            }
        }
        // Finish up
        return new UNAInvoke(base, proto.ret, proto.args, llc.toArray(new UNAInvoke.Command[0]));
    }
    public boolean get2Word(UNAType ut) {
        return ut.bytes > 4 && is32;
    }
}
