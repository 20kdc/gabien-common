/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

import java.util.LinkedList;

/**
 * Class for an ABI. Might become abstract one day.
 * Deliberately independent of static UNA class values.
 * Created 26th May, 2023.
 */
public final class UNAABI {
    private final UNAInvoke.Mode base;
    private final boolean is32;
    private final int pullDownWordIndex;
    private final int gpStart, gpEnd;
    private final int fpStart, fpEnd;
    private final int stackStart, stackEnd;
    private final boolean fpMigratesToGP;

    UNAABI(UNAInvoke.Mode b, boolean b32, boolean be, int gpc, int fpc, boolean fpm) {
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
    public IUNAProto synthesize(VType ret, VType[] args) {
        LinkedList<UNAInvoke.Command> llc = new LinkedList<>();
        // Allocators
        int nextGP = gpStart;
        int nextFP = fpStart;
        int nextStack = stackStart;
        // Main arg loop
        for (int i = 0; i < args.length; i++) {
            VType arg = args[i];
            boolean canGPAlloc = true;
            // FP
            if (arg.isFP) {
                if (nextFP != fpEnd) {
                    // Allocated to FP file
                    llc.add(new UNAInvoke.Command(i, 0, -1L, 0, nextFP++));
                    continue;
                }
                canGPAlloc = fpMigratesToGP;
            }
            boolean arg2W = arg.get2Word(is32);
            int words = arg2W ? 2 : 1;
            for (int w = 0; w < words; w++) {
                boolean pullDown = arg2W && w == pullDownWordIndex;
                // GP
                if (canGPAlloc)
                    if (nextGP != gpEnd) {
                        llc.add(new UNAInvoke.Command(i, pullDown ? 32 : 0, -1L, 0, nextGP++));
                        continue;
                    }
                // well, that failed! what about stack?
                if (nextStack == stackEnd)
                    throw new RuntimeException("The provided arguments exceeded the capacity of UNA's invoking function.");
                llc.add(new UNAInvoke.Command(i, 0, -1L, 0, nextStack++));
            }
        }
        // Finish up
        return new UNAInvoke(base, ret.isFP, ret.getIs32(is32), args.length, llc.toArray(new UNAInvoke.Command[0]));
    }

    /**
     * Value type.
     */
    public static enum VType {
        I8(false, true, true),
        I16(false, true, true),
        I32(false, true, true),
        I64(false, false, false),
        Float(true, true, true),
        Double(true, false, false),
        Pointer(false, true, false);
        private final boolean isFP, is32in32, is32in64;
        private VType(boolean isFP, boolean is32in32, boolean is32in64) {
            this.isFP = isFP;
            this.is32in32 = is32in32;
            this.is32in64 = is32in64;
        }
        private boolean getIs32(boolean abiIs32) {
            return abiIs32 ? is32in32 : is32in64;
        }
        private boolean get2Word(boolean abiIs32) {
            if (!abiIs32)
                return false;
            return !getIs32(abiIs32);
        }
        public static VType ofChar(char chr) {
            if (chr == 'I')
                return VType.I32;
            if (chr == 'L')
                return VType.I64;
            if (chr == 'F')
                return VType.Float;
            if (chr == 'D')
                return VType.Double;
            if (chr == 'P')
                return VType.Pointer;
            return null;
        }
    }
}
