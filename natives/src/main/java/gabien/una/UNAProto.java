/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.una;

/**
 * Details of the function signature required by all ABIs.
 * This represents a C prototype (with the var-args specialized).
 * Created 27th May 2023.
 */
public final class UNAProto {
    public final UNAType ret;
    // Package-local so that it can be borrowed by trusted code, but meant to be secret so it's read-only
    UNAType[] args;
    // If -1, this isn't a VA function. Otherwise, index of first VA arg.
    public final int vaIdx;

    public UNAProto(UNAType r, UNAType[] a, int vai) {
        ret = r;
        args = new UNAType[a.length];
        System.arraycopy(a, 0, args, 0, a.length);
        vaIdx = vai;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ret.protoChar);
        sb.append('(');
        for (int i = 0; i < args.length; i++) {
            if (vaIdx == i)
                sb.append('|');
            sb.append(args[i].protoChar);
        }
        sb.append(')');
        return sb.toString();
    }

    public int getArgCount() {
        return args.length;
    }

    public UNAType getArg(int i) {
        return args[i];
    }
}
