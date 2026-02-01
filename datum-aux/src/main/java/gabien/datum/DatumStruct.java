/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.datum;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import datum.DatumSrcLoc;
import datum.DatumSymbol;
import datum.DatumTreeUtils;
import datum.DatumVisitor;
import gabien.uslx.append.GenericUtils;

/**
 * Makes it reasonably easy to declare Datum datatypes.
 * Created 1st February, 2026. 
 */
public class DatumStruct<GT> implements DatumStructBase<GT> {
    @SuppressWarnings("rawtypes")
    private static final ConcurrentHashMap<Class, Map> PROTOTYPES = new ConcurrentHashMap<>();

    public DatumStruct() {
    }

    /**
     * Creates a visitor to fill in this class.
     */
    @Override
    public final DatumVisitor newVisitor(GT globalContext) {
        return new DatumKVDHVisitor<DatumStruct<GT>, Object>(getStructPrototype(), this, globalContext);
    }

    /**
     * Returns the 'struct prototype'; a finished map
     */
    public final Map<String, DatumKVDHVisitor.Handler<DatumStruct<GT>, Object>> getStructPrototype() {
        @SuppressWarnings("rawtypes")
        final Class clz = getClass();
        @SuppressWarnings({ "unchecked" })
        Map<String, DatumKVDHVisitor.Handler<DatumStruct<GT>, Object>> res = (Map<String, DatumKVDHVisitor.Handler<DatumStruct<GT>, Object>>) PROTOTYPES.get(clz);
        if (res == null) {
            res = new HashMap<>();
            for (final Field f : clz.getFields()) {
                DatumStructField dsf = f.getAnnotation(DatumStructField.class);
                if (dsf != null) {
                    final Function<DatumTreeUtils.VisitorLambda, DatumVisitor> factory = createVisitorFactory(f.getGenericType());
                    res.put(dsf.value(), (k, c, gt) -> {
                        return factory.apply((v, srcLoc) -> {
                            try {
                                f.set(c, v);
                            } catch (Exception ex) {
                                throw new RuntimeException("In " + f + " of " + clz, ex);
                            }
                        });
                    });
                }
            }
            for (final Method m : clz.getMethods()) {
                DatumStructField dsf = m.getAnnotation(DatumStructField.class);
                if (dsf != null) {
                    res.put(dsf.value(), (k, c, gt) -> {
                        try {
                            return (DatumVisitor) m.invoke(c, gt);
                        } catch (Exception ex) {
                            throw new RuntimeException("In " + m + " of " + clz, ex);
                        }
                    });
                }
            }
            res = Collections.unmodifiableMap(res);
            PROTOTYPES.put(clz, res);
        }
        return res;
    }

    /**
     * Automatic visitor factory.
     * Notably, this can be overridden if a particular species of struct requires particular semantics.
     */
    protected Function<DatumTreeUtils.VisitorLambda, DatumVisitor> createVisitorFactory(Type targetTy) {
        if (targetTy instanceof Class) {
            Class<?> targetClass = (Class<?>) targetTy;
            // All the primitive types that DatumTreeVisitor will directly decode for you, apart from linked list
            if (targetClass == boolean.class || targetClass == Boolean.class || targetClass == DatumSymbol.class || targetClass == long.class || targetClass == Long.class || targetClass == double.class || targetClass == Double.class) {
                return receiver -> DatumTreeUtils.decVisitor(receiver);
            }
            // Act nicely here by coercing symbols to strings.
            // We don't do this the other way around - if you ask explicitly for DatumSymbol it's assumed you meant it.
            if (targetClass == String.class) {
                return receiver -> DatumTreeUtils.decVisitor((v, srcLoc) -> {
                    if (v instanceof DatumSymbol)
                        receiver.handle(((DatumSymbol) v).id, srcLoc);
                    else
                        receiver.handle(v, srcLoc);
                });
            }
            // Missing from the above are these primitive casts:
            if (targetClass == byte.class || targetClass == Byte.class) {
                return receiver -> DatumTreeUtils.decVisitor((v, srcLoc) -> {
                    receiver.handle((byte) DatumTreeUtils.cInt(v), srcLoc);
                });
            }
            if (targetClass == short.class || targetClass == Short.class) {
                return receiver -> DatumTreeUtils.decVisitor((v, srcLoc) -> {
                    receiver.handle((short) DatumTreeUtils.cInt(v), srcLoc);
                });
            }
            if (targetClass == int.class || targetClass == Integer.class) {
                return receiver -> DatumTreeUtils.decVisitor((v, srcLoc) -> {
                    receiver.handle(DatumTreeUtils.cInt(v), srcLoc);
                });
            }
            if (targetClass == float.class || targetClass == Float.class) {
                return receiver -> DatumTreeUtils.decVisitor((v, srcLoc) -> {
                    receiver.handle(DatumTreeUtils.cFloat(v), srcLoc);
                });
            }
            // Handle non-generic arrays.
            if (targetClass.isArray())
                return createArrayVisitorFactory(targetClass.getComponentType());
        } else if (targetTy instanceof GenericArrayType) {
            // Handle generic arrays.
            return createArrayVisitorFactory(((GenericArrayType) targetTy).getGenericComponentType());
        }

        throw new RuntimeException("Can't create visitor factory for " + targetTy + "!");
    }

    /**
     * Automatic visitor factory for arrays.
     */
    protected final Function<DatumTreeUtils.VisitorLambda, DatumVisitor> createArrayVisitorFactory(Type elmTy) {
        final Class<?> elmCls = GenericUtils.getConcreteType(elmTy);
        final Function<DatumTreeUtils.VisitorLambda, DatumVisitor> elmFac = createVisitorFactory(elmTy);
        return receiver -> {
            return new DatumExpectListVisitor(() -> {
                final LinkedList<Object> al = new LinkedList<>(); 
                return new DatumSeqVisitor() {
                    @Override
                    public DatumVisitor handle(int idx) {
                        return elmFac.apply((v, srcLoc) -> {
                            al.add(v);
                        });
                    }

                    @Override
                    public void visitEnd(DatumSrcLoc srcLoc) { 
                        // We don't want to proxy visitEnd to the type visitor.
                        // Instead, clean up and submit to the original receiver.
                        receiver.handle(al.toArray((Object[]) Array.newInstance(elmCls, al.size())), srcLoc);
                    }
                };
            });
        };
    }
}
