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
class UNAInvoke implements IUNAFnType {
    public static final int BASE_A = 0;
    public static final int SIZE_A = 16;
    public static final int BASE_F = 16;
    // effective size of F-file is ABI-defined
    private final Mode mode;
    private final UNAType ret;
    private final UNAType[] args;
    private final Command[] commands;

    public UNAInvoke(Mode m, UNAType ret, UNAType[] args, Command[] cmds) {
        mode = m;
        this.ret = ret;
        this.args = args;
        commands = cmds;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("inv{m=");
        sb.append(mode);
        sb.append(",r=");
        sb.append(ret);
        sb.append(",c=[");
        for (UNAType a : args) {
            sb.append(a);
            sb.append(',');
        }
        sb.append("],c=[");
        for (Command c : commands) {
            sb.append(c);
            sb.append(',');
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public final long call(
            long code,
            long i0, long i1, long i2, long i3, long i4, long i5, long i6, long i7,
            long i8, long i9, long iA, long iB, long iC, long iD, long iE, long iF
    ) {
        // Sign-extend args
        if (args.length >= 1)
            i0 = args[0].signExtend(i0);
        if (args.length >= 2)
            i1 = args[1].signExtend(i1);
        if (args.length >= 3)
            i2 = args[2].signExtend(i2);
        if (args.length >= 4)
            i3 = args[3].signExtend(i3);
        if (args.length >= 5)
            i4 = args[4].signExtend(i4);
        if (args.length >= 6)
            i5 = args[5].signExtend(i5);
        if (args.length >= 7)
            i6 = args[6].signExtend(i6);
        if (args.length >= 8)
            i7 = args[7].signExtend(i7);
        if (args.length >= 9)
            i8 = args[8].signExtend(i8);
        if (args.length >= 10)
            i9 = args[9].signExtend(i9);
        if (args.length >= 11)
            iA = args[10].signExtend(iA);
        if (args.length >= 12)
            iB = args[11].signExtend(iB);
        if (args.length >= 13)
            iC = args[12].signExtend(iC);
        if (args.length >= 14)
            iD = args[13].signExtend(iD);
        if (args.length >= 15)
            iE = args[14].signExtend(iE);
        if (args.length >= 16)
            iF = args[15].signExtend(iF);
        // Actually run the scary VM
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
            loadRegister = loadRegister >> cmd.shiftDown;
            loadRegister &= cmd.mask;
            loadRegister = loadRegister << cmd.shiftUp;
            switch (cmd.destReg) {
            case 0: a0 |= loadRegister; break; case 1: a1 |= loadRegister; break;
            case 2: a2 |= loadRegister; break; case 3: a3 |= loadRegister; break;
            case 4: a4 |= loadRegister; break; case 5: a5 |= loadRegister; break;
            case 6: a6 |= loadRegister; break; case 7: a7 |= loadRegister; break;
            case 8: a8 |= loadRegister; break; case 9: a9 |= loadRegister; break;
            case 10: aA |= loadRegister; break; case 11: aB |= loadRegister; break;
            case 12: aC |= loadRegister; break; case 13: aD |= loadRegister; break;
            case 14: aE |= loadRegister; break; case 15: aF |= loadRegister; break;
            case 16: f0 |= loadRegister; break; case 17: f1 |= loadRegister; break;
            case 18: f2 |= loadRegister; break; case 19: f3 |= loadRegister; break;
            case 20: f4 |= loadRegister; break; case 21: f5 |= loadRegister; break;
            case 22: f6 |= loadRegister; break; case 23: f7 |= loadRegister; break;
            }
        }
        long rv = 0;
        switch (mode) {
        case x86_cdecl: rv = c86c(
                (int) a0, (int) a1, (int) a2, (int) a3, (int) a4, (int) a5, (int) a6, (int) a7,
                (int) a8, (int) a9, (int) aA, (int) aB, (int) aC, (int) aD, (int) aE, (int) aF,
                (int) code
                );
        case x86_stdcall: rv = c86s(
                (int) a0, (int) a1, (int) a2, (int) a3, (int) a4, (int) a5, (int) a6, (int) a7,
                (int) a8, (int) a9, (int) aA, (int) aB, (int) aC, (int) aD, (int) aE, (int) aF,
                (int) code, args.length
                );
        case x86_64_windows: rv = c8664w(
                a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, aA, aB, aC, aD, aE, aF,
                code, ret.isFP, f0, f1, f2, f3
                );
        case x86_64_unix: rv = c8664u(
                a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, aA, aB, aC, aD, aE, aF,
                code, ret.isFP, f0, f1, f2, f3, f4, f5, f6, f7
                );
        }
        return ret.signExtend(rv);
    }

    /* Invoke */
    public static native long c86c(
            int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7,
            int a8, int a9, int aA, int aB, int aC, int aD, int aE, int aF,
            int code);

    public static native long c86s(
            int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7,
            int a8, int a9, int aA, int aB, int aC, int aD, int aE, int aF,
            int code, int ac);

    private static native long c8664w(
            long a0, long a1, long a2, long a3, long a4, long a5, long a6, long a7,
            long a8, long a9, long aA, long aB, long aC, long aD, long aE, long aF,
            long code, boolean doubleRet,
            long f0, long f1, long f2, long f3);

    private static native long c8664u(
            long a0, long a1, long a2, long a3, long a4, long a5, long a6, long a7,
            long a8, long a9, long aA, long aB, long aC, long aD, long aE, long aF,
            long code, boolean doubleRet,
            long f0, long f1, long f2, long f3, long f4, long f5, long f6, long f7);

    public static final class Command {
        public final int sourceReg;
        public final int shiftDown;
        public final long mask;
        public final int shiftUp;
        public final int destReg;
        public Command(int src, int shiftDown, long mask, int shiftUp, int dst) {
            sourceReg = src;
            this.shiftDown = shiftDown;
            this.mask = mask;
            this.shiftUp = shiftUp;
            destReg = dst;
        }
        @Override
        public String toString() {
            return "s" + sourceReg + "d" + destReg;
        }
    }

    public static enum Mode {
        x86_cdecl,
        x86_stdcall,
        x86_64_windows,
        x86_64_unix,
    }
}