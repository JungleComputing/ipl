/* $Id$ */

package ibis.io.jme;

import java.io.IOException;
import java.util.Hashtable;

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
final class AlternativeTypeInfo extends IOProperties implements Constants {

    /**
     * Maintains all <code>AlternativeTypeInfo</code> structures in a
     * hashmap, to be accessed through their classname.
     */
    private static Hashtable alternativeTypes
            = new Hashtable();

    private static class ArrayReaderWriter extends IbisReaderWriter {
        public void writeObject(ObjectOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            out.writeArray(ref, t.clazz, unshared);
        }
        
        public Object readObject(ObjectInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            return in.readArray(t.clazz, typeHandle);
        }
    }

    private static class JMESerializableReaderWriter extends IbisReaderWriter {
        public void writeObject(ObjectOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            ((JMESerializable) ref).generated_JME_WriteObject(out);
        }
        
        public Object readObject(ObjectInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            return t.gen.generated_newInstance(in);
        }
    }

    private static class ExternalizableReaderWriter extends IbisReaderWriter {
        public void writeObject(ObjectOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            out.push_current_object(ref, 0);
            ((Externalizable) ref).writeExternal(
                    out.getJavaObjectOutputStream());
            out.pop_current_object();
        }
        
        public Object readObject(ObjectInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            Object obj;
            try {
                // Also calls parameter-less constructor
                obj = t.clazz.newInstance();
            } catch(Throwable e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Caught exception, now rethrow as ClassNotFound", e);
                }
                throw new ClassNotFoundException("Could not instantiate");
            }
            in.addObjectToCycleCheck(obj);
            in.push_current_object(obj, 0);
            ((Externalizable) obj).readExternal(
                    in.getJavaObjectInputStream());
            in.pop_current_object();
            return obj;
        }
    }

    private static class StringReaderWriter extends IbisReaderWriter {
        public void writeObject(ObjectOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            out.writeUTF((String) ref);
        }
        
        public Object readObject(ObjectInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            String o = in.readUTF();
            in.addObjectToCycleCheck(o);
            return o;
        }
    }

    private static class ClassReaderWriter extends IbisReaderWriter {
        public void writeObject(ObjectOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            super.writeHeader(out, ref, t, hashCode, unshared);
            out.writeUTF((String) ref);
        }
        
        public Object readObject(ObjectInputStream in,
                AlternativeTypeInfo t, int typeHandle)
                throws IOException, ClassNotFoundException {
            String o = in.readUTF();
            Object obj = in.getClassFromName(o);
            in.addObjectToCycleCheck(obj);
            return obj;
        }
    }

    private static class PrimitiveReaderWriter extends IbisReaderWriter {
        public void writeObject(ObjectOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
        	if ( ref.getClass() == Boolean.class) {
        		super.writeHeader(out, ref, t, hashCode, unshared);
        		out.writeBoolean(((Boolean)ref).booleanValue());
        	}
        	else if (ref.getClass() == Byte.class) {
        		super.writeHeader(out, ref, t, hashCode, unshared);
        		out.writeByte(((Byte)ref).byteValue());
        	}
        	else if (ref.getClass() == Short.class) {
        		super.writeHeader(out, ref, t, hashCode, unshared);
        		out.writeShort(((Short)ref).shortValue());
        	}
        	else if (ref.getClass() == Double.class) {
        		super.writeHeader(out, ref, t, hashCode, unshared);
        		out.writeDouble(((Double)ref).doubleValue());
        	}
        	else if (ref.getClass() == Float.class) {
        		super.writeHeader(out, ref, t, hashCode, unshared);
        		out.writeFloat(((Float)ref).floatValue());
        	}
        	else if (ref.getClass() == Integer.class) {
        		super.writeHeader(out, ref, t, hashCode, unshared);
        		out.writeInt(((Integer)ref).intValue());
        	}
        	else if (ref.getClass() == Long.class) {
        		super.writeHeader(out, ref, t, hashCode, unshared);
        		out.writeLong(((Long)ref).longValue());
        	}
        	else if (ref.getClass() == Character.class) {
        		super.writeHeader(out, ref, t, hashCode, unshared);
        		out.writeChar(((Character)ref).charValue());
        	}
        	else {
        		throw new SerializationError("Unknown primitive type!");
        	}
        }

		public Object readObject(ObjectInputStream in, AlternativeTypeInfo t, int typeHandle) throws IOException, ClassNotFoundException {
        	if ( t.clazz == Boolean.class) {
                Boolean o = new Boolean(in.readBoolean());
                in.addObjectToCycleCheck(o);
                return o;
        	}
        	else if (t.clazz == Byte.class) {
                Byte o = new Byte(in.readByte());
                in.addObjectToCycleCheck(o);
                return o;
        	}
        	else if (t.clazz == Short.class) {
                Short o = new Short(in.readShort());
                in.addObjectToCycleCheck(o);
                return o;
        	}
        	else if (t.clazz == Double.class) {
                Double o = new Double(in.readDouble());
                in.addObjectToCycleCheck(o);
                return o;
        	}
        	else if (t.clazz == Float.class) {
                Float o = new Float(in.readFloat());
                in.addObjectToCycleCheck(o);
                return o;
        	}
        	else if (t.clazz == Integer.class) {
                Integer o = new Integer(in.readInt());
                in.addObjectToCycleCheck(o);
                return o;
        	}
        	else if (t.clazz == Long.class) {
                Long o = new Long(in.readLong());
                in.addObjectToCycleCheck(o);
                return o;
        	}
        	else if (t.clazz == Character.class) {
                Character o = new Character(in.readChar());
                in.addObjectToCycleCheck(o);
                return o;
        	}
        	else {
        		throw new SerializationError("Unknown primitive type!");
        	}
		}
    }
    
    private static class NotSerializableReaderWriter extends IbisReaderWriter {
        public void writeObject(ObjectOutputStream out, Object ref,
                AlternativeTypeInfo t, int hashCode, boolean unshared)
                throws IOException {
            throw new NotSerializableException("Not serializable: " +
                    t.clazz.getName());
        }
        
		public Object readObject(ObjectInputStream in, AlternativeTypeInfo t, int typeHandle) throws IOException, ClassNotFoundException {
            throw new NotSerializableException("Not serializable: " +
                    t.clazz.getName());
		}
    }

    /**
     * The <code>Class</code> structure of the class represented by this
     * <code>AlternativeTypeInfo</code> structure.
     */
    Class clazz;

    final IbisReaderWriter readerWriter;

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
    ObjectStreamField[] serial_persistent_fields = null;

    /** Set if the class has a <code>readObject</code> method. */
    boolean hasReadObject;

    /** Set if the class has a <code>writeObject</code> method. */
    boolean hasWriteObject;

    /** Set if the class has a <code>writeReplace</code> method. */
    boolean hasReplace;

    /** Set if the class is Ibis serializable. */
    boolean isJMESerializable = false;

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
        AlternativeTypeInfo t = (AlternativeTypeInfo)alternativeTypes.get(type);

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

        type = Class.forName(classname);

        return getAlternativeTypeInfo(type);
    }

    /**
     * Constructor is private. Use {@link #getAlternativeTypeInfo(Class)} to
     * obtain the <code>AlternativeTypeInfo</code> for a type.
     */
    private AlternativeTypeInfo(Class clazz) {

        this.clazz = clazz;

        try {
            /*
             Here we figure out what field the type contains, and which fields 
             we should write. We must also sort them by type and name to ensure
             that we read them correctly on the other side. We cache all of
             this so we only do it once for each type.
             */

            // TODO: Implement in some other way? getSerialPersistentFields();

            /* see if the supertype is serializable 
				TODO: Some other way?
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
            */

            /* Now see if it has a writeObject/readObject. */
            /* TODO: How to handle ?
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
            */
            
            // Determines whether a class is Ibis-serializable.
            // We cannot use "instanceof ibis.io.Serializable", because that
            // would also return true if a parent class implements
            // ibis.io.Serializable, which is not good enough.
        	/* TODO:
        	 * The generator needs to stick a flag in the class or something
        	 * since we can't get the interfaces.
            Class[] intfs = clazz.getInterfaces();

            for (int i = 0; i < intfs.length; i++) {
                if (intfs[i].equals(ibis.io.Serializable.class)) {
                    isIbisSerializable = true;
                }
            }
            */

            isJMESerializable = ibis.io.jme.JMESerializable.class.isAssignableFrom(clazz);

            isExternalizable = ibis.io.jme.Externalizable.class.isAssignableFrom(clazz);

            isArray = clazz.isArray();
            isString = (clazz == java.lang.String.class);
            isClass = (clazz == java.lang.Class.class);
            if (isArray || isString || isClass) {
                gen = null;
            } else {
                Class gen_class = null;
                String name = clazz.getName() + "_ibis_io_Generator";
                try {
                    gen_class = Class.forName(name);
                } catch (ClassNotFoundException e) {
                        gen = null;
                }
                if (gen_class != null) {
                    try {
                        gen = (Generator) gen_class.newInstance();
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Could not instantiate " + name);
                        }
                        gen = null;
                    }
                }
            }
        } catch (Exception e) {
            throw new SerializationError("Cannot initialize serialization "
                    + "info for " + clazz.getName(), e);
        }

        readerWriter = createReaderWriter();
    }

    private IbisReaderWriter createReaderWriter() {
        if (isArray) {
            return new ArrayReaderWriter();
        }
        if (isJMESerializable) {
            return new JMESerializableReaderWriter();
        }
        if (isExternalizable) {
            return new ExternalizableReaderWriter();
        }
        if (isString) {
            return new StringReaderWriter();
        }
        if (isClass) {
            return new ClassReaderWriter();
        }
        if (this.clazz == Boolean.class ||
        		this.clazz == Byte.class ||
        		this.clazz == Short.class ||
        		this.clazz == Integer.class ||
        		this.clazz == Long.class ||
        		this.clazz == Float.class ||
        		this.clazz == Double.class ||
        		this.clazz == Character.class ) {
        	return new PrimitiveReaderWriter();
        }
        return new NotSerializableReaderWriter();
    }

    static Class getClass(String n) {
        Class c = null;
        try {
            c = Class.forName(n);
        } catch (ClassNotFoundException e) {
            throw new SerializationError(
                    "Internal error: could not load primitive array type " + n,
                    e);
        }
        return c;
    }
}
