/* $Id: IOGenerator.java 7063 2007-12-05 15:49:42Z nick $ */

package ibis.io.rewriter;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

/**
 * The RewriterConstants contains constants for various TYPE, METHOD and VARIABLE 
 * as well as other constants used by the CodeGenerator to generate the code.
 * 
 * @author Nick Palmer (npr200@few.vu.nl)
 *
 */
interface RewriterConstants {
	static final String TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM = "ibis.io.IbisSerializationInputStream";
	
	static final ObjectType ibis_input_stream = new ObjectType(
			TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM);
	
	static final Type[] ibis_input_stream_arrtp = new Type[] { ibis_input_stream };

	static final String TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM = "ibis.io.IbisSerializationOutputStream";
	
	static final ObjectType ibis_output_stream = new ObjectType(
			TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM);
	
	static final Type[] ibis_output_stream_arrtp = new Type[] { ibis_output_stream };
	
	static final String TYPE_SUN_INPUT_STREAM = "java.io.ObjectInputStream";
	
	static final String TYPE_SUN_OUTPUT_STREAM = "java.io.ObjectOutputStream";
	
	static final ObjectType sun_output_stream = new ObjectType(
			TYPE_SUN_OUTPUT_STREAM);
	
	static final ObjectType sun_input_stream = new ObjectType(
			TYPE_SUN_INPUT_STREAM);
	
	static final Type java_lang_class_type = Type.getType("Ljava/lang/Class;");
	
	static final char CASE_BOOLEAN = 'B';

	static final char CASE_CHAR = 'C';

	static final char CASE_DOUBLE = 'D';

	static final char CASE_FLOAT = 'F';

	static final char CASE_INT = 'I';

	static final char CASE_LONG = 'J';

	static final char CASE_OBJECT = 'Z';

	static final char CASE_SHORT = 'S';

	static final String FIELD_SERIAL_PERSISTENT_FIELDS = "serialPersistentFields";

	static final String FIELD_SERIAL_VERSION_UID = "serialVersionUID";

	static final String METHOD_$READ_OBJECT_WRAPPER$ = "$readObjectWrapper$";

	static final String METHOD_ADD_OBJECT_TO_CYCLE_CHECK = "addObjectToCycleCheck";
	
	static final String METHOD_APPEND = "append";

	static final String METHOD_CLINIT = "<clinit>";

	static final String METHOD_CREATE_UNINITIALIZED_OBJECT = "create_uninitialized_object";

	static final String METHOD_DEFAULT_READ_SERIALIZABLE_OBJECT = "defaultReadSerializableObject";

	static final String METHOD_DEFAULT_WRITE_SERIALIZABLE_OBJECT = "defaultWriteSerializableObject";

	static final String METHOD_FOR_NAME = "forName";

	static final String METHOD_GENERATED_DEFAULT_READ_OBJECT = "generated_DefaultReadObject";

	static final String METHOD_GENERATED_DEFAULT_WRITE_OBJECT = "generated_DefaultWriteObject";

	static final String METHOD_GENERATED_NEW_INSTANCE = "generated_newInstance";

	static final String METHOD_GENERATED_WRITE_OBJECT = "generated_WriteObject";

	static final String METHOD_GET = "get";

	static final String METHOD_GET_BOOLEAN = "getBoolean";

	static final String METHOD_GET_CHAR = "getChar";

	static final String METHOD_GET_DOUBLE = "getDouble";

	static final String METHOD_GET_FIELD = "getField";

	static final String METHOD_GET_FLOAT = "getFloat";

	static final String METHOD_GET_INT = "getInt";

	static final String METHOD_GET_JAVA_OBJECT_INPUT_STREAM = "getJavaObjectInputStream";

	static final String METHOD_GET_JAVA_OBJECT_OUTPUT_STREAM = "getJavaObjectOutputStream";

	static final String METHOD_GET_LONG = "getLong";

	static final String METHOD_GET_MODIFIERS = "getModifiers";

	static final String METHOD_GET_NAME = "getName";

	static final String METHOD_GET_OBJECT_FROM_CYCLE_CHECK = "getObjectFromCycleCheck";

	static final String METHOD_GET_SHORT = "getShort";

	static final String METHOD_GET_TYPE = "getType";

	static final String METHOD_GET_TYPE_CODE = "getTypeCode";

	static final String METHOD_IBIS_IO_GENERATOR = "_ibis_io_Generator";

	static final String METHOD_INIT = "<init>";

	static final String METHOD_POP_CURRENT_OBJECT = "pop_current_object";

	static final String METHOD_PUSH_CURRENT_OBJECT = "push_current_object";

	static final String METHOD_READ = "read";

	static final String METHOD_READ_ARRAY = "readArray";

	static final String METHOD_READ_BOOLEAN = "readBoolean";

	static final String METHOD_READ_BYTE = "readByte";

	static final String METHOD_READ_CHAR = "readChar";

	static final String METHOD_READ_CLASS = "readClass";

	static final String METHOD_READ_DOUBLE = "readDouble";

	static final String METHOD_READ_EXTERNAL = "readExternal";

	static final String METHOD_READ_FIELD = "readField";

	static final String METHOD_READ_FIELD_BOOLEAN = "readFieldBoolean";

	static final String METHOD_READ_FIELD_BYTE = "readFieldByte";

	static final String METHOD_READ_FIELD_CHAR = "readFieldChar";

	static final String METHOD_READ_FIELD_CLASS = "readFieldClass";

	static final String METHOD_READ_FIELD_DOUBLE = "readFieldDouble";

	static final String METHOD_READ_FIELD_FLOAT = "readFieldFloat";

	static final String METHOD_READ_FIELD_INT = "readFieldInt";

	static final String METHOD_READ_FIELD_LONG = "readFieldLong";

	static final String METHOD_READ_FIELD_OBJECT = "readFieldObject";

	static final String METHOD_READ_FIELD_SHORT = "readFieldShort";

	static final String METHOD_READ_FIELD_STRING = "readFieldString";

	static final String METHOD_READ_FLOAT = "readFloat";

	static final String METHOD_READ_INT = "readInt";

	static final String METHOD_READ_KNOWN_TYPE_HEADER = "readKnownTypeHeader";

	static final String METHOD_READ_LONG = "readLong";

	static final String METHOD_READ_OBJECT = "readObject";

	static final String METHOD_READ_SERIALIZABLE_OBJECT = "readSerializableObject";

	static final String METHOD_READ_SHORT = "readShort";

	static final String METHOD_READ_STRING = "readString";

	static final String METHOD_SET = "set";
	
	static final String METHOD_TO_STRING = "toString";

	static final String METHOD_WRITE_ARRAY = "writeArray";

	static final String METHOD_WRITE_BOOLEAN = "writeBoolean";

	static final String METHOD_WRITE_BYTE = "writeByte";

	static final String METHOD_WRITE_CHAR = "writeChar";

	static final String METHOD_WRITE_CLASS = "writeClass";

	static final String METHOD_WRITE_DOUBLE = "writeDouble";

	static final String METHOD_WRITE_EXTERNAL = "writeExternal";

	static final String METHOD_WRITE_FLOAT = "writeFloat";

	static final String METHOD_WRITE_INT = "writeInt";

	static final String METHOD_WRITE_KNOWN_ARRAY_HEADER = "writeKnownArrayHeader";

	static final String METHOD_WRITE_KNOWN_OBJECT_HEADER = "writeKnownObjectHeader";

	static final String METHOD_WRITE_LONG = "writeLong";

	static final String METHOD_WRITE_OBJECT = "writeObject";

	static final String METHOD_WRITE_SERIALIZABLE_OBJECT = "writeSerializableObject";

	static final String METHOD_WRITE_SHORT = "writeShort";

	static final String METHOD_WRITE_STRING = "writeString";
		
	static final String SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_I_V = "(Libis/io/IbisSerializationInputStream;I)V";

	static final String SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V = "(Libis/io/IbisSerializationInputStream;)V";

	static final String SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_I_V = "(Libis/io/IbisSerializationOutputStream;I)V";

	static final String SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_V = "(Libis/io/IbisSerializationOutputStream;)V";

	static final String SIGNATURE_LJAVA_IO_OBJECT_INPUT_STREAM_V = "(Ljava/io/ObjectInputStream;)V";

	static final String SIGNATURE_LJAVA_IO_OBJECT_OUTPUT_STREAM_V = "(Ljava/io/ObjectOutputStream;)V";

	static final String TYPE_BOOLEAN = "Boolean";

	static final String TYPE_BYTE = "Byte";

	static final String TYPE_CHAR = "Char";

	static final String TYPE_DOUBLE = "Double";

	static final String TYPE_FLOAT = "Float";

	static final String TYPE_IBIS_IO_CONSTANTS = "ibis.io.Constants";

	static final String TYPE_IBIS_IO_GENERATOR = "ibis.io.Generator";

	static final String TYPE_IBIS_IO_REPLACER = "ibis.io.Replacer";

	static final String TYPE_IBIS_IO_SERIALIZABLE = "ibis.io.Serializable";

	static final String TYPE_INT = "Int";

	static final String TYPE_JAVA_IO_EXTERNALIZABLE = "java.io.Externalizable";

	static final String TYPE_JAVA_IO_IOEXCEPTION = "java.io.IOException";

	static final String TYPE_JAVA_IO_OBJECT_INPUT = "java.io.ObjectInput";
	
	static final String TYPE_JAVA_IO_OBJECT_OUTPUT = "java.io.ObjectOutput";

	static final String TYPE_JAVA_IO_OBJECT_STREAM_FIELD = "java.io.ObjectStreamField";

	static final String TYPE_JAVA_IO_SERIALIZABLE = "java.io.Serializable";

	static final String TYPE_JAVA_LANG_CLASS = "java.lang.Class";

	static final String TYPE_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION = "java.lang.ClassNotFoundException";

	static final String TYPE_JAVA_LANG_ENUM = "java.lang.Enum";

	static final String TYPE_JAVA_LANG_EXCEPTION = "java.lang.Exception";

	static final String TYPE_JAVA_LANG_OBJECT = "java.lang.Object";

	static final String TYPE_JAVA_LANG_REFLECT_FIELD = "java.lang.reflect.Field";

	static final String TYPE_JAVA_LANG_STRING = "java.lang.String";

	static final String TYPE_JAVA_LANG_STRING_BUFFER = "java.lang.StringBuffer";
	
	static final String TYPE_LJAVA_IO_OBJECT_STREAM_FIELD = "[Ljava/io/ObjectStreamField;";

	static final String TYPE_LONG = "Long";

	static final String TYPE_SHORT = "Short";

	static final String VARIABLE_INPUT_STREAM = "is";

	static final String VARIABLE_LEVEL = "lvl";

	static final String VARIABLE_OUTPUT_STREAM = "os";

	static final String VARIABLE_REPLACER = "replacer";

}
