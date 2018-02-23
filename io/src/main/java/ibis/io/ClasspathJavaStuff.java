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
import java.io.ObjectInputStream;
import java.io.ObjectStreamField;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ClasspathJavaStuff extends JavaDependantStuff {

    /**
     * Java-dependant stuff for GCJ/Classpath libraries.
     */
        
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];

    static Method newObject = null;
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
    
    private static class Dummy extends ObjectInputStream {

        protected Dummy() throws IOException, SecurityException {
            super();
        }        
    }
    
    private static Dummy dummyObjectInputStream = null;

    static {
	try {
	    // We need enough of an ObjectInputStream to access its newObject method.
            dummyObjectInputStream = new Dummy();
            
	    newObject = getMethod(ObjectInputStream.class,
		    "newObject", Class.class, Constructor.class);
	    
	    setFieldBoolean = getMethod(ObjectStreamField.class,
		    "setBooleanField", Object.class, Boolean.TYPE);
	    setFieldByte = getMethod(ObjectStreamField.class,
		    "setByteField", Object.class, Byte.TYPE);
	    setFieldShort = getMethod(ObjectStreamField.class,
		    "setShortField", Object.class,  Short.TYPE);
	    setFieldInt = getMethod(ObjectStreamField.class,
		    "setIntField", Object.class, Integer.TYPE);
	    setFieldLong = getMethod(ObjectStreamField.class,
		    "setLongField", Object.class, Long.TYPE);
	    setFieldChar = getMethod(ObjectStreamField.class,
		    "setCharField", Object.class, Character.TYPE);
	    setFieldFloat = getMethod(ObjectStreamField.class,
		    "setFloatField", Object.class, Float.TYPE);
	    setFieldDouble = getMethod(ObjectStreamField.class,
		    "setDoubleField", Object.class, Double.TYPE);
	    setFieldObject = getMethod(ObjectStreamField.class,
		    "setObjectField", Object.class, Object.class);
	    available = true;
	    logger.info("Classpath java stuff found!");
	} catch(Throwable e) {
	    logger.info("No Classpath java stuff: got exception", e);
	}
    }

    private Constructor<?> constructor = null;

    private static Method getMethod(Class<?> cl, String name, Class<?>... params) throws NoSuchMethodException {
	Method m = cl.getDeclaredMethod(name, params);
	m.setAccessible(true);
	return m;
    }

    ClasspathJavaStuff(Class<?> clazz) {
	super(clazz);
	if (! available) {
	    throw new Error("ClasspathJavaStuff not available");
	}

	// Find the class of the constructor that needs to be called
	// when creating a new instance of this class. This is the
	// empty constructor of the first non-serializable class in the
	// hierarchy.
	Class<?> constructorClass = clazz;

	// Find the first non-serializable class in the hierarchy.
	while (constructorClass != null
		&& java.io.Serializable.class.isAssignableFrom(constructorClass)) {
	    constructorClass = constructorClass.getSuperclass();
	}

	// Obtain the empty constructor.
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
		constructor = null;
	    } else if (!Modifier.isPublic(constructorModifiers)
		    && !Modifier.isProtected(constructorModifiers)) {
		if (! constructorClass.getPackage().getName().equals(clazz.getPackage().getName())) {
		    constructor = null;
		}
	    }
	}
    }

    Object newInstance() {
	if (constructor == null) {
	    return null;
	}
	try {
	    return newObject.invoke(dummyObjectInputStream, clazz, constructor);
	} catch (Throwable e) {
	    return null;
	}
    }

    void setFieldBoolean(Object ref, String fieldname, boolean d)
	    throws IOException {
	try {
	    setFieldBoolean.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldByte(Object ref, String fieldname, byte d) throws IOException {
	try {
	    setFieldByte.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldChar(Object ref, String fieldname, char d) throws IOException {
	try {
	    setFieldChar.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldClass(Object ref, String fieldname, Class<?> d)
	    throws IOException {
	try {
	    setFieldObject.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldDouble(Object ref, String fieldname, double d)
	    throws IOException {
	try {
	    setFieldDouble.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldFloat(Object ref, String fieldname, float d)
	    throws IOException {
	try {
	    setFieldFloat.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldInt(Object ref, String fieldname, int d) throws IOException {
	try {
	    setFieldInt.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldLong(Object ref, String fieldname, long d) throws IOException {
	try {
	    setFieldLong.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldObject(Object ref, String fieldname, Object d, String fieldsig)
	    throws IOException {
	try {
	    setFieldObject.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldShort(Object ref, String fieldname, short d)
	    throws IOException {
	try {
	    setFieldShort.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }

    void setFieldString(Object ref, String fieldname, String d)
	    throws IOException {
	try {
	    setFieldObject.invoke(objectStreamClass.getField(fieldname), ref, d);
	} catch (Throwable e) {
	    throw new IbisIOException("Got exception", e);
	}
    }
}
