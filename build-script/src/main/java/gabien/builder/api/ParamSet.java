/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.builder.api;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Defines a param set.
 * Params are typically defined by annotations on fields.
 * Created 17th February, 2025.
 */
public class ParamSet {
    /**
     * Parses args. Returns true if parse went OK.
     */
    public final boolean parseArgs(String[] args, int offset, int length, Diagnostics diag) {
        HashSet<Param> params = new HashSet<>();
        HashSet<Param> paramsSpecified = new HashSet<>();
        TreeMap<String, Param> paramsMap = new TreeMap<>();
        buildParams(params);

        // post "--"-mode; disables arg parsing mostly
        boolean contentArgsOnly = true;
        for (Param p : params) {
            if (paramsMap.putIfAbsent(p.name, p) != null)
                diag.warn("In " + this + ", parameter " + p.name + " is duplicated.");
            if (!p.name.equals(""))
                contentArgsOnly = false;
        }

        int end = offset + length;
        while (offset < end) {
            String arg = args[offset++];
            if (contentArgsOnly || !arg.startsWith("--")) {
                offset--;
                arg = "";
            } else if (arg.equals("--")) {
                offset--;
                arg = "";
                contentArgsOnly = true;
            }
            Param p = paramsMap.get(arg);
            if (p == null) {
                if (arg.equals("")) {
                    diag.error("This does not accept content args.");
                } else {
                    diag.error("Unknown arg " + arg + ".");
                }
            } else {
                if (!paramsSpecified.add(p))
                    if (!p.isMultiple)
                        diag.warn(p.niceName + " specified multiple times.");
                if (p instanceof Switch) {
                    try {
                        ((Switch) p).apply();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                } else if (p instanceof Value) {
                    if (offset < end) {
                        String value = args[offset++];
                        try {
                            ((Value) p).applyValue(value);
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }
                    } else {
                        diag.error(p.niceName + " expected value.");
                    }
                } else {
                    diag.error(p.niceName + " uses unexpected param type.");
                }
            }
        }

        boolean ok = true;
        for (Param p : params) {
            if (!p.isOptional) {
                if (!paramsSpecified.contains(p)) {
                    diag.error("Parameter " + p.niceName + " not specified.");
                    ok = false;
                }
            }
        }

        return ok;
    }

    public final void buildParams(Set<Param> params) {
        for (final Field f : getClass().getFields()) {
            if (ParamSet.class.isAssignableFrom(f.getType())) {
                try {
                    ((ParamSet) f.get(this)).buildParams(params);
                } catch (Exception ex) {
                    throw new RuntimeException("during descent into " + f, ex);
                }
            }
            ToolSwitch sw = f.getAnnotation(ToolSwitch.class);
            if (sw != null) {
                params.add(new Switch(sw.name(), sw.desc(), false) {
                    @Override
                    public void apply() throws Exception {
                        f.set(ParamSet.this, true);
                    }
                });
            }
            ToolParam tp = f.getAnnotation(ToolParam.class);
            if (tp != null) {
                Class<?> fty = f.getType();
                if (fty.isArray()) {
                    Class<?> comp = fty.getComponentType();
                    Function<String, Object> parser = makeParser(comp, f);
                    params.add(new Value(tp.name(), tp.valueMeaning(), tp.desc(), tp.optional(), true) {
                        @Override
                        public void applyValue(String value) throws Exception {
                            Object next = parser.apply(value);
                            Object arr = f.get(ParamSet.this);
                            Object nextArr;
                            int index;
                            if (arr == null) {
                                index = 0;
                                nextArr = Array.newInstance(comp, 1);
                            } else {
                                index = Array.getLength(arr);
                                nextArr = Array.newInstance(comp, index + 1);
                                System.arraycopy(arr, 0, nextArr, 0, index);
                            }
                            Array.set(nextArr, index, next);
                            f.set(ParamSet.this, nextArr);
                        }
                    });
                } else {
                    Function<String, Object> parser = makeParser(fty, f);
                    params.add(new Value(tp.name(), tp.valueMeaning(), tp.desc(), tp.optional(), false) {
                        @Override
                        public void applyValue(String value) throws Exception {
                            f.set(ParamSet.this, parser.apply(value));
                        }
                    });
                }
            }
        }
        buildCustomParams(params);
    }

    private Function<String, Object> makeParser(Class<?> ty, Field f) {
        if (ty == boolean.class) {
            return (v) -> Boolean.parseBoolean(v);
        } else if (ty == String.class) {
            return (v) -> v;
        } else if (ty == File.class) {
            return (v) -> new File(v);
        } else if (ty == byte.class) {
            return (v) -> Byte.parseByte(v);
        } else if (ty == short.class) {
            return (v) -> Short.parseShort(v);
        } else if (ty == int.class) {
            return (v) -> Integer.parseInt(v);
        } else if (ty == long.class) {
            return (v) -> Long.parseLong(v);
        } else {
            throw new RuntimeException("Cannot translate param type: " + f);
        }
    }

    public void buildCustomParams(Set<Param> params) {
        // nothing here
    }

    public static abstract class Param {
        public final String niceName;
        public final String name;
        public final String description;
        public final boolean isOptional;
        public final boolean isMultiple;

        public Param(String name, String d, boolean isOptional, boolean isMultiple) {
            this.name = name;
            this.niceName = name.equals("") ? "Content" : ("Arg " + name);
            this.description = d;
            this.isOptional = isOptional;
            this.isMultiple = isMultiple;
        }

        public abstract String describeInline();
    }

    public static abstract class Switch extends Param {
        public Switch(String name, String d, boolean isMultiple) {
            super(name, d, true, isMultiple);
        }

        public abstract void apply() throws Exception;

        @Override
        public final String describeInline() {
            return name;
        }
    }

    public static abstract class Value extends Param {
        public final String valueMeaning;

        public Value(String name, String valueMeaning, String d, boolean isOptional, boolean isMultiple) {
            super(name, d, isOptional, isMultiple);
            this.valueMeaning = valueMeaning;
        }

        public abstract void applyValue(String value) throws Exception;

        @Override
        public final String describeInline() {
            if (name.equals("")) {
                return (isOptional ? "[" : "") + valueMeaning + (isMultiple ? "..." : "") + (isOptional ? "]" : "");
            } else {
                return (isOptional ? "[" : (isMultiple ? "(" : "")) + name + " " + valueMeaning + (isOptional ? "]" : (isMultiple ? ")" : "")) + (isMultiple ? "..." : "");
            }
        }
    }
}
