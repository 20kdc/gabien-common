/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.una;

/**
 * Base ABI class.
 * Deliberately independent of static UNA class values.
 * Extracted from what is now UNAABIThreefold on 27th May 2023.
 */
public abstract class UNAABI {
    protected final UNASysTypeInfo typeInfo;

    public UNAABI(UNASysTypeInfo typeInfo) {
        this.typeInfo = typeInfo;
    }

    /**
     * Emulates the compiler's argument GP/FP allocator to produce an invoker.
     */
    public abstract IUNAFnType of(UNAProto proto);

    /**
     * Emulates the compiler's argument GP/FP allocator to produce an invoker.
     */
    public IUNAFnType of(String sig) {
        return of(typeInfo.sig(sig));
    }

}