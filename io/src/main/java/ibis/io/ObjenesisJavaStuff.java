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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.objenesis.ObjenesisSerializer;
import org.objenesis.instantiator.ObjectInstantiator;

final class ObjenesisJavaStuff extends JavaDependantStuff {


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
                    .forName("sun.misc.Unsafe");
            Field uf = cl.getDeclaredField("theUnsafe");
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
            logger.info("Objenesis Java Stuff not available", e);
        }
    }

    ObjenesisJavaStuff(Class<?> clazz) {
        super(clazz);
        if (! available) {
            throw new Error("ObjenesisJavaStuff not available");
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
	System.err.println("calling newInstance on " + clazz.getCanonicalName());
	ObjenesisSerializer os = new ObjenesisSerializer();
	ObjectInstantiator<?> ins = os.getInstantiatorOf(clazz);
	return ins.newInstance();
    }
}
