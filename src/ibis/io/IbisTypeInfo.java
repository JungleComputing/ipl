/* $Id$ */

package ibis.io;

import java.util.HashMap;

class IbisTypeInfo implements IbisStreamFlags {

    private static HashMap typeInfos = new HashMap();

    Class clazz;

    boolean isArray;

    boolean isString;

    boolean isClass;

    // for ibis.io.Serializable    
    Generator gen;

    // for java.io.Serializable
    AlternativeTypeInfo altInfo;

    public static IbisTypeInfo getIbisTypeInfo(Class clzz) {
        IbisTypeInfo t = (IbisTypeInfo) typeInfos.get(clzz);

        if (t == null) {
            t = new IbisTypeInfo(clzz);
            typeInfos.put(clzz, t);
        }
        return t;
    }

    private IbisTypeInfo(Class clzz) {
        clazz = clzz;
        isArray = clazz.isArray();
        isString = (clazz == java.lang.String.class);
        isClass = (clazz == java.lang.Class.class);
        if (isArray || isString || isClass) {
            gen = null;
        } else {
            Class gen_class = null;
            String name = clazz.getName() + "_ibis_io_Generator";
            try {
                gen_class = Class.forName(name);
            } catch (ClassNotFoundException e) {
                // The loading of the class failed.
                // Maybe, Ibis was loaded using the primordial classloader
                // and the needed class was not.
                try {
                    gen_class = Thread.currentThread().getContextClassLoader()
                            .loadClass(name);
                } catch (Exception e1) {
                    if (DEBUG) {
                        System.err.println("Class " + name + " not found!");
                    }
                    gen = null;
                }
            }
            if (gen_class != null) {
                try {
                    gen = (Generator) gen_class.newInstance();
                } catch (Exception e) {
                    if (DEBUG) {
                        System.out.println("Could not instantiate " + name);
                    }
                    gen = null;
                }
            }
        }

        if (gen == null) {
            altInfo = AlternativeTypeInfo.getAlternativeTypeInfo(clazz);
        }
    }

    static Class getClass(String n) {
        Class c = null;
        try {
            c = Class.forName(n);
        } catch (ClassNotFoundException e) {
            throw new SerializationError(
                    "Internal error: could not load primitive array type " + n,
                    e);
        }
        return c;
    }
}
