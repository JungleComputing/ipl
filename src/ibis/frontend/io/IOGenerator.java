package ibis.frontend.io;

import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.verifier.*;

/* TODO: docs.
*/

public class IOGenerator {
    private static final String ibis_input_stream_name = "ibis.io.IbisSerializationInputStream";
    private static final String ibis_output_stream_name= "ibis.io.IbisSerializationOutputStream";
    private static final String sun_input_stream_name  = "java.io.ObjectInputStream";
    private static final String sun_output_stream_name = "java.io.ObjectOutputStream";

    private static final ObjectType ibis_input_stream  = new ObjectType(ibis_input_stream_name);
    private static final ObjectType ibis_output_stream = new ObjectType(ibis_output_stream_name);
    private static final ObjectType sun_input_stream   = new ObjectType(sun_input_stream_name);
    private static final ObjectType sun_output_stream  = new ObjectType(sun_output_stream_name);

    private static final Type[] ibis_input_stream_arrtp = new Type[] { ibis_input_stream };
    private static final Type[] ibis_output_stream_arrtp = new Type[] { ibis_output_stream };

    private static final Type	java_lang_class_type = Type.getType("Ljava/lang/Class;");

    private class SerializationInfo {

	String	write_name;
	String	read_name;
	String	final_read_name;
	Type 	tp;
	Type[]	param_tp_arr;
	boolean primitive;

	SerializationInfo(String wn, String rn, String frn, Type t, Type param_tp, boolean primitive) {
	    this.write_name = wn;
	    this.read_name  = rn;
	    this.final_read_name = frn;
	    this.tp  = t;
	    this.param_tp_arr = new Type[] { param_tp };
	    this.primitive = primitive;
	}
    }


    private class CodeGenerator {
	JavaClass	clazz;
	ClassGen	gen;
	String		classname;
	String		super_classname;
	JavaClass	super_class;
	boolean		super_is_serializable;
	boolean		super_is_ibis_serializable;
	boolean		super_has_ibis_constructor;
	boolean		super_is_externalizable;
	boolean		has_serial_persistent_fields;
	boolean		final_fields;
	Field[]		fields;
	Method[]	methods;
	InstructionFactory factory;
	ConstantPoolGen	constantpool;


	CodeGenerator(JavaClass cl) {
	    clazz		= cl;
	    gen 		= new ClassGen(clazz);
	    classname		= clazz.getClassName();
	    super_classname	= clazz.getSuperclassName();
	    super_class		= Repository.lookupClass(super_classname);
	    fields		= gen.getFields();
	    methods		= gen.getMethods();
	    factory 		= new InstructionFactory(gen);
	    constantpool	= gen.getConstantPool();

	    versionUID();

	    super_is_serializable = isSerializable(super_class);
	    super_is_externalizable = isExternalizable(super_class);
	    super_is_ibis_serializable = isIbisSerializable(super_class);
	    super_has_ibis_constructor = hasIbisConstructor(super_class);
	    has_serial_persistent_fields = hasSerialPersistentFields();
	    final_fields = hasFinalFields();
	}

	/**
	 * Get the serialversionuid of a class that is about to be
	 * rewritten. If necessary, a serialVersionUID field is added.
	 */
	private void versionUID() {
	    for (int i = 0; i < fields.length; i++) {
		Field f = fields[i];
		if (f.getName().equals("serialVersionUID") &&
		    f.isFinal() &&
		    f.isStatic()) {
		    /* Already present. Just return. */
		    return;
		}
	    }

	    try {
		Class	cl = Class.forName(classname, false, null);
		java.io.ObjectStreamClass ocl = java.io.ObjectStreamClass.lookup(cl);
		if (ocl == null) {
		    System.err.println("IOGenerator attempts to rewrite non-Serializable class " + classname + " -- ignore");
		    return;
		}
		long	uid = ocl.getSerialVersionUID();
		FieldGen f = new FieldGen(Constants.ACC_PRIVATE|Constants.ACC_FINAL|Constants.ACC_STATIC, 
					  Type.LONG,
					  "serialVersionUID",
					  constantpool);
		f.setInitValue(uid);
		gen.addField(f.getField());
		fields = gen.getFields();
	    } catch(ClassNotFoundException e) {
	    }
	}


	private boolean hasSerialPersistentFields() {
	    for (int i = 0; i < fields.length; i++) {
		Field f = fields[i];
		if (f.getName().equals("serialPersistentFields") &&
		    f.isFinal() &&
		    f.isStatic() &&
		    f.isPrivate() &&
		    f.getSignature().equals("[Ljava/io/ObjectStreamField;")) {
		    return true;
		}
	    }
	    return false;
	}


	private boolean hasFinalFields() {
	    for (int i = 0; i < fields.length; i++) {
		if (fields[i].isFinal()) {
		    return true;
		}
	    }
	    return false;
	}


	private int findMethod(String name, String signature) {
	    for (int i = 0; i < methods.length; i++) {
		if (methods[i].getName().equals(name) &&
		    methods[i].getSignature().equals(signature)) return i;
	    }
	    return -1;
	}


	private boolean hasWriteObject() {
	    return findMethod("writeObject", "(Ljava/io/ObjectOutputStream;)V") != -1;
	}


	private boolean hasReadObject() {
	    return findMethod("readObject", "(Ljava/io/ObjectInputStream;)V") != -1;
	}


	private boolean hasIbisConstructor(JavaClass cl) {
	    Method[] methods = cl.getMethods();

	    for (int i = 0; i < methods.length; i++) {
		if (methods[i].getName().equals("<init>") &&
		    methods[i].getSignature().equals("(Libis/io/IbisSerializationInputStream;)V")) return true;
	    }
	    return false;
	}


	private Instruction createGeneratedWriteObjectInvocation(String name, short invmode) {
	    return factory.createInvoke(name,
					"generated_WriteObject",
					Type.VOID,
					ibis_output_stream_arrtp,
					invmode);
	}


	private Instruction createGeneratedDefaultReadObjectInvocation(String name, InstructionFactory factory, short invmode) {
	    return factory.createInvoke(name,
					"generated_DefaultReadObject",
					Type.VOID,
					new Type[] {ibis_input_stream, Type.INT},
					invmode);
	}


	private Instruction createInitInvocation(String name, InstructionFactory f) {
	    return f.createInvoke(name,
				  "<init>",
				  Type.VOID,
				  ibis_input_stream_arrtp,
				  Constants.INVOKESPECIAL);
	}


	private Instruction createGeneratedDefaultWriteObjectInvocation(String name) {
	    return factory.createInvoke(name,
					"generated_DefaultWriteObject",
					Type.VOID,
					new Type[] {ibis_output_stream, Type.INT},
					Constants.INVOKESPECIAL);
	}


	private Instruction createWriteObjectInvocation() {
	    return factory.createInvoke(classname,
					"writeObject",
					Type.VOID,
					new Type[] {sun_output_stream},
					Constants.INVOKESPECIAL);
	}


	private int getClassDepth(JavaClass clazz) {
	    if (! isSerializable(clazz)) {
		return 0;
	    }
	    return 1 + getClassDepth(Repository.lookupClass(clazz.getSuperclassName()));
	}


	void generateMethods() {
	    /* Generate the necessary (empty) methods. */

	    if (verbose) {
		System.out.println("  Generating empty methods for class : " + classname);
		System.out.println("    " + classname + " implements java.io.Serializable -> adding ibis.io.Serializable");
	    }

	    /* add the ibis.io.Serializable interface to the class */
	    gen.addInterface("ibis.io.Serializable");

	    /* Construct a write method */
	    InstructionList il = new InstructionList();
	    il.append(new RETURN());

	    int flags = Constants.ACC_PUBLIC | (gen.isFinal() ? Constants.ACC_FINAL : 0);

	    MethodGen write_method = new MethodGen( flags,
						    Type.VOID,
						    ibis_output_stream_arrtp,
						    new String[] { "os" },
						    "generated_WriteObject",
						    classname,
						    il,
						    constantpool);

	    write_method.addException("java.io.IOException");
	    gen.addMethod(write_method.getMethod());

	    /* ... and a default_write_method */
	    il = new InstructionList();
	    il.append(new RETURN());

	    MethodGen default_write_method =
				     new MethodGen( flags,
						    Type.VOID,
						    new Type[] {ibis_output_stream, Type.INT},
						    new String[] { "os", "lvl"},
						    "generated_DefaultWriteObject",
						    classname,
						    il,
						    constantpool);

	    default_write_method.addException("java.io.IOException");
	    gen.addMethod(default_write_method.getMethod());

	    /* ... and a default_read_method */
	    il = new InstructionList();
	    il.append(new RETURN());

	    MethodGen default_read_method =
				     new MethodGen( flags,
						    Type.VOID,
						    new Type[] {ibis_input_stream, Type.INT},
						    new String[] { "os", "lvl" },
						    "generated_DefaultReadObject",
						    classname,
						    il,
						    constantpool);

	    default_read_method.addException("java.io.IOException");
	    gen.addMethod(default_read_method.getMethod());

	    /* Construct a read-of-the-stream constructor, but only when we can actually use it. */
	    if (super_is_externalizable ||
		! super_is_serializable ||
		force_generated_calls ||
		super_has_ibis_constructor) {
		il = new InstructionList();
		il.append(new RETURN());

		MethodGen read_cons = new MethodGen(Constants.ACC_PROTECTED,
						    Type.VOID,
						    ibis_input_stream_arrtp,
						    new String[] { "is" },
						    "<init>",
						    classname,
						    il,
						    constantpool);
		read_cons.addException("java.io.IOException");
		gen.addMethod(read_cons.getMethod());
	    }
	    else if (hasReadObject()) {
		il = new InstructionList();
		il.append(new RETURN());
		MethodGen readobjectWrapper = new MethodGen(Constants.ACC_PROTECTED,
							    Type.VOID,
							    ibis_input_stream_arrtp,
							    new String[] { "is" },
							    "$readObjectWrapper$",
							    classname,
							    il,
							    constantpool);
		readobjectWrapper.addException("java.io.IOException");
		gen.addMethod(readobjectWrapper.getMethod());
	    }

	    /* Now, create a new class structure, which has these methods. */
	    JavaClass newclazz = gen.getJavaClass();

	    if (target_classes.remove(clazz)) {
		Repository.removeClass(classname);
		Repository.addClass(newclazz);
		target_classes.add(newclazz);
	    }
	    if (classes_to_save.remove(clazz)) {
		classes_to_save.add(newclazz);
	    }
	    clazz = newclazz;
	}

	private InstructionList writeInstructions(Field field) {
	    String field_sig = field.getSignature();
	    Type field_type = Type.getType(field_sig);
	    SerializationInfo info = getSerializationInfo(field_type);

	    Type t = info.tp;
	    InstructionList temp = new InstructionList();

	    if (! info.primitive) {
		t = Type.getType(field_sig);
	    }

	    temp.append(new ALOAD(1));
	    temp.append(new ALOAD(0));
	    temp.append(factory.createFieldAccess(classname,
						  field.getName(),
						  t,
						  Constants.GETFIELD));
	    temp.append(factory.createInvoke(info.primitive ?
						ibis_output_stream_name :
						sun_output_stream_name,
					     info.write_name,
					     Type.VOID,
					     info.param_tp_arr,
					     Constants.INVOKEVIRTUAL));

	    return temp;
	}

	private InstructionList readInstructions(Field field, boolean from_constructor) {
	    String field_sig = field.getSignature();
	    Type field_type = Type.getType(field_sig);
	    SerializationInfo info = getSerializationInfo(field_type);

	    Type t = info.tp;
	    InstructionList temp = new InstructionList();

	    if (! info.primitive) {
		t = Type.getType(field_sig);
	    }

	    if (from_constructor || ! field.isFinal()) {
		temp.append(new ALOAD(0));
		temp.append(new ALOAD(1));
		temp.append(factory.createInvoke(info.primitive ?
						    ibis_input_stream_name:
						    sun_input_stream_name,
						 info.read_name,
						 info.tp,
						 Type.NO_ARGS,
						 Constants.INVOKEVIRTUAL));

		if (! info.primitive) {
		    temp.append(factory.createCheckCast((ReferenceType) t));
		}

		temp.append(factory.createFieldAccess(classname,
						      field.getName(),
						      t,
						      Constants.PUTFIELD));
	    }
	    else {
		temp.append(new ALOAD(1));
		temp.append(new ALOAD(0));
		int ind = constantpool.addString(field.getName());
		temp.append(new LDC(ind));
		if (! info.primitive) {
		    int ind2 = constantpool.addString(field_sig);
		    temp.append(new LDC(ind2));
		}
		temp.append(factory.createInvoke(ibis_input_stream_name,
						 info.final_read_name,
						 Type.VOID,
						 info.primitive ?
						    new Type[] { Type.OBJECT, Type.STRING} :
						    new Type[] { Type.OBJECT, Type.STRING, Type.STRING},
						 Constants.INVOKEVIRTUAL));
	    }

	    return temp;
	}

	private InstructionList writeReferenceField(Field field) {
	    Type field_type = Type.getType(field.getSignature());
	    InstructionList write_il = new InstructionList();

	    boolean isfinal = false;
	    JavaClass field_class = null;

	    if (verbose) System.out.println("    writing reference field " + field.getName() + " of type " + field_type.getSignature());

	    if (field_type instanceof ObjectType) {
		field_class = Repository.lookupClass(((ObjectType)field_type).getClassName());
		if (field_class != null && field_class.isFinal()) isfinal = true;
	    }
	    if (isfinal &&
		(! (Repository.implementationOf(field_class, "java.rmi.Remote") ||
		    Repository.implementationOf(field_class, "ibis.rmi.Remote"))) &&
		!field_type.getSignature().startsWith("Ljava/")) {

		write_il.append(new ALOAD(1));
		write_il.append(new ALOAD(0));
		write_il.append(factory.createFieldAccess(classname,
						    field.getName(),
						    field_type,
						    Constants.GETFIELD));
		write_il.append(factory.createInvoke(ibis_output_stream_name,
					       "writeKnownObjectHeader",
					       Type.INT,
					       new Type[] { Type.OBJECT },
					       Constants.INVOKEVIRTUAL));
		write_il.append(new ISTORE(2));
		write_il.append(new ILOAD(2));
		write_il.append(new ICONST(1));

		IF_ICMPNE ifcmp  = new IF_ICMPNE(null);

		write_il.append(ifcmp);

		write_il.append(new ALOAD(0));
		write_il.append(factory.createFieldAccess(classname,
						    field.getName(),
						    field_type,
						    Constants.GETFIELD));
		write_il.append(new ALOAD(1));

		write_il.append(createGeneratedWriteObjectInvocation(field_class.getClassName(), Constants.INVOKEVIRTUAL));

		InstructionHandle target = write_il.append(new NOP());
		ifcmp.setTarget(target);

	    } else {
		write_il.append(writeInstructions(field));
	    }
	    return write_il;
	}

	private InstructionList serialPersistentWrites(MethodGen write_gen) {
	    Instruction persistent_field_access = factory.createFieldAccess(classname, "serialPersistentFields", new ArrayType(new ObjectType("java.io.ObjectStreamField"), 1), Constants.GETSTATIC);
	    InstructionList write_il = new InstructionList();
	    int [] case_values = new int[] { 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z'};
	    InstructionHandle [] case_handles = new InstructionHandle[case_values.length];
	    GOTO[] gotos = new GOTO[case_values.length+1];

	    for (int i = 0; i < gotos.length; i++) {
		gotos[i] = new GOTO(null);
	    }


	    write_il.append(new SIPUSH((short) 0));
	    write_il.append(new ISTORE(2));

	    GOTO gto = new GOTO(null);
	    write_il.append(gto);

	    InstructionHandle loop_body_start = write_il.append(persistent_field_access);
	    write_il.append(new ILOAD(2));
	    write_il.append(new AALOAD());
	    write_il.append(factory.createInvoke("java.io.ObjectStreamField", "getName", Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
	    write_il.append(new ASTORE(3));

	    write_il.append(persistent_field_access);
	    write_il.append(new ILOAD(2));
	    write_il.append(new AALOAD());
	    write_il.append(factory.createInvoke("java.lang.Object", "getClass", new ObjectType("java.lang.Class"), Type.NO_ARGS, Constants.INVOKEVIRTUAL));
	    write_il.append(new ASTORE(4));

	    InstructionHandle begin_try = write_il.append(new PUSH(constantpool, classname));
	    write_il.append(factory.createInvoke("java.lang.Class", "forName", java_lang_class_type, new Type[] { Type.STRING}, Constants.INVOKESTATIC));
	    write_il.append(factory.createInvoke("java.lang.Class", "getField", new ObjectType("java.lang.reflect.Field"), new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
	    write_il.append(new ASTORE(5));
	    
	    write_il.append(persistent_field_access);
	    write_il.append(new ILOAD(2));
	    write_il.append(new AALOAD());
	    write_il.append(factory.createInvoke("java.io.ObjectStreamField", "getTypeCode", Type.CHAR, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

	    case_handles[0] = write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(5));
	    write_il.append(new ALOAD(0));
	    write_il.append(factory.createInvoke("java.lang.reflect.Field", "getBoolean", Type.BOOLEAN, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream", "writeBoolean", Type.VOID, new Type[] { Type.BOOLEAN }, Constants.INVOKEVIRTUAL));
	    write_il.append(gotos[0]);

	    case_handles[1] = write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(5));
	    write_il.append(new ALOAD(0));
	    write_il.append(factory.createInvoke("java.lang.reflect.Field", "getChar", Type.CHAR, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream", "writeChar", Type.VOID, new Type[] { Type.INT }, Constants.INVOKEVIRTUAL));
	    write_il.append(gotos[1]);

	    case_handles[2] = write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(5));
	    write_il.append(new ALOAD(0));
	    write_il.append(factory.createInvoke("java.lang.reflect.Field", "getDouble", Type.DOUBLE, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream", "writeDouble", Type.VOID, new Type[] { Type.DOUBLE }, Constants.INVOKEVIRTUAL));
	    write_il.append(gotos[2]);

	    case_handles[3] = write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(5));
	    write_il.append(new ALOAD(0));
	    write_il.append(factory.createInvoke("java.lang.reflect.Field", "getFloat", Type.FLOAT, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream", "writeFloat", Type.VOID, new Type[] { Type.FLOAT }, Constants.INVOKEVIRTUAL));
	    write_il.append(gotos[3]);

	    case_handles[4] = write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(5));
	    write_il.append(new ALOAD(0));
	    write_il.append(factory.createInvoke("java.lang.reflect.Field", "getInt", Type.INT, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream", "writeInt", Type.VOID, new Type[] { Type.INT }, Constants.INVOKEVIRTUAL));
	    write_il.append(gotos[4]);

	    case_handles[5] = write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(5));
	    write_il.append(new ALOAD(0));
	    write_il.append(factory.createInvoke("java.lang.reflect.Field", "getLong", Type.LONG, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream", "writeLong", Type.VOID, new Type[] { Type.LONG }, Constants.INVOKEVIRTUAL));
	    write_il.append(gotos[5]);

	    case_handles[6] = write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(5));
	    write_il.append(new ALOAD(0));
	    write_il.append(factory.createInvoke("java.lang.reflect.Field", "getShort", Type.SHORT, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream", "writeShort", Type.VOID, new Type[] { Type.INT }, Constants.INVOKEVIRTUAL));
	    write_il.append(gotos[6]);

	    case_handles[7] = write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(5));
	    write_il.append(new ALOAD(0));
	    write_il.append(factory.createInvoke("java.lang.reflect.Field", "getBoolean", Type.BOOLEAN, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream", "writeBoolean", Type.VOID, new Type[] { Type.BOOLEAN }, Constants.INVOKEVIRTUAL));
	    write_il.append(gotos[7]);

	    InstructionHandle default_handle = write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(5));
	    write_il.append(new ALOAD(0));
	    write_il.append(factory.createInvoke("java.lang.reflect.Field", "get", Type.OBJECT, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream", "doWriteObject", Type.VOID, new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
	    InstructionHandle end_try = write_il.append(gotos[8]);

	    write_il.insert(case_handles[0], new SWITCH(case_values, case_handles, default_handle));

	    InstructionHandle handler = write_il.append(new ASTORE(6));
	    write_il.append(factory.createNew("java.io.IOException"));
	    write_il.append(new DUP());
	    write_il.append(factory.createNew("java.lang.StringBuffer"));
	    write_il.append(new DUP());
	    write_il.append(factory.createInvoke("java.lang.StringBuffer", "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
	    write_il.append(new PUSH(constantpool, "Could not write field "));
	    write_il.append(factory.createInvoke("java.lang.StringBuffer", "append", Type.STRINGBUFFER, new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
	    write_il.append(new ALOAD(3));
	    write_il.append(factory.createInvoke("java.lang.StringBuffer", "append", Type.STRINGBUFFER, new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("java.lang.StringBuffer", "toString", Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
	    write_il.append(factory.createInvoke("java.io.IOException", "<init>", Type.VOID, new Type[] { Type.STRING }, Constants.INVOKESPECIAL));
	    write_il.append(new ATHROW());

	    InstructionHandle gotos_target = write_il.append(new IINC(2, 1));

	    for (int i = 0; i < gotos.length; i++) {
		gotos[i].setTarget(gotos_target);
	    }
	    InstructionHandle loop_test = write_il.append(new ILOAD(2));
	    write_il.append(persistent_field_access);
	    gto.setTarget(loop_test);
	    write_il.append(new ARRAYLENGTH());
	    write_il.append(new IF_ICMPLT(loop_body_start));

	    write_gen.addExceptionHandler(begin_try, end_try, handler, new ObjectType("java.lang.Exception"));

	    return write_il;
	}

	private InstructionList generateDefaultWrites(int dpth, MethodGen write_gen) {
	    InstructionList write_il = new InstructionList();

	    if (has_serial_persistent_fields) {
		return serialPersistentWrites(write_gen);
	    }

	    for (int i=0;i<fields.length;i++) {
		Field field = fields[i];

		/* Don't send fields that are STATIC or TRANSIENT */
		if (! (field.isStatic() ||
		       field.isTransient())) {
		    Type field_type = Type.getType(field.getSignature());

		    if ((field_type instanceof ReferenceType) &&
			! field_type.equals(Type.STRING) &&
			! field_type.equals(java_lang_class_type)) {
			write_il.append(writeReferenceField(field));
		    }
		}
	    }

	    /* then handle java.lang.String and java.lang.Class */

	    for (int i=0;i<fields.length;i++) {
		Field field = fields[i];

		/* Don't send fields that are STATIC or TRANSIENT  */
		if (! (field.isStatic() ||
		       field.isTransient())) {
		    String sig = field.getSignature();
		    Type field_type = Type.getType(sig);

		    if (field_type.equals(Type.STRING) || field_type.equals(java_lang_class_type)) {
			if (verbose) System.out.println("    writing field " + field.getName() + " of type " + sig);

			write_il.append(writeInstructions(field));
		    }
		}
	    }

	    /* then handle the primitive fields */

	    for (int i=0;i<fields.length;i++) {
		Field field = fields[i];

		/* Don't send fields that are STATIC, or TRANSIENT */
		if (! (field.isStatic() ||
		       field.isTransient())) {
		    String sig = field.getSignature();
		    Type field_type = Type.getType(sig);

		    if (field_type instanceof BasicType) {
			if (verbose) System.out.println("    writing basic field " + field.getName() + " of type " + field_type.getSignature());

			write_il.append(writeInstructions(field));
		    }
		}
	    }
	    return write_il;
	}

	private InstructionList readReferenceField(Field field, boolean from_constructor) {
	    Type field_type = Type.getType(field.getSignature());
	    InstructionList read_il = new InstructionList();
	    boolean isfinal = false;
	    JavaClass field_class = null;

	    if (verbose) System.out.println("    reading reference field " + field.getName() + " of type " + field_type.getSignature());

	    if (field_type instanceof ObjectType) {
		field_class = Repository.lookupClass(((ObjectType)field_type).getClassName());
		if (field_class != null && field_class.isFinal()) isfinal = true;
	    }

	    if (isfinal &&
		( hasIbisConstructor(field_class) ||
		  (isSerializable(field_class) && force_generated_calls)) &&
		(! (Repository.implementationOf(field_class, "java.rmi.Remote") ||
		    Repository.implementationOf(field_class, "ibis.rmi.Remote"))) &&
		!field_type.getSignature().startsWith("Ljava/")) {

		read_il.append(new ALOAD(1));
		read_il.append(factory.createInvoke(ibis_input_stream_name,
					       "readKnownTypeHeader",
					       Type.INT,
					       Type.NO_ARGS,
					       Constants.INVOKEVIRTUAL));
		read_il.append(new ISTORE(2));
		read_il.append(new ILOAD(2));
		read_il.append(new ICONST(-1));

		IF_ICMPNE ifcmp  = new IF_ICMPNE(null);
		read_il.append(ifcmp);

		read_il.append(new ALOAD(0));

		read_il.append(factory.createNew((ObjectType)field_type));
		read_il.append(new DUP());
		read_il.append(new ALOAD(1));
		read_il.append(createInitInvocation(field_class.getClassName(), factory));
		read_il.append(factory.createFieldAccess(classname,
							 field.getName(),
							 field_type,
							 Constants.PUTFIELD));

		GOTO gto  = new GOTO(null);
		read_il.append(gto);

		InstructionHandle cmp_goto = read_il.append(new ILOAD(2));
		ifcmp.setTarget(cmp_goto);

		read_il.append(new ICONST(0));

		IF_ICMPEQ ifcmpeq = new IF_ICMPEQ(null);
		read_il.append(ifcmpeq);
		read_il.append(new ALOAD(0));
		read_il.append(new ALOAD(1));
		read_il.append(new ILOAD(2));
		read_il.append(factory.createInvoke(ibis_input_stream_name,
					       "getObjectFromCycleCheck",
					       Type.OBJECT,
					       new Type[] { Type.INT },
					       Constants.INVOKEVIRTUAL));

		read_il.append(factory.createCheckCast((ObjectType)field_type));
		read_il.append(factory.createFieldAccess(classname,
						    field.getName(),
						    field_type,
						    Constants.PUTFIELD));

		InstructionHandle target = read_il.append(new NOP());
		ifcmpeq.setTarget(target);
		gto.setTarget(target);
	    } else {
		read_il.append(readInstructions(field, from_constructor));
	    }

	    return read_il;
	}

	private InstructionHandle generateReadField(String tpname, Type tp, InstructionList read_il, GOTO gto, boolean from_constructor) {
	    InstructionHandle h;

	    if (from_constructor || ! final_fields) {
		h = read_il.append(new ALOAD(5));
		read_il.append(new ALOAD(0));
		read_il.append(new ALOAD(1));
		if (tpname.equals("")) {
		    read_il.append(factory.createInvoke(ibis_input_stream_name, "doReadObject", Type.OBJECT, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
		}
		else {
		    read_il.append(factory.createInvoke(ibis_input_stream_name, "read" + tpname, tp, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
		}
		read_il.append(factory.createInvoke("java.lang.reflect.Field", "set" + tpname, Type.VOID, new Type[] { Type.OBJECT, tp }, Constants.INVOKEVIRTUAL));
		read_il.append(gto);

		return h;
	    }

	    h = read_il.append(new ILOAD(6));
	    read_il.append(new PUSH(constantpool, Constants.ACC_FINAL));
	    read_il.append(new IAND());
	    IF_ICMPEQ eq = new IF_ICMPEQ(null);
	    read_il.append(eq);
	    read_il.append(new ALOAD(1));
	    read_il.append(new ALOAD(0));
	    read_il.append(new ALOAD(3));
	    if (tpname.equals("")) {
		read_il.append(new ALOAD(5));
		read_il.append(factory.createInvoke("java.lang.reflect.Field", "getType", new ObjectType("java.lang.Class"), Type.NO_ARGS, Constants.INVOKEVIRTUAL));
		read_il.append(factory.createInvoke("java.lang.Class", "getName", Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

		read_il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream", "readFieldObject", Type.VOID, new Type[] { Type.OBJECT, Type.STRING, Type.STRING }, Constants.INVOKEVIRTUAL));
	    }
	    else {
		read_il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream", "readField" + tpname, Type.VOID, new Type[] { Type.OBJECT, Type.STRING }, Constants.INVOKEVIRTUAL));
	    }
	    GOTO gto2 = new GOTO(null);
	    read_il.append(gto2);
	    eq.setTarget(read_il.append(new ALOAD(5)));
	    read_il.append(new ALOAD(0));
	    read_il.append(new ALOAD(1));
	    if (tpname.equals("")) {
		read_il.append(factory.createInvoke(ibis_input_stream_name, "doReadObject", tp, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
	    }
	    else {
		read_il.append(factory.createInvoke(ibis_input_stream_name, "read" + tpname, tp, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
	    }
	    read_il.append(factory.createInvoke("java.lang.reflect.Field", "set" + tpname, Type.VOID, new Type[] { Type.OBJECT, tp}, Constants.INVOKEVIRTUAL));
	    gto2.setTarget(read_il.append(gto));

	    return h;
	}

	private InstructionList serialPersistentReads(boolean from_constructor, MethodGen read_gen) {
	    Instruction persistent_field_access = factory.createFieldAccess(classname, "serialPersistentFields", new ArrayType(new ObjectType("java.io.ObjectStreamField"), 1), Constants.GETSTATIC);
	    InstructionList read_il = new InstructionList();
	    int [] case_values = new int[] { 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z'};
	    InstructionHandle [] case_handles = new InstructionHandle[case_values.length];
	    GOTO[] gotos = new GOTO[case_values.length+1];

	    for (int i = 0; i < gotos.length; i++) {
		gotos[i] = new GOTO(null);
	    }


	    read_il.append(new SIPUSH((short) 0));
	    read_il.append(new ISTORE(2));

	    GOTO gto = new GOTO(null);
	    read_il.append(gto);

	    InstructionHandle loop_body_start = read_il.append(persistent_field_access);
	    read_il.append(new ILOAD(2));
	    read_il.append(new AALOAD());
	    read_il.append(factory.createInvoke("java.io.ObjectStreamField", "getName", Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
	    read_il.append(new ASTORE(3));

	    read_il.append(persistent_field_access);
	    read_il.append(new ILOAD(2));
	    read_il.append(new AALOAD());
	    read_il.append(factory.createInvoke("java.lang.Object", "getClass", new ObjectType("java.lang.Class"), Type.NO_ARGS, Constants.INVOKEVIRTUAL));
	    read_il.append(new ASTORE(4));

	    InstructionHandle begin_try = read_il.append(new PUSH(constantpool, classname));
	    read_il.append(factory.createInvoke("java.lang.Class", "forName", java_lang_class_type, new Type[] { Type.STRING}, Constants.INVOKESTATIC));
	    read_il.append(factory.createInvoke("java.lang.Class", "getField", new ObjectType("java.lang.reflect.Field"), new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
	    read_il.append(new ASTORE(5));

	    if (! from_constructor && final_fields) {
		read_il.append(new ALOAD(5));
		read_il.append(factory.createInvoke("java.lang.reflect.Field", "getModifiers", Type.INT, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
		read_il.append(new ISTORE(6));
	    }
	    
	    read_il.append(persistent_field_access);
	    read_il.append(new ILOAD(2));
	    read_il.append(new AALOAD());
	    read_il.append(factory.createInvoke("java.io.ObjectStreamField", "getTypeCode", Type.CHAR, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

	    case_handles[0] = generateReadField("Byte", Type.BYTE, read_il, gotos[0], from_constructor);
	    case_handles[1] = generateReadField("Char", Type.CHAR, read_il, gotos[1], from_constructor);
	    case_handles[2] = generateReadField("Double", Type.DOUBLE, read_il, gotos[2], from_constructor);
	    case_handles[3] = generateReadField("Float", Type.FLOAT, read_il, gotos[3], from_constructor);
	    case_handles[4] = generateReadField("Int", Type.INT, read_il, gotos[4], from_constructor);
	    case_handles[5] = generateReadField("Long", Type.LONG, read_il, gotos[5], from_constructor);
	    case_handles[6] = generateReadField("Short", Type.SHORT, read_il, gotos[6], from_constructor);
	    case_handles[7] = generateReadField("Boolean", Type.BOOLEAN, read_il, gotos[7], from_constructor);

	    InstructionHandle default_handle = generateReadField("", Type.OBJECT, read_il, gotos[8], from_constructor);

	    InstructionHandle end_try = read_il.getEnd();

	    read_il.insert(case_handles[0], new SWITCH(case_values, case_handles, default_handle));

	    InstructionHandle handler = read_il.append(new ASTORE(6));
	    read_il.append(factory.createNew("java.io.IOException"));
	    read_il.append(new DUP());
	    read_il.append(factory.createNew("java.lang.StringBuffer"));
	    read_il.append(new DUP());
	    read_il.append(factory.createInvoke("java.lang.StringBuffer", "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
	    read_il.append(new PUSH(constantpool, "Could not read field "));
	    read_il.append(factory.createInvoke("java.lang.StringBuffer", "append", Type.STRINGBUFFER, new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
	    read_il.append(new ALOAD(3));
	    read_il.append(factory.createInvoke("java.lang.StringBuffer", "append", Type.STRINGBUFFER, new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
	    read_il.append(factory.createInvoke("java.lang.StringBuffer", "toString", Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
	    read_il.append(factory.createInvoke("java.io.IOException", "<init>", Type.VOID, new Type[] { Type.STRING }, Constants.INVOKESPECIAL));
	    read_il.append(new ATHROW());

	    InstructionHandle gotos_target = read_il.append(new IINC(2, 1));

	    for (int i = 0; i < gotos.length; i++) {
		gotos[i].setTarget(gotos_target);
	    }
	    InstructionHandle loop_test = read_il.append(new ILOAD(2));
	    read_il.append(persistent_field_access);
	    gto.setTarget(loop_test);
	    read_il.append(new ARRAYLENGTH());
	    read_il.append(new IF_ICMPLT(loop_body_start));

	    read_gen.addExceptionHandler(begin_try, end_try, handler, new ObjectType("java.lang.Exception"));

	    return read_il;
	}

	private InstructionList generateDefaultReads(boolean from_constructor, int dpth, MethodGen read_gen) {
	    InstructionList read_il = new InstructionList();

	    if (has_serial_persistent_fields) {
		return serialPersistentReads(from_constructor, read_gen);
	    }

	    for (int i=0;i<fields.length;i++) {
		Field field = fields[i];

		/* Don't send fields that are STATIC or TRANSIENT */
		if (! (field.isStatic() ||
		       field.isTransient())) {
		    Type field_type = Type.getType(field.getSignature());

		    if ((field_type instanceof ReferenceType) &&
			! field_type.equals(Type.STRING) &&
			! field_type.equals(java_lang_class_type)) {
			read_il.append(readReferenceField(field, from_constructor));
		    }
		}
	    }

	    /* then handle Strings */

	    for (int i=0;i<fields.length;i++) {
		Field field = fields[i];

		/* Don't send fields that are STATIC or TRANSIENT  */
		if (! (field.isStatic() ||
		       field.isTransient())) {
		    Type field_type = Type.getType(field.getSignature());

		    if (field_type.equals(Type.STRING) || field_type.equals(java_lang_class_type)) {
			if (verbose) System.out.println("    writing field " + field.getName() + " of type " + field_type.getSignature());

			read_il.append(readInstructions(field, from_constructor));
		    }
		}
	    }

	    /* then handle the primitive fields */

	    for (int i=0;i<fields.length;i++) {
		Field field = fields[i];

		/* Don't send fields that are STATIC, or TRANSIENT */
		if (! (field.isStatic() ||
		       field.isTransient())) {
		    Type field_type = Type.getType(field.getSignature());

		    if (field_type instanceof BasicType) {
			if (verbose) System.out.println("    writing basic field " + field.getName() + " of type " + field_type.getSignature());

			read_il.append(readInstructions(field, from_constructor));
		    }
		}
	    }
	    return read_il;
	}

	private boolean doVerify(JavaClass c) {
	    Verifier verf = VerifierFactory.getVerifier(c.getClassName());
	    boolean verification_failed = false;

	    if (verbose) {
		System.out.println("Verifying " + c.getClassName());
	    }

	    VerificationResult res = verf.doPass1();
	    if (res.getStatus() == VerificationResult.VERIFIED_REJECTED) {
		System.out.println("Verification pass 1 failed.");
		System.out.println(res.getMessage());
		verification_failed = true;
	    }
	    else {
		res = verf.doPass2();
		if (res.getStatus() == VerificationResult.VERIFIED_REJECTED) {
		    System.out.println("Verification pass 2 failed.");
		    System.out.println(res.getMessage());
		    verification_failed = true;
		}
		else {
		    Method[] methods = c.getMethods();
		    for (int i = 0; i < methods.length; i++) {
			if (verbose) {
			    System.out.println("verifying method " + methods[i].getName());
			}
			res = verf.doPass3a(i);
			if (res.getStatus() == VerificationResult.VERIFIED_REJECTED) {
			    System.out.println("Verification pass 3a failed for method " + methods[i].getName());
			    System.out.println(res.getMessage());
			    verification_failed = true;
			}
			else {
			    res = verf.doPass3b(i);
			    if (res.getStatus() == VerificationResult.VERIFIED_REJECTED) {
				System.out.println("Verification pass 3b failed for method " + methods[i].getName());
				System.out.println(res.getMessage());
				verification_failed = true;
			    }
			}
		    }
		}
	    }
	    return ! verification_failed;
	}

	private JavaClass generateInstanceGenerator() {

	    /* Here we create a 'generator' object. We need this extra object for three reasons:

	       1) Because the object is created from the 'ibis.io' package (the Serialization code),
		  we may not be allowed to create a new instance of the object (due to inter-package
		  access restrictions, e.g. the object may not be public). Because the generator is
			  in the same package as the target object, it can create a new object for us.

		   ?? How about totally private objects ?? can sun serialization handle this ??

	       2) Using this generator object, we can do a normal 'new' of the target type. This is
			  important, because using 'newInstance' is 6 times more expensive than 'new'.

	       3) We do not want to invoke a default constructor, but a special constructor that
		  immediately reads the object state from the stream. This cannot be done
		  (efficiently) with newInstance
	    */

	    if (verbose) System.out.println("  Generating InstanceGenerator class for " + classname);

	    String name = classname + "_ibis_io_Generator";

	    ObjectType class_type = new ObjectType(classname);

	    String classfilename = name.substring(name.lastIndexOf('.')+1) + ".class";
	    ClassGen gen = new ClassGen(name, "ibis.io.Generator", classfilename, Constants.ACC_FINAL|Constants.ACC_PUBLIC|Constants.ACC_SUPER, null);
	    InstructionFactory factory = new InstructionFactory(gen);

	    InstructionList il = new InstructionList();

	    if (! super_is_externalizable &&
		super_is_serializable &&
		! super_has_ibis_constructor &&
		! force_generated_calls) {
		/* This is a difficult case. We cannot call a constructor, because
		   this constructor would be obliged to call a constructor for the super-class.
		   So, we do it differently: generate calls to IbisSerializationInputStream methods
		   which call native methods ... I don't know another solution to this problem.
		*/
		/* First, create the object. Through a native call, because otherwise
		   the object would be marked uninitialized, and the code would not pass
		   bytecode verification. This native call also takes care of calling the
		   constructor of the first non-serializable superclass.
		*/
		il.append(new ALOAD(1));
		int ind = gen.getConstantPool().addString(classname);
		il.append(new LDC(ind));
		il.append(factory.createInvoke(ibis_input_stream_name,
					       "create_uninitialized_object",
					       Type.OBJECT,
					       new Type[] { Type.STRING},
					       Constants.INVOKEVIRTUAL));
		il.append(factory.createCheckCast(class_type));
		il.append(new ASTORE(2));

		/* Now read the superclass. */
		il.append(new ALOAD(1));
		il.append(new ALOAD(2));
		ind = gen.getConstantPool().addString(super_classname);
		il.append(new LDC(ind));
		il.append(factory.createInvoke(ibis_input_stream_name,
					       "readSerializableObject",
					       Type.VOID,
					       new Type[] {Type.OBJECT, Type.STRING},
					       Constants.INVOKEVIRTUAL));

		/* Now, if the class has a readObject, call it. Otherwise, read its fields,
		   by calling generated_DefaultReadObject.
		*/
		if (hasReadObject()) {
		    il.append(new ALOAD(2));
		    il.append(new ALOAD(1));
		    il.append(factory.createInvoke(classname,
						   "$readObjectWrapper$",
						   Type.VOID,
						   ibis_input_stream_arrtp,
						   Constants.INVOKEVIRTUAL));
		}
		else {
		    int dpth = getClassDepth(clazz);

		    il.append(new ALOAD(2));
		    il.append(new ALOAD(1));
		    il.append(new SIPUSH((short)dpth));
		    il.append(createGeneratedDefaultReadObjectInvocation(classname, factory, Constants.INVOKEVIRTUAL));
		}
		il.append(new ALOAD(2));
	    }
	    else {
		il.append(factory.createNew(class_type));
		il.append(new DUP());
		il.append(new ALOAD(1));
		il.append(createInitInvocation(classname, factory));
	    }
	    il.append(new ARETURN());

	    /*
	      0       new DITree
	      3       dup
	      4       aload_1
	      5       invokespecial DITree(ibis.io.IbisSerializationInputStream)
	      8       areturn
	    */

	    MethodGen method = new MethodGen(Constants.ACC_FINAL | Constants.ACC_PUBLIC,
					     Type.OBJECT,
					     ibis_input_stream_arrtp,
					     new String[] { "is" },
					     "generated_newInstance",
					     name,
					     il,
					     gen.getConstantPool());

	    method.setMaxStack(3);
	    method.setMaxLocals();
	    method.addException("java.io.IOException");
	    gen.addMethod(method.getMethod());

	    il = new InstructionList();
	    il.append(new ALOAD(0));
	    il.append(factory.createInvoke("ibis.io.Generator",
					   "<init>",
					   Type.VOID,
					   Type.NO_ARGS,
					   Constants.INVOKESPECIAL));
	    il.append(new RETURN());

	    method = new MethodGen(Constants.ACC_PUBLIC,
				   Type.VOID,
				   Type.NO_ARGS,
				   null,
				   "<init>",
				   name,
				   il,
				   gen.getConstantPool());

	    method.setMaxStack(1);
	    method.setMaxLocals();
	    gen.addMethod(method.getMethod());

	    return gen.getJavaClass();
	}

	void generateCode() {
	    /* Generate code inside the methods */
	    int write_method_index = findMethod("generated_WriteObject",
						"(Libis/io/IbisSerializationOutputStream;)V");;
	    int default_write_method_index = findMethod("generated_DefaultWriteObject",
							"(Libis/io/IbisSerializationOutputStream;I)V");
	    int default_read_method_index = findMethod("generated_DefaultReadObject",
						       "(Libis/io/IbisSerializationInputStream;I)V");
	    int read_cons_index = findMethod("<init>",
					     "(Libis/io/IbisSerializationInputStream;)V");
	    int read_wrapper_index = findMethod("$readObjectWrapper$",
						"(Libis/io/IbisSerializationInputStream;)V");

	    if (verbose) {
		System.out.println("  Generating method code class for class : " + classname);
		System.out.println("    Number of fields " + fields.length);
	    }

	    int dpth = getClassDepth(clazz);

	    /* void generated_DefaultWriteObject(IbisSerializationOutputStream out, int level) {
		    if (level == dpth) {
			... write fields ... (the code resulting from the generateDefaultWrites() call).
		    }
		    else if (level < dpth) {
			super.generated_DefaultWriteObject(out, level);
		    }
	       }
	    */

	    MethodGen write_gen = new MethodGen(methods[default_write_method_index], classname, constantpool);

	    InstructionList write_il = new InstructionList();
	    InstructionHandle end = write_gen.getInstructionList().getStart();

	    write_il.append(new ILOAD(2));
	    write_il.append(new SIPUSH((short)dpth));
	    IF_ICMPNE ifcmpne = new IF_ICMPNE(null);
	    write_il.append(ifcmpne);
	    write_il.append(generateDefaultWrites(dpth, write_gen));
	    write_il.append(new GOTO(end));
	    if (super_is_ibis_serializable || super_is_serializable) {
		InstructionHandle i = write_il.append(new ILOAD(2));
		ifcmpne.setTarget(i);
		write_il.append(new SIPUSH((short)dpth));
		write_il.append(new IF_ICMPGT(end));
		if (super_is_ibis_serializable || force_generated_calls) {
		    write_il.append(new ALOAD(0));
		    write_il.append(new ALOAD(1));
		    write_il.append(new ILOAD(2));
		    write_il.append(createGeneratedDefaultWriteObjectInvocation(super_classname));
		}
		else {
		    /*  Superclass is not rewritten.
		    */
		    write_il.append(new ALOAD(1));
		    write_il.append(new ALOAD(0));
		    write_il.append(new ILOAD(2));
		    write_il.append(factory.createInvoke(ibis_output_stream_name,
							 "defaultWriteSerializableObject",
							 Type.VOID,
							 new Type[] {Type.OBJECT, Type.INT},
							 Constants.INVOKEVIRTUAL));
		}
	    }
	    else {
		ifcmpne.setTarget(end);
	    }
	    write_il.append(write_gen.getInstructionList());

	    write_gen.setInstructionList(write_il);
	    write_gen.setMaxStack(write_gen.getMaxStack(constantpool, write_il, write_gen.getExceptionHandlers()));
	    write_gen.setMaxLocals();

	    gen.setMethodAt(write_gen.getMethod(), default_write_method_index);

	    MethodGen read_gen = new MethodGen(methods[default_read_method_index], classname, constantpool);

	    InstructionList read_il = new InstructionList();
	    end = read_gen.getInstructionList().getStart();

	    read_il.append(new ILOAD(2));
	    read_il.append(new SIPUSH((short)dpth));
	    ifcmpne = new IF_ICMPNE(null);
	    read_il.append(ifcmpne);
	    read_il.append(generateDefaultReads(false, dpth, read_gen));
	    read_il.append(new GOTO(end));

	    if (super_is_ibis_serializable || super_is_serializable) {
		InstructionHandle i = read_il.append(new ILOAD(2));
		ifcmpne.setTarget(i);
		read_il.append(new SIPUSH((short)dpth));
		read_il.append(new IF_ICMPGT(end));
		if (super_is_ibis_serializable || force_generated_calls) {
		    read_il.append(new ALOAD(0));
		    read_il.append(new ALOAD(1));
		    read_il.append(new ILOAD(2));
		    read_il.append(createGeneratedDefaultReadObjectInvocation(super_classname, factory, Constants.INVOKESPECIAL));
		}
		else {
		    /*  Superclass is not rewritten.
		    */
		    read_il.append(new ALOAD(1));
		    read_il.append(new ALOAD(0));
		    read_il.append(new ILOAD(2));
		    read_il.append(factory.createInvoke(ibis_input_stream_name,
							 "defaultReadSerializableObject",
							 Type.VOID,
							 new Type[] {Type.OBJECT, Type.INT},
							 Constants.INVOKEVIRTUAL));
		}
	    }
	    else {
		ifcmpne.setTarget(end);
	    }

	    read_il.append(read_gen.getInstructionList());

	    read_gen.setInstructionList(read_il);
	    read_gen.setMaxStack(read_gen.getMaxStack(constantpool, read_il, read_gen.getExceptionHandlers()));
	    read_gen.setMaxLocals();

	    gen.setMethodAt(read_gen.getMethod(), default_read_method_index);

	    /* Now, produce the read constructor. It only exists if the superclass
	       is not serializable, or if the superclass has an ibis constructor, or
	       is assumed to have one (-force option).
	    */

	    read_il = null;
	    if (super_is_externalizable || super_has_ibis_constructor || ! super_is_serializable || force_generated_calls) {
		read_il = new InstructionList();
		if (super_is_externalizable || ! super_is_serializable) {
		    read_il.append(new ALOAD(0));
		    read_il.append(factory.createInvoke(super_classname,
							"<init>",
							Type.VOID,
							Type.NO_ARGS,
							Constants.INVOKESPECIAL));

		    read_il.append(new ALOAD(1));
		    read_il.append(new ALOAD(0));
		    read_il.append(factory.createInvoke(ibis_input_stream_name,
							"addObjectToCycleCheck",
							Type.VOID,
							new Type[] {Type.OBJECT},
							Constants.INVOKEVIRTUAL));
		    if (super_is_externalizable) {
			read_il.append(new ALOAD(0));
			read_il.append(new ALOAD(1));
			read_il.append(factory.createInvoke(super_classname,
							    "readExternal",
							    Type.VOID,
							    new Type[] { new ObjectType("java.io.ObjectInput") },
							    Constants.INVOKESPECIAL));
		    }
		}
		else {
		    read_il.append(new ALOAD(0));
		    read_il.append(new ALOAD(1));
		    read_il.append(createInitInvocation(super_classname, factory));
		}
	    }

	    /* Now, produce generated_WriteObject. */
	    write_il = new InstructionList();
	    write_gen = new MethodGen(methods[write_method_index], classname, constantpool);

	    /* write the superclass if neccecary */
	    if (super_is_externalizable) {
		write_il.append(new ALOAD(0));
		write_il.append(new ALOAD(1));
		write_il.append(factory.createInvoke(super_classname,
						     "writeExternal",
						     Type.VOID,
						     new Type[] {new ObjectType("java.io.ObjectOutput")},
						     Constants.INVOKESPECIAL));
	    }
	    else if (super_is_ibis_serializable || (force_generated_calls && super_is_serializable)) {
		write_il.append(new ALOAD(0));
		write_il.append(new ALOAD(1));
		write_il.append(createGeneratedWriteObjectInvocation(super_classname, Constants.INVOKESPECIAL));

	    } else if (super_is_serializable) {
		int ind = constantpool.addString(super_classname);
		write_il.append(new ALOAD(1));
		write_il.append(new ALOAD(0));
		write_il.append(new LDC(ind));
		write_il.append(factory.createInvoke(ibis_output_stream_name,
						     "writeSerializableObject",
						     Type.VOID,
						     new Type[] {Type.OBJECT, Type.STRING},
						     Constants.INVOKEVIRTUAL));
	    } else {
	    }

	    /* and now ... generated_WriteObject should either call the classes writeObject, if it has one,
	       or call generated_DefaultWriteObject. The read constructor should either call readObject,
	       or call generated_DefaultReadObject.
	    */
	    if (hasWriteObject()) {
		/* First, get and set IbisSerializationOutputStream's idea of the current object. */
		write_il.append(new ALOAD(1));
		write_il.append(new ALOAD(0));
		write_il.append(new SIPUSH((short)dpth));
		write_il.append(factory.createInvoke(ibis_output_stream_name,
						     "push_current_object",
						     Type.VOID,
						     new Type[] {Type.OBJECT, Type.INT},
						     Constants.INVOKEVIRTUAL));

		/* Then, call writeObject. */
		write_il.append(new ALOAD(0));
		write_il.append(new ALOAD(1));
		write_il.append(createWriteObjectInvocation());

		/* And then, restore IbisSerializationOutputStream's idea of the current object. */
		write_il.append(new ALOAD(1));
		write_il.append(factory.createInvoke(ibis_output_stream_name,
						     "pop_current_object",
						     Type.VOID,
						     Type.NO_ARGS,
						     Constants.INVOKEVIRTUAL));
	    }
	    else {
		write_il.append(generateDefaultWrites(dpth, write_gen));
	    }

	    /* Now, do the same for the reading side. */
	    MethodGen mgen = null;
	    int index = -1;
	    if (read_il != null) {
		mgen = new MethodGen(methods[read_cons_index], classname, constantpool);
		index = read_cons_index;
	    }
	    else if (hasReadObject()) {
		mgen = new MethodGen(methods[read_wrapper_index], classname, constantpool);
		read_il = new InstructionList();
		index = read_wrapper_index;
	    }

	    if (read_il != null) {
		if (hasReadObject()) {
		    /* First, get and set IbisSerializationInputStream's idea of the current object. */
		    read_il.append(new ALOAD(1));
		    read_il.append(new ALOAD(0));
		    read_il.append(new SIPUSH((short)dpth));
		    read_il.append(factory.createInvoke(ibis_input_stream_name,
							"push_current_object",
							Type.VOID,
							new Type[] {Type.OBJECT, Type.INT},
							Constants.INVOKEVIRTUAL));

		    /* Then, call readObject. */
		    read_il.append(new ALOAD(0));
		    read_il.append(new ALOAD(1));
		    read_il.append(factory.createInvoke(classname,
					"readObject",
					Type.VOID,
					new Type[] {sun_input_stream},
					Constants.INVOKESPECIAL));

		    /* And then, restore IbisSerializationOutputStream's idea of the current object. */
		    read_il.append(new ALOAD(1));
		    read_il.append(factory.createInvoke(ibis_input_stream_name,
							 "pop_current_object",
							 Type.VOID,
							 Type.NO_ARGS,
							 Constants.INVOKEVIRTUAL));
		}
		else {
		    read_il.append(generateDefaultReads(true, dpth, mgen));
		}

		read_il.append(mgen.getInstructionList());
		mgen.setInstructionList(read_il);

		mgen.setMaxStack(mgen.getMaxStack(constantpool, read_il, mgen.getExceptionHandlers()));
		mgen.setMaxLocals();

		gen.setMethodAt(mgen.getMethod(), index);
	    }

	    write_gen = new MethodGen(methods[write_method_index], classname, constantpool);
	    write_il.append(write_gen.getInstructionList());
	    write_gen.setInstructionList(write_il);

	    write_gen.setMaxStack(write_gen.getMaxStack(constantpool, write_il, write_gen.getExceptionHandlers()));
	    write_gen.setMaxLocals();

	    gen.setMethodAt(write_gen.getMethod(), write_method_index);

	    clazz = gen.getJavaClass();

	    Repository.removeClass(classname);
	    Repository.addClass(clazz);

	    if (verify) doVerify(clazz);

	    JavaClass gen = generateInstanceGenerator();

	    Repository.addClass(gen);

	    if (verify) doVerify(gen);

	    classes_to_save.add(clazz);
	    classes_to_save.add(gen);
	}
    }


    boolean verbose = false;
    boolean local = true;
    boolean file = false;
    boolean force_generated_calls = false;
    boolean verify = false;
    String pack;

    Hashtable primitiveSerialization;
    SerializationInfo referenceSerialization;

    Vector classes_to_rewrite, target_classes, classes_to_save;

    public IOGenerator(boolean verbose, boolean local, boolean file, boolean force_generated_calls, boolean verify, String pack) {
	ObjectType tp;

	this.verbose = verbose;
	this.local = local;
	this.file = file;
	this.pack = pack;
	this.force_generated_calls = force_generated_calls;
	this.verify = verify;
	if (force_generated_calls && verify) {
	    System.err.println("Warning: cannot have both -force and -verify");
	    this.verify = false;
	}

	classes_to_rewrite = new Vector();
	target_classes = new Vector();
	classes_to_save = new Vector();

	primitiveSerialization = new Hashtable();

	primitiveSerialization.put(Type.BOOLEAN, new SerializationInfo("writeBoolean", "readBoolean", "readFieldBoolean", Type.BOOLEAN, Type.BOOLEAN, true));

	primitiveSerialization.put(Type.BYTE, new SerializationInfo("writeByte", "readByte", "readFieldByte", Type.BYTE, Type.INT, true));

	primitiveSerialization.put(Type.SHORT, new SerializationInfo("writeShort", "readShort", "readFieldShort", Type.SHORT, Type.INT, true));

	primitiveSerialization.put(Type.CHAR, new SerializationInfo("writeChar", "readChar", "readFieldChar", Type.CHAR, Type.INT, true));

	primitiveSerialization.put(Type.INT, new SerializationInfo("writeInt", "readInt", "readFieldInt", Type.INT, Type.INT, true));
	primitiveSerialization.put(Type.LONG, new SerializationInfo("writeLong", "readLong", "readFieldLong", Type.LONG, Type.LONG, true));

	primitiveSerialization.put(Type.FLOAT, new SerializationInfo("writeFloat", "readFloat", "readFieldFloat", Type.FLOAT, Type.FLOAT, true));

	primitiveSerialization.put(Type.DOUBLE, new SerializationInfo("writeDouble", "readDouble", "readFieldDouble", Type.DOUBLE, Type.DOUBLE, true));
	primitiveSerialization.put(Type.STRING, new SerializationInfo("writeUTF", "readUTF", "readFieldUTF", Type.STRING, Type.STRING, true));
	primitiveSerialization.put(java_lang_class_type, new SerializationInfo("writeClass", "readClass", "readFieldClass", java_lang_class_type, java_lang_class_type, true));

	referenceSerialization = new SerializationInfo("writeObject", "readObject", "readFieldObject", Type.OBJECT, Type.OBJECT, false);
    }

    SerializationInfo getSerializationInfo(Type tp) {
	SerializationInfo temp = (SerializationInfo) primitiveSerialization.get(tp);
	return (temp == null ? referenceSerialization : temp);
    }

    private boolean isSerializable(JavaClass clazz) {
	return Repository.implementationOf(clazz, "java.io.Serializable");
    }

    private boolean isExternalizable(JavaClass clazz) {
	return directImplementationOf(clazz, "java.io.Externalizable");
    }

    private boolean isIbisSerializable(JavaClass clazz) {
	return directImplementationOf(clazz, "ibis.io.Serializable");
    }

    private void addTargetClass(JavaClass clazz) {
	if (!target_classes.contains(clazz) && ! isIbisSerializable(clazz)) {
	    target_classes.add(clazz);
	    if (verbose) System.out.println("Adding target class : " + clazz.getClassName());
	}
    }

    private void addRewriteClass(Type t) {
	if (t instanceof ArrayType) {
	    addRewriteClass(((ArrayType)t).getBasicType());
	}
	else if (t instanceof ObjectType) {
	    String name = ((ObjectType)t).getClassName();
	    JavaClass c = Repository.lookupClass(name);
	    if (c != null) {
		addClass(c);
	    }
	}
    }

    private void addRewriteClass(JavaClass clazz) {
	if (!classes_to_rewrite.contains(clazz) && ! isIbisSerializable(clazz)) {
	    classes_to_rewrite.add(clazz);
	    if (verbose) System.out.println("Adding rewrite class : " + clazz.getClassName());
	}
    }

    private void  addClass(JavaClass clazz) {
	if (! classes_to_rewrite.contains(clazz)) {
	    boolean serializable = false;

	    if (isExternalizable(clazz)) return;

	    JavaClass super_classes[] = Repository.getSuperClasses(clazz);

	    if (super_classes != null) {
		for (int i = 0; i < super_classes.length; i++) {
		    if (isSerializable(super_classes[i])) {
			serializable = true;
			if (! isIbisSerializable(super_classes[i])) {
			    addRewriteClass(super_classes[i]);
			} else {
			    if (verbose) System.out.println(clazz.getClassName() + " already implements ibis.io.Serializable");
			}
		    }
		}
	    }

	    serializable |= isSerializable(clazz);

	    if (serializable) {
		addRewriteClass(clazz);
		addTargetClass(clazz);
	    }
	}
    }

    private static boolean isFinal(Type t) {
	if (t instanceof BasicType) return true;
	if (t instanceof ArrayType) {
	    return isFinal(((ArrayType)t).getBasicType());
	}
	if (t instanceof ObjectType) {
	    String name = ((ObjectType)t).getClassName();
	    JavaClass c = Repository.lookupClass(name);
	    if (c == null) return false;
	    return c.isFinal();
	}
	return false;
    }


    void addReferencesToRewrite(JavaClass clazz) {

	/* Find all references to final reference types and add these to the rewrite list */
	Field[] fields = clazz.getFields();

	for (int i=0;i<fields.length;i++) {
	    Field field = fields[i];

	    /* Don't send fields that are STATIC or TRANSIENT */
	    if (! (field.isStatic() ||
	           field.isTransient())) {
		Type field_type = Type.getType(field.getSignature());

		if (!(field_type instanceof BasicType) &&
		    (field_type != Type.STRING) &&
		    isFinal(field_type)) {
		    addRewriteClass(field_type);
		}
	    }
	}
    }


    private static boolean directImplementationOf(JavaClass clazz, String name) {
	String names[] = clazz.getInterfaceNames();
	String supername = clazz.getSuperclassName();

	if (supername.equals(name)) return true;

	if (names == null) return false;
	for (int i = 0; i < names.length; i++) {
	    if (names[i].equals(name)) return true;
	}
	return false;
    }

    private static boolean predecessor(String c1, JavaClass c2) {
	String n = c2.getSuperclassName();

// System.out.println("comparing " + c1 + ", " + n);
	if (n.equals(c1)) return true;
	if (n.equals("java.lang.Object")) return false;
	return predecessor(c1, Repository.lookupClass(n));
    }

    private void do_sort_classes(Vector t) {
	int l = t.size();

	for (int i = 0; i < l; i++) {
	    JavaClass clazz = (JavaClass)t.get(i);
	    int sav_index = i;
	    for (int j = i+1; j < l; j++) {
		JavaClass clazz2 = (JavaClass)t.get(j);

		if (predecessor(clazz2.getClassName(), clazz)) {
// System.out.println(clazz2.getClassName() + " should be dealt with before " + clazz.getClassName());
		    clazz = clazz2;
		    sav_index = j;
		}
	    }
	    if (sav_index != i) {
		t.setElementAt(t.get(i), sav_index);
		t.setElementAt(clazz, i);
	    }
	}
    }

    public void scanClass(String [] classnames) {

	/* do the following here....

	   for each of the classes in args

	     - load it.
	     - scan to see if it's parent is serializable
	       - if so, add parent to rewrite list
	     - scan to see if it is serializable
	       - if so, add to rewrite list

	   for each of the classes in the rewrite list

	     - check if it contains references to final serializable objects
	       - if so, add these objects to the rewrite list
 		     - check if it already extends ibis.io.Serializable
	       - if not, add it and add the neccesary methods (empty)
	     - check if it is a target
	       - if so, add it to the target list

	   for each of the objects on the target list

                     - generate the code for the methods
	     - save the class file

	*/


	if (verbose) System.out.println("Loading classes");
	if (verbose) {
	    for (int i = 0; i < classnames.length; i++) {
		System.out.print(classnames[i] + " ");
	    }
	    System.out.println();
	}

	for (int i=0;i<classnames.length;i++) {
	    if (verbose) System.out.println("  Loading class : " + classnames[i]);

	    JavaClass clazz = null;
	    if(!file) {
		clazz = Repository.lookupClass(classnames[i]);
	    } else {
		String className = new String(classnames[i]);

		System.err.println("class name = " + className);
		try {
		    ClassParser p = new ClassParser(classnames[i].replace('.', java.io.File.separatorChar) + ".class");
		    clazz = p.parse();
		    if (clazz != null) {
			Repository.removeClass(className);
			Repository.addClass(clazz);
		    }
		} catch (Exception e) {
		    System.err.println("got exception while loading class: " + e);
		    System.exit(1);
		}
	    }

	    if (clazz != null) {
		if (! isIbisSerializable(clazz)) {
		    addClass(clazz);
		} else {
		    if (verbose) System.out.println(clazz.getClassName() + " already implements ibis.io.Serializable");
		}
	    }
	}

	if (verbose) System.out.println("Preparing classes");

	for (int i=0;i<classes_to_rewrite.size();i++) {
	    JavaClass clazz = (JavaClass)classes_to_rewrite.get(i);
	    addReferencesToRewrite(clazz);
	}

	/* Sort class to rewrite. Super classes first.  */
	do_sort_classes(classes_to_rewrite);

	for (int i=0;i<classes_to_rewrite.size();i++) {
	    JavaClass clazz = (JavaClass)classes_to_rewrite.get(i);
	    new CodeGenerator(clazz).generateMethods();
	}

	if (verbose) System.out.println("Rewriting classes");

	/* Sort target_classes. Super classes first.  */
	do_sort_classes(target_classes);

	for (int i=0;i<target_classes.size();i++) {
	    JavaClass clazz = (JavaClass)target_classes.get(i);
	    if (! clazz.isInterface()) {
		if (true || verbose) System.out.println("  Rewrite class : " + clazz.getClassName());
		new CodeGenerator(clazz).generateCode();
	    }
	}

	if (verbose) System.out.println("Saving classes");

	for (int i=0;i<classes_to_save.size();i++) {
	    JavaClass clazz = (JavaClass)classes_to_save.get(i);
	    String cl = clazz.getClassName();
	    String classfile = "";

	    try {
		if(local) {
		    int index = cl.lastIndexOf('.');
		    classfile = cl.substring(index+1) + ".class";
		} else {
		    classfile = cl.replace('.', java.io.File.separatorChar) + ".class";
		}
		if (verbose) System.out.println("  Saving class : " + classfile);
		clazz.dump(classfile);
	    } catch (IOException e) {
		System.err.println("got exception while writing " + classfile + ": " + e);
		System.exit(1);
	    }
	}
    }

    private static void usage() {
	System.out.println("Usage : java IOGenerator [-dir|-local] [-package <package>] [-v] " +
		   "<fully qualified classname list | classfiles>");
	System.exit(1);
    }


    public static void main(String[] args) throws IOException {
	boolean verbose = false;
	boolean local = true;
	boolean file = false;
	boolean force_generated_calls = false;
	boolean verify = false;
	Vector files = new Vector();
	String pack = null;

	if (args.length == 0) {
	    usage();
	}

	for(int i=0; i<args.length; i++) {
		if(args[i].equals("-v")) {
		verbose = true;
	    } else if(!args[i].startsWith("-")) {
		files.add(args[i]);
	    } else if (args[i].equals("-dir")) {
		local = false;
	    } else if (args[i].equals("-local")) {
		local = true;
	    } else if (args[i].equals("-file")) {
		file = true;
	    } else if (args[i].equals("-force")) {
		force_generated_calls = true;
	    } else if (args[i].equals("-verify")) {
		verify = true;
	    } else if (args[i].equals("-package")) {
		pack = args[i+1];
		i++; // skip arg
	    } else {
		usage();
	    }
	}

	String[] newArgs = new String[files.size()];
	for(int i=0; i<files.size(); i++) {
	    int index = ((String)files.elementAt(i)).lastIndexOf(".class");

	    if (index != -1) {
		if(pack == null) {
		    newArgs[i] = ((String)files.elementAt(i)).substring(0, index);
		} else {
		    newArgs[i] = pack + "." + ((String)files.elementAt(i)).substring(0, index);
		}
	    } else {
		if(pack == null) {
		    newArgs[i] = (String)files.elementAt(i);
		} else {
		    newArgs[i] = pack + "." + ((String)files.elementAt(i));
		}
	    }
	    int colon = newArgs[i].indexOf(':');
	    if (colon != -1) {
		newArgs[i] = newArgs[i].substring(colon + 1);
	    }
	    newArgs[i] = newArgs[i].replace(java.io.File.separatorChar, '.');
	}

	new IOGenerator(verbose, local, file, force_generated_calls, verify, pack).scanClass(newArgs);
    }
}
