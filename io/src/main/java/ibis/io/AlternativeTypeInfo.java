/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>AlternativeTypeInfo</code> class maintains information about a
 * specific <code>Class</code>, such as a list of serializable fields, whether
 * it has <code>readObject</code> and <code>writeObject</code> methods, et
 * cetera.
 *
 * The serializable fields are first ordered alphabetically, and then by type,
 * in the order: double, long, float, int, short, char, byte, boolean,
 * reference. This determines the order in which fields are serialized.
 */
final class AlternativeTypeInfo {

    private static Logger logger = LoggerFactory
            .getLogger(AlternativeTypeInfo.class);

    /**
     * Maintains all <code>AlternativeTypeInfo</code> structures in a hashmap,
     * to be accessed through their classname.
     */
    private static HashMap<Class<?>, AlternativeTypeInfo> alternativeTypes = new HashMap<Class<?>, AlternativeTypeInfo>();

    private static class ArrayWriter extends IbisWriter {
        @Override
        void writeObject(IbisSerializationOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            out.writeArray(ref, t.clazz, unshared);
        }
    }

    private static class IbisSerializableWriter extends IbisWriter {
        @Override
        void writeObject(IbisSerializationOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            ((Serializable) ref).generated_WriteObject(out);
        }
    }

    private static class ExternalizableWriter extends IbisWriter {
        @Override
        void writeObject(IbisSerializationOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            out.push_current_object(ref, 0);
            ((java.io.Externalizable) ref)
                    .writeExternal(out.getJavaObjectOutputStream());
            out.pop_current_object();
        }
    }

    private static class StringWriter extends IbisWriter {
        @Override
        void writeObject(IbisSerializationOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            out.writeUTF((String) ref);
        }
    }

    private static class ClassWriter extends IbisWriter {
        @Override
        void writeObject(IbisSerializationOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            out.writeUTF(((Class<?>) ref).getName());
        }
    }

    private class EnumWriter extends IbisWriter {
        @Override
        void writeObject(IbisSerializationOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            out.writeUTF(((Enum<?>) ref).name());
        }
    }

    private static class SerializableWriter extends IbisWriter {
        @Override
        void writeObject(IbisSerializationOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            out.push_current_object(ref, 0);
            try {
                out.alternativeWriteObject(t, ref);
            } catch (IllegalAccessException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Caught exception, rethrow as NotSerializableException",
                            e);
                }
                throw new IbisNotSerializableException(
                        "Serializable failed for : " + t.clazz.getName(), e);
            }
            out.pop_current_object();
            IbisSerializationOutputStream.addStatSendObject(ref);
        }
    }

    private static class NotSerializableWriter extends IbisWriter {
        @Override
        void writeObject(IbisSerializationOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            throw new IbisNotSerializableException(
                    "Not serializable: " + t.clazz.getName());
        }
    }

    private static class IbisSerializableReader extends IbisReader {
        @Override
        Object readObject(IbisSerializationInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            return t.gen.generated_newInstance(in);
        }
    }

    private static class ArrayReader extends IbisReader {
        @Override
        Object readObject(IbisSerializationInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            return in.readArray(t.clazz, typeHandle);
        }
    }

    private static class StringReader extends IbisReader {
        @Override
        Object readObject(IbisSerializationInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            String o = in.readUTF();
            in.addObjectToCycleCheck(o);
            return o;
        }
    }

    private static class ClassReader extends IbisReader {
        @Override
        Object readObject(IbisSerializationInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            String o = in.readUTF();
            Object obj = JavaDependantStuff.getClassFromName(o);
            in.addObjectToCycleCheck(obj);
            return obj;
        }
    }

    @SuppressWarnings("unchecked")
    private static class EnumReader extends IbisReader {
        @Override
        @SuppressWarnings("rawtypes")
        Object readObject(IbisSerializationInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            String o = in.readUTF();
            Object obj;
            Class<?> clazz = t.clazz;
            while (!clazz.isEnum()) {
                clazz = clazz.getSuperclass();
                if (clazz == null || clazz == java.lang.Object.class) {
                    throw new IOException(
                            "Exception while reading enumeration: class is not an enumeration");
                }
            }
            try {
                obj = Enum.valueOf((Class) clazz, o);
            } catch (Throwable e) {
                throw new IOException(
                        "Exception while reading enumeration" + e);
            }
            in.addObjectToCycleCheck(obj);
            return obj;
        }
    }

    private static class ExternalizableReader extends IbisReader {
        @Override
        Object readObject(IbisSerializationInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            Object obj;
            try {
                // Also calls parameter-less constructor
                obj = t.clazz.getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Caught exception, now rethrow as ClassNotFound",
                            e);
                }
                throw new ClassNotFoundException("Could not instantiate", e);
            }
            in.addObjectToCycleCheck(obj);
            in.push_current_object(obj, 0);
            ((java.io.Externalizable) obj)
                    .readExternal(in.getJavaObjectInputStream());
            in.pop_current_object();
            return obj;
        }
    }

    private static class SerializableReader extends IbisReader {
        @Override
        Object readObject(IbisSerializationInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            Object obj = in.create_uninitialized_object(t.clazz);
            in.push_current_object(obj, 0);
            try {
                in.alternativeReadObject(t, obj);
            } catch (IllegalAccessException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Caught exception, now rethrow as NotSerializableException",
                            e);
                }
                throw new IbisNotSerializableException("handle " + typeHandle,
                        e);
            }
            in.pop_current_object();
            return obj;
        }
    }

    /**
     * The <code>Class</code> structure of the class represented by this
     * <code>AlternativeTypeInfo</code> structure.
     */
    Class<?> clazz;

    /**
     * Some Java-implementation-dependant stuff resides here.
     */
    private JavaDependantStuff javaDependantStuff;

    /** The sorted list of serializable fields. */
    Field[] serializable_fields;

    final IbisWriter writer;

    final IbisReader reader;

    /**
     * For each field, indicates whether the field is final. This is significant
     * for deserialization, because it determines the way in which the field can
     * be assigned to. The bytecode verifier does not allow arbitraty
     * assignments to final fields.
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
     * The "level" of a serializable class. The "level" of a serializable class
     * is computed as follows: - if its superclass is serializable: the level of
     * the superclass + 1. - if its superclass is not serializable: 1.
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

    /** Set if the class is Ibis serializable. */
    boolean isIbisSerializable = false;

    /** Set if the class is serializable. */
    boolean isSerializable = false;

    /** Set if the class is externalizable. */
    boolean isExternalizable = false;

    /** Set if the class represents an array. */
    boolean isArray = false;

    /** Set if the class represents a string. */
    boolean isString;

    /** Set if the class represents a class. */
    boolean isClass;

    /** Helper class for this class, generated by IOGenerator. */
    Generator gen;

    /**
     * A <code>Comparator</code> implementation for sorting the fields array.
     */
    private static class FieldComparator implements Comparator<Field> {
        /**
         * Compare fields alphabetically.
         */
        public int compare(Field f1, Field f2) {
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

    /**
     * Return the name of the class.
     *
     * @return the name of the class.
     */
    @Override
    public String toString() {
        return clazz.getName();
    }

    /**
     * Tries to create an object through the newInstance method of
     * JavaDependantStuff. Note that we cannot just call newInstance() from the
     * class, since that method calls the wrong constructor. The first
     * non-serializable class in the type hierarchy needs to have a
     * parameter-less constructor that is visible from this class.
     *
     * Returns <code>null</code> if it fails for some reason.
     *
     * @return the object, or <code>null</code>.
     */
    public Object newInstance() {
        return getJavaDependantStuff().newInstance();
    }

    /**
     * Gets the <code>AlternativeTypeInfo</code> for class <code>type</code>.
     *
     * @param type
     *            the <code>Class</code> of the requested type.
     * @return the <code>AlternativeTypeInfo</code> structure for this type.
     */
    public static synchronized AlternativeTypeInfo getAlternativeTypeInfo(
            Class<?> type) {
        AlternativeTypeInfo t = alternativeTypes.get(type);

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
     * @param classname
     *            the name of the requested type.
     * @return the <code>AlternativeTypeInfo</code> structure for this type.
     */
    public static synchronized AlternativeTypeInfo getAlternativeTypeInfo(
            String classname) throws ClassNotFoundException {
        Class<?> type = null;

        try {
            type = Class.forName(classname);
        } catch (ClassNotFoundException e) {
            type = Thread.currentThread().getContextClassLoader()
                    .loadClass(classname);
        }

        return getAlternativeTypeInfo(type);
    }

    /**
     * Gets the method with the given name, parameter types and return type.
     *
     * @param name
     *            the name of the method
     * @param paramTypes
     *            its parameter types
     * @param returnType
     *            its return type
     * @return the requested method, or <code>null</code> if it cannot be found.
     */
    private Method getMethod(String name, Class<?>[] paramTypes,
            Class<?> returnType) {
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
            if (!method.canAccess(method.getDeclaringClass())) {
                temporary_method = method;
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
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
     * @param o
     *            the object on which <code>writeObject</code> is to be invoked
     * @param out
     *            the <code>ObjectOutputStream</code> to be given as parameter
     * @exception IOException
     *                when anything goes wrong
     */
    void invokeWriteObject(Object o, ObjectOutputStream out)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        // System.out.println("invoke writeObject");
        writeObjectMethod.invoke(o, new Object[] { out });
    }

    /**
     * Invokes the <code>readObject</code> method on object <code>o</code>.
     *
     * @param o
     *            the object on which <code>readObject</code> is to be invoked
     * @param in
     *            the <code>ObjectInputStream</code> to be given as parameter
     * @exception IOException
     *                when anything goes wrong
     */
    void invokeReadObject(Object o, ObjectInputStream in)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        // System.out.println("invoke readObject");
        readObjectMethod.invoke(o, new Object[] { in });
    }

    /**
     * Invokes the <code>readResolve</code> method on object <code>o</code>.
     *
     * @param o
     *            the object on which <code>readResolve</code> is to be invoked
     * @exception IOException
     *                when anything goes wrong
     */
    Object invokeReadResolve(Object o) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        return readResolveMethod.invoke(o, new Object[0]);
    }

    /**
     * Invokes the <code>writeReplace</code> method on object <code>o</code>.
     *
     * @param o
     *            the object on which <code>writeReplace</code> is to be invoked
     * @exception IOException
     *                when anything goes wrong
     */
    Object invokeWriteReplace(Object o) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        return writeReplaceMethod.invoke(o, new Object[0]);
    }

    /**
     * Constructor is private. Use {@link #getAlternativeTypeInfo(Class)} to
     * obtain the <code>AlternativeTypeInfo</code> for a type.
     */
    @SuppressWarnings("deprecation")
    private AlternativeTypeInfo(Class<?> clazz) {

        this.clazz = clazz;
        if (SunJavaStuff.available) {
            javaDependantStuff = new SunJavaStuff(clazz);
        } else if (HarmonyJavaStuff.available) {
            javaDependantStuff = new HarmonyJavaStuff(clazz);
        } else if (DalvikJavaStuff.available) {
            javaDependantStuff = new DalvikJavaStuff(clazz);
        } else if (DalvikJavaStuffV2.available) {
            javaDependantStuff = new DalvikJavaStuffV2(clazz);
        } else if (DalvikJavaStuffV3.available) {
            javaDependantStuff = new DalvikJavaStuffV3(clazz);
        } else if (ClasspathJavaStuff.available) {
            javaDependantStuff = new ClasspathJavaStuff(clazz);
        }

        try {
            /*
             * Here we figure out what field the type contains, and which fields
             * we should write. We must also sort them by type and name to
             * ensure that we read them correctly on the other side. We cache
             * all of this so we only do it once for each type.
             */

            getSerialPersistentFields();

            /* see if the supertype is serializable */
            Class<?> superClass = clazz.getSuperclass();

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
                    new Class<?>[] { ObjectOutputStream.class }, Void.TYPE);
            readObjectMethod = getMethod("readObject",
                    new Class<?>[] { ObjectInputStream.class }, Void.TYPE);

            hasWriteObject = writeObjectMethod != null;
            hasReadObject = readObjectMethod != null;

            writeReplaceMethod = getMethod("writeReplace", new Class[0],
                    Object.class);

            readResolveMethod = getMethod("readResolve", new Class[0],
                    Object.class);

            hasReplace = writeReplaceMethod != null;

            // Determines whether a class is Ibis-serializable.
            // We cannot use "instanceof ibis.io.Serializable", because that
            // would also return true if a parent class implements
            // ibis.io.Serializable, which is not good enough.

            Class<?>[] intfs = clazz.getInterfaces();

            for (int i = 0; i < intfs.length; i++) {
                if (intfs[i].equals(ibis.io.Serializable.class)) {
                    isIbisSerializable = true;
                }
            }

            isSerializable = java.io.Serializable.class.isAssignableFrom(clazz);

            isExternalizable = java.io.Externalizable.class
                    .isAssignableFrom(clazz);

            isArray = clazz.isArray();
            isString = (clazz == java.lang.String.class);
            isClass = (clazz == java.lang.Class.class);
            if (isArray || isString || isClass) {
                gen = null;
            } else {
                Class<?> gen_class = null;
                String name = clazz.getName() + "_ibis_io_Generator";
                try {
                    gen_class = Class.forName(name);
                } catch (ClassNotFoundException e) {
                    // The loading of the class failed.
                    // Maybe, Ibis was loaded using the primordial classloader
                    // and the needed class was not.
                    try {
                        gen_class = Thread.currentThread()
                                .getContextClassLoader().loadClass(name);
                    } catch (Exception e1) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Class " + name + " not found!");
                        }
                        gen = null;
                    }
                }
                if (gen_class != null) {
                    try {
                        gen = (Generator) gen_class.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Could not instantiate " + name);
                        }
                        gen = null;
                    }
                }
            }

            Field[] fields = clazz.getDeclaredFields();

            /*
             * getDeclaredFields does not specify or guarantee a specific order.
             * Therefore, we sort the fields alphabetically, as does the
             * IOGenerator.
             */
            java.util.Arrays.sort(fields, fieldComparator);

            int len = fields.length;

            /*
             * Create the datastructures to cache the fields we need. Since we
             * don't know the size yet, we create large enough arrays, which
             * will later be replaced;
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
                /*
                 * Now count and store all the difference field types (only the
                 * ones that we should write!). Note that we store them into the
                 * array sorted by name !
                 */
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];

                    if (field == null) {
                        continue;
                    }

                    int modifiers = field.getModifiers();

                    if ((modifiers
                            & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
                        Class<?> field_type = field.getType();

                        /*
                         * This part is a bit scary. We basically switch of the
                         * Java field access checks so we are allowed to read
                         * private fields ....
                         */
                        if (!field.isAccessible()) {
                                temporary_field = field;
                            AccessController.doPrivileged(
                                    new PrivilegedAction<Object>() {
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
                    Class<?> field_type = serial_persistent_fields[i].getType();
                    if (field != null && !field.isAccessible()) {
                        temporary_field = field;
                        AccessController
                                .doPrivileged(new PrivilegedAction<Object>() {
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
                        fields_final[i] = ((serializable_fields[i]
                                .getModifiers() & Modifier.FINAL) != 0);
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

        writer = createWriter();
        reader = createReader();
    }

    private IbisWriter createWriter() {
        if (isArray) {
            return new ArrayWriter();
        }
        if (isIbisSerializable) {
            return new IbisSerializableWriter();
        }
        if (isExternalizable) {
            return new ExternalizableWriter();
        }
        if (isString) {
            return new StringWriter();
        }
        if (isClass) {
            return new ClassWriter();
        }
        if (isEnum()) {
            return new EnumWriter();
        }
        if (isSerializable) {
            return new SerializableWriter();
        }
        return new NotSerializableWriter();
    }

    private IbisReader createReader() {
        if (isArray) {
            return new ArrayReader();
        }
        if (gen != null) {
            return new IbisSerializableReader();
        }
        if (isExternalizable) {
            return new ExternalizableReader();
        }
        if (isString) {
            return new StringReader();
        }
        if (isClass) {
            return new ClassReader();
        }
        if (isEnum()) {
            return new EnumReader();
        }
        return new SerializableReader();
    }

    private boolean isEnum() {
        Class<?> superClass = clazz;
        while (superClass != null && superClass != java.lang.Object.class) {
            if (superClass.isEnum()) {
                return true;
            }
            superClass = superClass.getSuperclass();
        }
        return false;
    }

    /**
     * Looks for a declaration of serialPersistentFields, and, if present, makes
     * it accessible, and stores it in <code>serial_persistent_fields</code>.
     */
    @SuppressWarnings("deprecation")
    private void getSerialPersistentFields() {
        try {
            Field f = clazz.getDeclaredField("serialPersistentFields");
            int mask = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
            if ((f.getModifiers() & mask) == mask) {
                if (!f.isAccessible()) {
                    temporary_field = f;
                    AccessController
                            .doPrivileged(new PrivilegedAction<Object>() {
                                public Object run() {
                                    temporary_field.setAccessible(true);
                                    return null;
                                }
                            });
                }
                serial_persistent_fields = (java.io.ObjectStreamField[]) f
                        .get(null);
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
     * <code>tp</code> in either <code>SerialPersistentFields</code>, if it
     * exists, or in the <code>serializable_fields</code> array. An exception is
     * thrown when such a field is not found.
     *
     * @param name
     *            name of the field we are looking for
     * @param tp
     *            type of the field we are looking for
     * @return index in either <code>serial_persistent_fields</code> or
     *         <code>serializable_fields</code>.
     * @exception IllegalArgumentException
     *                when no such field is found.
     */
    int getOffset(String name, Class<?> tp) throws IllegalArgumentException {
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
                        if (name.equals(
                                serial_persistent_fields[i].getName())) {
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
        throw new IllegalArgumentException(
                "no field named " + name + " with type " + tp);
    }

    static Class<?> getClass(String n) {
        Class<?> c = null;
        try {
            c = Class.forName(n);
        } catch (ClassNotFoundException e) {
            throw new SerializationError(
                    "Internal error: could not load primitive array type " + n,
                    e);
        }
        return c;
    }

    public JavaDependantStuff getJavaDependantStuff() {
        if (javaDependantStuff == null) {
            throw new Error(
                    "Unrecognized Java version, ibis serialization not supported. Java version = "
                            + System.getProperty("java.version"));
        }
        return javaDependantStuff;
    }

    public void setJavaDependantStuff(JavaDependantStuff javaDependantStuff) {
        this.javaDependantStuff = javaDependantStuff;
    }
}
