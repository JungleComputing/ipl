package ibis.frontend.io;

import java.util.*;
import java.util.jar.*;
import java.io.*;
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.*;

public class IOGenerator {

    class SerializationInfo { 

	String	write_name;
	String	read_name;
	Type 	tp;

	boolean primitive;
	
	SerializationInfo(String wn, String rn, Type t,  boolean primitive) { 
	    this.write_name = wn;
	    this.read_name  = rn;
	    this.tp  = t;
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
	    temp.append(fc.createInvoke("ibis.io.MantaOutputStream",
					write_name,
					Type.VOID,
					new Type[] { tp },
					Constants.INVOKEVIRTUAL));
		    
	    return temp;
	}

	InstructionList readInstructions(String class_name, Field field, InstructionFactory fc) { 
	    Type t = tp;
	    InstructionList temp = new InstructionList();

	    if (! primitive) {
		t = Type.getType(field.getSignature());
	    }

	    temp.append(new ALOAD(0));
	    temp.append(new ALOAD(1));
	    temp.append(fc.createInvoke("ibis.io.MantaInputStream",
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

	    return temp;
	}
    }

    boolean verbose = false;
    boolean local = true;
    boolean file = false;
    String pack;

    Hashtable primitiveSerialization;
    SerializationInfo referenceSerialization;

    Vector classes_to_rewrite, target_classes, classes_to_save;

    public IOGenerator(boolean verbose, boolean local, boolean file, String[] args, int num, String pack) { 
	ObjectType tp;

	this.verbose = verbose;
	this.local = local;
	this.file = file;
	this.pack = pack;

	classes_to_rewrite = new Vector();
	target_classes = new Vector();
	classes_to_save = new Vector();

	primitiveSerialization = new Hashtable();

	primitiveSerialization.put(Type.BOOLEAN, new SerializationInfo("writeBoolean", "readBoolean", Type.BOOLEAN, true));
	
	primitiveSerialization.put(Type.BYTE, new SerializationInfo("writeByte", "readByte", Type.BYTE, true));
	
	primitiveSerialization.put(Type.SHORT, new SerializationInfo("writeShort", "readShort", Type.SHORT, true));
	
	primitiveSerialization.put(Type.CHAR, new SerializationInfo("writeChar", "readChar", Type.CHAR, true));
	
	primitiveSerialization.put(Type.INT, new SerializationInfo("writeInt", "readInt", Type.INT, true));
	primitiveSerialization.put(Type.LONG, new SerializationInfo("writeLong", "readLong", Type.LONG, true));

	primitiveSerialization.put(Type.FLOAT, new SerializationInfo("writeFloat", "readFloat", Type.FLOAT, true));

	primitiveSerialization.put(Type.DOUBLE, new SerializationInfo("writeDouble", "readDouble", Type.DOUBLE, true));
	primitiveSerialization.put(Type.STRING, new SerializationInfo("writeUTF", "readUTF", Type.STRING, true));

	referenceSerialization = new SerializationInfo("writeObject", "readObject", Type.OBJECT, false);
    }
    
    SerializationInfo getSerializationInfo(Type tp) { 
	SerializationInfo temp = (SerializationInfo) primitiveSerialization.get(tp);		
	return (temp == null ? referenceSerialization : temp);
    } 

    boolean isSerializable(JavaClass clazz) { 
	return Repository.implementationOf(clazz, "java.io.Serializable");
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
		    addRewriteClass(super_classes[i]);
		}
	    }
	}

	serializable |= isSerializable(clazz);

	if (serializable) {
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
						new Type[] {new ObjectType("ibis.io.MantaOutputStream")},
						new String[] { "os" },
						"generated_WriteObject",
						clazz.getClassName(),
						il,
						clazz.getConstantPool());

	write_method.addException("ibis.ipl.IbisIOException");
	clazz.addMethod(write_method.getMethod());

	/* Construct a read-of-the-stream constructor */
	il = new InstructionList();

	il.append(new RETURN());
	
	MethodGen read_cons = new MethodGen(Constants.ACC_PUBLIC,
					    Type.VOID,
					    new Type[] {new ObjectType("ibis.io.MantaInputStream")},
					    new String[] { "is" },
					    "<init>",
					    clazz.getClassName(),
					    il,
					    clazz.getConstantPool());
	read_cons.addException("ibis.ipl.IbisIOException");
	clazz.addMethod(read_cons.getMethod());
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
	
	ClassGen gen = new ClassGen(name, "ibis.io.Generator", name.substring(name.lastIndexOf('.')+1) + ".class", Constants.ACC_FINAL|Constants.ACC_PUBLIC|Constants.ACC_SUPER, null);
	InstructionFactory factory = new InstructionFactory(gen);

	InstructionList il = new InstructionList();
	
	il.append(factory.createNew(class_type));
	il.append(new DUP());
	il.append(new ALOAD(1));
	il.append(factory.createInvoke(clazz.getClassName(),
				       "<init>",
				       Type.VOID,
				       new Type [] { new ObjectType("ibis.io.MantaInputStream") },
				       Constants.INVOKESPECIAL));
	il.append(new ARETURN());

	/*
	  0       new DITree
	  3       dup
	  4       aload_1
	  5       invokespecial DITree(ibis.io.MantaInputStream)
	  8       areturn
	*/
	
	MethodGen method = new MethodGen(Constants.ACC_FINAL | Constants.ACC_PUBLIC,
					 Type.OBJECT,
					 new Type[] {new ObjectType("ibis.io.MantaInputStream")},
					 new String[] { "is" },
					 "generated_newInstance",
					 name,
					 il,
					 gen.getConstantPool());

	method.setMaxStack(3);
	method.setMaxLocals();
	method.addException("ibis.ipl.IbisIOException");
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

    void generateCode(JavaClass clazz) { 

	if (verbose) System.out.println("  Generating method code class for class : " + clazz.getClassName());

	if(clazz.isInterface()) return;

	String super_class_name = clazz.getSuperclassName();
	JavaClass super_clazz = Repository.lookupClass(super_class_name);

	boolean super_is_serializable = isSerializable(super_clazz);

	/* Generate code inside the methods */ 

	ClassGen cg = new ClassGen(clazz);

	Field[] fields = cg.getFields();
	
	if (verbose) System.out.println("    Number of fields " + fields.length);
	
	Method[] class_methods = cg.getMethods();
	int write_method_index = -1;
	int read_cons_index = -1;

	for (int i = 0; i < class_methods.length; i++) {
//	    System.out.println("name: " + class_methods[i].getName() + ", signature: " + class_methods[i].getSignature());
	    if (class_methods[i].getName().equals("generated_WriteObject") &&
		class_methods[i].getSignature().equals("(Libis/io/MantaOutputStream;)V")) {
		write_method_index = i;
	    }
	    else if (class_methods[i].getName().equals("<init>") &&
		class_methods[i].getSignature().equals("(Libis/io/MantaInputStream;)V")) {
		read_cons_index = i;
	    }
	}

	InstructionList write_il = new InstructionList();

	InstructionList read_cons_il = new InstructionList();

	InstructionFactory factory = new InstructionFactory(cg);
	
	/* Generate the code to write all the data. Note that the code is generated in reverse order */

	/* first handle the reference fields */

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
		        (field_class == null ||
			 ! (Repository.implementationOf(field_class, "java.rmi.Remote") ||
			    Repository.implementationOf(field_class, "ibis.rmi.Remote"))) &&
			!field_type.getSignature().startsWith("Ljava/")) {

			read_cons_il.append(new ALOAD(1));
			read_cons_il.append(factory.createInvoke("ibis.io.MantaInputStream",
						       "readKnownTypeHeader",
						       Type.INT,
						       Type.NO_ARGS,
						       Constants.INVOKEVIRTUAL));
			read_cons_il.append(new ISTORE(2));
			read_cons_il.append(new ILOAD(2));
			read_cons_il.append(new ICONST(-1));

			IF_ICMPNE ifcmp  = new IF_ICMPNE(null);
			read_cons_il.append(ifcmp);

			read_cons_il.append(new ALOAD(0));

			read_cons_il.append(factory.createNew((ObjectType)field_type));
			read_cons_il.append(new DUP());
			read_cons_il.append(new ALOAD(1));
			read_cons_il.append(factory.createInvoke(field_class.getClassName(),
						       "<init>",
						       Type.VOID,
						       new Type[] {new ObjectType("ibis.io.MantaInputStream")},
						       Constants.INVOKESPECIAL));
			read_cons_il.append(factory.createFieldAccess(clazz.getClassName(),
							    field.getName(),
							    field_type,
							    Constants.PUTFIELD));

			GOTO gto  = new GOTO(null);
			read_cons_il.append(gto);

		        InstructionHandle cmp_goto = read_cons_il.append(new ILOAD(2));
			ifcmp.setTarget(cmp_goto);

			read_cons_il.append(new ICONST(0));

			IF_ICMPEQ ifcmpeq = new IF_ICMPEQ(null);
			read_cons_il.append(ifcmpeq);
			read_cons_il.append(new ALOAD(0));
			read_cons_il.append(new ALOAD(1));
			read_cons_il.append(new ILOAD(2));
			read_cons_il.append(factory.createInvoke("ibis.io.MantaInputStream",
						       "getObjectFromCycleCheck",
						       Type.OBJECT,
						       new Type[] { Type.INT },
						       Constants.INVOKEVIRTUAL));
			
			read_cons_il.append(factory.createCheckCast((ObjectType)field_type));
			read_cons_il.append(factory.createFieldAccess(clazz.getClassName(),
							    field.getName(),
							    field_type,
							    Constants.PUTFIELD));

			InstructionHandle target = read_cons_il.append(new NOP());
			ifcmpeq.setTarget(target);
			gto.setTarget(target);

			/* write part: */

			write_il.append(new ALOAD(1));
			write_il.append(new ALOAD(0));
			write_il.append(factory.createFieldAccess(clazz.getClassName(),
							    field.getName(),
							    field_type,
							    Constants.GETFIELD));
			write_il.append(factory.createInvoke("ibis.io.MantaOutputStream",
						       "writeKnownObjectHeader",
						       Type.INT,
						       new Type[] { Type.OBJECT },
						       Constants.INVOKEVIRTUAL));
		        write_il.append(new ISTORE(2));
			write_il.append(new ILOAD(2));
			write_il.append(new ICONST(1));

			ifcmp = new IF_ICMPNE(null);
			write_il.append(ifcmp);

			write_il.append(new ALOAD(0));
			write_il.append(factory.createFieldAccess(clazz.getClassName(),
							    field.getName(),
							    field_type,
							    Constants.GETFIELD));
			write_il.append(new ALOAD(1));

			write_il.append(factory.createInvoke(field_class.getClassName(),
						       "generated_WriteObject",
						       Type.VOID,
						       new Type[] {new ObjectType("ibis.io.MantaOutputStream")},
						       Constants.INVOKEVIRTUAL));

			target = write_il.append(new NOP());
			ifcmp.setTarget(target);
			
		    } else { 
			SerializationInfo info = getSerializationInfo(field_type);				
			write_il.append(info.writeInstructions(clazz.getClassName(), field, factory));		
			read_cons_il.append(info.readInstructions(clazz.getClassName(), field, factory));		
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
		    write_il.append(info.writeInstructions(clazz.getClassName(), field, factory));		
		    read_cons_il.append(info.readInstructions(clazz.getClassName(), field, factory));		
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
		    write_il.append(info.writeInstructions(clazz.getClassName(), field, factory));		
		    read_cons_il.append(info.readInstructions(clazz.getClassName(), field, factory));		
		}
	    }
	} 			
	
	/* finally write/read the superclass if neccecary */
	
	if (super_is_serializable) { 
// @@@ Hier een geval vergeten: super is wel serializable, maar niet herschreven, plese fix :-)  --Rob
	    InstructionList temp = new InstructionList();

	    temp.append(new ALOAD(0));
	    temp.append(new ALOAD(1));
	    temp.append(factory.createInvoke(super_clazz.getClassName(),
					     "generated_WriteObject",
					     Type.VOID,
					     new Type[] {new ObjectType("ibis.io.MantaOutputStream")},
					     Constants.INVOKESPECIAL));

	    write_il.insert(temp);	

	    temp = new InstructionList();

	    temp.append(new ALOAD(0));
	    temp.append(new ALOAD(1));
	    temp.append(factory.createInvoke(super_clazz.getClassName(),
					     "<init>",
					     Type.VOID,
					     new Type[] {new ObjectType("ibis.io.MantaInputStream")},
					     Constants.INVOKESPECIAL));

	    read_cons_il.insert(temp);
	} else {
	    InstructionList temp = new InstructionList();

	    temp.append(new ALOAD(0));
	    temp.append(factory.createInvoke(super_clazz.getClassName(),
					     "<init>",
					     Type.VOID,
					     Type.NO_ARGS,
					     Constants.INVOKESPECIAL));
	    temp.append(new ALOAD(1));
	    temp.append(new ALOAD(0));

	    temp.append(factory.createInvoke("ibis.io.MantaInputStream",
					     "addObjectToCycleCheck",
					     Type.VOID,
					     new Type[] {Type.OBJECT},
					     Constants.INVOKEVIRTUAL));
	    read_cons_il.insert(temp);
	} 


	MethodGen read_cons_gen = new MethodGen(class_methods[read_cons_index], clazz.getClassName(), cg.getConstantPool());
	read_cons_il.append(read_cons_gen.getInstructionList());
	read_cons_gen.setInstructionList(read_cons_il);

	read_cons_gen.setMaxStack(read_cons_gen.getMaxStack(cg.getConstantPool(), read_cons_il, read_cons_gen.getExceptionHandlers()));
	read_cons_gen.setMaxLocals();

	cg.setMethodAt(read_cons_gen.getMethod(), read_cons_index);
	
	MethodGen write_gen = new MethodGen(class_methods[write_method_index], clazz.getClassName(), cg.getConstantPool());
	write_il.append(write_gen.getInstructionList());
	write_gen.setInstructionList(write_il);

	write_gen.setMaxStack(write_gen.getMaxStack(cg.getConstantPool(), write_il, write_gen.getExceptionHandlers()));
	write_gen.setMaxLocals();

	cg.setMethodAt(write_gen.getMethod(), write_method_index);

	clazz = cg.getJavaClass();

	JavaClass gen = generate_InstanceGenerator(clazz);

	Repository.addClass(gen);

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

	    if (! directImplementationOf(clazz, "ibis.io.Serializable")) {
		addClass(clazz);
	    } else { 
		if (verbose) System.out.println(clazz.getClassName() + " already implements ibis.io.Serializable");
	    }
	}

	if (verbose) System.out.println("Preparing classes");

	for (int i=0;i<classes_to_rewrite.size();i++) { 
	    JavaClass clazz = (JavaClass)classes_to_rewrite.get(i);
	    addReferencesToRewrite(clazz);
	    ClassGen clazzgen = new ClassGen(clazz);
	    generateMethods(clazzgen);
	    JavaClass newclazz = clazzgen.getJavaClass();
	    if (target_classes.remove(clazz)) {
		target_classes.add(newclazz);
	    }
	    if (classes_to_save.remove(clazz)) {
		classes_to_save.add(newclazz);
	    }
	    
	}

	if (verbose) System.out.println("Rewriting classes");

	for (int i=0;i<target_classes.size();i++) { 
	    JavaClass clazz = (JavaClass)target_classes.get(i);
	    generateCode(clazz);
	}

	if (verbose) System.out.println("Saving classes");

	for (int i=0;i<classes_to_save.size();i++) { 
	    JavaClass clazz = (JavaClass)classes_to_save.get(i);
	    String cl = clazz.getClassName();

	    int index = cl.lastIndexOf('.');
	    String classfile = cl.substring(index+1) + ".class";

	    if (verbose) System.out.println("  Saving class : " + classfile);
	    try {
		if(local) {
		    clazz.dump(classfile);
		} else {
		    String pkg = clazz.getPackageName();
		    if (pkg != null) {
			pkg.replace('.', '/');
			classfile = pkg + "/" + classfile;
		    }
		    clazz.dump(classfile);
		}
	    } catch (IOException e) {
		System.err.println("got exception while writing " + classfile + ": " + e);
		System.exit(1);
	    }
	}
    }

    private static void usage() {
	System.out.println("Usage : java IOGenerator [-dir] [-package <package>] [-v] " +
		   "<fully qualified classname list | classfiles>");
	System.exit(1);
    }

    public static void main(String[] args) throws IOException {
	boolean verbose = false;
	boolean local = true;
	boolean file = false;
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
	    } else if (args[i].equals("-file")) {
		file = true;
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

	new IOGenerator(verbose, local, file, newArgs, newArgs.length, pack).scanClass(newArgs, newArgs.length);
    }
}
