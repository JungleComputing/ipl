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
package ibis.io;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class HarmonyJavaStuff extends JavaDependentStuff {

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    static Class<?> accessorClass = null;
    static Object accessor = null;
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
    static Method getFieldID = null;
    static Method getMethodID = null;
    static boolean available = false;

    private static class Dummy extends java.io.ObjectInputStream {

        protected Dummy() throws IOException, SecurityException {
            super();
        }
    }

    static {
        try {
            // Try to work around security check in AccessorFactory.
            Dummy dummy = new Dummy();
            Class<?> cl = dummy.getClass().getSuperclass();
            Field f = cl.getDeclaredField("accessor");
            f.setAccessible(true);
            accessor = f.get(dummy);
            // dummy.close(); Gives null-pointer exception

            accessorClass = Class.forName("org.apache.harmony.misc.accessors.ObjectAccessor");

            newInstance = getMethod(accessorClass, "newInstance", Class.class, Long.TYPE, Object[].class);
            setFieldBoolean = getMethod(accessorClass, "setBoolean", Object.class, Long.TYPE, Boolean.TYPE);
            setFieldByte = getMethod(accessorClass, "setByte", Object.class, Long.TYPE, Byte.TYPE);
            setFieldShort = getMethod(accessorClass, "setShort", Object.class, Long.TYPE, Short.TYPE);
            setFieldInt = getMethod(accessorClass, "setInt", Object.class, Long.TYPE, Integer.TYPE);
            setFieldLong = getMethod(accessorClass, "setLong", Object.class, Long.TYPE, Long.TYPE);
            setFieldChar = getMethod(accessorClass, "setChar", Object.class, Long.TYPE, Character.TYPE);
            setFieldFloat = getMethod(accessorClass, "setFloat", Object.class, Long.TYPE, Float.TYPE);
            setFieldDouble = getMethod(accessorClass, "setDouble", Object.class, Long.TYPE, Double.TYPE);
            setFieldObject = getMethod(accessorClass, "setObject", Object.class, Long.TYPE, Object.class);
            getFieldID = getMethod(accessorClass, "getFieldID", Class.class, String.class);
            getMethodID = getMethod(accessorClass, "getMethodID", Class.class, String.class, Class[].class);
            available = true;
        } catch (Throwable e) {
            logger.info("No Harmony Java stuff, got exception", e);
        }
    }

    private Class<?> constructorClass = null;
    private long constructorID;

    private static Method getMethod(Class<?> cl, String name, Class<?>... params) throws SecurityException, NoSuchMethodException {
        return cl.getDeclaredMethod(name, params);
    }

    HarmonyJavaStuff(Class<?> clazz) {
        super(clazz);

        if (!available) {
            throw new Error("HarmonyJavaStuff not available");
        }
        // Find the class of the constructor that needs to be called
        // when creating a new instance of this class. This is the
        // empty constructor of the first non-serializable class in the
        // hierarchy.
        constructorClass = clazz;

        // Find the first non-serializable class in the hierarchy.
        while (constructorClass != null && java.io.Serializable.class.isAssignableFrom(constructorClass)) {
            constructorClass = constructorClass.getSuperclass();
        }

        // Obtain the empty constructor.
        Constructor<?> constructor = null;
        if (constructorClass != null) {
            try {
                constructor = constructorClass.getDeclaredConstructor(EMPTY_CLASS_ARRAY);
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
            } else if (!Modifier.isPublic(constructorModifiers) && !Modifier.isProtected(constructorModifiers)) {
                if (!constructorClass.getPackage().getName().equals(clazz.getPackage().getName())) {
                    constructorClass = null;
                }
            }

            if (constructorClass != null) {
                try {
                    constructorID = (Long) getMethodID.invoke(accessor, constructorClass, null, EMPTY_CLASS_ARRAY);
                } catch (Throwable e) {
                    constructorClass = null;
                }
            }
        }
    }

    private long getFieldID(String name) throws IbisIOException {
        // TODO: cache these?
        try {
            return (Long) getFieldID.invoke(accessor, clazz, name);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    Object newInstance() {
        if (constructorClass == null) {
            return null;
        }
        try {
            return newInstance.invoke(accessor, clazz, constructorID, null);
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    void setFieldBoolean(Object ref, String fieldname, boolean d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldBoolean.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldByte(Object ref, String fieldname, byte d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldByte.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldChar(Object ref, String fieldname, char d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldChar.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldClass(Object ref, String fieldname, Class<?> d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldObject.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldDouble(Object ref, String fieldname, double d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldDouble.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldFloat(Object ref, String fieldname, float d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldFloat.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldInt(Object ref, String fieldname, int d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldInt.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldLong(Object ref, String fieldname, long d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldLong.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldObject(Object ref, String fieldname, Object d, String fieldsig) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldObject.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldShort(Object ref, String fieldname, short d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldShort.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

    @Override
    void setFieldString(Object ref, String fieldname, String d) throws IOException {
        long id = getFieldID(fieldname);
        try {
            setFieldShort.invoke(accessor, ref, id, d);
        } catch (Throwable e) {
            throw new IbisIOException("Got exception", e);
        }
    }

}
