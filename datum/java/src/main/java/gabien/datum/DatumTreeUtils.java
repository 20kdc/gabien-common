/*
 * gabien-datum - Quick to implement S-expression format
 * Written starting in 2023 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.datum;

import java.util.List;

/**
 * Created 17th February 2023
 */
public final class DatumTreeUtils {
    private DatumTreeUtils() {
        
    }

    public static DatumDecToLambdaVisitor decVisitor(DatumDecToLambdaVisitor.Handler h) {
        return new DatumDecToLambdaVisitor(h);
    }

    public static DatumSymbol sym(String s) {
        return new DatumSymbol(s);
    }

    public static boolean isSym(Object o, String s) {
        if (o instanceof DatumSymbol)
            return ((DatumSymbol) o).id.equals(s);
        return false;
    }

    public static int cInt(Object o) {
        return ((Number) o).intValue();
    }

    public static long cLong(Object o) {
        return ((Number) o).longValue();
    }

    public static double cDouble(Object o) {
        return ((Number) o).doubleValue();
    }

    public static float cFloat(Object o) {
        return ((Number) o).floatValue();
    }

    /**
     * Best not to confuse this with Arrays.asList.
     */
    @SuppressWarnings("unchecked")
    public static List<Object> cList(Object o) {
        return (List<Object>) o;
    }
}
