package ibis.frontend.io;

import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.verifier.*;

/* TODO: Deal with SerializablePersistentFields ... For now, the alternativeReadObject
   mechanism deals with it.
*/

public class IOGenerator {

    class SerializationInfo {

	String	write_name;
	String	read_name;
	String	final_read_name;
	Type 	tp;
	Type	param_tp;

	boolean primitive;

	SerializationInfo(String wn, String rn, String frn, Type t, Type param_tp, boolean primitive) {
	    this.write_name = wn;
	    this.read_name  = rn;
	    this.final_read_name = frn;
	    this.tp  = t;
	    this.param_tp = param_tp;
	    this.primitive = primitive;
	}

	InstructionList writeInstructions(String class_name, Field field, InstructionFactory fc) {
	    Type t = tp;
	    InstructionList temp = new InstructionList();

	    if (! primitive) {
		t = Type.getType(field.getSignature());
	    }

	    temp.append(new ALOAD(1));
	    temp.append(new ALOAD(0));
	    temp.append(fc.createFieldAccess(class_name,
					     field.getName(),
					     t,
					     Constants.GETFIELD));
	    temp.append(fc.createInvoke(primitive ?
					    "ibis.io.IbisSerializationOutputStream" :
					    "java.io.ObjectOutputStream",
					write_name,
					Type.VOID,
					new Type[] { param_tp },
					Constants.INVOKEVIRTUAL));

	    return temp;
	}

	InstructionList readInstructions(String class_name, Field field, InstructionFactory fc, boolean from_constructor, ClassGen cg) {
	    Type t = tp;
	    InstructionList temp = new InstructionList();

	    if (! primitive) {
		t = Type.getType(field.getSignature());
	    }

	    if (from_constructor || ! field.isFinal()) {
		temp.append(new ALOAD(0));
		temp.append(new ALOAD(1));
		temp.append(fc.createInvoke(primitive ?
						"ibis.io.IbisSerializationInputStream":
						"java.io.ObjectInputStream",
					    read_name,
					    tp,
					    Type.NO_ARGS,
					    Constants.INVOKEVIRTUAL));

		if (! primitive) {
		    temp.append(fc.createCheckCast((ReferenceType) t));
		}

		temp.append(fc.createFieldAccess(class_name,
					         field.getName(),
					         t,
					         Constants.PUTFIELD));
	    }
	    else {
		temp.append(new ALOAD(1));
		temp.append(new ALOAD(0));
		int ind = cg.getConstantPool().addString(field.getName());
		temp.append(new LDC(ind));
		if (primitive) {
		    temp.append(fc.createInvoke("ibis.io.IbisSerializationInputStream",
						final_read_name,
						Type.VOID,
						new Type[] { Type.OBJECT, Type.STRING},
						Constants.INVOKEVIRTUAL));
		}
		else {
		    int ind2 = cg.getConstantPool().addString(field.getSignature());
		    temp.append(new LDC(ind2));
		    temp.append(fc.createInvoke("ibis.io.IbisSerializationInputStream",
						final_read_name,
						Type.VOID,
						new Type[] { Type.OBJECT, Type.STRING, Type.STRING},
						Constants.INVOKEVIRTUAL));
		}
	    }

	    return temp;
	}
    }

    boolean verbose = false;
    boolean local = true;
    boolean file = false;
    boolean force_generated_calls = false;
    String pack;

    Hashtable primitiveSerialization;
    SerializationInfo referenceSerialization;

    Vector classes_to_rewrite, target_classes, classes_to_save;

    public IOGenerator(boolean verbose, boolean local, boolean file, boolean force_generated_calls, String[] args, int num, String pack) {
	ObjectType tp;

	this.verbose = verbose;
	this.local = local;
	this.file = file;
	this.pack = pack;
	this.force_generated_calls = force_generated_calls;

	classes_to_rewrite = new Vector();
	target_classes = new Vector();
	classes_to_save = new Vector();

	primitiveSerialization = new Hashtable();

	primitiveSerialization.put(Type.BOOLEAN, new SerializationInfo("writeBoolean", "readBoolean", "read_field_boolean", Type.BOOLEAN, Type.BOOLEAN, true));

	primitiveSerialization.put(Type.BYTE, new SerializationInfo("writeByte", "readByte", "read_field_byte", Type.BYTE, Type.INT, true));

	primitiveSerialization.put(Type.SHORT, new SerializationInfo("writeShort", "readShort", "read_field_short", Type.SHORT, Type.INT, true));

	primitiveSerialization.put(Type.CHAR, new SerializationInfo("writeChar", "readChar", "read_field_char", Type.CHAR, Type.INT, true));

	primitiveSerialization.put(Type.INT, new SerializationInfo("writeInt", "readInt", "read_field_short", Type.INT, Type.INT, true));
	primitiveSerialization.put(Type.LONG, new SerializationInfo("writeLong", "readLong", "read_field_long", Type.LONG, Type.LONG, true));

	primitiveSerialization.put(Type.FLOAT, new SerializationInfo("writeFloat", "readFloat", "read_field_float", Type.FLOAT, Type.FLOAT, true));

	primitiveSerialization.put(Type.DOUBLE, new SerializationInfo("writeDouble", "readDouble", "read_field_double", Type.DOUBLE, Type.DOUBLE, true));
	primitiveSerialization.put(Type.STRING, new SerializationInfo("writeUTF", "readUTF", "read_field_UTF", Type.STRING, Type.STRING, true));

	referenceSerialization = new SerializationInfo("writeObject", "readObject", "read_field_object", Type.OBJECT, Type.OBJECT, false);
    }

    SerializationInfo getSerializationInfo(Type tp) {
	SerializationInfo temp = (SerializationInfo) primitiveSerialization.get(tp);
	return (temp == null ? referenceSerialization : temp);
    }

    boolean isSerializable(JavaClass clazz) {
	return Repository.implementationOf(clazz, "java.io.Serializable");
    }

    boolean isIbisSerializable(JavaClass clazz) {
	return directImplementationOf(clazz, "ibis.io.Serializable");
    }

    boolean has_ibis_constructor(JavaClass clazz) {
	Method[] methods = clazz.getMethods();

	for (int i = 0; i < methods.length; i++) {
	    if (methods[i].getName().equals("<init>") &&
	        methods[i].getSignature().equals("(Libis/io/IbisSerializationInputStream;)V")) return true;
	}
	return false;
    }

    void addTargetClass(JavaClass clazz) {
	if (!target_classes.contains(clazz)) {
	    target_classes.add(clazz);
	    if (verbose) System.out.println("Adding target class : " + clazz.getClassName());
	}
    }

    void addRewriteClass(Type t) {
	if (t instanceof ArrayType) {
	    addRewriteClass(((ArrayType)t).getBasicType());
	}
	else if (t instanceof ObjectType) {
	    String name = ((ObjectType)t).getClassName();
	    JavaClass c = Repository.lookupClass(name);
	    if (c != null) {
		addRewriteClass(c);
	    }
	}
    }

    void addRewriteClass(JavaClass clazz) {
	if (!classes_to_rewrite.contains(clazz)) {
	    classes_to_rewrite.add(clazz);
	    if (verbose) System.out.println("Adding rewrite class : " + clazz.getClassName());
	}
    }

    void  addClass(JavaClass clazz) {

	boolean serializable = false;

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
	    Field[] fields = clazz.getFields();

	    for (int i = 0; i < fields.length; i++) {
		Field f = fields[i];
		if (f.getName().equals("serialPersistentFields") &&
		    f.isFinal() &&
		    f.isStatic() &&
		    f.isPrivate() &&
		    f.getSignature().equals("[Ljava/io/ObjectStreamField;")) {
		    /*  Don't touch these. alternativeWriteObject and friends should deal with this.
			In general, it is probably not possible to handle this in the IOGenerator.
		    */
		    System.err.println("class " + clazz.getClassName() + " has serialPersistentFields, so is not rewritten");
		    return;
		}
	    }

	    addRewriteClass(clazz);
	    addTargetClass(clazz);
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

    void generateMethods(ClassGen clazz) {
	/* Generate the necessary (empty) methods. */

	if (verbose) System.out.println("  Generating empty methods for class : " + clazz.getClassName());

	if (verbose) System.out.println("    " + clazz.getClassName() + " implements java.io.Serializable -> adding ibis.io.Serializable");
	/* add the ibis.io.Serializable interface to the class */
	clazz.addInterface("ibis.io.Serializable");

	/* Construct a write method */
	InstructionList il = new InstructionList();
	il.append(new RETURN());

	MethodGen write_method = new MethodGen( Constants.ACC_PUBLIC | (clazz.isFinal() ? Constants.ACC_FINAL : 0),
						Type.VOID,
						new Type[] {new ObjectType("ibis.io.IbisSerializationOutputStream")},
						new String[] { "os" },
						"generated_WriteObject",
						clazz.getClassName(),
						il,
						clazz.getConstantPool());

	write_method.addException("java.io.IOException");
	clazz.addMethod(write_method.getMethod());

	/* ... and a default_write_method */
	il = new InstructionList();
	il.append(new RETURN());

	MethodGen default_write_method =
			         new MethodGen( Constants.ACC_PUBLIC | (clazz.isFinal() ? Constants.ACC_FINAL : 0),
						Type.VOID,
						new Type[] {new ObjectType("ibis.io.IbisSerializationOutputStream"), Type.INT},
						new String[] { "os", "lvl"},
						"generated_DefaultWriteObject",
						clazz.getClassName(),
						il,
						clazz.getConstantPool());

	default_write_method.addException("java.io.IOException");
	clazz.addMethod(default_write_method.getMethod());

	/* ... and a default_read_method */
	il = new InstructionList();
	il.append(new RETURN());

	MethodGen default_read_method =
			         new MethodGen( Constants.ACC_PUBLIC | (clazz.isFinal() ? Constants.ACC_FINAL : 0),
						Type.VOID,
						new Type[] {new ObjectType("ibis.io.IbisSerializationInputStream"), Type.INT},
						new String[] { "os", "lvl" },
						"generated_DefaultReadObject",
						clazz.getClassName(),
						il,
						clazz.getConstantPool());

	default_read_method.addException("java.io.IOException");
	clazz.addMethod(default_read_method.getMethod());

	/* Construct a read-of-the-stream constructor, but only when we can actually use it. */

	String super_class_name = clazz.getSuperclassName();
	JavaClass super_class = Repository.lookupClass(super_class_name);

	if (! isSerializable(super_class) ||
		(force_generated_calls || has_ibis_constructor(super_class))) {
	    il = new InstructionList();
	    il.append(new RETURN());

	    MethodGen read_cons = new MethodGen(Constants.ACC_PUBLIC,
						Type.VOID,
						new Type[] {new ObjectType("ibis.io.IbisSerializationInputStream")},
						new String[] { "is" },
						"<init>",
						clazz.getClassName(),
						il,
						clazz.getConstantPool());
	    read_cons.addException("java.io.IOException");
	    clazz.addMethod(read_cons.getMethod());
	}
    }

    JavaClass generate_InstanceGenerator(JavaClass clazz) {

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

	if (verbose) System.out.println("  Generating InstanceGenerator class for " + clazz.getClassName());

	String name = clazz.getClassName() + "_ibis_io_Generator";

	ObjectType class_type = new ObjectType(clazz.getClassName());

	String classfilename = name.substring(name.lastIndexOf('.')+1) + ".class";
	ClassGen gen = new ClassGen(name, "ibis.io.Generator", classfilename, Constants.ACC_FINAL|Constants.ACC_PUBLIC|Constants.ACC_SUPER, null);
	InstructionFactory factory = new InstructionFactory(gen);

	InstructionList il = new InstructionList();

	String super_class_name = clazz.getSuperclassName();

	JavaClass super_class = Repository.lookupClass(super_class_name);

	boolean super_is_serializable = isSerializable(super_class);
	boolean super_has_ibis_constructor = has_ibis_constructor(super_class);

	if (super_is_serializable && ! super_has_ibis_constructor && ! force_generated_calls) {
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
	    int ind = gen.getConstantPool().addString(clazz.getClassName());
	    il.append(new LDC(ind));
	    il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream",
					   "create_uninitialized_object",
					   Type.OBJECT,
					   new Type[] { Type.STRING},
					   Constants.INVOKEVIRTUAL));
	    il.append(factory.createCheckCast(class_type));
	    il.append(new ASTORE(2));

	    /* Now read the superclass. */
	    il.append(new ALOAD(1));
	    il.append(new ALOAD(2));
	    ind = gen.getConstantPool().addString(super_class_name);
	    il.append(new LDC(ind));
	    il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream",
					   "readSerializableObject",
					   Type.VOID,
					   new Type[] {Type.OBJECT, Type.STRING},
					   Constants.INVOKEVIRTUAL));

	    /* Now, if the class has a readObject, call it. Otherwise, read its fields,
	       by calling generated_DefaultReadObject.
	    */
	    if (has_read_object(clazz)) {
		/* TODO !!! */
	    }
	    else {
		int dpth = get_class_depth(clazz);

		il.append(new ALOAD(2));
		il.append(new ALOAD(1));
		il.append(new SIPUSH((short)dpth));
		il.append(factory.createInvoke(clazz.getClassName(),
					       "generated_DefaultReadObject",
					       Type.VOID,
					       new Type[] {new ObjectType("ibis.io.IbisSerializationInputStream"), Type.INT},
					       Constants.INVOKEVIRTUAL));
	    }
	    il.append(new ALOAD(2));
	}
	else {
	    il.append(factory.createNew(class_type));
	    il.append(new DUP());
	    il.append(new ALOAD(1));
	    il.append(factory.createInvoke(clazz.getClassName(),
					   "<init>",
					   Type.VOID,
					   new Type [] { new ObjectType("ibis.io.IbisSerializationInputStream") },
					   Constants.INVOKESPECIAL));
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
					 new Type[] {new ObjectType("ibis.io.IbisSerializationInputStream")},
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

    private int get_class_depth(JavaClass clazz) {
	String class_name = clazz.getClassName();

	if (! isSerializable(clazz)) return 0;
	return 1 + get_class_depth(Repository.lookupClass(clazz.getSuperclassName()));
    }

    private static boolean has_read_object(JavaClass clazz) {
	Method[] methods = clazz.getMethods();

	for (int i = 0; i < methods.length; i++) {
	    if (methods[i].getName().equals("readObject") &&
	        methods[i].getSignature().equals("(Ljava/io/ObjectInputStream;)V")) return true;
	}
	return false;
    }

    private static boolean has_write_object(JavaClass clazz) {
	Method[] methods = clazz.getMethods();

	for (int i = 0; i < methods.length; i++) {
	    if (methods[i].getName().equals("writeObject") &&
	        methods[i].getSignature().equals("(Ljava/io/ObjectOutputStream;)V")) return true;
	}

        return false;
    }

    private InstructionList get_default_writes(Field[] fields, InstructionFactory factory, String class_name) {
	InstructionList write_il = new InstructionList();

	for (int i=0;i<fields.length;i++) {
	    Field field = fields[i];

	    /* Don't send fields that are STATIC or TRANSIENT */
	    if (! (field.isStatic() ||
		   field.isTransient())) {
		Type field_type = Type.getType(field.getSignature());

		if ((field_type instanceof ReferenceType) &&
		    ! field_type.equals(Type.STRING)) {
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
			write_il.append(factory.createFieldAccess(class_name,
							    field.getName(),
							    field_type,
							    Constants.GETFIELD));
			write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream",
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
			write_il.append(factory.createFieldAccess(class_name,
							    field.getName(),
							    field_type,
							    Constants.GETFIELD));
			write_il.append(new ALOAD(1));

			write_il.append(factory.createInvoke(field_class.getClassName(),
						       "generated_WriteObject",
						       Type.VOID,
						       new Type[] {new ObjectType("ibis.io.IbisSerializationOutputStream")},
						       Constants.INVOKEVIRTUAL));

			InstructionHandle target = write_il.append(new NOP());
			ifcmp.setTarget(target);

		    } else {
			SerializationInfo info = getSerializationInfo(field_type);
			write_il.append(info.writeInstructions(class_name, field, factory));
		    }
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

		if (field_type.equals(Type.STRING)) {
		    if (verbose) System.out.println("    writing string field " + field.getName() + " of type " + field_type.getSignature());

		    SerializationInfo info = getSerializationInfo(field_type);
		    write_il.append(info.writeInstructions(class_name, field, factory));
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

		    SerializationInfo info = getSerializationInfo(field_type);
		    write_il.append(info.writeInstructions(class_name, field, factory));
		}
	    }
	}
	return write_il;
    }

    private InstructionList get_default_reads(Field[] fields, InstructionFactory factory, String class_name, boolean from_constructor, ClassGen cg) {
	InstructionList read_il = new InstructionList();
	for (int i=0;i<fields.length;i++) {
	    Field field = fields[i];

	    /* Don't send fields that are STATIC or TRANSIENT */
	    if (! (field.isStatic() ||
		   field.isTransient())) {
		Type field_type = Type.getType(field.getSignature());

		if ((field_type instanceof ReferenceType) &&
		    ! field_type.equals(Type.STRING)) {
		    boolean isfinal = false;
		    JavaClass field_class = null;

		    if (verbose) System.out.println("    writing reference field " + field.getName() + " of type " + field_type.getSignature());

		    if (field_type instanceof ObjectType) {
			field_class = Repository.lookupClass(((ObjectType)field_type).getClassName());
			if (field_class != null && field_class.isFinal()) isfinal = true;
		    }

		    if (isfinal &&
			( has_ibis_constructor(field_class) ||
			  (isSerializable(field_class) && force_generated_calls)) &&
			(! (Repository.implementationOf(field_class, "java.rmi.Remote") ||
			    Repository.implementationOf(field_class, "ibis.rmi.Remote"))) &&
			!field_type.getSignature().startsWith("Ljava/")) {

			read_il.append(new ALOAD(1));
			read_il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream",
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
			read_il.append(factory.createInvoke(field_class.getClassName(),
						       "<init>",
						       Type.VOID,
						       new Type[] {new ObjectType("ibis.io.IbisSerializationInputStream")},
						       Constants.INVOKESPECIAL));
			read_il.append(factory.createFieldAccess(class_name,
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
			read_il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream",
						       "getObjectFromCycleCheck",
						       Type.OBJECT,
						       new Type[] { Type.INT },
						       Constants.INVOKEVIRTUAL));

			read_il.append(factory.createCheckCast((ObjectType)field_type));
			read_il.append(factory.createFieldAccess(class_name,
							    field.getName(),
							    field_type,
							    Constants.PUTFIELD));

			InstructionHandle target = read_il.append(new NOP());
			ifcmpeq.setTarget(target);
			gto.setTarget(target);
		    } else {
			SerializationInfo info = getSerializationInfo(field_type);
			read_il.append(info.readInstructions(class_name, field, factory, from_constructor, cg));
		    }
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

		if (field_type.equals(Type.STRING)) {
		    if (verbose) System.out.println("    writing string field " + field.getName() + " of type " + field_type.getSignature());

		    SerializationInfo info = getSerializationInfo(field_type);
		    read_il.append(info.readInstructions(class_name, field, factory, from_constructor, cg));
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

		    SerializationInfo info = getSerializationInfo(field_type);
		    read_il.append(info.readInstructions(class_name, field, factory, from_constructor, cg));
		}
	    }
	}
	return read_il;
    }

    void generateCode(JavaClass clazz) {
	String class_name = clazz.getClassName();

	if (verbose) System.out.println("  Generating method code class for class : " + class_name);

	if(clazz.isInterface()) return;

	String super_class_name = clazz.getSuperclassName();

	JavaClass super_class = Repository.lookupClass(super_class_name);

	boolean super_is_serializable = isSerializable(super_class);
	boolean super_is_ibis_serializable = isIbisSerializable(super_class);
	boolean super_has_ibis_constructor = has_ibis_constructor(super_class);

	/* Generate code inside the methods */

	ClassGen cg = new ClassGen(clazz);

	Field[] fields = cg.getFields();

	if (verbose) System.out.println("    Number of fields " + fields.length);

	Method[] class_methods = cg.getMethods();
	int write_method_index = -1;
	int default_write_method_index = -1;
	int default_read_method_index = -1;
	int read_cons_index = -1;

	for (int i = 0; i < class_methods.length; i++) {
	    if (class_methods[i].getName().equals("generated_WriteObject") &&
		class_methods[i].getSignature().equals("(Libis/io/IbisSerializationOutputStream;)V")) {
		write_method_index = i;
	    }
	    else if (class_methods[i].getName().equals("generated_DefaultWriteObject") &&
		class_methods[i].getSignature().equals("(Libis/io/IbisSerializationOutputStream;I)V")) {
		default_write_method_index = i;
	    }
	    else if (class_methods[i].getName().equals("generated_DefaultReadObject") &&
		class_methods[i].getSignature().equals("(Libis/io/IbisSerializationInputStream;I)V")) {
		default_read_method_index = i;
	    }
	    else if (class_methods[i].getName().equals("<init>") &&
		class_methods[i].getSignature().equals("(Libis/io/IbisSerializationInputStream;)V")) {
		read_cons_index = i;
	    }
	}

	InstructionFactory factory = new InstructionFactory(cg);

	int dpth = get_class_depth(clazz);

	/* void generated_DefaultWriteObject(IbisSerializationOutputStream out, int level) {
		if (level == dpth) {
		    ... write fields ... (the code resulting from the get_default_writes() call).
		}
		else if (level < dpth) {
		    super.generated_DefaultWriteObject(out, level);
		}
	   }
	*/

	MethodGen write_gen = new MethodGen(class_methods[default_write_method_index], class_name, cg.getConstantPool());

	InstructionList write_il = new InstructionList();
	InstructionHandle end = write_gen.getInstructionList().getStart();

	write_il.append(new ILOAD(2));
	write_il.append(new SIPUSH((short)dpth));
	IF_ICMPNE ifcmpne = new IF_ICMPNE(null);
	write_il.append(ifcmpne);
	write_il.append(get_default_writes(fields, factory, class_name));
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
		write_il.append(factory.createInvoke(super_class_name,
						     "generated_DefaultWriteObject",
						     Type.VOID,
						     new Type[] {new ObjectType("ibis.io.IbisSerializationOutputStream"), Type.INT},
						     Constants.INVOKESPECIAL));
	    }
	    else {
		/*  Superclass is not rewritten.
		*/
		write_il.append(new ALOAD(1));
		write_il.append(new ALOAD(0));
		write_il.append(new ILOAD(2));
		write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream",
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
	write_gen.setMaxStack(write_gen.getMaxStack(cg.getConstantPool(), write_il, write_gen.getExceptionHandlers()));
	write_gen.setMaxLocals();

	cg.setMethodAt(write_gen.getMethod(), default_write_method_index);

	MethodGen read_gen = new MethodGen(class_methods[default_read_method_index], class_name, cg.getConstantPool());

	InstructionList read_il = new InstructionList();
	end = read_gen.getInstructionList().getStart();

	read_il.append(new ILOAD(2));
	read_il.append(new SIPUSH((short)dpth));
	ifcmpne = new IF_ICMPNE(null);
	read_il.append(ifcmpne);
	read_il.append(get_default_reads(fields, factory, class_name, false, cg));
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
		read_il.append(factory.createInvoke(super_class_name,
						     "generated_DefaultReadObject",
						     Type.VOID,
						     new Type[] {new ObjectType("ibis.io.IbisSerializationInputStream"), Type.INT},
						     Constants.INVOKESPECIAL));
	    }
	    else {
		/*  Superclass is not rewritten.
		*/
		read_il.append(new ALOAD(1));
		read_il.append(new ALOAD(0));
		read_il.append(new ILOAD(2));
		read_il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream",
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
	read_gen.setMaxStack(read_gen.getMaxStack(cg.getConstantPool(), read_il, read_gen.getExceptionHandlers()));
	read_gen.setMaxLocals();

	cg.setMethodAt(read_gen.getMethod(), default_read_method_index);

	/* Now, produce the read constructor. It only exists if the superclass
	   is not serializable, or if the superclass has an ibis constructor, or
	   is assumed to have one (-force option).
	*/

	read_il = null;
	if (super_has_ibis_constructor || ! super_is_serializable || force_generated_calls) {
	    read_il = new InstructionList();
	    if (! super_is_serializable) {
		read_il.append(new ALOAD(0));
		read_il.append(factory.createInvoke(super_class_name,
						    "<init>",
						    Type.VOID,
						    Type.NO_ARGS,
						    Constants.INVOKESPECIAL));

		read_il.append(new ALOAD(1));
		read_il.append(new ALOAD(0));
		read_il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream",
						    "addObjectToCycleCheck",
						    Type.VOID,
						    new Type[] {Type.OBJECT},
						    Constants.INVOKEVIRTUAL));
	    }
	    else {
		read_il.append(new ALOAD(0));
		read_il.append(new ALOAD(1));
		read_il.append(factory.createInvoke(super_class_name,
						    "<init>",
						    Type.VOID,
						    new Type[] {new ObjectType("ibis.io.IbisSerializationInputStream")},
						    Constants.INVOKESPECIAL));
	    }
	}

	/* Now, produce generated_WriteObject. */
	write_il = new InstructionList();

	/* write the superclass if neccecary */
	if (super_is_ibis_serializable || (force_generated_calls && super_is_serializable)) {
	    write_il.append(new ALOAD(0));
	    write_il.append(new ALOAD(1));
	    write_il.append(factory.createInvoke(super_class_name,
						 "generated_WriteObject",
						 Type.VOID,
						 new Type[] {new ObjectType("ibis.io.IbisSerializationOutputStream")},
						 Constants.INVOKESPECIAL));

	} else if (super_is_serializable) {
	    int ind = cg.getConstantPool().addString(super_class_name);
	    write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(0));
	    write_il.append(new LDC(ind));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream",
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
	if (has_write_object(clazz)) {
	    /* First, get and set IbisSerializationOutputStream's idea of the current object. */
	    write_il.append(new ALOAD(1));
	    write_il.append(new ALOAD(0));
	    write_il.append(new SIPUSH((short)dpth));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream",
						 "push_current_object",
						 Type.VOID,
						 new Type[] {Type.OBJECT, Type.INT},
						 Constants.INVOKEVIRTUAL));

	    /* Then, call writeObject. */
	    write_il.append(new ALOAD(0));
	    write_il.append(new ALOAD(1));
	    write_il.append(factory.createInvoke(class_name,
						 "writeObject",
						 Type.VOID,
						 new Type[] {new ObjectType("java.io.ObjectOutputStream")},
						 Constants.INVOKESPECIAL));

	    /* And then, restore IbisSerializationOutputStream's idea of the current object. */
	    write_il.append(new ALOAD(1));
	    write_il.append(factory.createInvoke("ibis.io.IbisSerializationOutputStream",
						 "pop_current_object",
						 Type.VOID,
						 Type.NO_ARGS,
						 Constants.INVOKEVIRTUAL));
	}
	else {
	    write_il.append(get_default_writes(fields, factory, class_name));
	}

	/* Now, do the same for the reading side. */
	if (read_il != null) {
	    if (has_read_object(clazz)) {
		/* First, get and set IbisSerializationInputStream's idea of the current object. */
		read_il.append(new ALOAD(1));
		read_il.append(new ALOAD(0));
		read_il.append(new SIPUSH((short)dpth));
		read_il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream",
						     "push_current_object",
						     Type.VOID,
						     new Type[] {Type.OBJECT, Type.INT},
						     Constants.INVOKEVIRTUAL));

		/* Then, call readObject. */
		read_il.append(new ALOAD(0));
		read_il.append(new ALOAD(1));
		read_il.append(factory.createInvoke(class_name,
						     "readObject",
						     Type.VOID,
						     new Type[] {new ObjectType("java.io.ObjectInputStream")},
						     Constants.INVOKESPECIAL));

		/* And then, restore IbisSerializationOutputStream's idea of the current object. */
		read_il.append(new ALOAD(1));
		read_il.append(factory.createInvoke("ibis.io.IbisSerializationInputStream",
						     "pop_current_object",
						     Type.VOID,
						     Type.NO_ARGS,
						     Constants.INVOKEVIRTUAL));
	    }
	    else {
		read_il.append(get_default_reads(fields, factory, class_name, true, cg));
	    }

	    MethodGen read_cons_gen = new MethodGen(class_methods[read_cons_index], class_name, cg.getConstantPool());
	    read_il.append(read_cons_gen.getInstructionList());
	    read_cons_gen.setInstructionList(read_il);

	    read_cons_gen.setMaxStack(read_cons_gen.getMaxStack(cg.getConstantPool(), read_il, read_cons_gen.getExceptionHandlers()));
	    read_cons_gen.setMaxLocals();

	    cg.setMethodAt(read_cons_gen.getMethod(), read_cons_index);
	}

	write_gen = new MethodGen(class_methods[write_method_index], class_name, cg.getConstantPool());
	write_il.append(write_gen.getInstructionList());
	write_gen.setInstructionList(write_il);

	write_gen.setMaxStack(write_gen.getMaxStack(cg.getConstantPool(), write_il, write_gen.getExceptionHandlers()));
	write_gen.setMaxLocals();

	cg.setMethodAt(write_gen.getMethod(), write_method_index);

	clazz = cg.getJavaClass();

	Repository.removeClass(class_name);
	Repository.addClass(clazz);

do_verify(clazz);

	JavaClass gen = generate_InstanceGenerator(clazz);

	Repository.addClass(gen);

do_verify(gen);

	classes_to_save.add(clazz);
	classes_to_save.add(gen);
    }


    public void scanClass(String classname) {
	String[] tmp = new String[1];
	tmp[0] = classname;
	scanClass(tmp, 1);
    }

    public static boolean directImplementationOf(JavaClass clazz, String name) {
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

    public void scanClass(String [] classnames, int num) {

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

	for (int i=0;i<num;i++) {
	    if (verbose) System.out.println("  Loading class : " + classnames[i]);

	    JavaClass clazz = null;
	    if(!file) {
		clazz = Repository.lookupClass(classnames[i]);
	    } else {
		String className = new String(classnames[i]);

		className.replace('/', '.');
		System.err.println("class name = " + className);
		try {
		    ClassParser p = new ClassParser(classnames[i] + ".class");
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

	    try {
		/* BCEL throws an exception here if it cannot find some class.
		 * In this case, we won't rewrite.
		 */
		if (! isIbisSerializable(clazz)) {
		    addClass(clazz);
		} else {
		    if (verbose) System.out.println(clazz.getClassName() + " already implements ibis.io.Serializable");
		}
	    } catch(Exception e) {
		System.out.println("Got an exception while checking " + clazz.getClassName() + ", not rewritten");
		// System.out.println("Exception = " + e);
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
	    ClassGen clazzgen = new ClassGen(clazz);
	    generateMethods(clazzgen);
	    JavaClass newclazz = clazzgen.getJavaClass();
	    if (target_classes.remove(clazz)) {
		Repository.removeClass(clazz.getClassName());
		Repository.addClass(newclazz);
		target_classes.add(newclazz);
	    }
	    if (classes_to_save.remove(clazz)) {
		classes_to_save.add(newclazz);
	    }
	}

	if (verbose) System.out.println("Rewriting classes");

	/* Sort target_classes. Super classes first.  */
	do_sort_classes(target_classes);

	for (int i=0;i<target_classes.size();i++) {
	    JavaClass clazz = (JavaClass)target_classes.get(i);
	    generateCode(clazz);
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
		    classfile = cl.replace('.', '/') + ".class";
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

    private boolean do_verify(JavaClass c) {
	Verifier verf = VerifierFactory.getVerifier(c.getClassName());
	boolean verification_failed = false;

System.out.println("Verifying " + c.getClassName());

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

    public static void main(String[] args) throws IOException {
	boolean verbose = false;
	boolean local = true;
	boolean file = false;
	boolean force_generated_calls = false;
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
	}

	new IOGenerator(verbose, local, file, force_generated_calls, newArgs, newArgs.length, pack).scanClass(newArgs, newArgs.length);
    }
}
