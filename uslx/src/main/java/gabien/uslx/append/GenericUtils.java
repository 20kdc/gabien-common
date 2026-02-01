/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */
package gabien.uslx.append;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created 1st February, 2026.
 */
public class GenericUtils {
    /**
     * Gets an array type's component, or returns null if not an array type.
     */
    public static Type getArrayComponentType(Type t) {
        if (t instanceof Class)
            return ((Class<?>) t).getComponentType();
        if (t instanceof GenericArrayType)
            return ((GenericArrayType) t).getGenericComponentType();
        return null;
    }
    /**
     * Converts a type to the corresponding concrete class.
     * Returns null if this is impossible.
     */
    public static Class<?> getConcreteType(Type t) {
        if (t instanceof Class) {
            return (Class<?>) t;
        } else if (t instanceof GenericArrayType) {
            Class<?> elmType = getConcreteType(((GenericArrayType) t).getGenericComponentType());
            // Ugh. This is bad code, and I should feel bad.
            return Array.newInstance(elmType, 0).getClass();
        } else if (t instanceof ParameterizedType) {
            // The way getRawType returns a Type here rather than a Class is weird.
            return getConcreteType(((ParameterizedType) t).getRawType());
        }
        return null;
    }
}
