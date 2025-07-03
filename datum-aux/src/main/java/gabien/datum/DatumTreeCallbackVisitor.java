/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.datum;

import java.util.function.Consumer;

import datum.DatumPositionedException;
import datum.DatumSrcLoc;
import datum.DatumTreeVisitor;

/**
 * Does some casting shenanigans for a convenient API.
 * Created 2nd July 2025.
 */
public class DatumTreeCallbackVisitor<O> extends DatumTreeVisitor {
    public final Consumer<O> target;
    public DatumTreeCallbackVisitor(Consumer<O> target) {
        this.target = target;
    }

    @Override
    public void visitEnd(DatumSrcLoc loc) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void visitTree(Object obj, DatumSrcLoc srcLoc) {
        try {
            target.accept((O) obj);
        } catch (Exception ex) {
            throw new DatumPositionedException(srcLoc, ex);
        }
    }
}
