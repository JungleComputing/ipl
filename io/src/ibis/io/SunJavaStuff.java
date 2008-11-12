package ibis.io;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class SunJavaStuff extends JavaDependantStuff {

    /** newInstance method of ObjectStreamClass, if it exists. */
    private static Method newInstance = null;

    static {
        try {
            newInstance = ObjectStreamClass.class.getDeclaredMethod(
                    "newInstance", new Class[] {});
        } catch (Exception e) {
            // ignored.
        }
        if (newInstance != null) {
            try {
                newInstance.setAccessible(true);
            } catch (Exception e) {
                newInstance = null;
            }
        }
    }

    // Only works as of Java 1.4, earlier versions of Java don't have Unsafe.
    // Use introspection, so that it at least compiles on systems that don't
    // have unsafe.
    private static Object unsafe = null;

    private static Method unsafeObjectFieldOffsetMethod;

    private static Method unsafePutDoubleMethod;

    private static Method unsafePutLongMethod;

    private static Method unsafePutFloatMethod;

    private static Method unsafePutIntMethod;

    private static Method unsafePutShortMethod;

    private static Method unsafePutCharMethod;

    private static Method unsafePutBooleanMethod;

    private static Method unsafePutByteMethod;

    private static Method unsafePutObjectMethod;
    
    static boolean available = false;

    static {
        try {
            // unsafe = Unsafe.getUnsafe();
            // does not work when a classloader is present, so we get it
            // from ObjectStreamClass.
            Class<?> cl = Class
                    .forName("java.io.ObjectStreamClass$FieldReflector");
            Field uf = cl.getDeclaredField("unsafe");
            uf.setAccessible(true);
            unsafe = uf.get(null);
            cl = unsafe.getClass();
            unsafeObjectFieldOffsetMethod = cl.getMethod("objectFieldOffset",
                    new Class[] { Field.class });
            unsafePutDoubleMethod = cl.getMethod("putDouble", new Class[] {
                    Object.class, Long.TYPE, Double.TYPE });
            unsafePutLongMethod = cl.getMethod("putLong", new Class[] {
                    Object.class, Long.TYPE, Long.TYPE });
            unsafePutFloatMethod = cl.getMethod("putFloat", new Class[] {
                    Object.class, Long.TYPE, Float.TYPE });
            unsafePutIntMethod = cl.getMethod("putInt", new Class[] {
                    Object.class, Long.TYPE, Integer.TYPE });
            unsafePutShortMethod = cl.getMethod("putShort", new Class[] {
                    Object.class, Long.TYPE, Short.TYPE });
            unsafePutCharMethod = cl.getMethod("putChar", new Class[] {
                    Object.class, Long.TYPE, Character.TYPE });
            unsafePutByteMethod = cl.getMethod("putByte", new Class[] {
                    Object.class, Long.TYPE, Byte.TYPE });
            unsafePutBooleanMethod = cl.getMethod("putBoolean", new Class[] {
                    Object.class, Long.TYPE, Boolean.TYPE });
            unsafePutObjectMethod = cl.getMethod("putObject", new Class[] {
                    Object.class, Long.TYPE, Object.class });
            available = true;
        } catch (Throwable e) {
            unsafe = null;
        }
    }

    SunJavaStuff(Class<?> clazz) {
        super(clazz);
        if (! available) {
            throw new Error("SunJavaStuff not available");
        }
    }

    /**
     * This method assigns the specified value to a final field.
     * 
     * @param ref
     *                object with a final field
     * @param fieldname
     *                name of the field
     * @param classname
     *                the name of the class
     * @param d
     *                value to be assigned
     * @exception IOException
     *                    is thrown when an IO error occurs.
     */
    void setFieldDouble(Object ref, String fieldname, double d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutDoubleMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    public void setFieldLong(Object ref, String fieldname, long d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutLongMethod.invoke(unsafe, ref, key, d);
            return;
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    public void setFieldFloat(Object ref, String fieldname, float d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutFloatMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    public void setFieldInt(Object ref, String fieldname, int d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutIntMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    public void setFieldShort(Object ref, String fieldname, short d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutShortMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    public void setFieldChar(Object ref, String fieldname, char d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutCharMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    public void setFieldByte(Object ref, String fieldname, byte d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutByteMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    public void setFieldBoolean(Object ref, String fieldname, boolean d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutBooleanMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    public void setFieldString(Object ref, String fieldname, String d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutObjectMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     * 
     */
    public void setFieldClass(Object ref, String fieldname, Class<?> d)
            throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutObjectMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     * 
     * @param fieldsig
     *                signature of the field
     */
    public void setFieldObject(Object ref, String fieldname, Object d,
            String fieldsig) throws IOException {
        try {
            Field f = clazz.getDeclaredField(fieldname);
            if (d != null && !f.getType().isInstance(d)) {
                throw new IbisIOException("wrong field type");
            }
            Object key = unsafeObjectFieldOffsetMethod.invoke(unsafe, f);
            unsafePutObjectMethod.invoke(unsafe, ref, key, d);
        } catch (Throwable ex) {
            throw new IbisIOException("got exception", ex);
        }
    }

    /**
     * Try to create an object through the newInstance method of
     * ObjectStreamClass. Return null if it fails for some reason.
     */
    Object newInstance() {
        try {
            return newInstance.invoke(objectStreamClass,
                    (java.lang.Object[]) null);
        } catch (Throwable e) {
            // System.out.println("newInstance fails: got exception " + e);
            return null;
        }
    }
}
