package ibis.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class DalvikJavaStuff extends JavaDependantStuff {
    
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
    static Method newInstance = null;
    
    static {
        try {
            newInstance = ObjectInputStream.class.getDeclaredMethod(
                    "newInstance", new Class[] {Class.class, Class.class});
        } catch (Throwable e) {
            // ignored
        }
        if (newInstance != null) {
            try {
                newInstance.setAccessible(true);
            } catch (Throwable e) {
                newInstance = null;
            }
        }
    }
    
    private Class<?>constructorClass = null;
    
    DalvikJavaStuff(Class<?> clazz) throws IOException {
        super(clazz);
        if (newInstance == null) {
            throw new IOException("newInstance method not found");
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
            return newInstance.invoke(new Class[] { clazz, constructorClass} );
        } catch (Throwable e) {
             return null;
        }
    }

    void setFieldBoolean(Object ref, String fieldname, boolean d)
            throws IOException {
        // TODO Auto-generated method stub

    }

    void setFieldByte(Object ref, String fieldname, byte d) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void setFieldChar(Object ref, String fieldname, char d) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void setFieldClass(Object ref, String fieldname, Class<?> d)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void setFieldDouble(Object ref, String fieldname, double d)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void setFieldFloat(Object ref, String fieldname, float d)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void setFieldInt(Object ref, String fieldname, int d) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void setFieldLong(Object ref, String fieldname, long d) throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void setFieldObject(Object ref, String fieldname, Object d, String fieldsig)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void setFieldShort(Object ref, String fieldname, short d)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    void setFieldString(Object ref, String fieldname, String d)
            throws IOException {
        // TODO Auto-generated method stub

    }

}
