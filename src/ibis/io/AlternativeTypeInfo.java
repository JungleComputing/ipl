package ibis.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.HashMap;

/**
 * The <code>AlternativeTypeInfo</code> class maintains information about
 * a specific <code>Class</code>, such as a list of serializable fields, 
 * whether it has <code>readObject</code> and <code>writeObject</code>
 * methods, et cetera.
 *
 * The serializable fields are first ordered alphabetically, and then
 * by type, in the order: double, long, float, int, short, char, byte,
 * boolean, reference. This determines the order in which fields are
 * serialized.
 */
final class AlternativeTypeInfo {

    /**
     * Maintains all <code>AlternativeTypeInfo</code> structures in a
     * hashmap, to be accessed through their classname.
     */
    private static HashMap alternativeTypes = new HashMap();

    /** newInstance method of ObjectStreamClass, when it exists. */
    private static Method newInstance = null;

    /**
     * The <code>Class</code> structure of the class represented by this
     * <code>AlternativeTypeInfo</code> structure.
     */
    Class clazz;

    /** The ObjectStreamClass of clazz. */
    private ObjectStreamClass objectStreamClass;

    /** The sorted list of serializable fields. */
    Field[] serializable_fields;

    /**
     * For each field, indicates whether the field is final.
     * This is significant for deserialization, because it determines the
     * way in which the field can be assigned to. The bytecode verifier
     * does not allow arbitraty assignments to final fields.
     */
    boolean[] fields_final;

    /** Number of <code>double</code> fields. */
    int double_count;

    /** Number of <code>long</code> fields. */
    int long_count;

    /** Number of <code>float</code> fields. */
    int float_count;

    /** Number of <code>int</code> fields. */
    int int_count;

    /** Number of <code>short</code> fields. */
    int short_count;

    /** Number of <code>char</code> fields. */
    int char_count;

    /** Number of <code>byte</code> fields. */
    int byte_count;

    /** Number of <code>boolean</code> fields. */
    int boolean_count;

    /** Number of <code>reference</code> fields. */
    int reference_count;

    /** Indicates whether the superclass is serializable. */
    boolean superSerializable;

    /** The <code>AlternativeTypeInfo</code> structure of the superclass. */

    AlternativeTypeInfo alternativeSuperInfo;

    /**
     * The "level" of a serializable class.
     * The "level" of a serializable class is computed as follows:
     * - if its superclass is serializable:
     *       the level of the superclass + 1.
     * - if its superclass is not serializable:
     *       1.
     */
    int level;

    /** serialPersistentFields of the class, if the class declares them. */
    java.io.ObjectStreamField[] serial_persistent_fields = null;

    /** Set if the class has a <code>readObject</code> method. */
    boolean hasReadObject;

    /** Set if the class has a <code>writeObject</code> method. */
    boolean hasWriteObject;

    /** Set if the class has a <code>writeReplace</code> method. */
    boolean hasReplace;

    /**
     * A <code>Comparator</code> implementation for sorting the
     * fields array.
     */
    private static class FieldComparator implements Comparator {
        /**
         * Compare fields alphabetically.
         */
        public int compare(Object o1, Object o2) {
            Field f1 = (Field) o1;
            Field f2 = (Field) o2;

            return f1.getName().compareTo(f2.getName());
        }
    }

    /** A <code>Comparator</code> for sorting the array of fields. */
    private static FieldComparator fieldComparator = new FieldComparator();

    /** The <code>writeObject</code> method, if there is one. */
    private Method writeObjectMethod;

    /** The <code>readObject</code> method, if there is one. */
    private Method readObjectMethod;

    /** The <code>writeReplace</code> method, if there is one. */
    private Method writeReplaceMethod;

    /** The <code>readResolve</code> method, if there is one. */
    private Method readResolveMethod;

    /** This is needed for the private field access hack. */
    Field temporary_field;

    /** This is needed for the private method access hack. */
    Method temporary_method;

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

    /**
     * Return the name of the class.
     *
     * @return the name of the class.
     */
    public String toString() {
        return clazz.getName();
    }

    /**
     * Try to create an object through the newInstance method of
     * ObjectStreamClass.
     * Return null if it fails for some reason.
     */
    Object newInstance() {
        if (newInstance != null) {
            try {
                return newInstance.invoke(objectStreamClass,
                        (java.lang.Object[]) null);
            } catch (Exception e) {
                // System.out.println("newInstance fails: got exception " + e);
                return null;
            }
        }
        // System.out.println("newInstance fails: no newInstance method");
        return null;
    }

    /**
     * Gets the <code>AlternativeTypeInfo</code> for class <code>type</code>.
     *
     * @param type the <code>Class</code> of the requested type.
     * @return the <code>AlternativeTypeInfo</code> structure for this type.
     */
    public static synchronized AlternativeTypeInfo getAlternativeTypeInfo(
            Class type) {
        AlternativeTypeInfo t
                = (AlternativeTypeInfo) alternativeTypes.get(type);

        if (t == null) {
            t = new AlternativeTypeInfo(type);
            alternativeTypes.put(type, t);
        }

        return t;
    }

    /**
     * Gets the <code>AlternativeTypeInfo</code> for class
     * <code>classname</code>.
     *
     * @param classname the name of the requested type.
     * @return the <code>AlternativeTypeInfo</code> structure for this type.
     */
    public static synchronized AlternativeTypeInfo getAlternativeTypeInfo(
            String classname) throws ClassNotFoundException {
        Class type = null;

        try {
            type = Class.forName(classname);
        } catch (ClassNotFoundException e) {
            type = Thread.currentThread().getContextClassLoader().loadClass(
                    classname);
        }

        return getAlternativeTypeInfo(type);
    }

    /**
     * Gets the method with the given name, parameter types and return type.
     *
     * @param name		the name of the method
     * @param paramTypes	its parameter types
     * @param returnType	its return type
     * @return			the requested method, or <code>null</code> if
     * 				it cannot be found.
     */
    private Method getMethod(String name, Class[] paramTypes, Class returnType) {
        try {
            Method method = clazz.getDeclaredMethod(name, paramTypes);

            /* Check return type. */
            if (method.getReturnType() != returnType) {
                return null;
            }

            /* Check if method is static. */
            if ((method.getModifiers() & Modifier.STATIC) != 0) {
                return null;
            }

            /* Make method accessible, so that it may be called. */
            if (!method.isAccessible()) {
                temporary_method = method;
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        temporary_method.setAccessible(true);
                        return null;
                    }
                });
            }
            return method;
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Invokes the <code>writeObject</code> method on object <code>o</code>.
     *
     * @param o		the object on which <code>writeObject</code> is to
     * 			be invoked
     * @param out	the <code>ObjectOutputStream</code> to be given
     * 			as parameter
     * @exception IOException
     * 			when anything goes wrong
     */
    void invokeWriteObject(Object o, ObjectOutputStream out)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        //	System.out.println("invoke writeObject");
        writeObjectMethod.invoke(o, new Object[] { out });
    }

    /**
     * Invokes the <code>readObject</code> method on object <code>o</code>.
     *
     * @param o		the object on which <code>readObject</code> is to
     * 			be invoked
     * @param in	the <code>ObjectInputStream</code> to be given
     * 			as parameter
     * @exception IOException
     * 			when anything goes wrong
     */
    void invokeReadObject(Object o, ObjectInputStream in)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        //	System.out.println("invoke readObject");
        readObjectMethod.invoke(o, new Object[] { in });
    }

    /**
     * Invokes the <code>readResolve</code> method on object <code>o</code>.
     *
     * @param o		the object on which <code>readResolve</code> is to
     * 			be invoked
     * @exception IOException
     * 			when anything goes wrong
     */
    Object invokeReadResolve(Object o) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        return readResolveMethod.invoke(o, new Object[0]);
    }

    /**
     * Invokes the <code>writeReplace</code> method on object <code>o</code>.
     *
     * @param o		the object on which <code>writeReplace</code> is to
     * 			be invoked
     * @exception IOException
     * 			when anything goes wrong
     */
    Object invokeWriteReplace(Object o) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        return writeReplaceMethod.invoke(o, new Object[0]);
    }

    /**
     * Constructor is private. Use {@link #getAlternativeTypeInfo(Class)} to
     * obtain the <code>AlternativeTypeInfo</code> for a type.
     */
    private AlternativeTypeInfo(Class clazz) {

        this.clazz = clazz;

        if (newInstance != null) {
            objectStreamClass = ObjectStreamClass.lookup(clazz);
        }

        try {
            /*
             Here we figure out what field the type contains, and which fields 
             we should write. We must also sort them by type and name to ensure
             that we read them correctly on the other side. We cache all of
             this so we only do it once for each type.
             */

            getSerialPersistentFields();

            /* see if the supertype is serializable */
            Class superClass = clazz.getSuperclass();

            if (superClass != null) {
                if (java.io.Serializable.class.isAssignableFrom(superClass)) {
                    superSerializable = true;
                    alternativeSuperInfo = getAlternativeTypeInfo(superClass);
                    level = alternativeSuperInfo.level + 1;
                } else {
                    superSerializable = false;
                    level = 1;
                }
            }

            /* Now see if it has a writeObject/readObject. */

            writeObjectMethod = getMethod("writeObject",
                    new Class[] { ObjectOutputStream.class }, Void.TYPE);
            readObjectMethod = getMethod("readObject",
                    new Class[] { ObjectInputStream.class }, Void.TYPE);

            hasWriteObject = writeObjectMethod != null;
            hasReadObject = readObjectMethod != null;

            writeReplaceMethod = getMethod("writeReplace", new Class[0],
                    Object.class);

            readResolveMethod = getMethod("readResolve", new Class[0],
                    Object.class);

            hasReplace = writeReplaceMethod != null;

            Field[] fields = clazz.getDeclaredFields();

            /* getDeclaredFields does not specify or guarantee a specific
             * order. Therefore, we sort the fields alphabetically, as does
             * the IOGenerator.
             */
            java.util.Arrays.sort(fields, fieldComparator);

            int len = fields.length;

            /* Create the datastructures to cache the fields we need. Since
             * we don't know the size yet, we create large enough arrays,
             * which will later be replaced;
             */
            if (serial_persistent_fields != null) {
                len = serial_persistent_fields.length;
            }

            Field[] double_fields = new Field[len];
            Field[] long_fields = new Field[len];
            Field[] float_fields = new Field[len];
            Field[] int_fields = new Field[len];
            Field[] short_fields = new Field[len];
            Field[] char_fields = new Field[len];
            Field[] byte_fields = new Field[len];
            Field[] boolean_fields = new Field[len];
            Field[] reference_fields = new Field[len];

            if (serial_persistent_fields == null) {
                /* Now count and store all the difference field types (only the
                 * ones that we should write!). Note that we store them into
                 * the array sorted by name !
                 */
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];

                    if (field == null) {
                        continue;
                    }

                    int modifiers = field.getModifiers();

                    if ((modifiers & (Modifier.TRANSIENT | Modifier.STATIC))
                            == 0) {
                        Class field_type = field.getType();

                        /* This part is a bit scary. We basically switch of the
                         * Java field access checks so we are allowed to read
                         * private fields ....
                         */
                        if (!field.isAccessible()) {
                            temporary_field = field;
                            AccessController.doPrivileged(
                                    new PrivilegedAction() {
                                        public Object run() {
                                            temporary_field.setAccessible(true);
                                            return null;
                                        }
                                    });
                        }

                        if (field_type.isPrimitive()) {
                            if (field_type == Boolean.TYPE) {
                                boolean_fields[boolean_count++] = field;
                            } else if (field_type == Character.TYPE) {
                                char_fields[char_count++] = field;
                            } else if (field_type == Byte.TYPE) {
                                byte_fields[byte_count++] = field;
                            } else if (field_type == Short.TYPE) {
                                short_fields[short_count++] = field;
                            } else if (field_type == Integer.TYPE) {
                                int_fields[int_count++] = field;
                            } else if (field_type == Long.TYPE) {
                                long_fields[long_count++] = field;
                            } else if (field_type == Float.TYPE) {
                                float_fields[float_count++] = field;
                            } else if (field_type == Double.TYPE) {
                                double_fields[double_count++] = field;
                            }
                        } else {
                            reference_fields[reference_count++] = field;
                        }
                    }
                }
            } else {
                for (int i = 0; i < serial_persistent_fields.length; i++) {
                    Field field = findField(serial_persistent_fields[i]);
                    Class field_type = serial_persistent_fields[i].getType();
                    if (field != null && !field.isAccessible()) {
                        temporary_field = field;
                        AccessController.doPrivileged(new PrivilegedAction() {
                            public Object run() {
                                temporary_field.setAccessible(true);
                                return null;
                            }
                        });
                    }

                    if (field_type.isPrimitive()) {
                        if (field_type == Boolean.TYPE) {
                            boolean_fields[boolean_count++] = field;
                        } else if (field_type == Character.TYPE) {
                            char_fields[char_count++] = field;
                        } else if (field_type == Byte.TYPE) {
                            byte_fields[byte_count++] = field;
                        } else if (field_type == Short.TYPE) {
                            short_fields[short_count++] = field;
                        } else if (field_type == Integer.TYPE) {
                            int_fields[int_count++] = field;
                        } else if (field_type == Long.TYPE) {
                            long_fields[long_count++] = field;
                        } else if (field_type == Float.TYPE) {
                            float_fields[float_count++] = field;
                        } else if (field_type == Double.TYPE) {
                            double_fields[double_count++] = field;
                        }
                    } else {
                        reference_fields[reference_count++] = field;
                    }
                }
            }

            // Now resize the datastructures.
            int size = double_count + long_count + float_count + int_count
                    + short_count + char_count + byte_count + boolean_count
                    + reference_count;
            int index = 0;

            if (size > 0) {
                serializable_fields = new Field[size];

                System.arraycopy(double_fields, 0, serializable_fields, index,
                        double_count);
                index += double_count;

                System.arraycopy(long_fields, 0, serializable_fields, index,
                        long_count);
                index += long_count;

                System.arraycopy(float_fields, 0, serializable_fields, index,
                        float_count);
                index += float_count;

                System.arraycopy(int_fields, 0, serializable_fields, index,
                        int_count);
                index += int_count;

                System.arraycopy(short_fields, 0, serializable_fields, index,
                        short_count);
                index += short_count;

                System.arraycopy(char_fields, 0, serializable_fields, index,
                        char_count);
                index += char_count;

                System.arraycopy(byte_fields, 0, serializable_fields, index,
                        byte_count);
                index += byte_count;

                System.arraycopy(boolean_fields, 0, serializable_fields, index,
                        boolean_count);
                index += boolean_count;

                System.arraycopy(reference_fields, 0, serializable_fields,
                        index, reference_count);

                fields_final = new boolean[size];

                for (int i = 0; i < size; i++) {
                    if (serializable_fields[i] != null) {
                        fields_final[i]
                            = ((serializable_fields[i].getModifiers()
                                        & Modifier.FINAL) != 0);
                    } else {
                        fields_final[i] = false;
                    }
                }
            } else {
                serializable_fields = null;
            }
        } catch (Exception e) {
            throw new SerializationError("Cannot initialize serialization "
                    + "info for " + clazz.getName(), e);
        }
    }

    /**
     * Looks for a declaration of serialPersistentFields, and, if present,
     * makes it accessible, and stores it in
     * <code>serial_persistent_fields</code>.
     */
    private void getSerialPersistentFields() {
        try {
            Field f = clazz.getDeclaredField("serialPersistentFields");
            int mask = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
            if ((f.getModifiers() & mask) == mask) {
                if (!f.isAccessible()) {
                    temporary_field = f;
                    AccessController.doPrivileged(new PrivilegedAction() {
                        public Object run() {
                            temporary_field.setAccessible(true);
                            return null;
                        }
                    });
                }
                serial_persistent_fields
                        = (java.io.ObjectStreamField[]) f.get(null);
            }
        } catch (Exception e) {
            // ignored, no serialPersistentFields
        }
    }

    /**
     * Gets the field with fieldname indicated by the name in <code>of</code>.
     * If not present, returns <code>null</code>.
     */
    private Field findField(ObjectStreamField of) {
        try {
            return clazz.getDeclaredField(of.getName());
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * Gets the index of a field with name <code>name</code> and type
     * <code>tp</code> in either <code>SerialPersistentFields</code>, if
     * it exists, or in the <code>serializable_fields</code> array.
     * An exception is thrown when such a field is not found.
     *
     * @param name	name of the field we are looking for
     * @param tp	type of the field we are looking for
     * @return index in either <code>serial_persistent_fields</code> or
     * <code>serializable_fields</code>.
     * @exception IllegalArgumentException when no such field is found.
     */
    int getOffset(String name, Class tp) throws IllegalArgumentException {
        int offset = 0;

        if (tp.isPrimitive()) {
            if (serial_persistent_fields != null) {
                for (int i = 0; i < serial_persistent_fields.length; i++) {
                    if (serial_persistent_fields[i].getType() == tp) {
                        if (name.equals(
                                    serial_persistent_fields[i].getName())) {
                            return offset;
                        }
                        offset++;
                    }
                }
            } else if (serializable_fields != null) {
                for (int i = 0; i < serializable_fields.length; i++) {
                    if (serializable_fields[i].getType() == tp) {
                        if (name.equals(serializable_fields[i].getName())) {
                            return offset;
                        }
                        offset++;
                    }
                }
            }
        } else {
            if (serial_persistent_fields != null) {
                for (int i = 0; i < serial_persistent_fields.length; i++) {
                    if (!serial_persistent_fields[i].getType().isPrimitive()) {
                        if (name.equals(serial_persistent_fields[i].getName())) {
                            return offset;
                        }
                        offset++;
                    }
                }
            } else if (serializable_fields != null) {
                for (int i = 0; i < serializable_fields.length; i++) {
                    if (!serializable_fields[i].getType().isPrimitive()) {
                        if (name.equals(serializable_fields[i].getName())) {
                            return offset;
                        }
                        offset++;
                    }
                }
            }
        }
        throw new IllegalArgumentException("no field named " + name
                + " with type " + tp);
    }
}
