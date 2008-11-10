package ibis.io;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class JavaDependantStuff {

    private static ClassLoader customClassLoader;

    static final Logger logger = LoggerFactory
            .getLogger(JavaDependantStuff.class);

    static final boolean DEBUG = IOProperties.DEBUG;

    static {
        String clName = System.getProperty(IOProperties.s_classloader);
        if (clName != null) {
            // we try to instantiate it
            try {
                Class<?> classDefinition = Class.forName(clName);
                customClassLoader = (ClassLoader) classDefinition.newInstance();
            } catch (Exception e) {
                logger.warn("Warning: could not find or load custom "
                        + "classloader " + clName, e);
            }
        }
    }

    Class<?> clazz;

    JavaDependantStuff(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * This method tries to load a class given its name. It tries the default
     * classloader, and the one from the thread context. Also, apparently some
     * classloaders do not understand array classes, and from the Java
     * documentation, it is not clear that they should. Therefore, if the
     * typeName indicates an array type, and the obvious attempts to load the
     * class fail, this method also tries to load the base type of the array.
     * 
     * @param typeName
     *                the name of the type to be loaded
     * @exception ClassNotFoundException
     *                    is thrown when the class could not be loaded.
     * @return the loaded class
     */
    static Class<?> getClassFromName(String typeName)
            throws ClassNotFoundException {
        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            try {
                if (DEBUG && logger.isDebugEnabled()) {
                    logger.debug("Could not load class " + typeName
                            + " using Class.forName(), trying "
                            + "Thread.currentThread()."
                            + "getContextClassLoader().loadClass()");
                    logger.debug("Default class loader is "
                            + JavaDependantStuff.class
                                    .getClassLoader());
                    logger.debug("now trying "
                            + Thread.currentThread().getContextClassLoader());
                }
                return Thread.currentThread().getContextClassLoader()
                        .loadClass(typeName);
            } catch (ClassNotFoundException e2) {
                int dim = 0;

                /*
                 * Some classloaders are not able to load array classes.
                 * Therefore, if the name describes an array, try again with the
                 * base type.
                 */
                if (typeName.length() > 0 && typeName.charAt(0) == '[') {
                    char[] s = typeName.toCharArray();
                    while (dim < s.length && s[dim] == '[') {
                        dim++;
                    }
                    int begin = dim;
                    int end = s.length;
                    if (dim < s.length && s[dim] == 'L') {
                        begin++;
                    }
                    if (s[end - 1] == ';') {
                        end--;
                    }
                    typeName = typeName.substring(begin, end);

                    int dims[] = new int[dim];
                    for (int i = 0; i < dim; i++)
                        dims[i] = 0;

                    /*
                     * Now try to load the base class, create an array from it
                     * and then return its class.
                     */
                    return java.lang.reflect.Array.newInstance(
                            getClassFromName(typeName), dims).getClass();
                }
                return loadClassFromCustomCL(typeName);
            }
        }
    }

    private static Class<?> loadClassFromCustomCL(String className)
            throws ClassNotFoundException {
        if (DEBUG && logger.isDebugEnabled()) {
            System.out.println("loadClassTest " + className);
        }
        if (customClassLoader == null) {
            throw new ClassNotFoundException(className);
        }
        if (DEBUG && logger.isDebugEnabled()) {
            System.out.println("******* Calling custom classloader");
        }
        return customClassLoader.loadClass(className);
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
    abstract void setFieldDouble(Object ref, String fieldname, double d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    abstract void setFieldLong(Object ref, String fieldname, long d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    abstract void setFieldFloat(Object ref, String fieldname, float d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    abstract void setFieldInt(Object ref, String fieldname, int d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    abstract void setFieldShort(Object ref, String fieldname, short d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    abstract void setFieldChar(Object ref, String fieldname, char d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    abstract void setFieldByte(Object ref, String fieldname, byte d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    abstract void setFieldBoolean(Object ref, String fieldname, boolean d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     */
    abstract void setFieldString(Object ref, String fieldname, String d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     * 
     * @exception ClassNotFoundException
     *                    when the class could not be loaded.
     */
    abstract void setFieldClass(Object ref, String fieldname, Class<?> d)
            throws IOException;

    /**
     * See {@link #setFieldDouble(Object, String, double)} for a description.
     * 
     * @param fieldsig
     *                signature of the field
     */
    abstract void setFieldObject(Object ref, String fieldname, Object d,
            String fieldsig) throws IOException;
    
    /**
     * Creates a new object of this class, or <code>null</code> if it fails.
     * @return the new object, or <code>null</code>.
     */
    abstract Object newInstance();

}
