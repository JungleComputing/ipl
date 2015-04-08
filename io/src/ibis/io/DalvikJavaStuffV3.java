package ibis.io;

import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

// For Android 4/5.
class DalvikJavaStuffV3 extends JavaDependantStuff {

    /** newInstance method of ObjectStreamClass, if it exists. */
    private static Method newInstance = null;

    private static Method getReflectionField;

    static boolean available = false;

    static {
        try {
            newInstance = ObjectStreamClass.class.getDeclaredMethod(
                    "newInstance", new Class[] {Class.class});
            newInstance.setAccessible(true);

            getReflectionField = ObjectStreamClass.class.getDeclaredMethod("getReflectionField",
                    new Class[] { ObjectStreamField.class});
            getReflectionField.setAccessible(true);
            available = true;
        } catch (Throwable e) {
            logger.info("Dalvik Java Stuff V3 not available", e);
        }
    }

    DalvikJavaStuffV3(Class<?> clazz) {
        super(clazz);
        if (! available) {
            throw new Error("Dalvik Java Stuff V3 not available");
        }
    }

    /**
     * This method assigns the specified value to a final field.
     * 
     * @param ref
     *                object with a final field
     * @param fieldname
     *                name of the field
     * @param d
     *                value to be assigned
     * @exception IOException
     *                    is thrown when an IO error occurs.
     */
    public void setFieldDouble(Object ref, String fieldname, double d)
            throws IOException {
        try {
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.setDouble(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.setLong(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.setFloat(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.setInt(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.setShort(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.setChar(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.setByte(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.setBoolean(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.set(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.set(ref, d);
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
            Field f = (Field) getReflectionField.invoke(objectStreamClass, objectStreamClass.getField(fieldname));
            f.set(ref, d);
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
                    clazz);
        } catch (Throwable e) {
            // System.out.println("newInstance fails: got exception " + e);
            return null;
        }
    }
}
