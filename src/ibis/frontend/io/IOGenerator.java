package ibis.frontend.io;

import java.util.*;
import java.util.jar.*;
import java.io.*;
import com.ibm.jikesbt.*;   

public class IOGenerator {

	class SerializationInfo { 

		BT_Class clazz;
		BT_Method writeMethod;
		BT_Method readMethod;
		boolean primitive;
		
		SerializationInfo(BT_Class clazz, BT_Method writeMethod, BT_Method readMethod, boolean primitive) { 
			this.clazz = clazz;
			this.writeMethod = writeMethod;
			this.readMethod  = readMethod;
			this.primitive = primitive;
		} 		

		BT_Ins [] writeInstructions(BT_Field field) { 
			
			BT_Ins temp[] = { 
				BT_Ins.make(BT_Opcodes.opc_aload_1), 
				BT_Ins.make(BT_Opcodes.opc_aload_0), 
				BT_Ins.make(BT_Opcodes.opc_getfield, field), 
				BT_Ins.make(BT_Opcodes.opc_invokevirtual, writeMethod)
			};
					
			return temp;
		}

		BT_Ins [] readInstructions(BT_Field field) { 

			if (primitive) { 			
				BT_Ins temp[] = { 
					BT_Ins.make(BT_Opcodes.opc_aload_0), 
					BT_Ins.make(BT_Opcodes.opc_aload_1), 
					BT_Ins.make(BT_Opcodes.opc_invokevirtual, readMethod),
					BT_Ins.make(BT_Opcodes.opc_putfield, field)
				};
				
				return temp;
			} else { 
				BT_Ins temp[] = { 
					BT_Ins.make(BT_Opcodes.opc_aload_0), 
					BT_Ins.make(BT_Opcodes.opc_aload_1), 
					BT_Ins.make(BT_Opcodes.opc_invokevirtual, readMethod),
					BT_Ins.make(BT_Opcodes.opc_checkcast, field.type),				       
					BT_Ins.make(BT_Opcodes.opc_putfield, field) 
				};
				
				return temp;
			} 
		}
	}

	static boolean verbose = false;

	Hashtable primitiveSerialization;
	SerializationInfo referenceSerialization;

	BT_Class interface_MantaSerializable;
	BT_Class abstract_MantaGenerator;

	BT_Method addObjectToCycleCheck_read_method;
	BT_Method getObjectFromCycleCheck_read_method;
	BT_Method readKnownTypeHeader_read_method;
	BT_Method writeKnownObjectHeader_write_method;

	Vector classes_to_rewrite, target_classes, classes_to_save;

	IOGenerator() { 
		BT_Class clazz;
		BT_Method writeMethod;
		BT_Method readMethod;
		SerializationInfo info;
		
		classes_to_rewrite = new Vector();
		target_classes = new Vector();
		classes_to_save = new Vector();

		primitiveSerialization = new Hashtable();

		clazz = BT_Class.getBoolean();
		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeBoolean", "(boolean)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readBoolean", "()");
		primitiveSerialization.put(clazz, new SerializationInfo(clazz, writeMethod, readMethod, true));
		
		clazz = BT_Class.getByte();
		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeByte", "(int)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readByte", "()");
		primitiveSerialization.put(clazz, new SerializationInfo(clazz, writeMethod, readMethod, true));
		
		clazz = BT_Class.getShort();
		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeShort", "(int)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readShort", "()");
		primitiveSerialization.put(clazz, new SerializationInfo(clazz, writeMethod, readMethod, true));
		
		clazz = BT_Class.getChar();
		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeChar", "(int)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readChar", "()");
		primitiveSerialization.put(clazz, new SerializationInfo(clazz, writeMethod, readMethod, true));
		
		clazz = BT_Class.getInt();
		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeInt", "(int)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readInt", "()");
		primitiveSerialization.put(clazz, new SerializationInfo(clazz, writeMethod, readMethod, true));

		clazz = BT_Class.getLong();
		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeLong", "(long)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readLong", "()");
		primitiveSerialization.put(clazz, new SerializationInfo(clazz, writeMethod, readMethod, true));

		clazz = BT_Class.getFloat();
		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeFloat", "(float)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readFloat", "()");
		primitiveSerialization.put(clazz, new SerializationInfo(clazz, writeMethod, readMethod, true));

		clazz = BT_Class.getDouble();
		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeDouble", "(double)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readDouble", "()");
		primitiveSerialization.put(clazz, new SerializationInfo(clazz, writeMethod, readMethod, true));

		clazz = BT_Class.forName("java.lang.String");	
		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeUTF", "(java.lang.String)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readUTF", "()");
		primitiveSerialization.put(clazz, new SerializationInfo(clazz, writeMethod, readMethod, true));

		writeMethod = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeObject", "(java.lang.Object)");
		readMethod  = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readObject", "()");
		referenceSerialization = new SerializationInfo(null, writeMethod, readMethod, false);

		interface_MantaSerializable = BT_Class.forName("ibis.io.Serializable");	
		abstract_MantaGenerator = BT_Class.forName("ibis.io.Generator");	

		addObjectToCycleCheck_read_method = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "addObjectToCycleCheck", "(java.lang.Object)");
		getObjectFromCycleCheck_read_method = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "getObjectFromCycleCheck", "(int)");
		readKnownTypeHeader_read_method = BT_Repository.findMethodWithArgs("ibis.io.MantaInputStream", "readKnownTypeHeader", "()");
		
		writeKnownObjectHeader_write_method = BT_Repository.findMethodWithArgs("ibis.io.MantaOutputStream", "writeKnownObjectHeader", "(java.lang.Object)"); 
	}
	
	SerializationInfo getSerializationInfo(BT_Class clazz) { 
		SerializationInfo temp = (SerializationInfo) primitiveSerialization.get(clazz);		
		return (temp == null ? referenceSerialization : temp);
	} 

	boolean isSerializable(BT_Class clazz) { 
		return (clazz.getParents().findClass("java.io.Serializable") != null); 
	} 

	boolean recursiveIsSerializable(BT_Class clazz) { 

		if (!isSerializable(clazz)) { 
			BT_Class super_clazz = clazz.getSuperClass();
			return ((super_clazz == null) ? false : recursiveIsSerializable(super_clazz));
		} else {
			return true;
		}
	}

	void addTargetClass(BT_Class clazz) { 
		if (!target_classes.contains(clazz)) { 
			target_classes.add(clazz);
			if (verbose) System.out.println("Adding target class : " + clazz.fullName());
		}
	} 

	void addRewriteClass(BT_Class clazz) { 
		if (!classes_to_rewrite.contains(clazz)) { 
			classes_to_rewrite.add(clazz);
			if (verbose) System.out.println("Adding rewrite class : " + clazz.fullName());
		}
	}

	boolean addClass(BT_Class clazz, boolean isTarget) { 
		BT_Class super_clazz = clazz.getSuperClass();
		boolean serializable = false;

		if (super_clazz != null) {
			serializable = addClass(super_clazz, false);
		} 

		serializable |= isSerializable(clazz);


		if (serializable) {
			addRewriteClass(clazz);
			if(isTarget) {
				addTargetClass(clazz);
			}
		} 

		return serializable;
	} 

	void addReferencesToRewrite(BT_Class clazz) { 

		/* Find all references to final reference types and add these to the rewrite list */
		BT_FieldVector fields = clazz.getFields();

		for (int i=0;i<fields.size();i++) { 
			BT_Field field = fields.elementAt(i);
			
			/* Don't send fields that are STATIC or TRANSIENT */
			if ((field.flags & (BT_Item.STATIC | BT_Item.TRANSIENT)) == 0) { 
				BT_Class field_type = field.type;       		

				if (!field_type.isBasicTypeClass && 
				    !field_type.fullName().equals("java.lang.String") && 	       
				    field_type.isFinal()) { 
					addRewriteClass(field_type);
				}
			}
		} 			
	}

	void generateMethods(BT_Class clazz) { 

		/* Generate the necessary (empty) methods. */

		if (verbose) System.out.println("  Generating empty methods for class : " + clazz.fullName());

		if (verbose) System.out.println("    " + clazz.className() + " implements java.io.Serializable -> adding ibis.io.Serializable");		
		/* add the ibis.io.Serializable interface to the class */
		clazz.attachParent(interface_MantaSerializable);
		
		short flags = BT_Method.PUBLIC;
		
		if (clazz.isFinal()) { 
			flags = (short) (flags + BT_Method.FINAL);
		}
			
		/* Construct a write method */
		BT_Ins write_ins[] = { 
			BT_Ins.make(BT_Opcodes.opc_return) 
		};
		
		BT_Method write_method = new BT_Method(clazz, flags, "void", "generated_WriteObject", "(ibis.io.MantaOutputStream)", new BT_CodeAttribute(write_ins));
		BT_ClassVector class_vector = new BT_ClassVector();
		class_vector.addUnique(BT_Class.forName("ibis.ipl.IbisIOException"));
		write_method.attributes.addElement(new BT_ExceptionsAttribute(class_vector));
		
//		/* Construct a read method */
//		BT_Ins read_ins[] = { 
//			BT_Ins.make(BT_Opcodes.opc_return) 
//		};
		
//		BT_Method read_method = new BT_Method(clazz, flags, "void", "generated_ReadObject", "(ibis.io.MantaInputStream)", new BT_CodeAttribute(read_ins));
//		class_vector = new BT_ClassVector();
//		class_vector.addUnique(BT_Class.forName("java.io.IOException"));
//		class_vector.addUnique(BT_Class.forName("java.lang.ClassNotFoundException"));
//		read_method.attributes.addElement(new BT_ExceptionsAttribute(class_vector));
			
		/* Construct a read-of-the-stream constructor */
		BT_Ins read_cons_ins[] = { 
			BT_Ins.make(BT_Opcodes.opc_return) 
		};
		
		BT_Method read_cons = new BT_Method(clazz, BT_Method.PUBLIC, "void", "<init>", "(ibis.io.MantaInputStream)", new BT_CodeAttribute(read_cons_ins));
		class_vector = new BT_ClassVector();
		class_vector.addUnique(BT_Class.forName("ibis.ipl.IbisIOException"));
		class_vector.addUnique(BT_Class.forName("java.lang.ClassNotFoundException"));
		read_cons.attributes.addElement(new BT_ExceptionsAttribute(class_vector));
	}

	BT_Class generate_InstanceGenerator(BT_Class clazz, BT_Method read_constructor) { 

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
			   
		if (verbose) System.out.println("  Generating InstanceGenerator class for " + clazz.className());

		String name = clazz.useName() + "_ibis_io_Generator";
		
		BT_Class gen = new BT_Class(name, (short) (BT_Class.PUBLIC + BT_Class.FINAL));
		
		/*
		  0       new DITree
		  3       dup
		  4       aload_1
		  5       invokespecial DITree(ibis.io.MantaInputStream)
		  8       areturn
		*/
		
		BT_Ins method_ins[] = { 
			BT_Ins.make(BT_Opcodes.opc_new, clazz), 
			BT_Ins.make(BT_Opcodes.opc_dup), 			
			BT_Ins.make(BT_Opcodes.opc_aload_1), 			
			BT_Ins.make(BT_Opcodes.opc_invokespecial, read_constructor),
			BT_Ins.make(BT_Opcodes.opc_areturn) 
		};
		
		BT_Method method = new BT_Method(gen, (short)(BT_Method.PUBLIC + BT_Method.FINAL), "java.lang.Object", "generated_newInstance", "(ibis.io.MantaInputStream)", new BT_CodeAttribute(method_ins));
		BT_ClassVector class_vector = new BT_ClassVector();
		class_vector.addUnique(BT_Class.forName("ibis.ipl.IbisIOException"));
		class_vector.addUnique(BT_Class.forName("java.lang.ClassNotFoundException"));
		method.attributes.addElement(new BT_ExceptionsAttribute(class_vector));
		
		gen.resetSuperClass(abstract_MantaGenerator);		
		
		BT_Method super_cons = abstract_MantaGenerator.findMethod("void", "<init>", "()");
		
		BT_Ins cons_ins[] = { 
			BT_Ins.make(BT_Opcodes.opc_aload_0), 
			BT_Ins.make(BT_Opcodes.opc_invokespecial, super_cons),
			BT_Ins.make(BT_Opcodes.opc_return)
		};
		
		BT_Method cons = new BT_Method(gen, (short)BT_Method.PUBLIC, "void", "<init>", "()", new BT_CodeAttribute(cons_ins));
		
		return gen;
	} 

	void generateCode(BT_Class clazz) { 

		if (verbose) System.out.println("  Generating method code class for class : " + clazz.className());

		BT_Class super_clazz = clazz.getSuperClass();

		boolean super_is_serializable = recursiveIsSerializable(super_clazz);		

		/* Generate code inside te methods */ 

		BT_FieldVector fields = clazz.getFields();
		
		if (verbose) System.out.println("    Number of fields " + fields.size());
		
		BT_Method write_method = clazz.findMethod("void", "generated_WriteObject", "(ibis.io.MantaOutputStream)");
		BT_CodeAttribute write_code = write_method.getCode();

//		BT_Method read_method = clazz.findMethod("void", "generated_ReadObject", "(ibis.io.MantaInputStream)");
//		BT_CodeAttribute read_code = read_method.getCode();
		
		BT_Method read_cons = clazz.findMethod("void", "<init>", "(ibis.io.MantaInputStream)");
		BT_CodeAttribute read_cons_code = read_cons.getCode();
		
		/* Generate the code to write all the data. Note that the code is generated in reverse order */

		/* first handle the reference fields */

		for (int i=0;i<fields.size();i++) { 
			BT_Field field = fields.elementAt(i);
			
			/* Don't send fields that are STATIC or TRANSIENT */
			if ((field.flags & (BT_Item.STATIC | BT_Item.TRANSIENT)) == 0) { 
				BT_Class field_type = field.type;       		

				if (!field_type.isBasicTypeClass && !field_type.fullName().equals("java.lang.String")) { 	       
					if (verbose) System.out.println("    writing reference field " + field.fullName() + " of type " + field_type.fullName());

					if (field_type.isFinal() && !field_type.fullName().startsWith("java.")) { 

						BT_Method rec_read = field_type.findMethod("<init>", "(ibis.io.MantaInputStream)");
											
						BT_Ins direct_read[] = { 
							/*  0 */ BT_Ins.make(BT_Opcodes.opc_aload_1), 
							/*  1 */ BT_Ins.make(BT_Opcodes.opc_invokevirtual, readKnownTypeHeader_read_method),							
							/*  2 */ BT_Ins.make(BT_Opcodes.opc_istore_2), 
							/*  3 */ BT_Ins.make(BT_Opcodes.opc_iload_2), 
							/*  4 */ BT_Ins.make(BT_Opcodes.opc_iconst_m1), 
							/*  5 */ null, /* if_icmpne 13 */
							/*  6 */ BT_Ins.make(BT_Opcodes.opc_aload_0),
							/*  7 */ BT_Ins.make(BT_Opcodes.opc_new, field_type),
							/*  8 */ BT_Ins.make(BT_Opcodes.opc_dup),
							/*  9 */ BT_Ins.make(BT_Opcodes.opc_aload_1),
							/* 10 */ BT_Ins.make(BT_Opcodes.opc_invokespecial, rec_read),
							/* 11 */ BT_Ins.make(BT_Opcodes.opc_putfield, field),
							/* 12 */ null, /* goto 22 */
							/* 13 */ BT_Ins.make(BT_Opcodes.opc_iload_2),
							/* 14 */ BT_Ins.make(BT_Opcodes.opc_iconst_0),
							/* 15 */ null, /* if_icpne 22 */
							/* 16 */ BT_Ins.make(BT_Opcodes.opc_aload_0),
							/* 17 */ BT_Ins.make(BT_Opcodes.opc_aload_1),
							/* 18 */ BT_Ins.make(BT_Opcodes.opc_iload_2),
							/* 19 */ BT_Ins.make(BT_Opcodes.opc_invokevirtual, getObjectFromCycleCheck_read_method),
							/* 20 */ BT_Ins.make(BT_Opcodes.opc_checkcast, field_type),
							/* 21 */ BT_Ins.make(BT_Opcodes.opc_putfield, field),
							/* 22 */ BT_Ins.make(BT_Opcodes.opc_nop) /* destination for the last jump */						
						};

						direct_read[5]  = new BT_JumpOffsetIns(BT_Opcodes.opc_if_icmpne, -1, direct_read[13]);
						direct_read[12] = new BT_JumpOffsetIns(BT_Opcodes.opc_goto, -1, direct_read[22]);
						direct_read[15] = new BT_JumpOffsetIns(BT_Opcodes.opc_if_icmpeq, -1, direct_read[22]);
						
						read_cons_code.insertInstructionsAt(direct_read, 0);
						
						BT_Method rec_write = field_type.findMethod("generated_WriteObject", "(ibis.io.MantaOutputStream)");
					
						BT_Ins direct_write[] = { 
							/*  0 */ BT_Ins.make(BT_Opcodes.opc_aload_1), 
							/*  1 */ BT_Ins.make(BT_Opcodes.opc_aload_0),							
							/*  2 */ BT_Ins.make(BT_Opcodes.opc_getfield, field), 
							/*  3 */ BT_Ins.make(BT_Opcodes.opc_invokevirtual, writeKnownObjectHeader_write_method), 
							/*  4 */ BT_Ins.make(BT_Opcodes.opc_istore_2), 
							/*  5 */ BT_Ins.make(BT_Opcodes.opc_iload_2), 
							/*  6 */ BT_Ins.make(BT_Opcodes.opc_iconst_1),
							/*  7 */ null, /* if_icmpne */
							/*  8 */ BT_Ins.make(BT_Opcodes.opc_aload_0),
							/*  9 */ BT_Ins.make(BT_Opcodes.opc_getfield, field),
							/* 10 */ BT_Ins.make(BT_Opcodes.opc_aload_1), 
							/* 11 */ BT_Ins.make(BT_Opcodes.opc_invokevirtual, rec_write),
							/* 12 */ BT_Ins.make(BT_Opcodes.opc_nop) /* destination for the last jump */						
						};

						direct_write[7] = new BT_JumpOffsetIns(BT_Opcodes.opc_if_icmpne, -1, direct_write[12]);

						write_code.insertInstructionsAt(direct_write, 0);

					} else { 
						SerializationInfo info = getSerializationInfo(field_type);				
						write_code.insertInstructionsAt(info.writeInstructions(field), 0);		
						//read_code.insertInstructionsAt(info.readInstructions(field), 0);		
						read_cons_code.insertInstructionsAt(info.readInstructions(field), 0);		
					}
				}
			}
		} 			

		/* then handle Strings */

		for (int i=0;i<fields.size();i++) { 
			BT_Field field = fields.elementAt(i);
			
			/* Don't send fields that are STATIC or TRANSIENT  */
			if ((field.flags & (BT_Item.STATIC | BT_Item.TRANSIENT)) == 0) { 
				BT_Class field_type = field.type;       		

				if (field_type.fullName().equals("java.lang.String")) { 	       

					if (verbose) System.out.println("    writing reference field " + field.fullName() + " of type " + field_type.fullName());

					SerializationInfo info = getSerializationInfo(field_type);				
					write_code.insertInstructionsAt(info.writeInstructions(field), 0);		
					//read_code.insertInstructionsAt(info.readInstructions(field), 0);		
					read_cons_code.insertInstructionsAt(info.readInstructions(field), 0);		
				}
			}
		} 			
	
		/* then handle the primitive fields */

		for (int i=0;i<fields.size();i++) { 
			BT_Field field = fields.elementAt(i);
			
			/* Don't send fields that are STATIC, or TRANSIENT */
			if ((field.flags & (BT_Item.STATIC | BT_Item.TRANSIENT)) == 0) { 
				BT_Class field_type = field.type;       		

				if (field_type.isBasicTypeClass) { 	       
					if (verbose) System.out.println("    writing reference field " + field.fullName() + " of type " + field_type.fullName());

					SerializationInfo info = getSerializationInfo(field_type);				
					write_code.insertInstructionsAt(info.writeInstructions(field), 0);		
					//read_code.insertInstructionsAt(info.readInstructions(field), 0);		
					read_cons_code.insertInstructionsAt(info.readInstructions(field), 0);		
				}
			}
		} 			
		
		/* finally write/read the superclass if neccecary */
		
		if (super_is_serializable) { 

			BT_Method super_write = super_clazz.findMethod("void", "generated_WriteObject", "(ibis.io.MantaOutputStream)");
			
			BT_Ins temp_write[] = { 
				BT_Ins.make(BT_Opcodes.opc_aload_0), 
				BT_Ins.make(BT_Opcodes.opc_aload_1), 
				BT_Ins.make(BT_Opcodes.opc_invokespecial, super_write),
			};

			write_code.insertInstructionsAt(temp_write, 0);	

//			BT_Method super_read = super_clazz.findMethod("void", "generated_ReadObject", "(ibis.io.MantaInputStream)");
//			
//			BT_Ins temp_read[] = { 
//				BT_Ins.make(BT_Opcodes.opc_aload_0), 
//				BT_Ins.make(BT_Opcodes.opc_aload_1), 
//				BT_Ins.make(BT_Opcodes.opc_invokespecial, super_read),
//			};
//
//			read_code.insertInstructionsAt(temp_read, 0);

			BT_Method super_read_cons = super_clazz.findMethod("void", "<init>", "(ibis.io.MantaInputStream)");

			BT_Ins temp_cons_read[] = { 
				BT_Ins.make(BT_Opcodes.opc_aload_0), 
				BT_Ins.make(BT_Opcodes.opc_aload_1), 
				BT_Ins.make(BT_Opcodes.opc_invokespecial, super_read_cons),
			};

			read_cons_code.insertInstructionsAt(temp_cons_read, 0);
		} else {
			BT_Method super_cons = super_clazz.findMethod("void", "<init>", "()");

			BT_Ins temp_cons[] = { 
				BT_Ins.make(BT_Opcodes.opc_aload_0), 
				BT_Ins.make(BT_Opcodes.opc_invokespecial, super_cons),
				BT_Ins.make(BT_Opcodes.opc_aload_1), 
				BT_Ins.make(BT_Opcodes.opc_aload_0), 
				BT_Ins.make(BT_Opcodes.opc_invokevirtual, addObjectToCycleCheck_read_method),
			};

			read_cons_code.insertInstructionsAt(temp_cons, 0);
		} 

		BT_Class gen = generate_InstanceGenerator(clazz, read_cons);			
		classes_to_save.add(clazz);
		classes_to_save.add(gen);
	} 

	void scanClass(String [] classnames, int num) { 
		
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
			BT_Class clazz = (BT_Class)BT_Class.forName(classnames[i]);

			if (clazz.getParents().findClass("ibis.io.Serializable") == null) { 
				addClass(clazz, true);
			} else { 
				if (verbose) System.out.println(clazz.className() + " already implements ibis.io.Serializable");
			}
		}

		if (verbose) System.out.println("Preparing classes");

		for (int i=0;i<classes_to_rewrite.size();i++) { 
			BT_Class clazz = (BT_Class)classes_to_rewrite.get(i);
			addReferencesToRewrite(clazz);
			generateMethods(clazz);
		}

		if (verbose) System.out.println("Rewriting classes");

		for (int i=0;i<target_classes.size();i++) { 
			BT_Class clazz = (BT_Class)target_classes.get(i);
			generateCode(clazz);
		}

		if (verbose) System.out.println("Saving classes");

		for (int i=0;i<classes_to_save.size();i++) { 
			BT_Class clazz = (BT_Class)classes_to_save.get(i);
			if (verbose) System.out.println("  Saving class : " + clazz.className());
			clazz.write();
		}
	} 

	public static void main(String[] args) throws IOException {

		int num = 0;
		int size = args.length;

		if (args.length == 0) { 
			System.out.println("Usage : java IOGenerator [-v] <fully qualified classname list | classfiles>");
			System.exit(1);
		}

		for (int i=0;i<size;i++) { 
			if (args[i].equals("-v")) {
				verbose = true;
				args[i] = args[size-1];
				args[size-1] = null;
				size--;
			} 
				
			int index = args[i].lastIndexOf(".class");
			
			if (index != -1) { 
				args[i] = args[i].substring(0, index);
			}
			
			num++;
		}

		BT_Factory.factory = new MyFactory(args, num);
		new IOGenerator().scanClass(args, num);
	} 
}
