package ibis.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Java-dependant stuff for the Android, which has the Dalvik JVM
 * and libraries.
 * Dalvik is partly based on the Apache Harmony implementation,
 * although its serialization code is quite different from the
 * latest stable Harmony release at the time of this writing
 * (apache-harmony-src-r681495).
 */
public class DalvikJavaStuff extends JavaDependantStuff {
    
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    
    static Method newInstance = null;
    static Method setFieldBoolean = null;
    static Method setFieldByte = null;
    static Method setFieldShort = null;
    static Method setFieldInt = null;
    static Method setFieldLong = null;
    static Method setFieldChar = null;
    static Method setFieldFloat = null;
    static Method setFieldDouble = null;
    static Method setFieldObject = null;
    static boolean available = false;
    
    static {
        newInstance = getMethod(ObjectInputStream.class,
                "newInstance", Class.class, Class.class);
        setFieldBoolean = getMethod(ObjectInputStream.class,
                "setField", Object.class, Class.class, String.class, Boolean.TYPE);
        setFieldByte = getMethod(ObjectInputStream.class,
                "setField", Object.class, Class.class, String.class, Byte.TYPE);
        setFieldShort = getMethod(ObjectInputStream.class,
                "setField", Object.class, Class.class, String.class, Short.TYPE);
        setFieldInt = getMethod(ObjectInputStream.class,
                "setField", Object.class, Class.class, String.class, Integer.TYPE);
        setFieldLong = getMethod(ObjectInputStream.class,
                "setField", Object.class, Class.class, String.class, Long.TYPE);
        setFieldChar = getMethod(ObjectInputStream.class,
                "setField", Object.class, Class.class, String.class, Character.TYPE);
        setFieldFloat = getMethod(ObjectInputStream.class,
                "setField", Object.class, Class.class, String.class, Float.TYPE);
        setFieldDouble = getMethod(ObjectInputStream.class,
                "setField", Object.class, Class.class, String.class, Double.TYPE);
        setFieldObject = getMethod(ObjectInputStream.class,
                "objSetField", Object.class, Class.class, String.class, String.class,
                Object.class);
        available = isAvailable();
    }
    
    private Class<?>constructorClass = null;
    
    private static Method getMethod(Class<?> cl, String name, Class<?>... params) {
        try {
            Method m = cl.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch(Throwable e) {
            return null;
        }
    }
    
    static boolean isAvailable() {
        
        if (newInstance == null) {
            return false;
        }
        if (setFieldByte == null) {
            return false;
        }
        if (setFieldShort == null) {
            return false;
        }
        if (setFieldInt == null) {
            return false;
        }
        if (setFieldLong == null) {
            return false;
        }
        if (setFieldFloat == null) {
            return false;
        }
        if (setFieldDouble == null) {
            return false;
        }
        if (setFieldBoolean == null) {
            return false;
        }
        if (setFieldChar == null) {
            return false;
        }
        if (setFieldObject == null) {
            return false;
        }
        return true;
    }
    
    
    DalvikJavaStuff(Class<?> clazz) {
        super(clazz);
        if (! available) {
            throw new Error("DalvikJavaStuff not available");
        }
        
        // Find the class of the constructor that needs to be called
        // when creating a new instance of this class. This is the
        // empty constructor of the first non-serializable class in the
        // hierarchy.
        constructorClass = clazz;

        // Find the first non-serializable class in the hierarchy.
        while (constructorClass != null
                 && java.io.Serializable.class.isAssignableFrom(constructorClass)) {
            constructorClass = constructorClass.getSuperclass();
        }
        
        // Obtain the empty constructor.
        Constructor<?> constructor = null;
        if (constructorClass != null) {
            try {
                constructor = constructorClass
                        .getDeclaredConstructor(EMPTY_CLASS_ARRAY);
            } catch (NoSuchMethodException e) {
                // Ignored
            }
        }
        
        // Check visibility of constructor
        if (constructor != null) {
            int constructorModifiers = constructor.getModifiers();

            // Now we must check if the empty constructor is visible to the
            // instantiation class
            if (Modifier.isPrivate(constructorModifiers)) {
                constructorClass = null;
            } else if (!Modifier.isPublic(constructorModifiers)
                    && !Modifier.isProtected(constructorModifiers)) {
                if (! constructorClass.getPackage().getName().equals(clazz.getPackage().getName())) {
                    constructorClass = null;
                }
            }
        }
    }
    
    

    Object newInstance() {
        if (constructorClass == null) {
            return null;
        }
        try {
            return newInstance.invoke(null, clazz, constructorClass);
        } catch (Throwable e) {
             return null;
        }
    }

    void setFieldBoolean(Object ref, String fieldname, boolean d)
            throws IOException {
        try {
            setFieldBoolean.invoke(null, ref, clazz, fieldname, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldByte(Object ref, String fieldname, byte d) throws IOException {
        try {
            setFieldByte.invoke(null, ref, clazz, fieldname, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldChar(Object ref, String fieldname, char d) throws IOException {
        try {
            setFieldChar.invoke(null, ref, clazz, fieldname, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldClass(Object ref, String fieldname, Class<?> d)
            throws IOException {
        try {
            setFieldObject.invoke(null, ref, clazz, fieldname, "Ljava.lang.Class;", d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldDouble(Object ref, String fieldname, double d)
            throws IOException {
        try {
            setFieldDouble.invoke(null, ref, clazz, fieldname, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldFloat(Object ref, String fieldname, float d)
            throws IOException {
        try {
            setFieldFloat.invoke(null, ref, clazz, fieldname, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldInt(Object ref, String fieldname, int d) throws IOException {
        try {
            setFieldInt.invoke(null, ref, clazz, fieldname, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldLong(Object ref, String fieldname, long d) throws IOException {
        try {
            setFieldLong.invoke(null, ref, clazz, fieldname, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldObject(Object ref, String fieldname, Object d, String fieldsig)
            throws IOException {
        try {
            setFieldObject.invoke(null, ref, clazz, fieldname, fieldsig, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldShort(Object ref, String fieldname, short d)
            throws IOException {
        try {
            setFieldShort.invoke(null, ref, clazz, fieldname, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    void setFieldString(Object ref, String fieldname, String d)
            throws IOException {
        try {
            setFieldObject.invoke(null, ref, clazz, fieldname, "Ljava.lang.String;", d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

}
