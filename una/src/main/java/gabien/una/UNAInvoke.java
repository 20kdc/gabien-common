/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

/**
 * Invoke functions. An instance of the class represents an "invoke pattern".
 * The invoke pattern is a small VM for loading values.
 * Created 25th May, 2023.
 */
public class UNAInvoke {
    private final int variant;
    private final Command[] commands;

    public UNAInvoke(Convention cv, VType ret, VType[] args) {
        UNA.checkSetup();
        variant = 0;
        commands = new Command[0];
    }

    public final long call(
            long code,
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8, long i9, long iA, long iB, long iC, long iD, long iE, long iF
    ) {
        long a0 = 0, a1 = 0, a2 = 0, a3 = 0, a4 = 0, a5 = 0, a6 = 0, a7 = 0;
        long a8 = 0, a9 = 0, aA = 0, aB = 0, aC = 0, aD = 0, aE = 0, aF = 0;
        long f0 = 0, f1 = 0, f2 = 0, f3 = 0, f4 = 0, f5 = 0, f6 = 0, f7 = 0;
        for (Command cmd : commands) {
            long loadRegister = 0;
            switch (cmd.sourceReg) {
            case 0: loadRegister = i0; break; case 1: loadRegister = i1; break;
            case 2: loadRegister = i2; break; case 3: loadRegister = i3; break;
            case 4: loadRegister = i4; break; case 5: loadRegister = i5; break;
            case 6: loadRegister = i6; break; case 7: loadRegister = i7; break;
            case 8: loadRegister = i8; break; case 9: loadRegister = i9; break;
            case 10: loadRegister = iA; break; case 11: loadRegister = iB; break;
            case 12: loadRegister = iC; break; case 13: loadRegister = iD; break;
            case 14: loadRegister = iE; break; case 15: loadRegister = iF; break;
            }
            if (cmd.pullDown)
                loadRegister = loadRegister >> 32;
            switch (cmd.destReg) {
            case 0: a0 = loadRegister; break; case 1: a1 = loadRegister; break;
            case 2: a2 = loadRegister; break; case 3: a3 = loadRegister; break;
            case 4: a4 = loadRegister; break; case 5: a5 = loadRegister; break;
            case 6: a6 = loadRegister; break; case 7: a7 = loadRegister; break;
            case 8: a8 = loadRegister; break; case 9: a9 = loadRegister; break;
            case 10: aA = loadRegister; break; case 11: aB = loadRegister; break;
            case 12: aC = loadRegister; break; case 13: aD = loadRegister; break;
            case 14: aE = loadRegister; break; case 15: aF = loadRegister; break;
            case 16: f0 = loadRegister; break; case 17: f1 = loadRegister; break;
            case 18: f2 = loadRegister; break; case 19: f3 = loadRegister; break;
            case 20: f4 = loadRegister; break; case 21: f5 = loadRegister; break;
            case 22: f6 = loadRegister; break; case 23: f7 = loadRegister; break;
            }
        }
        return call(
                a0, a1, a2, a3, a4, a5, a6, a7,
                a8, a9, aA, aB, aC, aD, aE, aF,
                f0, f1, f2, f3, f4, f5, f6, f7,
                code, variant
        );
    }

    /**
     * Returns the amount of floating-point registers in the ABI.
     * Note that if the answer is 8, then the amount may be higher.
     */
    public static int countFloatRegisters() {
        for (int i = 0; i < 24; i++) {
            System.out.println(zcTest(i));
        }
        return 24;
    }
    private static int zcTest(int idx) {
        return (int) call(
                0, 1, 2, 3, 4, 5, 6, 7,
                8, 9, 10, 11, 12, 13, 14, 15,
                16, 17, 18, 19, 20, 21, 22, 23,
                UNA.getZeroCounter(idx), 0
        );
    }

    /* Invoke */
    private static native long call(
            long a0, long a1, long a2, long a3, long a4, long a5, long a6, long a7,
            long a8, long a9, long aA, long aB, long aC, long aD, long aE, long aF,
            long f0, long f1, long f2, long f3, long f4, long f5, long f6, long f7,
            long code, int variant);

    private static native long stdcall(
            long a0, long a1, long a2, long a3, long a4, long a5, long a6, long a7,
            long a8, long a9, long aA, long aB, long aC, long aD, long aE, long aF,
            long code, int variant);

    private static final class Command {
        public final int sourceReg;
        public final boolean pullDown;
        public final int destReg;
        public Command(int src, boolean pd, int dst) {
            sourceReg = src;
            pullDown = pd;
            destReg = dst;
        }
    }

    public static enum VType {
        I32,
        I64,
        F32,
        F64,
        Pointer
    }

    public static enum Convention {
        Default,
        Stdcall
    }
}
