/* $Id: IOGenerator.java 7063 2007-12-05 15:49:42Z nick $ */

package ibis.io.rewriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

/**
 * SerializationInfo contains static methods for determining information about a particular
 * class as well as an interface to determine the proper method names for serialization of
 * a particular type.
 * 
 * @author Nick Palmer (npr200@few.vu.nl)
 *
 */
class SerializationInfo implements RewriterConstants {

    private static class FieldComparator implements Comparator<Field> {
        public int compare(Field f1, Field f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }

    static FieldComparator fieldComparator = new FieldComparator();
    
	static HashMap<String, Long> serialversionids = new HashMap<String, Long>();
	
    static HashMap<Type, SerializationInfo> primitiveSerialization = new HashMap<Type, SerializationInfo> ();

    static SerializationInfo referenceSerialization = new SerializationInfo(METHOD_WRITE_OBJECT,
            METHOD_READ_OBJECT, METHOD_READ_FIELD_OBJECT, Type.OBJECT, false);

	static {
        primitiveSerialization.put(Type.BOOLEAN, new SerializationInfo(
                METHOD_WRITE_BOOLEAN, METHOD_READ_BOOLEAN, METHOD_READ_FIELD_BOOLEAN,
                Type.BOOLEAN, true));

        primitiveSerialization.put(Type.BYTE, new SerializationInfo(
                METHOD_WRITE_BYTE, METHOD_READ_BYTE, METHOD_READ_FIELD_BYTE, Type.BYTE, true));

        primitiveSerialization.put(Type.SHORT, new SerializationInfo(
                METHOD_WRITE_SHORT, METHOD_READ_SHORT, METHOD_READ_FIELD_SHORT, Type.SHORT, true));

        primitiveSerialization.put(Type.CHAR, new SerializationInfo(
                METHOD_WRITE_CHAR, METHOD_READ_CHAR, METHOD_READ_FIELD_CHAR, Type.CHAR, true));

        primitiveSerialization.put(Type.INT, new SerializationInfo("writeInt",
                METHOD_READ_INT, METHOD_READ_FIELD_INT, Type.INT, true));
        
        primitiveSerialization.put(Type.LONG, new SerializationInfo(
                METHOD_WRITE_LONG, METHOD_READ_LONG, METHOD_READ_FIELD_LONG, Type.LONG, true));

        primitiveSerialization.put(Type.FLOAT, new SerializationInfo(
                METHOD_WRITE_FLOAT, METHOD_READ_FLOAT, METHOD_READ_FIELD_FLOAT, Type.FLOAT, true));

        primitiveSerialization.put(Type.DOUBLE, new SerializationInfo(
                METHOD_WRITE_DOUBLE, METHOD_READ_DOUBLE, METHOD_READ_FIELD_DOUBLE, Type.DOUBLE,
                true));
        primitiveSerialization.put(Type.STRING, new SerializationInfo(
                METHOD_WRITE_STRING, METHOD_READ_STRING, METHOD_READ_FIELD_STRING, Type.STRING,
                true));
        primitiveSerialization.put(java_lang_class_type, new SerializationInfo(
                METHOD_WRITE_CLASS, METHOD_READ_CLASS, METHOD_READ_FIELD_CLASS,
                java_lang_class_type, true));
	}
    
	String write_name;

    String read_name;

    String final_read_name;

    Type tp;

    Type[] param_tp_arr;

    boolean primitive;

    SerializationInfo(String wn, String rn, String frn, Type t,
            boolean primitive) {
        this.write_name = wn;
        this.read_name = rn;
        this.final_read_name = frn;
        this.tp = t;
        this.param_tp_arr = new Type[] { t };
        this.primitive = primitive;
    }
    
    static SerializationInfo getSerializationInfo(Type tp) {
        SerializationInfo temp
                = primitiveSerialization.get(tp);
        return (temp == null ? referenceSerialization : temp);
    }

	static boolean directImplementationOf(JavaClass clazz,
            String name) {
        String names[] = clazz.getInterfaceNames();
        String supername = clazz.getSuperclassName();

        if (supername.equals(name)) {
            return true;
        }

        if (names == null) {
            return false;
        }
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    static boolean predecessor(String c1, JavaClass c2) {
        String n = c2.getSuperclassName();

        // System.out.println("comparing " + c1 + ", " + n);
        if (n.equals(c1)) {
            return true;
        }
        if (n.equals(TYPE_JAVA_LANG_OBJECT)) {
            return false;
        }
        return SerializationInfo.predecessor(c1, Repository.lookupClass(n));
    }

    static boolean isFinal(Type t) {
        if (t instanceof BasicType) {
            return true;
        }
        if (t instanceof ArrayType) {
            return SerializationInfo.isFinal(((ArrayType) t).getBasicType());
        }
        if (t instanceof ObjectType) {
            String name = ((ObjectType) t).getClassName();
            JavaClass c = Repository.lookupClass(name);
            if (c == null) {
                return false;
            }
            return c.isFinal();
        }
        return false;
    }

    static boolean isIbisSerializable(JavaClass clazz) {
        return directImplementationOf(clazz, TYPE_IBIS_IO_SERIALIZABLE);
    }

	static boolean isExternalizable(JavaClass clazz) {
        return Repository.implementationOf(clazz, TYPE_JAVA_IO_EXTERNALIZABLE);
    }
	
    static boolean isSerializable(JavaClass clazz) {
        return Repository.implementationOf(clazz, TYPE_JAVA_IO_SERIALIZABLE);
    }
	
    static boolean hasSerialPersistentFields(Field[] fields) {
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if (f.getName().equals(FIELD_SERIAL_PERSISTENT_FIELDS)
                    && f.isFinal()
                    && f.isStatic()
                    && f.isPrivate()
                    && f.getSignature().equals(
                            TYPE_LJAVA_IO_OBJECT_STREAM_FIELD)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasFinalFields(Field[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].isFinal()) {
                return true;
            }
        }
        return false;
    }

    static boolean hasIbisConstructor(JavaClass cl) {
        Method[] clMethods = cl.getMethods();

        for (int i = 0; i < clMethods.length; i++) {
            if (clMethods[i].getName().equals(METHOD_INIT)
                    && clMethods[i].getSignature().equals(
                            SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Computes the serial version UID value for the given class.
     * @param clazz TODO
     */
    static long computeSUID(JavaClass clazz) {
        if (!SerializationInfo.isSerializable(clazz)) {
            return 0L;
        }

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);

            // 1. The class name written using UTF encoding.
            dout.writeUTF(clazz.getClassName());

            // 2. The class modifiers written as a 32-bit integer.
            int classModifiers = clazz.getModifiers()
                    & (Constants.ACC_PUBLIC | Constants.ACC_FINAL
                            | Constants.ACC_INTERFACE
                            | Constants.ACC_ABSTRACT);

            // Only set ABSTRACT for an interface when it has methods.
            Method[] cMethods = clazz.getMethods();
            if ((classModifiers & Constants.ACC_INTERFACE) != 0) {
                if (cMethods.length > 0) {
                    classModifiers |= Constants.ACC_ABSTRACT;
                } else {
                    classModifiers &= ~Constants.ACC_ABSTRACT;
                }
            }
            dout.writeInt(classModifiers);

            // 3. The name of each interface sorted by name written using
            //    UTF encoding.
            String[] interfaceNames = clazz.getInterfaceNames();
            Arrays.sort(interfaceNames);
            for (int i = 0; i < interfaceNames.length; i++) {
                dout.writeUTF(interfaceNames[i]);
            }

            // 4. For each field of the class sorted by field name (except
            //    private static and private transient fields):
            Field[] cFields = clazz.getFields();
            Arrays.sort(cFields, fieldComparator);
            for (int i = 0; i < cFields.length; i++) {
                int mods = cFields[i].getModifiers();
                if (((mods & Constants.ACC_PRIVATE) == 0)
                        || ((mods & (Constants.ACC_STATIC
                                    | Constants.ACC_TRANSIENT)) == 0)) {
                    // 4.1. The name of the field in UTF encoding.
                    dout.writeUTF(cFields[i].getName());
                    // 4.2. The modifiers of the field written as a
                    //      32-bit integer.
                    dout.writeInt(mods);
                    // 4.3. The descriptor of the field in UTF encoding
                    dout.writeUTF(cFields[i].getSignature());
                }
            }

            // This is where the trouble starts for serialver.

            // 5. If a class initializer exists, write out the following:
            for (int i = 0; i < cMethods.length; i++) {
                if (cMethods[i].getName().equals(METHOD_CLINIT)) {
                    // 5.1. The name of the method, <clinit>, in UTF
                    //      encoding.
                    dout.writeUTF(METHOD_CLINIT);
                    // 5.2. The modifier of the method,
                    //      java.lang.reflect.Modifier.STATIC, written as
                    //      a 32-bit integer.
                    dout.writeInt(Constants.ACC_STATIC);
                    // 5.3. The descriptor of the method, ()V, in UTF
                    //      encoding.
                    dout.writeUTF("()V");
                    break;
                }
            }

            Arrays.sort(cMethods, new Comparator<Method>() {
                public int compare(Method o1, Method o2) {
                    String name1 = o1.getName();
                    String name2 = o2.getName();
                    if (name1.equals(name2)) {
                        String sig1 = o1.getSignature();
                        String sig2 = o2.getSignature();
                        return sig1.compareTo(sig2);
                    }
                    return name1.compareTo(name2);
                }
            });

            // 6. For each non-private constructor sorted by method name
            //    and signature:
            for (int i = 0; i < cMethods.length; i++) {
                if (cMethods[i].getName().equals(METHOD_INIT)) {
                    int mods = cMethods[i].getModifiers();
                    if ((mods & Constants.ACC_PRIVATE) == 0) {
                        // 6.1. The name of the method, <init>, in UTF
                        //      encoding.
                        dout.writeUTF(METHOD_INIT);
                        // 6.2. The modifiers of the method written as a
                        //      32-bit integer.
                        dout.writeInt(mods);
                        // 6.3. The descriptor of the method in UTF
                        //      encoding.
                        dout.writeUTF(cMethods[i].getSignature().replace(
                                '/', '.'));
                    }
                }
            }

            // 7. For each non-private method sorted by method name and
            //    signature:
            for (int i = 0; i < cMethods.length; i++) {
                if (!cMethods[i].getName().equals(METHOD_INIT)
                        && !cMethods[i].getName().equals(METHOD_CLINIT)) {
                    int mods = cMethods[i].getModifiers();
                    if ((mods & Constants.ACC_PRIVATE) == 0) {
                        // 7.1. The name of the method in UTF encoding.
                        dout.writeUTF(cMethods[i].getName());
                        // 7.2. The modifiers of the method written as a
                        //      32-bit integer.
                        dout.writeInt(mods);
                        // 7.3. The descriptor of the method in UTF
                        //      encoding.
                        dout.writeUTF(cMethods[i].getSignature().replace(
                                '/', '.'));
                    }
                }
            }

            dout.flush();

            // 8. The SHA-1 algorithm is executed on the stream of bytes
            //    produced by DataOutputStream and produces five 32-bit
            //    values sha[0..4].
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte[] hashBytes = md.digest(bout.toByteArray());

            long hash = 0;
            // Use the first 8 bytes.
            for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                hash = (hash << 8) | (hashBytes[i] & 0xFF);
            }
            return hash;
        } catch (Exception ex) {
            System.err.println("Warning: could not get serialVersionUID "
                    + "for class " + clazz.getClassName());
            return 0L;
        }
    }

	static long getSerialVersionUID(String classname, JavaClass clazz) {
		long uid;
		Long ui = serialversionids.get(classname);
        if (ui == null) {
            uid = SerializationInfo.computeSUID(clazz);
            serialversionids.put(classname, new Long(uid));
        } else {
            uid = ui.longValue();
        }
		return uid;
	}

    static int findMethod(Method[] methods, String name, String signature) {
    	for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().equals(name)
                    && methods[i].getSignature().equals(signature)) {
                return i;
            }
        }
        return -1;
    }

    static boolean hasWriteObject(Method[] methods) {
        return findMethod(methods, METHOD_WRITE_OBJECT, SIGNATURE_LJAVA_IO_OBJECT_OUTPUT_STREAM_V)
                != -1;
    }

    static boolean hasReadObject(Method[] methods) {
        return findMethod(methods, METHOD_READ_OBJECT, SIGNATURE_LJAVA_IO_OBJECT_INPUT_STREAM_V)
                != -1;
    }
}