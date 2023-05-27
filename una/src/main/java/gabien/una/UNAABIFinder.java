/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

/**
 * Class to find ABIs.
 * Created 26th May, 2023.
 */
public abstract class UNAABIFinder {
    private static final UNAABI X86_STDCALL = new UNAABI(UNAInvoke.Mode.x86_stdcall, UNASysTypeInfo.si32, true, false, 0, 0, false);
    private static final UNAABI X86_CDECL = new UNAABI(UNAInvoke.Mode.x86_cdecl, UNASysTypeInfo.si32, true, false, 0, 0, false);
    private static final UNAABI X86_64_MS = new UNAABI(UNAInvoke.Mode.x86_64_windows, UNASysTypeInfo.si64, false, false, 4, 4, false);
    private static final UNAABI X86_64_UNIX = new UNAABI(UNAInvoke.Mode.x86_64_unix, UNASysTypeInfo.si64, false, false, 6, 8, false);

    private UNAABIFinder() {
    }

    public static UNAABI getABI(Convention ccv) {
        UNA.checkSetup();
        String target = UNA.getArchOS();
        if (target.startsWith("x86-windows-")) {
            if (ccv == Convention.Stdcall || ccv == Convention.StdcallOnWindows)
                return X86_STDCALL;
            return X86_CDECL;
        } else if (target.startsWith("x86-")) {
            // Pretty safe bet
            if (ccv == Convention.Stdcall)
                return X86_STDCALL;
            return X86_CDECL;
        } else if (target.startsWith("x86_64-windows-")) {
            return X86_64_MS;
        } else if (target.startsWith("x86_64-")) {
            // Another pretty safe bet
            return X86_64_UNIX;
        }
        throw new RuntimeException("Unknown ArchOS string " + target + "; correct or patch UNAABIFinder");
    }

    /**
     * Represents a convention attribute.
     * These can and will be deliberately aliased when a compiler would also do so.
     */
    public enum Convention {
        // no specified attributes
        Default,
        // stdcall
        Stdcall,
        // stdcall, but only on Windows (like Khronos standards)
        StdcallOnWindows
    }
}
