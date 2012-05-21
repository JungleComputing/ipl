/* $Id: CodeGenerator.java 14636 2012-04-12 18:01:26Z ceriel $ */

package ibis.io.rewriter;

import ibis.compile.ASMRepository;

import java.util.Collections;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;


/**
 * The CodeGenerator is responsible for generation of the actual bytecode
 * used at runtime to do serialization.
 */
class ASMCodeGenerator implements ASMRewriterConstants, Opcodes {

    protected ClassNode clazz;

    protected ClassNode super_class;

    protected boolean super_is_serializable;

    protected boolean super_is_ibis_serializable;

    protected boolean super_has_ibis_constructor;

    protected boolean is_externalizable;

    protected boolean has_serial_persistent_fields;

    protected boolean has_final_fields;

    protected List<FieldNode> fields;

    protected List<MethodNode> methods;
    
    protected List<String> interfaces;

    protected ASMIOGenerator generator;

    private boolean is_abstract;

    public static ClassNode lookupClass(String name) {
        try {
            return ASMRepository.findClass(name);
        } catch(ClassNotFoundException e) {
            System.err.println("Warning: class " + name + " not found");
            e.printStackTrace(System.err);
            return null;
        }
    }
    
    public static String getPackageName(String name) {
        if (name.contains("/")) {
            return name.substring(0, name.lastIndexOf('/'));
        } else if (name.contains(".")) {
            return name.substring(0, name.lastIndexOf('.'));
        } else {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    ASMCodeGenerator(ASMIOGenerator generator, ClassNode cl) {
        this.generator = generator;
        clazz = cl;
        if (clazz.superName != null) {
            super_class = lookupClass(clazz.superName);
        }
        methods = clazz.methods;
        fields = clazz.fields;
        interfaces = clazz.interfaces;
        versionUID();

        /* getFields() does not specify or guarantee a specific order.
         * Therefore, we sort the fields alphabetically, and the
         * serialization code in ibis.io should do the same.
         */
        Collections.sort(fields, ASMSerializationInfo.fieldComparator);

        super_is_serializable = ASMSerializationInfo.isSerializable(super_class);
        is_externalizable = ASMSerializationInfo.isExternalizable(cl);
        is_abstract = (cl.access & ACC_ABSTRACT) == ACC_ABSTRACT;
        super_is_ibis_serializable = ASMSerializationInfo.isIbisSerializable(super_class);
        super_has_ibis_constructor = ASMSerializationInfo.hasIbisConstructor(super_class);
        has_serial_persistent_fields = ASMSerializationInfo.hasSerialPersistentFields(fields);
        has_final_fields = ASMSerializationInfo.hasFinalFields(fields);
    }

    /**
     * Get the serialversionuid of a class that is about to be
     * rewritten. If necessary, a serialVersionUID field is added.
     */
    private void versionUID() {
        for (FieldNode f : fields) {
            if (f.name.equals(FIELD_SERIAL_VERSION_UID)
                    && (f.access & (ACC_FINAL | ACC_STATIC)) == (ACC_FINAL | ACC_STATIC)) {
                /* Already present. Just return. */
                return;
            }
        }

        long uid = 0;
        uid = ASMSerializationInfo.getSerialVersionUID(clazz.name, clazz);

        if (uid != 0) {
            fields.add(new FieldNode(ACC_PRIVATE|ACC_FINAL|ACC_STATIC,
                    FIELD_SERIAL_VERSION_UID, "J", null, new Long(uid)));
        }
    }

    private MethodInsnNode createGeneratedWriteObjectInvocation(String name,
            int invmode) {
        return new MethodInsnNode(invmode, name, METHOD_GENERATED_WRITE_OBJECT,
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_V);
    }

    private MethodInsnNode createGeneratedDefaultReadObjectInvocation(
            String cl, int invmode) {
        return new MethodInsnNode(invmode, cl, METHOD_GENERATED_DEFAULT_READ_OBJECT,
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_I_V);
    }

    private MethodInsnNode createInitInvocation(ClassNode cl) {
        return new MethodInsnNode(INVOKESPECIAL, cl.name, METHOD_INIT,
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V);
    }

    private MethodInsnNode createGeneratedDefaultWriteObjectInvocation(String name) {
        return new MethodInsnNode(INVOKESPECIAL, name, METHOD_GENERATED_DEFAULT_WRITE_OBJECT,
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_I_V);
    }

    private MethodInsnNode createWriteObjectInvocation() {
        return new MethodInsnNode(INVOKESPECIAL, clazz.name, METHOD_WRITE_OBJECT,
                SIGNATURE_LJAVA_IO_OBJECT_OUTPUT_STREAM_V);
    }

    private int getClassDepth(ClassNode cl) {
        if (!ASMSerializationInfo.isSerializable(cl)) {
            return 0;
        }
        return 1 + getClassDepth(lookupClass(cl.superName));
    }

    void generateEmptyMethods() {
        /* Generate the necessary (empty) methods. */

        if (generator.isVerbose()) {
            System.out.println("  Generating empty methods for class : "
                    + clazz.name);
            System.out.println("    " + clazz.name
                    + " implements java.io.Serializable -> adding "
                    + IBIS_IO_SERIALIZABLE);
        }

        /* add the ibis.io.Serializable interface to the class */
        interfaces.add(IBIS_IO_SERIALIZABLE);

        /* Construct a write method */
        InsnList il = new InsnList();
        il.add(new InsnNode(RETURN));
        int flags = ACC_PUBLIC | ((clazz.access & ACC_FINAL) == ACC_FINAL ? ACC_FINAL : 0);
        MethodNode writeMethod = new MethodNode(ASM4, flags, METHOD_GENERATED_WRITE_OBJECT, SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_V,
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_V, new String[] { JAVA_IO_IOEXCEPTION });
        writeMethod.instructions = il;
        methods.add(writeMethod);

        /* ... and a default_write_method */
        il = new InsnList();
        il.add(new InsnNode(RETURN));
        MethodNode defaultWriteMethod = new MethodNode(ASM4, flags, METHOD_GENERATED_DEFAULT_WRITE_OBJECT, SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_I_V,
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_I_V, new String[] { JAVA_IO_IOEXCEPTION });
        defaultWriteMethod.instructions = il;
        methods.add(defaultWriteMethod);

        /* ... and a default_read_method */
        il = new InsnList();
        il.add(new InsnNode(RETURN));
        MethodNode defaultReadMethod = new MethodNode(ASM4, flags, METHOD_GENERATED_DEFAULT_READ_OBJECT, SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_I_V,
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_I_V,
                new String[] { JAVA_IO_IOEXCEPTION, JAVA_LANG_CLASSNOTFOUNDEXCEPTION });
        defaultReadMethod.instructions = il;
        methods.add(defaultReadMethod);

        /* Construct a read-of-the-stream constructor, but only when we
         * can actually use it.
         */ 
        if (is_externalizable || !super_is_serializable
                || generator.forceGeneratedCalls() || super_has_ibis_constructor) {
            il = new InsnList();
            il.add(new InsnNode(RETURN));
            MethodNode readCons = new MethodNode(ASM4, ACC_PUBLIC, METHOD_INIT, SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V,
                    SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V,
                    new String[] { JAVA_IO_IOEXCEPTION, JAVA_LANG_CLASSNOTFOUNDEXCEPTION });
            readCons.instructions = il;
            methods.add(readCons);
        } else if (ASMSerializationInfo.hasReadObject(methods)) {
            il = new InsnList();
            il.add(new InsnNode(RETURN));
            MethodNode readObjectWrapper = new MethodNode(ASM4, ACC_PUBLIC, METHOD_$READ_OBJECT_WRAPPER$, SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V,
                    SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V,
                    new String[] { JAVA_IO_IOEXCEPTION, JAVA_LANG_CLASSNOTFOUNDEXCEPTION });
            readObjectWrapper.instructions = il;
            methods.add(readObjectWrapper);
        }

        generator.replace(clazz, clazz);
    }

    private InsnList writeInstructions(FieldNode field) {
        Type field_type = Type.getType(field.desc);
        ASMSerializationInfo info = ASMSerializationInfo.getSerializationInfo(field_type);

        InsnList temp = new InsnList();

        temp.add(new VarInsnNode(ALOAD, 1));
        temp.add(new VarInsnNode(ALOAD, 0));
        temp.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
        temp.add(new MethodInsnNode(INVOKEVIRTUAL, TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                info.write_name, Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(info.signature))));
        return temp;
    }

    private InsnList readInstructions(FieldNode field,
            boolean from_constructor) {
        Type field_type = Type.getType(field.desc);
        ASMSerializationInfo info = ASMSerializationInfo.getSerializationInfo(field_type);

        InsnList temp = new InsnList();
 
        if (from_constructor || (field.access & ACC_FINAL) == 0) {
            temp.add(new VarInsnNode(ALOAD, 0));
            temp.add(new VarInsnNode(ALOAD, 1));
            temp.add(new MethodInsnNode(INVOKEVIRTUAL, TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    info.read_name, Type.getMethodDescriptor(Type.getType(info.signature))));
            if (!info.primitive) {
                temp.add(new TypeInsnNode(CHECKCAST, field_type.getInternalName()));
            }
            temp.add(new FieldInsnNode(PUTFIELD, clazz.name, field.name, field.desc));
        } else {
            temp.add(new VarInsnNode(ALOAD, 1));
            temp.add(new VarInsnNode(ALOAD, 0));
            temp.add(new LdcInsnNode(field.name));
            temp.add(new LdcInsnNode(clazz.name.replaceAll("/", ".")));
            String methodDescr;
            if (!info.primitive) {
                temp.add(new LdcInsnNode(field.desc));
                methodDescr = Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT,
                        TYPE_STRING, TYPE_STRING, TYPE_STRING);
            } else {
                methodDescr = Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT,
                        TYPE_STRING, TYPE_STRING);
            }
            temp.add(new MethodInsnNode(INVOKEVIRTUAL, TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    info.final_read_name, methodDescr));
        }

        return temp;
    }

    private String writeCallName(String name) {
        return METHOD_WRITE_ARRAY + name.substring(0, 1).toUpperCase()
        + name.substring(1);
    }

    private InsnList writeReferenceField(FieldNode field) {
        Type field_type = Type.getType(field.desc);
        InsnList write_il = new InsnList();

        boolean isfinal = false;
        boolean isarray = false;
        ClassNode field_class = null;
        String basicname = null;

        if (generator.isVerbose()) {
            System.out.println("    writing reference field "
                    + field.name + " of type "
                    + field.desc);
        }
        Type tp = field_type;
        if (tp.getSort() == Type.ARRAY) {
            isarray = true;
            if (tp.getDimensions() > 1) {
                tp = Type.getType(tp.getDescriptor().substring(1));
            } else {
                tp = tp.getElementType();
            }
        }
        if (tp.getSort() == Type.OBJECT) {
            field_class = lookupClass(tp.getInternalName());
            if (field_class != null && (field_class.access & ACC_FINAL) == ACC_FINAL) {
                isfinal = true;
            }
        } else if (isarray) {
            switch(tp.getSort()) {
            case Type.BOOLEAN:
                basicname = "boolean";
                break;
            case Type.BYTE:
                basicname = "byte";
                break;
            case Type.SHORT:
                basicname = "short";
                break;
            case Type.CHAR:
                basicname = "char";
                break;
            case Type.INT:
                basicname = "int";
                break;
            case Type.FLOAT:
                basicname = "float";
                break;
            case Type.LONG:
                basicname = "long";
                break;
            case Type.DOUBLE:
                basicname = "double";
                break;
            }
        }

        if (basicname != null
                || (isfinal
                        && (ASMSerializationInfo.hasIbisConstructor(field_class)
                                || (ASMSerializationInfo.isSerializable(field_class) && generator.forceGeneratedCalls())
                           )
                   )) {
            // If there is an object replacer, we cannot do the
            // "fast" code.
            write_il.add(new InsnNode(ACONST_NULL));
            write_il.add(new VarInsnNode(ALOAD, 1));
            write_il.add(new FieldInsnNode(GETFIELD, TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                    "replacer", "Libis/io/Replacer;"));
            LabelNode target = new LabelNode();
            write_il.add(new JumpInsnNode(IF_ACMPEQ, target));
            write_il.add(writeInstructions(field));
            LabelNode end = new LabelNode();
            write_il.add(new JumpInsnNode(GOTO, end));
            write_il.add(target);
            write_il.add(new VarInsnNode(ALOAD, 1));
            write_il.add(new VarInsnNode(ALOAD, 0));
            write_il.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
            if (basicname != null) {
                write_il.add(new FieldInsnNode(GETSTATIC, IBIS_IO_CONSTANTS,
                        "TYPE_" + basicname.toUpperCase(), "I"));
                write_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_KNOWN_ARRAY_HEADER,
                        Type.getMethodDescriptor(Type.INT_TYPE, TYPE_OBJECT, Type.INT_TYPE)));
            } else {
                write_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_KNOWN_OBJECT_HEADER,
                        Type.getMethodDescriptor(Type.INT_TYPE, TYPE_OBJECT)));
            }
            write_il.add(new VarInsnNode(ISTORE, 2));
            write_il.add(new VarInsnNode(ILOAD, 2));
            write_il.add(new LdcInsnNode(1));
            target = new LabelNode();
            write_il.add(new JumpInsnNode(IF_ICMPNE, target));
            if (isarray) {
                write_il.add(new VarInsnNode(ALOAD, 1));
                write_il.add(new VarInsnNode(ALOAD, 0));
                write_il.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
                write_il.add(new InsnNode(ARRAYLENGTH));
                write_il.add(new InsnNode(DUP));
                write_il.add(new VarInsnNode(ISTORE, 4));
                write_il.add(new MethodInsnNode(INVOKEVIRTUAL, TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                                METHOD_WRITE_INT, Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));
                if (basicname != null) {
                    write_il.add(new VarInsnNode(ALOAD, 1));
                    write_il.add(new VarInsnNode(ALOAD, 0));
                    write_il.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
                    write_il.add(new LdcInsnNode(0));
                    write_il.add(new VarInsnNode(ILOAD, 4));
                    write_il.add(new MethodInsnNode(INVOKEVIRTUAL, TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                                    writeCallName(basicname), 
                                    Type.getMethodDescriptor(Type.VOID_TYPE, field_type, Type.INT_TYPE, Type.INT_TYPE)));
                } else {
                    write_il.add(new LdcInsnNode(0));
                    write_il.add(new VarInsnNode(ISTORE, 3));
                    LabelNode gto = new LabelNode();
                    write_il.add(new JumpInsnNode(GOTO, gto));
                    LabelNode loop_body_start = new LabelNode();
                    write_il.add(loop_body_start);
                    write_il.add(new VarInsnNode(ALOAD, 1));
                    write_il.add(new VarInsnNode(ALOAD, 0));
                    write_il.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
                    write_il.add(new VarInsnNode(ILOAD, 3));
                    write_il.add(new InsnNode(AALOAD));
                    write_il.add(new MethodInsnNode(INVOKEVIRTUAL, TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                                    METHOD_WRITE_KNOWN_OBJECT_HEADER,
                                    Type.getMethodDescriptor(Type.INT_TYPE, TYPE_OBJECT)));
                    write_il.add(new VarInsnNode(ISTORE, 2));
                    write_il.add(new VarInsnNode(ILOAD, 2));
                    write_il.add(new LdcInsnNode(1));
                    LabelNode ifcmp1 = new LabelNode();
                    write_il.add(new JumpInsnNode(IF_ICMPNE, ifcmp1));
                    write_il.add(new VarInsnNode(ALOAD, 0));
                    write_il.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
                    write_il.add(new VarInsnNode(ILOAD, 3));
                    write_il.add(new InsnNode(AALOAD));
                    write_il.add(new VarInsnNode(ALOAD, 1));
                    write_il.add(createGeneratedWriteObjectInvocation(field_class.name, INVOKEVIRTUAL));
                    write_il.add(ifcmp1);
                    write_il.add(new IincInsnNode(3, 1));
                    write_il.add(gto);
                    write_il.add(new VarInsnNode(ILOAD, 4));
                    write_il.add(new VarInsnNode(ILOAD, 3));
                    write_il.add(new JumpInsnNode(IF_ICMPGT, loop_body_start));
                }
            } else {
                write_il.add(new VarInsnNode(ALOAD, 0));
                write_il.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
                write_il.add(new VarInsnNode(ALOAD, 1));
                write_il.add(createGeneratedWriteObjectInvocation(
                        field_class.name, INVOKEVIRTUAL));
            }
            write_il.add(target);
            write_il.add(end);
        } else {
            write_il.add(writeInstructions(field));
        }
        return write_il;
    }

    @SuppressWarnings("unchecked")
    private InsnList serialPersistentWrites(MethodNode write_gen) {
        String serialPersistentFieldsSig = "[L" + JAVA_IO_OBJECTSTREAMFIELD + ";";
        InsnList write_il = new InsnList();
        int[] case_values = new int[] { CASE_BOOLEAN, CASE_CHAR, CASE_DOUBLE, CASE_FLOAT, CASE_INT, CASE_LONG, CASE_SHORT,
                CASE_OBJECT };
        
        LabelNode[] case_handles = new LabelNode[case_values.length];
        for (int i = 0; i < case_handles.length; i++) {
            case_handles[i] = new LabelNode();
        }
        
        LabelNode gotoTarget = new LabelNode();
        
        LabelNode defaultHandle = new LabelNode();

        write_il.add(new LdcInsnNode(0));
        write_il.add(new VarInsnNode(ISTORE,2));
        LabelNode gto = new LabelNode();
        write_il.add(new JumpInsnNode(GOTO, gto));
        LabelNode loop_body_start = new LabelNode();
        write_il.add(loop_body_start);
        write_il.add(new FieldInsnNode(GETSTATIC, clazz.name, FIELD_SERIAL_PERSISTENT_FIELDS,
                        serialPersistentFieldsSig));
        write_il.add(new VarInsnNode(ILOAD,2));
        write_il.add(new InsnNode(AALOAD));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_IO_OBJECTSTREAMFIELD,
                        METHOD_GET_NAME, Type.getMethodDescriptor(TYPE_STRING)));
        write_il.add(new VarInsnNode(ASTORE,3));
        LabelNode begin_try = new LabelNode();
        write_il.add(begin_try);
        write_il.add(new LdcInsnNode(clazz.name.replaceAll("/", ".")));
        write_il.add(new MethodInsnNode(INVOKESTATIC, JAVA_LANG_CLASS, METHOD_FOR_NAME,
                Type.getMethodDescriptor(TYPE_CLASS, TYPE_STRING)));
        write_il.add(new VarInsnNode(ALOAD,3));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_CLASS, METHOD_GET_DECLAREDFIELD,
                Type.getMethodDescriptor(Type.getObjectType("java/lang/reflect/Field"), TYPE_STRING)));
        write_il.add(new VarInsnNode(ASTORE,4));

        write_il.add(new FieldInsnNode(GETSTATIC, clazz.name, FIELD_SERIAL_PERSISTENT_FIELDS,
                serialPersistentFieldsSig));
        write_il.add(new VarInsnNode(ILOAD,2));
        write_il.add(new InsnNode(AALOAD));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_IO_OBJECTSTREAMFIELD,
                METHOD_GET_TYPE_CODE, Type.getMethodDescriptor(Type.CHAR_TYPE)));

        write_il.add(new LookupSwitchInsnNode(defaultHandle, case_values, case_handles));
        write_il.add(case_handles[0]);
        write_il.add(new VarInsnNode(ALOAD,1));
        write_il.add(new VarInsnNode(ALOAD,4));
        write_il.add(new VarInsnNode(ALOAD,0));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_BOOLEAN,
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, TYPE_OBJECT)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, 
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_BOOLEAN,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.BOOLEAN_TYPE)));
        write_il.add(new JumpInsnNode(GOTO, gotoTarget));

        write_il.add(case_handles[1]);
        write_il.add(new VarInsnNode(ALOAD,1));
        write_il.add(new VarInsnNode(ALOAD,4));
        write_il.add(new VarInsnNode(ALOAD,0));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_CHAR,
                Type.getMethodDescriptor(Type.CHAR_TYPE, TYPE_OBJECT)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, 
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_CHAR,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.CHAR_TYPE)));
        write_il.add(new JumpInsnNode(GOTO, gotoTarget));

        write_il.add(case_handles[2]);
        write_il.add(new VarInsnNode(ALOAD,1));
        write_il.add(new VarInsnNode(ALOAD,4));
        write_il.add(new VarInsnNode(ALOAD,0));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_DOUBLE,
                Type.getMethodDescriptor(Type.DOUBLE_TYPE, TYPE_OBJECT)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, 
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_DOUBLE,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.DOUBLE_TYPE)));
        write_il.add(new JumpInsnNode(GOTO, gotoTarget));

        write_il.add(case_handles[3]);
        write_il.add(new VarInsnNode(ALOAD,1));
        write_il.add(new VarInsnNode(ALOAD,4));
        write_il.add(new VarInsnNode(ALOAD,0));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_FLOAT,
                Type.getMethodDescriptor(Type.FLOAT_TYPE, TYPE_OBJECT)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, 
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_FLOAT,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.FLOAT_TYPE)));
        write_il.add(new JumpInsnNode(GOTO, gotoTarget));

        write_il.add(case_handles[4]);
        write_il.add(new VarInsnNode(ALOAD,1));
        write_il.add(new VarInsnNode(ALOAD,4));
        write_il.add(new VarInsnNode(ALOAD,0));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_INT,
                Type.getMethodDescriptor(Type.INT_TYPE, TYPE_OBJECT)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, 
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_INT,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE)));
        write_il.add(new JumpInsnNode(GOTO, gotoTarget));

        write_il.add(case_handles[5]);
        write_il.add(new VarInsnNode(ALOAD,1));
        write_il.add(new VarInsnNode(ALOAD,4));
        write_il.add(new VarInsnNode(ALOAD,0));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_LONG,
                Type.getMethodDescriptor(Type.LONG_TYPE, TYPE_OBJECT)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, 
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_LONG,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.LONG_TYPE)));
        write_il.add(new JumpInsnNode(GOTO, gotoTarget));

        write_il.add(case_handles[6]);
        write_il.add(new VarInsnNode(ALOAD,1));
        write_il.add(new VarInsnNode(ALOAD,4));
        write_il.add(new VarInsnNode(ALOAD,0));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_SHORT,
                Type.getMethodDescriptor(Type.SHORT_TYPE, TYPE_OBJECT)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, 
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_SHORT,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.SHORT_TYPE)));
        write_il.add(new JumpInsnNode(GOTO, gotoTarget));

        write_il.add(case_handles[7]);
        write_il.add(new VarInsnNode(ALOAD,1));
        write_il.add(new VarInsnNode(ALOAD,4));
        write_il.add(new VarInsnNode(ALOAD,0));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_BOOLEAN,
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, TYPE_OBJECT)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, 
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_BOOLEAN,
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.BOOLEAN_TYPE)));
        write_il.add(new JumpInsnNode(GOTO, gotoTarget));

        write_il.add(defaultHandle);
        write_il.add(new VarInsnNode(ALOAD,1));
        write_il.add(new VarInsnNode(ALOAD,4));
        write_il.add(new VarInsnNode(ALOAD,0));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_REFLECT_FIELD,
                METHOD_GET,
                Type.getMethodDescriptor(TYPE_OBJECT, TYPE_OBJECT)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, 
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_OBJECT,
                Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT)));
        write_il.add(new JumpInsnNode(GOTO, gotoTarget));

        LabelNode handler = new LabelNode();
        write_il.add(handler);
        write_il.add(new VarInsnNode(ASTORE, 5));
        write_il.add(new TypeInsnNode(NEW, JAVA_IO_IOEXCEPTION));
        write_il.add(new InsnNode(DUP));
        write_il.add(new TypeInsnNode(NEW, JAVA_LANG_STRINGBUFFER));
        write_il.add(new InsnNode(DUP));
        write_il.add(new MethodInsnNode(INVOKESPECIAL, JAVA_LANG_STRINGBUFFER,
                        METHOD_INIT, "()V"));
        write_il.add(new LdcInsnNode("Could not write field "));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_STRINGBUFFER,
                METHOD_APPEND, Type.getMethodDescriptor(TYPE_STRINGBUFFER, TYPE_STRING)));
        write_il.add(new VarInsnNode(ALOAD,3));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_STRINGBUFFER,
                METHOD_APPEND, Type.getMethodDescriptor(TYPE_STRINGBUFFER, TYPE_STRING)));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_STRINGBUFFER,
                METHOD_TO_STRING, Type.getMethodDescriptor(TYPE_STRING)));
        write_il.add(new MethodInsnNode(INVOKESPECIAL, JAVA_IO_IOEXCEPTION,
                METHOD_INIT, Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_STRING)));
        write_il.add(new VarInsnNode(ALOAD, 5));
        write_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_IO_IOEXCEPTION,
                "initCause", Type.getMethodDescriptor(TYPE_THROWABLE, TYPE_THROWABLE)));
        write_il.add(new InsnNode(ATHROW));

        write_il.add(gotoTarget);
        write_il.add(new IincInsnNode(2, 1));
        write_il.add(gto);
        write_il.add(new VarInsnNode(ILOAD,2));
        write_il.add(new FieldInsnNode(GETSTATIC, clazz.name, FIELD_SERIAL_PERSISTENT_FIELDS,
                serialPersistentFieldsSig));
        write_il.add(new InsnNode(ARRAYLENGTH));
        write_il.add(new JumpInsnNode(IF_ICMPLT, loop_body_start));

        write_gen.tryCatchBlocks.add(new TryCatchBlockNode(begin_try, handler, handler, JAVA_LANG_THROWABLE));
        return write_il;
    }

    private InsnList generateDefaultWrites(MethodNode write_gen) {
        InsnList write_il = new InsnList();

        if (has_serial_persistent_fields) {
            return serialPersistentWrites(write_gen);
        }

        /* handle the primitive fields */
        for (FieldNode field : fields) {
            /* Don't send fields that are STATIC, or TRANSIENT */
            if ((field.access & (ACC_STATIC | ACC_TRANSIENT)) == 0) {
                Type field_type = Type.getType(field.desc);
                if (field_type.getSort() != Type.ARRAY && field_type.getSort() != Type.OBJECT) {
                    if (generator.isVerbose()) {
                        System.out.println("    writing basic field "
                                + field.name + " of type "
                                + field.desc);
                    }

                    write_il.add(writeInstructions(field));
                }
            }
        }

        /* handle the reference fields */
        for (FieldNode field : fields) {
            /* Don't send fields that are STATIC or TRANSIENT */
            if ((field.access & (ACC_STATIC | ACC_TRANSIENT)) == 0) {
                Type field_type = Type.getType(field.desc);
                if (field_type.getSort() == Type.ARRAY || field_type.getSort() == Type.OBJECT) {
                    if (generator.isVerbose()) {
                        System.out.println("    writing field "
                                + field.name + " of type "
                                + field.desc);
                    }
                    if (! field.desc.equals(JAVA_LANG_STRING) && ! field.desc.equals(JAVA_LANG_CLASS)) {
                        write_il.add(writeReferenceField(field));
                    } else {
                        write_il.add(writeInstructions(field));
                    }
                }
            }
        }

        return write_il;
    }

    private String readCallName(String name) {
        return METHOD_READ_ARRAY + name.substring(0, 1).toUpperCase()
        + name.substring(1);
    }

    private InsnList readReferenceField(FieldNode field,
            boolean from_constructor) {
        Type field_type = Type.getType(field.desc);
        InsnList read_il = new InsnList();

        boolean isfinal = false;
        boolean isarray = false;
        ClassNode field_class = null;
        String basicname = null;

        if (generator.isVerbose()) {
            System.out.println("    reading reference field "
                    + field.name + " of type "
                    + field.desc);
        }
        Type tp = field_type;
        if (tp.getSort() == Type.ARRAY) {
            isarray = true;
            if (tp.getDimensions() > 1) {
                tp = Type.getType(tp.getDescriptor().substring(1));
            } else {
                tp = tp.getElementType();
            }
        }
        if (tp.getSort() == Type.OBJECT) {
            field_class = lookupClass(tp.getInternalName());
             if (field_class != null && (field_class.access & ACC_FINAL) == ACC_FINAL) {
                isfinal = true;
            }
        } else if (isarray) {
            switch(tp.getSort()) {
            case Type.BOOLEAN:
                basicname = "boolean";
                break;
            case Type.BYTE:
                basicname = "byte";
                break;
            case Type.SHORT:
                basicname = "short";
                break;
            case Type.CHAR:
                basicname = "char";
                break;
            case Type.INT:
                basicname = "int";
                break;
            case Type.FLOAT:
                basicname = "float";
                break;
            case Type.LONG:
                basicname = "long";
                break;
            case Type.DOUBLE:
                basicname = "double";
                break;
            }
        }

        if ((basicname != null)
                || (isfinal
                        && (ASMSerializationInfo.hasIbisConstructor(field_class)
                                || (ASMSerializationInfo.isSerializable(field_class)
                                        && generator.forceGeneratedCalls())))) {
            read_il.add(new VarInsnNode(ALOAD,1));
            read_il.add(new MethodInsnNode(INVOKEVIRTUAL, TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ_KNOWN_TYPE_HEADER,
                    Type.getMethodDescriptor(Type.INT_TYPE)));
            read_il.add(new VarInsnNode(ISTORE,2));
            read_il.add(new VarInsnNode(ILOAD,2));
            read_il.add(new LdcInsnNode(-1));
            LabelNode ifcmp = new LabelNode();
            read_il.add(new JumpInsnNode(IF_ICMPNE, ifcmp));

            if (isarray) {
                if (basicname != null) {
                    String callname = readCallName(basicname);

                    read_il.add(new VarInsnNode(ALOAD,0));
                    read_il.add(new VarInsnNode(ALOAD,1));
                    read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                            TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM, callname,
                            Type.getMethodDescriptor(field_type)));
                    read_il.add(new FieldInsnNode(PUTFIELD, clazz.name, field.name, field.desc));
                } else {
                    Type el_type = field_type.getElementType();
                    read_il.add(new VarInsnNode(ALOAD,0));
                    read_il.add(new VarInsnNode(ALOAD,1));
                    read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                            TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM, METHOD_READ_INT,
                            Type.getMethodDescriptor(Type.INT_TYPE)));
                    read_il.add(new InsnNode(DUP));
                    read_il.add(new VarInsnNode(ISTORE,3));
                    read_il.add(new TypeInsnNode(ANEWARRAY, el_type.getInternalName()));
                    read_il.add(new FieldInsnNode(PUTFIELD, clazz.name, field.name, field.desc));
                    read_il.add(new VarInsnNode(ALOAD,1));
                    read_il.add(new VarInsnNode(ALOAD,0));
                    read_il.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
                    read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                            TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                            METHOD_ADD_OBJECT_TO_CYCLE_CHECK,
                            Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT)));
                    read_il.add(new LdcInsnNode(0));
                    read_il.add(new VarInsnNode(ISTORE,4));
                    LabelNode gto1 = new LabelNode();
                    read_il.add(new JumpInsnNode(GOTO, gto1));
                    LabelNode loop_body_start = new LabelNode();
                    read_il.add(loop_body_start);
                    read_il.add(new VarInsnNode(ALOAD,1));
                    read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                            TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                            METHOD_READ_KNOWN_TYPE_HEADER,
                            Type.getMethodDescriptor(Type.INT_TYPE)));
                    read_il.add(new VarInsnNode(ISTORE,2));
                    read_il.add(new VarInsnNode(ILOAD,2));
                    read_il.add(new LdcInsnNode(-1));
                    LabelNode ifcmp1 = new LabelNode();
                    read_il.add(new JumpInsnNode(IF_ICMPNE, ifcmp1));
                    read_il.add(new VarInsnNode(ALOAD,0));
                    read_il.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
                    read_il.add(new VarInsnNode(ILOAD,4));
                    read_il.add(new TypeInsnNode(NEW, el_type.getInternalName()));
                    read_il.add(new InsnNode(DUP));
                    read_il.add(new VarInsnNode(ALOAD,1));
                    read_il.add(new MethodInsnNode(INVOKESPECIAL, field_class.name, METHOD_INIT,
                            SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V));
                    read_il.add(new InsnNode(AASTORE));
                    LabelNode gto2 = new LabelNode();
                    read_il.add(new JumpInsnNode(GOTO, gto2));
                    read_il.add(ifcmp1);
                    read_il.add(new VarInsnNode(ILOAD, 2));
                    read_il.add(new LdcInsnNode(0));
                    LabelNode ifcmpeq2 = new LabelNode();
                    read_il.add(new JumpInsnNode(IF_ICMPEQ, ifcmpeq2));
                    read_il.add(new VarInsnNode(ALOAD,0));
                    read_il.add(new FieldInsnNode(GETFIELD, clazz.name, field.name, field.desc));
                    read_il.add(new VarInsnNode(ILOAD,4));
                    read_il.add(new VarInsnNode(ALOAD,1));
                    read_il.add(new VarInsnNode(ILOAD,2));
                    read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                            TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                            METHOD_GET_OBJECT_FROM_CYCLE_CHECK,
                            Type.getMethodDescriptor(TYPE_OBJECT, Type.INT_TYPE)));
                    read_il.add(new TypeInsnNode(CHECKCAST, el_type.getInternalName()));
                    read_il.add(new InsnNode(AASTORE));
                    read_il.add(ifcmpeq2);
                    read_il.add(gto2);
                    read_il.add(new IincInsnNode(4, 1));
                    read_il.add(gto1);
                    read_il.add(new VarInsnNode(ILOAD,3));
                    read_il.add(new VarInsnNode(ILOAD,4));
                    read_il.add(new JumpInsnNode(IF_ICMPGT, loop_body_start));
                }
            } else {
                read_il.add(new VarInsnNode(ALOAD,0));
                read_il.add(new TypeInsnNode(NEW, field_type.getInternalName()));
                read_il.add(new InsnNode(DUP));
                read_il.add(new VarInsnNode(ALOAD,1));
                read_il.add(new MethodInsnNode(INVOKESPECIAL, field_class.name, METHOD_INIT,
                        SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V));
                read_il.add(new FieldInsnNode(PUTFIELD, clazz.name, field.name, field.desc));
            }
            LabelNode gto = new LabelNode();
            read_il.add(new JumpInsnNode(GOTO, gto));
            read_il.add(ifcmp);
            read_il.add(new VarInsnNode(ILOAD,2));
            read_il.add(new LdcInsnNode(0));
            LabelNode ifcmpeq = new LabelNode();
            read_il.add(new JumpInsnNode(IF_ICMPEQ, ifcmpeq));
            read_il.add(new VarInsnNode(ALOAD,0));
            read_il.add(new VarInsnNode(ALOAD,1));
            read_il.add(new VarInsnNode(ILOAD,2));
            read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_GET_OBJECT_FROM_CYCLE_CHECK,
                    Type.getMethodDescriptor(TYPE_OBJECT, Type.INT_TYPE)));
            read_il.add(new TypeInsnNode(CHECKCAST, field_type.getInternalName()));
            read_il.add(new FieldInsnNode(PUTFIELD, clazz.name, field.name, field.desc));
            read_il.add(ifcmpeq);
            read_il.add(gto);
        } else {
            read_il.add(readInstructions(field, from_constructor));
        }

        return read_il;
    }

    private void generateReadField(String tpname, Type tp,
            InsnList read_il, LabelNode gto, LabelNode caseLabel, boolean from_constructor) {
        read_il.add(caseLabel);
        if (from_constructor || !has_final_fields) {
            read_il.add(new VarInsnNode(ALOAD,4));
            read_il.add(new VarInsnNode(ALOAD,0));
            read_il.add(new VarInsnNode(ALOAD,1));
            if (tpname.equals("")) {
                read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_READ_OBJECT,
                        Type.getMethodDescriptor(TYPE_OBJECT)));
            } else {
                read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_READ + tpname, Type.getMethodDescriptor(tp)));
            }
            read_il.add(
                    new MethodInsnNode(INVOKEVIRTUAL,
                            JAVA_LANG_REFLECT_FIELD,
                            METHOD_SET + tpname,
                            Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, tp)));
            read_il.add(new JumpInsnNode(GOTO, gto));
            return;
        }

        read_il.add(new VarInsnNode(ILOAD,5));
        read_il.add(new LdcInsnNode(ACC_FINAL));
        read_il.add(new InsnNode(IAND));
        LabelNode eq = new LabelNode();
        read_il.add(new JumpInsnNode(IFEQ, eq));
        read_il.add(new VarInsnNode(ALOAD,1));
        read_il.add(new VarInsnNode(ALOAD,0));
        read_il.add(new VarInsnNode(ALOAD,3));
        if (tpname.equals("")) {
            read_il.add(new VarInsnNode(ALOAD,4));
            read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    JAVA_LANG_REFLECT_FIELD,
                    METHOD_GET_TYPE,
                    Type.getMethodDescriptor(TYPE_CLASS)));
            read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    JAVA_LANG_CLASS,
                    METHOD_GET_NAME,
                    Type.getMethodDescriptor(TYPE_STRING)));
            read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ_FIELD_OBJECT,
                    Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, TYPE_STRING, TYPE_STRING)));
        } else {
            read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                     TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                     METHOD_READ_FIELD + tpname,
                     Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, TYPE_STRING)));
        }
        LabelNode gto2 = new LabelNode();
        read_il.add(new JumpInsnNode(GOTO, gto2));
        read_il.add(eq);
        read_il.add(new VarInsnNode(ALOAD,4));
        read_il.add(new VarInsnNode(ALOAD,0));
        read_il.add(new VarInsnNode(ALOAD,1));
        if (tpname.equals("")) {
            read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ_OBJECT, Type.getMethodDescriptor(tp)));
        } else {
            read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ + tpname, Type.getMethodDescriptor(tp)));
        }
        read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                JAVA_LANG_REFLECT_FIELD,
                METHOD_SET + tpname, 
                Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, tp)));
        read_il.add(gto2);
        read_il.add(new JumpInsnNode(GOTO, gto));
    }

    @SuppressWarnings("unchecked")
    private InsnList serialPersistentReads(boolean from_constructor,
            MethodNode read_gen) {        
        InsnList read_il = new InsnList();
        int[] case_values = new int[] { CASE_BOOLEAN, CASE_CHAR, CASE_DOUBLE, CASE_FLOAT, CASE_INT, CASE_LONG, CASE_SHORT,
                CASE_OBJECT };
        String serialPersistentFieldsSig = "[L" + JAVA_IO_OBJECTSTREAMFIELD + ";";
        
        LabelNode[] case_handles = new LabelNode[case_values.length];
        for (int i = 0; i < case_handles.length; i++) {
            case_handles[i] = new LabelNode();
        }
        
        LabelNode gotoTarget = new LabelNode();
        
        LabelNode defaultHandle = new LabelNode();

        read_il.add(new LdcInsnNode(0));
        read_il.add(new VarInsnNode(ISTORE,2));
        LabelNode gto = new LabelNode();
        read_il.add(new JumpInsnNode(GOTO, gto));
        LabelNode loop_body_start = new LabelNode();
        read_il.add(loop_body_start);
        read_il.add(new FieldInsnNode(GETSTATIC, clazz.name, FIELD_SERIAL_PERSISTENT_FIELDS,
                serialPersistentFieldsSig));
        read_il.add(new VarInsnNode(ILOAD,2));
        read_il.add(new InsnNode(AALOAD));
        read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                JAVA_IO_OBJECTSTREAMFIELD,
                METHOD_GET_NAME, Type.getMethodDescriptor(TYPE_STRING)));
        read_il.add(new VarInsnNode(ASTORE,3));
        LabelNode begin_try = new LabelNode();
        read_il.add(begin_try);
        read_il.add(new LdcInsnNode(clazz.name.replaceAll("/", ".")));
        read_il.add(new MethodInsnNode(INVOKESTATIC,
                JAVA_LANG_CLASS, METHOD_FOR_NAME,
                Type.getMethodDescriptor(TYPE_CLASS, TYPE_STRING)));
        read_il.add(new VarInsnNode(ALOAD,3));
        read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                JAVA_LANG_CLASS, METHOD_GET_DECLAREDFIELD,
                Type.getMethodDescriptor(Type.getObjectType(JAVA_LANG_REFLECT_FIELD), TYPE_STRING)));
        read_il.add(new VarInsnNode(ASTORE,4));

        if (!from_constructor && has_final_fields) {
            read_il.add(new VarInsnNode(ALOAD,4));
            read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    JAVA_LANG_REFLECT_FIELD,
                    METHOD_GET_MODIFIERS,
                    Type.getMethodDescriptor(Type.INT_TYPE)));
            read_il.add(new VarInsnNode(ISTORE,5));
        }

        read_il.add(new FieldInsnNode(GETSTATIC, clazz.name, FIELD_SERIAL_PERSISTENT_FIELDS,
                serialPersistentFieldsSig));
        read_il.add(new VarInsnNode(ILOAD,2));
        read_il.add(new InsnNode(AALOAD));
        read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                JAVA_IO_OBJECTSTREAMFIELD,
                METHOD_GET_TYPE_CODE,
                Type.getMethodDescriptor(Type.CHAR_TYPE)));
        read_il.add(new LookupSwitchInsnNode(defaultHandle, case_values, case_handles));
        generateReadField("Byte", Type.BYTE_TYPE, read_il,
                gotoTarget, case_handles[0], from_constructor);
        generateReadField("Char", Type.CHAR_TYPE, read_il,
                gotoTarget, case_handles[1], from_constructor);
        generateReadField("Double", Type.DOUBLE_TYPE, read_il,
                gotoTarget, case_handles[2], from_constructor);
        generateReadField("Float", Type.FLOAT_TYPE, read_il,
                gotoTarget, case_handles[3], from_constructor);
        generateReadField("Int", Type.INT_TYPE, read_il,
                gotoTarget, case_handles[4], from_constructor);
        generateReadField("Long", Type.LONG_TYPE, read_il,
                gotoTarget, case_handles[5], from_constructor);
        generateReadField("Short", Type.SHORT_TYPE, read_il,
                gotoTarget, case_handles[6], from_constructor);
        generateReadField("Boolean", Type.BOOLEAN_TYPE,
                read_il, gotoTarget, case_handles[7], from_constructor);
        generateReadField("", TYPE_OBJECT,
                read_il, gotoTarget, defaultHandle, from_constructor);

        LabelNode end_try = new LabelNode();
        read_il.add(end_try);
        LabelNode handler = new LabelNode();
        read_il.add(handler);
        read_il.add(new VarInsnNode(ASTORE,6));
        read_il.add(new TypeInsnNode(NEW, JAVA_IO_IOEXCEPTION));
        read_il.add(new InsnNode(DUP));
        read_il.add(new TypeInsnNode(NEW, JAVA_LANG_STRINGBUFFER));
        read_il.add(new InsnNode(DUP));
        read_il.add(new MethodInsnNode(INVOKESPECIAL,
                JAVA_LANG_STRINGBUFFER,
                METHOD_INIT, "()V"));
        read_il.add(new LdcInsnNode("Could not read field "));
        read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                JAVA_LANG_STRINGBUFFER,
                METHOD_APPEND,
                Type.getMethodDescriptor(TYPE_STRINGBUFFER, TYPE_STRING)));
        read_il.add(new VarInsnNode(ALOAD,3));
        read_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_STRINGBUFFER,
                METHOD_APPEND, Type.getMethodDescriptor(TYPE_STRINGBUFFER, TYPE_STRING)));
        read_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_LANG_STRINGBUFFER,
                METHOD_TO_STRING, Type.getMethodDescriptor(TYPE_STRING)));
        read_il.add(new MethodInsnNode(INVOKESPECIAL, JAVA_IO_IOEXCEPTION,
                METHOD_INIT, Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_STRING)));
        read_il.add(new VarInsnNode(ALOAD, 6));
        read_il.add(new MethodInsnNode(INVOKEVIRTUAL, JAVA_IO_IOEXCEPTION,
                "initCause", Type.getMethodDescriptor(TYPE_THROWABLE, TYPE_THROWABLE)));
        read_il.add(new InsnNode(ATHROW));
        read_il.add(gotoTarget);
        read_il.add(new IincInsnNode(2, 1));
        read_il.add(gto);
        read_il.add(new VarInsnNode(ILOAD,2));
        read_il.add(new FieldInsnNode(GETSTATIC, clazz.name, FIELD_SERIAL_PERSISTENT_FIELDS,
                serialPersistentFieldsSig));
        read_il.add(new InsnNode(ARRAYLENGTH));
        read_il.add(new JumpInsnNode(IF_ICMPLT, loop_body_start));

        read_gen.tryCatchBlocks.add(new TryCatchBlockNode(begin_try, end_try, handler, JAVA_LANG_THROWABLE));

        return read_il;
    }

    private InsnList generateDefaultReads(boolean from_constructor,
            MethodNode read_gen) {
        InsnList read_il = new InsnList();

        if (has_serial_persistent_fields) {
            return serialPersistentReads(from_constructor, read_gen);
        }

        /* handle the primitive fields */
        for (FieldNode field : fields) {
            /* Don't send fields that are STATIC, or TRANSIENT */
            if ((field.access & (ACC_STATIC | ACC_TRANSIENT)) == 0) {
                Type field_type = Type.getType(field.desc);
                if (field_type.getSort() != Type.ARRAY && field_type.getSort() != Type.OBJECT) {
                    if (generator.isVerbose()) {
                        System.out.println("    reading basic field "
                                + field.name + " of type "
                                + field.desc);
                    }

                    read_il.add(readInstructions(field, from_constructor));
                }
            }
        }

        /* handle the reference fields. */
        for (FieldNode field : fields) {
            /* Don't send fields that are STATIC or TRANSIENT */
            if ((field.access & (ACC_STATIC | ACC_TRANSIENT)) == 0) {
                Type field_type = Type.getType(field.desc);
                if (field_type.getSort() == Type.ARRAY || field_type.getSort() == Type.OBJECT) {
                    if (generator.isVerbose()) {
                        System.out.println("    reading field "
                                + field.name + " of type "
                                + field.desc);
                    }
                    if (! field.desc.equals(JAVA_LANG_STRING) && ! field.desc.equals(JAVA_LANG_CLASS)) {
                        read_il.add(readReferenceField(field, from_constructor));
                    } else {
                        read_il.add(readInstructions(field, from_constructor));
                    }
                }
            }
        }

        return read_il;
    }

    @SuppressWarnings("unchecked")
    private ClassNode generateInstanceGenerator() {

        /* Here we create a 'generator' object. We need this extra object
         * for three reasons: 
         * 1) Because the object is created from the 'ibis.io' package
         *    (the Serialization code), we may not be allowed to create a
         *    new instance of the object (due to inter-package access
         *    restrictions, e.g. the object may not be public). Because
         *    the generator is in the same package as the target object,
         *    it can create a new object for us.
         *
         * ?? How about totally private objects ??
         * can sun serialization handle this ??
         *
         * 2) Using this generator object, we can do a normal 'new' of the
         *    target type. This is important, because using 'newInstance' is
         *    6 times more expensive than 'new'.  
         * 3) We do not want to invoke a default constructor, but a special
         *    constructor that immediately reads the object state from the
         *    stream. This cannot be done (efficiently) with newInstance.
         */

        if (generator.isVerbose()) {
            System.out.println("  Generating InstanceGenerator class for "
                    + clazz.name);
        }

        String name = clazz.name + METHOD_IBIS_IO_GENERATOR;
        Type class_type = Type.getObjectType(clazz.name);
 
        ClassNode iogenGen = new ClassNode();
        iogenGen.version = clazz.version;
        iogenGen.access = ACC_FINAL | ACC_PUBLIC | ACC_SUPER;
        iogenGen.superName = IBIS_IO_GENERATOR;
        iogenGen.name = name;

        InsnList il = new InsnList();

        if (!is_externalizable && super_is_serializable
                && !super_has_ibis_constructor && !generator.forceGeneratedCalls()) {
            /* This is a difficult case. We cannot call a constructor,
             * because this constructor would be obliged to call a
             * constructor for the super-class.
             * So, we do it differently: generate calls to
             * IbisSerializationInputStream methods which call native
             * methods ... I don't know another solution to this problem.
             */
            /* First, create the object. Through a native call, because
             * otherwise the object would be marked uninitialized, and the
             * code would not pass bytecode verification. This native call
             * also takes care of calling the constructor of the first
             * non-serializable superclass.
             */
            il.add(new VarInsnNode(ALOAD,1));
            il.add(new LdcInsnNode(clazz.name.replaceAll("/", ".")));
            il.add(new MethodInsnNode(INVOKEVIRTUAL, TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_CREATE_UNINITIALIZED_OBJECT,
                    Type.getMethodDescriptor(TYPE_OBJECT, TYPE_STRING)));

            il.add(new TypeInsnNode(CHECKCAST, class_type.getInternalName()));
            il.add(new VarInsnNode(ASTORE,2));

            /* Now read the superclass. */
            il.add(new VarInsnNode(ALOAD,1));
            il.add(new VarInsnNode(ALOAD,2));
            il.add(new LdcInsnNode(clazz.superName.replaceAll("/", ".")));
            il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ_SERIALIZABLE_OBJECT,
                    Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, TYPE_STRING)));
 
            /* Now, if the class has a readObject, call it. Otherwise,
             * read its fields, by calling generated_DefaultReadObject.
             */
            if (ASMSerializationInfo.hasReadObject(methods)) {
                il.add(new VarInsnNode(ALOAD,2));
                il.add(new VarInsnNode(ALOAD,1));
                il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        clazz.name, METHOD_$READ_OBJECT_WRAPPER$,
                        SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V));
             } else {
                int dpth = getClassDepth(clazz);

                il.add(new VarInsnNode(ALOAD,2));
                il.add(new VarInsnNode(ALOAD,1));
                il.add(new LdcInsnNode(dpth));
                il.add(new MethodInsnNode(INVOKEVIRTUAL, clazz.name, METHOD_GENERATED_DEFAULT_READ_OBJECT,
                        SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_I_V));
            }
            il.add(new VarInsnNode(ALOAD,2));
        } else {
            il.add(new TypeInsnNode(NEW, clazz.name));
            il.add(new InsnNode(DUP));
            il.add(new VarInsnNode(ALOAD,1));
            il.add(createInitInvocation(clazz));
        }
        il.add(new InsnNode(ARETURN));

        /*
         0       new DITree
         3       dup
         4       aload_1
         5       invokespecial DITree(ibis.io.IbisSerializationInputStream)
         8       areturn
         */
        
        MethodNode method = new MethodNode(ASM4,
                ACC_FINAL | ACC_PUBLIC,
                METHOD_GENERATED_NEW_INSTANCE,
                Type.getMethodDescriptor(TYPE_OBJECT, TYPE_IBIS_IO_INPUT),
                null,
                new String[] {JAVA_IO_IOEXCEPTION, JAVA_LANG_CLASSNOTFOUNDEXCEPTION});
        
        method.instructions = il;

        iogenGen.methods.add(method);

        il = new InsnList();
        il.add(new VarInsnNode(ALOAD,0));
        il.add(new MethodInsnNode(INVOKESPECIAL,
                IBIS_IO_GENERATOR, METHOD_INIT,
                "()V"));
         il.add(new InsnNode(RETURN));

        method = new MethodNode(ASM4, ACC_PUBLIC, METHOD_INIT, "()V", null, new String[0]);
        method.instructions = il;
        iogenGen.methods.add(method);
        return iogenGen;
    }

    void generateCode() {
        /* Generate code inside the methods */
        if (generator.isVerbose()) {
            System.out.println("  Generating method code class for class : "
                    + clazz.name);
            System.out.println("    Number of fields " + fields.size());
        }

        int dpth = getClassDepth(clazz);

        fillInGeneratedDefaultWriteObjectMethod(dpth);
        fillInGeneratedDefaultReadObjectMethod(dpth);
        fillInGeneratedWriteObjectMethod(dpth);
        fillInGeneratedReadObjectMethod(dpth);

        ASMRepository.removeClass(clazz.name);
        ASMRepository.addClass(clazz);
        
        ClassNode instgen = null;
        
        if (! is_abstract) {
            instgen = generateInstanceGenerator();
            ASMRepository.addClass(instgen);
        }
        generator.markRewritten(clazz, instgen);
    }

    private void fillInGeneratedReadObjectMethod(int dpth) {
        /* Now, produce the read constructor. It only exists if the
         * superclass is not serializable, or if the superclass has an
         * ibis constructor, or is assumed to have one (-force option).
         */

        /* Now, do the same for the reading side. */
        MethodNode mgen = null;   
        InsnList read_il = null;
        if (is_externalizable || super_has_ibis_constructor
                || !super_is_serializable || generator.forceGeneratedCalls()) {
            read_il = new InsnList();
            if (is_externalizable) {
                read_il.add(new VarInsnNode(ALOAD,0));
                read_il.add(new MethodInsnNode(INVOKESPECIAL,
                        clazz.name, METHOD_INIT,
                        "()V"));
            } else if (!super_is_serializable) {
                read_il.add(new VarInsnNode(ALOAD,0));
                read_il.add(new MethodInsnNode(INVOKESPECIAL,
                        clazz.superName,
                        METHOD_INIT, "()V"));
            } else {
                read_il.add(new VarInsnNode(ALOAD,0));
                read_il.add(new VarInsnNode(ALOAD,1));
                read_il.add(new MethodInsnNode(INVOKESPECIAL, clazz.superName, METHOD_INIT,
                        SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V));
            }

            if (is_externalizable || !super_is_serializable) {
                read_il.add(new VarInsnNode(ALOAD,1));
                read_il.add(new VarInsnNode(ALOAD,0));
                read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_ADD_OBJECT_TO_CYCLE_CHECK,
                        Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT)));
            }

            mgen = ASMSerializationInfo.findMethod(methods,
                    METHOD_INIT, 
                    SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V);
        } else if (ASMSerializationInfo.hasReadObject(methods)) {
            mgen = ASMSerializationInfo.findMethod(methods,
                    METHOD_$READ_OBJECT_WRAPPER$, 
                    SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V);
            read_il = new InsnList();
        } else {
            // No readObject method generated.
        }

        if (read_il != null) {
            if (is_externalizable || ASMSerializationInfo.hasReadObject(methods)) {
                /* First, get and set IbisSerializationInputStream's idea of the current object. */
                read_il.add(new VarInsnNode(ALOAD,1));
                read_il.add(new VarInsnNode(ALOAD,0));
                read_il.add(new LdcInsnNode(dpth));
                read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_PUSH_CURRENT_OBJECT,
                        Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, Type.INT_TYPE)));
                read_il.add(new VarInsnNode(ALOAD,0));
                read_il.add(new VarInsnNode(ALOAD,1));
                read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_GET_JAVA_OBJECT_INPUT_STREAM,
                        Type.getMethodDescriptor(TYPE_JAVA_IO_OBJECTINPUT)));
                 if (is_externalizable) {
                    /* Invoke readExternal */
                    read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                            clazz.name, METHOD_READ_EXTERNAL,
                            Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_JAVA_IO_OBJECTINPUT)));
                 } else {
                    /* Invoke readObject. */
                    read_il.add(new MethodInsnNode(INVOKESPECIAL,
                            clazz.name, METHOD_READ_OBJECT,
                            Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_JAVA_IO_OBJECTINPUT))); 
                }

                /* And then, restore IbisSerializationOutputStream's idea of the current object. */
                read_il.add(new VarInsnNode(ALOAD,1));
                read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_POP_CURRENT_OBJECT,
                        "()V"));
            } else {
                read_il.add(generateDefaultReads(true, mgen));
            }

            read_il.add(mgen.instructions);
            mgen.instructions = read_il;
        }
    }

    private void fillInGeneratedWriteObjectMethod(int dpth) {

        InsnList write_il = new InsnList();
        // int flags = ACC_PUBLIC | (gen.isFinal() ? ACC_FINAL : 0);

        MethodNode write_gen = ASMSerializationInfo.findMethod(methods,
                METHOD_GENERATED_WRITE_OBJECT, 
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_V);

        /* write the superclass if neccecary */
        if (is_externalizable) {
            /* Nothing to be done for the superclass. */
        } else if (super_is_ibis_serializable
                || (generator.forceGeneratedCalls() && super_is_serializable)) {
            write_il.add(new VarInsnNode(ALOAD,0));
            write_il.add(new VarInsnNode(ALOAD,1));
            write_il.add(createGeneratedWriteObjectInvocation(
                    clazz.superName, INVOKESPECIAL));

        } else if (super_is_serializable) {
            write_il.add(new VarInsnNode(ALOAD,1));
            write_il.add(new VarInsnNode(ALOAD,0));
            write_il.add(new LdcInsnNode(clazz.superName.replaceAll("/", ".")));
            write_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                    METHOD_WRITE_SERIALIZABLE_OBJECT,
                    Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, TYPE_STRING)));
         }

        /* and now ... generated_WriteObject should either call the classes
         * writeObject, if it has one, or call generated_DefaultWriteObject.
         * The read constructor should either call readObject, or call
         * generated_DefaultReadObject.
         */
        if (is_externalizable || ASMSerializationInfo.hasWriteObject(methods)) {
            /* First, get and set IbisSerializationOutputStream's idea of
             * the current object.
             */
            write_il.add(new VarInsnNode(ALOAD,1));
            write_il.add(new VarInsnNode(ALOAD,0));
            write_il.add(new LdcInsnNode(dpth));
            write_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                    METHOD_PUSH_CURRENT_OBJECT,
                    Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, Type.INT_TYPE)));
             write_il.add(new VarInsnNode(ALOAD,0));
            write_il.add(new VarInsnNode(ALOAD,1));
            write_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                    METHOD_GET_JAVA_OBJECT_OUTPUT_STREAM,
                    Type.getMethodDescriptor(TYPE_JAVA_IO_OBJECTOUTPUT)));
            if (is_externalizable) {
                /* Invoke writeExternal */
                write_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        clazz.name, METHOD_WRITE_EXTERNAL,
                        Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_JAVA_IO_OBJECTINPUT)));
            } else {
                /* Invoke writeObject. */
                write_il.add(createWriteObjectInvocation());
            }

            /* And then, restore IbisSerializationOutputStream's idea of the current object. */
            write_il.add(new VarInsnNode(ALOAD,1));
            write_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                    METHOD_POP_CURRENT_OBJECT, "()V"));
        } else {
            write_il.add(generateDefaultWrites(write_gen));
        }
        
        write_il.add(write_gen.instructions);
        write_gen.instructions = write_il;
    }

    private void fillInGeneratedDefaultWriteObjectMethod(int dpth) {
        /* void generated_DefaultWriteObject(
         *         IbisSerializationOutputStream out, int level) {
         *     if (level == dpth) {
         *          ... write fields ... (the code resulting from the
         *                  generateDefaultWrites() call).
         *     } else if (level < dpth) {
         *         super.generated_DefaultWriteObject(out, level);
         *     }
         * }
         */

        MethodNode write_gen = ASMSerializationInfo.findMethod(
                methods,
                METHOD_GENERATED_DEFAULT_WRITE_OBJECT, 
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_I_V);

        InsnList write_il = new InsnList();
        LabelNode end = new LabelNode();
        write_il.add(new VarInsnNode(ILOAD,2));
        write_il.add(new LdcInsnNode(dpth));
        LabelNode ifcmpne = new LabelNode();
        write_il.add(new JumpInsnNode(IF_ICMPNE, ifcmpne));
        write_il.add(generateDefaultWrites(write_gen));
        write_il.add(new JumpInsnNode(GOTO, end));
        if (super_is_ibis_serializable || super_is_serializable) {
            write_il.add(ifcmpne);
            write_il.add(new VarInsnNode(ILOAD,2));
            write_il.add(new LdcInsnNode(dpth));
            write_il.add(new JumpInsnNode(IF_ICMPGT, end));
            if (super_is_ibis_serializable || generator.forceGeneratedCalls()) {
                write_il.add(new VarInsnNode(ALOAD,0));
                write_il.add(new VarInsnNode(ALOAD,1));
                write_il.add(new VarInsnNode(ILOAD,2));
                write_il.add(
                        createGeneratedDefaultWriteObjectInvocation(
                                clazz.superName));
            } else {
                /*  Superclass is not rewritten.
                 */
                write_il.add(new VarInsnNode(ALOAD,1));
                write_il.add(new VarInsnNode(ALOAD,0));
                write_il.add(new VarInsnNode(ILOAD,2));
                write_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                        METHOD_DEFAULT_WRITE_SERIALIZABLE_OBJECT,
                        Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, Type.INT_TYPE)));
            }
        } else {
            write_il.add(ifcmpne);
        }
        write_il.add(end);
        write_il.add(write_gen.instructions);
        write_gen.instructions = write_il;
    }

    private void fillInGeneratedDefaultReadObjectMethod(int dpth) {

        MethodNode read_gen = ASMSerializationInfo.findMethod(
                methods,
                METHOD_GENERATED_DEFAULT_READ_OBJECT, 
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_I_V);
        InsnList read_il = new InsnList();
        LabelNode end = new LabelNode();
        read_il.add(new VarInsnNode(ILOAD,2));
        read_il.add(new LdcInsnNode(dpth));
        LabelNode ifcmpne = new LabelNode();
        read_il.add(new JumpInsnNode(IF_ICMPNE, ifcmpne));
        read_il.add(generateDefaultReads(false, read_gen));
        read_il.add(new JumpInsnNode(GOTO, end));
        read_il.add(ifcmpne);
        if (super_is_ibis_serializable || super_is_serializable) {
            read_il.add(new VarInsnNode(ILOAD,2));
            read_il.add(new LdcInsnNode(dpth));
            read_il.add(new JumpInsnNode(IF_ICMPGT, end));
            if (super_is_ibis_serializable || generator.forceGeneratedCalls()) {
                read_il.add(new VarInsnNode(ALOAD,0));
                read_il.add(new VarInsnNode(ALOAD,1));
                read_il.add(new VarInsnNode(ILOAD,2));
                read_il.add(createGeneratedDefaultReadObjectInvocation(
                        clazz.superName, INVOKESPECIAL));
            } else {
                /*  Superclass is not rewritten.
                 */
                read_il.add(new VarInsnNode(ALOAD,1));
                read_il.add(new VarInsnNode(ALOAD,0));
                read_il.add(new VarInsnNode(ILOAD,2));
                read_il.add(new MethodInsnNode(INVOKEVIRTUAL,
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_DEFAULT_READ_SERIALIZABLE_OBJECT,
                        Type.getMethodDescriptor(Type.VOID_TYPE, TYPE_OBJECT, Type.INT_TYPE)));
            }
        }
        read_il.add(end);

        read_il.add(read_gen.instructions);
        read_gen.instructions = read_il;
    }
}
