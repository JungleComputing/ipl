package ibis.io.rewriter;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

interface JMERewriterConstants {
	
	static final String TYPE_IBIS_IO_JME_SERIALIZABLE = "ibis.io.jme.Serializable";

	static final String TYPE_IBIS_IO_JME_EXTERNALIZABLE = "ibis.io.jme.Externalizable";
	
	static final Object TYPE_LIBIS_IO_JME_OBJECT_STREAM_FIELD = "[Libis/io/jme/ObjectStreamField;";

	static final String TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM = "ibis.io.jme.ObjectInputStream";
	
	static final ObjectType jme_input_stream = new ObjectType(
			TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM);
	
	static final Type[] jme_input_stream_arrtp = new Type[] { jme_input_stream };

	static final String TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM = "ibis.io.jme.ObjectOutputStream";
	
	static final ObjectType jme_output_stream = new ObjectType(
			TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM);
	
	static final Type[] jme_output_stream_arrtp = new Type[] { jme_output_stream };
	
	static final String METHOD_GENERATED_JME_WRITE_OBJECT = "generated_JME_WriteObject";
	
	static final String METHOD_GENERATED_JME_DEFAULT_READ_OBJECT = "generated_JME_DefaultReadObject";
	
	static final String METHOD_GENERATED_JME_DEFAULT_WRITE_OBJECT = "generated_JME_DefaultWriteObject";

	static final String SIGNATURE_LIBIS_IO_JME_OBJECT_INPUT_STREAM_V = "(Libis/io/jme/ObjectInputStream;)V";

	static final String SIGNATURE_LIBIS_IO_JME_OBJECT_INPUT_STREAM_I_V = "(Libis/io/jme/ObjectInputStream;I)V";
	
	static final String SIGNATURE_LIBIS_IO_JME_OBJECT_OUTPUT_STREAM_V = "(Libis/io/jme/ObjectOutputStream;)V";
	
	static final String SIGNATURE_LIBIS_IO_JME_OBJECT_OUTPUT_STREAM_I_V = "(Libis/io/jme/ObjectOutputStream;I)V";
	
	static final String TYPE_IBIS_IO_JME_CONSTANTS = "ibis.io.jme.Constants";
	
	static final String TYPE_IBIS_IO_JME_JMESERIALIZABLE = "ibis.io.jme.JMESerializable";
	
	static final String TYPE_IBIS_IO_JME_OBJECT_STREAM_FIELD = "ibis.io.jme.ObjectStreamField";

	static final String TYPE_IBIS_IO_JME_GENERATOR = "ibis.io.jme.Generator";
	
	static final String TYPE_IBIS_IO_JME_NOT_SERIALIZABLE_EXCEPTION = "ibis.io.jme.NotSerializableException";
}
