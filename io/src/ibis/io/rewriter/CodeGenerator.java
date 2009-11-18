/* $Id$ */

package ibis.io.rewriter;

import java.util.Arrays;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.AALOAD;
import org.apache.bcel.generic.AASTORE;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ARRAYLENGTH;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.IAND;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.IF_ACMPEQ;
import org.apache.bcel.generic.IF_ICMPEQ;
import org.apache.bcel.generic.IF_ICMPGT;
import org.apache.bcel.generic.IF_ICMPLT;
import org.apache.bcel.generic.IF_ICMPNE;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.SIPUSH;
import org.apache.bcel.generic.SWITCH;
import org.apache.bcel.generic.Type;

/**
 * The CodeGenerator is responsible for generation of the actual bytecode
 * used at runtime to do serialization.
 * 
 * @author Nick Palmer (npr200@few.vu.nl)
 *
 */
class CodeGenerator implements RewriterConstants {

    protected JavaClass clazz;

    protected ClassGen gen;

    protected String classname;

    protected String super_classname;

    protected JavaClass super_class;

    protected boolean super_is_serializable;

    protected boolean super_is_ibis_serializable;

    protected boolean super_has_ibis_constructor;

    protected boolean is_externalizable;

    protected boolean has_serial_persistent_fields;

    protected boolean has_final_fields;

    protected Field[] fields;

    protected Method[] methods;

    protected InstructionFactory factory;

    protected ConstantPoolGen constantpool;

    protected IOGenerator generator;

    CodeGenerator(IOGenerator generator, JavaClass cl) {
        this.generator = generator;
        clazz = cl;
        gen = new ClassGen(clazz);
        classname = clazz.getClassName();
        super_classname = clazz.getSuperclassName();
        super_class = Repository.lookupClass(super_classname);
        fields = gen.getFields();
        methods = gen.getMethods();
        factory = new InstructionFactory(gen);
        constantpool = gen.getConstantPool();

        versionUID();

        /* getFields() does not specify or guarantee a specific order.
         * Therefore, we sort the fields alphabetically, and the
         * serialization code in ibis.io should do the same.
         */
        Arrays.sort(fields, SerializationInfo.fieldComparator);

        super_is_serializable = SerializationInfo.isSerializable(super_class);
        is_externalizable = SerializationInfo.isExternalizable(cl);
        super_is_ibis_serializable = SerializationInfo.isIbisSerializable(super_class);
        super_has_ibis_constructor = SerializationInfo.hasIbisConstructor(super_class);
        has_serial_persistent_fields = SerializationInfo.hasSerialPersistentFields(fields);
        has_final_fields = SerializationInfo.hasFinalFields(fields);
    }

    /**
     * Get the serialversionuid of a class that is about to be
     * rewritten. If necessary, a serialVersionUID field is added.
     */
    private void versionUID() {
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            if (f.getName().equals(FIELD_SERIAL_VERSION_UID) && f.isFinal()
                    && f.isStatic()) {
                /* Already present. Just return. */
                return;
            }
        }

        long uid = 0;
        uid = SerializationInfo.getSerialVersionUID(classname, clazz);

        if (uid != 0) {
            FieldGen f = new FieldGen(Constants.ACC_PRIVATE
                    | Constants.ACC_FINAL | Constants.ACC_STATIC,
                    Type.LONG, FIELD_SERIAL_VERSION_UID, constantpool);
            f.setInitValue(uid);
            gen.addField(f.getField());
            fields = gen.getFields();
        }
    }

    private Instruction createGeneratedWriteObjectInvocation(String name,
            short invmode) {
        return factory.createInvoke(name, METHOD_GENERATED_WRITE_OBJECT,
                Type.VOID, ibis_output_stream_arrtp, invmode);
    }

    private Instruction createGeneratedDefaultReadObjectInvocation(
            String name, InstructionFactory fac, short invmode) {
        return fac.createInvoke(name, METHOD_GENERATED_DEFAULT_READ_OBJECT,
                Type.VOID, new Type[] { ibis_input_stream, Type.INT },
                invmode);
    }

    private Instruction createInitInvocation(String name,
            InstructionFactory f) {
        return f.createInvoke(name, METHOD_INIT, Type.VOID,
                ibis_input_stream_arrtp, Constants.INVOKESPECIAL);
    }

    private Instruction createGeneratedDefaultWriteObjectInvocation(
            String name) {
        return factory.createInvoke(name, METHOD_GENERATED_DEFAULT_WRITE_OBJECT,
                Type.VOID, new Type[] { ibis_output_stream, Type.INT },
                Constants.INVOKESPECIAL);
    }

    private Instruction createWriteObjectInvocation() {
        return factory.createInvoke(classname, METHOD_WRITE_OBJECT, Type.VOID,
                new Type[] { sun_output_stream }, Constants.INVOKESPECIAL);
    }

    private int getClassDepth(JavaClass cl) {
        if (!SerializationInfo.isSerializable(cl)) {
            return 0;
        }
        return 1 + getClassDepth(Repository.lookupClass(
                cl.getSuperclassName()));
    }

    void generateEmptyMethods() {
        /* Generate the necessary (empty) methods. */

        if (generator.isVerbose()) {
            System.out.println("  Generating empty methods for class : "
                    + classname);
            System.out.println("    " + classname
                    + " implements java.io.Serializable -> adding "
                    + TYPE_IBIS_IO_SERIALIZABLE);
        }

        /* add the ibis.io.Serializable interface to the class */
        gen.addInterface(TYPE_IBIS_IO_SERIALIZABLE);

        /* Construct a write method */
        InstructionList il = new InstructionList();
        il.append(new RETURN());

        int flags = Constants.ACC_PUBLIC
        | (gen.isFinal() ? Constants.ACC_FINAL : 0);

        MethodGen write_method = new MethodGen(flags, Type.VOID,
                ibis_output_stream_arrtp, new String[] { VARIABLE_OUTPUT_STREAM },
                METHOD_GENERATED_WRITE_OBJECT, classname, il, constantpool);

        write_method.addException(TYPE_JAVA_IO_IOEXCEPTION);
        gen.addMethod(write_method.getMethod());

        /* ... and a default_write_method */
        il = new InstructionList();
        il.append(new RETURN());

        MethodGen default_write_method = new MethodGen(flags, Type.VOID,
                new Type[] { ibis_output_stream, Type.INT }, new String[] {
                VARIABLE_OUTPUT_STREAM, VARIABLE_LEVEL }, 
                METHOD_GENERATED_DEFAULT_WRITE_OBJECT,
                classname, il, constantpool);

        default_write_method.addException(TYPE_JAVA_IO_IOEXCEPTION);
        gen.addMethod(default_write_method.getMethod());

        /* ... and a default_read_method */
        il = new InstructionList();
        il.append(new RETURN());

        MethodGen default_read_method = new MethodGen(flags, Type.VOID,
                new Type[] { ibis_input_stream, Type.INT }, new String[] {
                VARIABLE_OUTPUT_STREAM, VARIABLE_LEVEL }, 
                METHOD_GENERATED_DEFAULT_READ_OBJECT,
                classname, il, constantpool);

        default_read_method.addException(TYPE_JAVA_IO_IOEXCEPTION);
        default_read_method.addException(
                TYPE_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
        gen.addMethod(default_read_method.getMethod());

        /* Construct a read-of-the-stream constructor, but only when we
         * can actually use it.
         */ 
        if (is_externalizable || !super_is_serializable
                || generator.forceGeneratedCalls() || super_has_ibis_constructor) {
            il = new InstructionList();
            il.append(new RETURN());

            MethodGen read_cons = new MethodGen(Constants.ACC_PUBLIC,
                    Type.VOID, ibis_input_stream_arrtp,
                    new String[] { VARIABLE_INPUT_STREAM }, METHOD_INIT, classname, il,
                    constantpool);
            read_cons.addException(TYPE_JAVA_IO_IOEXCEPTION);
            read_cons.addException(TYPE_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
            gen.addMethod(read_cons.getMethod());
        } else if (SerializationInfo.hasReadObject(methods)) {
            il = new InstructionList();
            il.append(new RETURN());
            MethodGen readobjectWrapper = new MethodGen(
                    Constants.ACC_PUBLIC, Type.VOID,
                    ibis_input_stream_arrtp, new String[] { VARIABLE_INPUT_STREAM },
                    METHOD_$READ_OBJECT_WRAPPER$, classname, il, constantpool);
            readobjectWrapper.addException(TYPE_JAVA_IO_IOEXCEPTION);
            readobjectWrapper.addException(
                    TYPE_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
            gen.addMethod(readobjectWrapper.getMethod());
        }

        /* Now, create a new class structure, which has these methods. */
        JavaClass newclazz = gen.getJavaClass();

        generator.replace(clazz, newclazz);

        clazz = newclazz;
    }

    private InstructionList writeInstructions(Field field) {
        String field_sig = field.getSignature();
        Type field_type = Type.getType(field_sig);
        SerializationInfo info = SerializationInfo.getSerializationInfo(field_type);

        Type t = info.tp;
        InstructionList temp = new InstructionList();

        if (!info.primitive) {
            t = Type.getType(field_sig);
        }

        temp.append(new ALOAD(1));
        temp.append(new ALOAD(0));
        temp.append(factory.createFieldAccess(classname, field.getName(),
                t, Constants.GETFIELD));
        temp.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                info.write_name, Type.VOID, info.param_tp_arr,
                Constants.INVOKEVIRTUAL));

        return temp;
    }

    private InstructionList readInstructions(Field field,
            boolean from_constructor) {
        String field_sig = field.getSignature();
        Type field_type = Type.getType(field_sig);
        SerializationInfo info = SerializationInfo.getSerializationInfo(field_type);

        Type t = info.tp;
        InstructionList temp = new InstructionList();

        if (!info.primitive) {
            t = Type.getType(field_sig);
        }

        if (from_constructor || !field.isFinal()) {
            temp.append(new ALOAD(0));
            temp.append(new ALOAD(1));
            temp.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    info.read_name, info.tp, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));

            if (!info.primitive) {
                temp.append(factory.createCheckCast((ReferenceType) t));
            }

            temp.append(factory.createFieldAccess(classname,
                    field.getName(), t, Constants.PUTFIELD));
        } else {
            temp.append(new ALOAD(1));
            temp.append(new ALOAD(0));
            int ind = constantpool.addString(field.getName());
            temp.append(new LDC(ind));
            ind = constantpool.addString(classname);
            temp.append(new LDC(ind));
            if (!info.primitive) {
                int ind2 = constantpool.addString(field_sig);
                temp.append(new LDC(ind2));
            }
            temp.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    info.final_read_name, Type.VOID,
                    info.primitive ? new Type[] {
                    Type.OBJECT, Type.STRING,
                    Type.STRING }
            : new Type[] { Type.OBJECT,
                            Type.STRING, Type.STRING,
                            Type.STRING },
                            Constants.INVOKEVIRTUAL));
        }

        return temp;
    }

    private String writeCallName(String name) {
        return METHOD_WRITE_ARRAY + name.substring(0, 1).toUpperCase()
        + name.substring(1);
    }

    private InstructionList writeReferenceField(Field field) {
        Type field_type = Type.getType(field.getSignature());
        InstructionList write_il = new InstructionList();

        boolean isfinal = false;
        boolean isarray = false;
        JavaClass field_class = null;
        String basicname = null;

        if (generator.isVerbose()) {
            System.out.println("    writing reference field "
                    + field.getName() + " of type "
                    + field_type.getSignature());
        }

        if (field_type instanceof ObjectType) {
            field_class = Repository.lookupClass(
                    ((ObjectType) field_type).getClassName());
            if (field_class != null && field_class.isFinal()) {
                isfinal = true;
            }
        } else if (field_type instanceof ArrayType) {
            isarray = true;
            Type el_type = ((ArrayType) field_type).getElementType();
            if (el_type instanceof ObjectType) {
                field_class = Repository.lookupClass(
                        ((ObjectType) el_type).getClassName());
                if (field_class != null && field_class.isFinal()) {
                    isfinal = true;
                }
            } else if (el_type instanceof BasicType) {
                basicname = el_type.toString();
            }
        }

        if ((basicname != null)
                || (isfinal
                        && (SerializationInfo.hasIbisConstructor(field_class)
                                || (SerializationInfo.isSerializable(field_class)
                                        && generator.forceGeneratedCalls())))) {
            // If there is an object replacer, we cannot do the
            // "fast" code.
            write_il.append(new ACONST_NULL());
            write_il.append(new ALOAD(1));
            write_il.append(factory.createFieldAccess(
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, VARIABLE_REPLACER, 
                    new ObjectType(TYPE_IBIS_IO_REPLACER),
                    Constants.GETFIELD));
            IF_ACMPEQ replacertest = new IF_ACMPEQ(null);
            write_il.append(replacertest);
            write_il.append(writeInstructions(field));
            GOTO toEnd = new GOTO(null);
            write_il.append(toEnd);

            // "fast" code.
            replacertest.setTarget(write_il.append(new ALOAD(1)));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createFieldAccess(classname,
                    field.getName(), field_type, Constants.GETFIELD));
            if (basicname != null) {
                write_il.append(factory.createFieldAccess(
                        TYPE_IBIS_IO_CONSTANTS,
                        "TYPE_" + basicname.toUpperCase(), Type.INT,
                        Constants.GETSTATIC));
                write_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_KNOWN_ARRAY_HEADER,
                        Type.INT, new Type[] { Type.OBJECT, Type.INT },
                        Constants.INVOKEVIRTUAL));
            } else {
                write_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_KNOWN_OBJECT_HEADER,
                        Type.INT, new Type[] { Type.OBJECT },
                        Constants.INVOKEVIRTUAL));
            }
            write_il.append(new ISTORE(2));
            write_il.append(new ILOAD(2));
            write_il.append(new ICONST(1));

            IF_ICMPNE ifcmp = new IF_ICMPNE(null);

            write_il.append(ifcmp);

            if (isarray) {
                write_il.append(new ALOAD(1));
                write_il.append(new ALOAD(0));
                write_il.append(factory.createFieldAccess(classname,
                        field.getName(), field_type, Constants.GETFIELD));
                write_il.append(new ARRAYLENGTH());
                write_il.append(new DUP());
                write_il.append(new ISTORE(4));
                write_il.append(
                        factory.createInvoke(
                                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                                METHOD_WRITE_INT,
                                Type.VOID, new Type[] { Type.INT },
                                Constants.INVOKEVIRTUAL));
                if (basicname != null) {
                    write_il.append(new ALOAD(1));
                    write_il.append(new ALOAD(0));
                    write_il.append(
                            factory.createFieldAccess(classname,
                                    field.getName(),
                                    field_type, Constants.GETFIELD));
                    write_il.append(new ICONST(0));
                    write_il.append(new ILOAD(4));
                    write_il.append(
                            factory.createInvoke(
                                    TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                                    writeCallName(basicname), Type.VOID,
                                    new Type[] { field_type, Type.INT,
                                        Type.INT },
                                        Constants.INVOKEVIRTUAL));
                } else {
                    write_il.append(new ICONST(0));
                    write_il.append(new ISTORE(3));
                    GOTO gto = new GOTO(null);
                    write_il.append(gto);

                    InstructionHandle loop_body_start
                    = write_il.append(new ALOAD(1));
                    write_il.append(new ALOAD(0));
                    write_il.append(
                            factory.createFieldAccess(classname,
                                    field.getName(), field_type,
                                    Constants.GETFIELD));
                    write_il.append(new ILOAD(3));
                    write_il.append(new AALOAD());

                    write_il.append(
                            factory.createInvoke(
                                    TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                                    METHOD_WRITE_KNOWN_OBJECT_HEADER, Type.INT,
                                    new Type[] { Type.OBJECT },
                                    Constants.INVOKEVIRTUAL));
                    write_il.append(new ISTORE(2));
                    write_il.append(new ILOAD(2));
                    write_il.append(new ICONST(1));
                    IF_ICMPNE ifcmp1 = new IF_ICMPNE(null);
                    write_il.append(ifcmp1);

                    write_il.append(new ALOAD(0));
                    write_il.append(
                            factory.createFieldAccess(classname,
                                    field.getName(),
                                    field_type, Constants.GETFIELD));
                    write_il.append(new ILOAD(3));
                    write_il.append(new AALOAD());
                    write_il.append(new ALOAD(1));
                    write_il.append(
                            createGeneratedWriteObjectInvocation(
                                    field_class.getClassName(),
                                    Constants.INVOKEVIRTUAL));

                    ifcmp1.setTarget(write_il.append(new IINC(3, 1)));
                    gto.setTarget(write_il.append(new ILOAD(4)));

                    write_il.append(new ILOAD(3));
                    write_il.append(new IF_ICMPGT(loop_body_start));
                }
            } else {
                write_il.append(new ALOAD(0));
                write_il.append(
                        factory.createFieldAccess(classname,
                                field.getName(), field_type,
                                Constants.GETFIELD));
                write_il.append(new ALOAD(1));

                write_il.append(
                        createGeneratedWriteObjectInvocation(
                                field_class.getClassName(),
                                Constants.INVOKEVIRTUAL));
            }

            InstructionHandle target = write_il.append(new NOP());
            ifcmp.setTarget(target);
            toEnd.setTarget(target);
        } else {
            write_il.append(writeInstructions(field));
        }
        return write_il;
    }

    private InstructionList serialPersistentWrites(MethodGen write_gen) {
        Instruction persistent_field_access = factory.createFieldAccess(
                classname, FIELD_SERIAL_PERSISTENT_FIELDS, new ArrayType(
                        new ObjectType(TYPE_JAVA_IO_OBJECT_STREAM_FIELD), 1),
                        Constants.GETSTATIC);
        InstructionList write_il = new InstructionList();
        int[] case_values = new int[] { CASE_BOOLEAN, CASE_CHAR, CASE_DOUBLE, CASE_FLOAT, CASE_INT, CASE_LONG, CASE_SHORT,
                CASE_OBJECT };
        InstructionHandle[] case_handles
        = new InstructionHandle[case_values.length];
        GOTO[] gotos = new GOTO[case_values.length + 1];

        for (int i = 0; i < gotos.length; i++) {
            gotos[i] = new GOTO(null);
        }

        write_il.append(new SIPUSH((short) 0));
        write_il.append(new ISTORE(2));

        GOTO gto = new GOTO(null);
        write_il.append(gto);

        InstructionHandle loop_body_start
        = write_il.append(persistent_field_access);
        write_il.append(new ILOAD(2));
        write_il.append(new AALOAD());
        write_il.append(
                factory.createInvoke(TYPE_JAVA_IO_OBJECT_STREAM_FIELD,
                        METHOD_GET_NAME, Type.STRING, Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));
        write_il.append(new ASTORE(3));

        InstructionHandle begin_try = write_il.append(new PUSH(
                constantpool, classname));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS, METHOD_FOR_NAME,
                java_lang_class_type, new Type[] { Type.STRING },
                Constants.INVOKESTATIC));
        write_il.append(new ALOAD(3));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS, METHOD_GET_FIELD,
                new ObjectType(TYPE_JAVA_LANG_REFLECT_FIELD),
                new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
        write_il.append(new ASTORE(4));

        write_il.append(persistent_field_access);
        write_il.append(new ILOAD(2));
        write_il.append(new AALOAD());
        write_il.append(factory.createInvoke(TYPE_JAVA_IO_OBJECT_STREAM_FIELD,
                METHOD_GET_TYPE_CODE, Type.CHAR, Type.NO_ARGS,
                Constants.INVOKEVIRTUAL));

        case_handles[0] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_BOOLEAN, Type.BOOLEAN, new Type[] { Type.OBJECT },
                Constants.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_BOOLEAN,
                Type.VOID, new Type[] { Type.BOOLEAN },
                Constants.INVOKEVIRTUAL));
        write_il.append(gotos[0]);

        case_handles[1] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_CHAR, Type.CHAR, new Type[] { Type.OBJECT },
                Constants.INVOKEVIRTUAL));
        write_il.append(
                factory.createInvoke(
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                        METHOD_WRITE_CHAR, Type.VOID, new Type[] { Type.INT },
                        Constants.INVOKEVIRTUAL));
        write_il.append(gotos[1]);

        case_handles[2] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_DOUBLE, Type.DOUBLE, new Type[] { Type.OBJECT },
                Constants.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_DOUBLE,
                Type.VOID, new Type[] { Type.DOUBLE },
                Constants.INVOKEVIRTUAL));
        write_il.append(gotos[2]);

        case_handles[3] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_FLOAT, Type.FLOAT, new Type[] { Type.OBJECT },
                Constants.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_FLOAT,
                Type.VOID, new Type[] { Type.FLOAT },
                Constants.INVOKEVIRTUAL));
        write_il.append(gotos[3]);

        case_handles[4] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_INT, Type.INT, new Type[] { Type.OBJECT },
                Constants.INVOKEVIRTUAL));
        write_il.append(
                factory.createInvoke(
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                        METHOD_WRITE_INT, Type.VOID, new Type[] { Type.INT },
                        Constants.INVOKEVIRTUAL));
        write_il.append(gotos[4]);

        case_handles[5] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_LONG, Type.LONG, new Type[] { Type.OBJECT },
                Constants.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_LONG,
                Type.VOID, new Type[] { Type.LONG },
                Constants.INVOKEVIRTUAL));
        write_il.append(gotos[5]);

        case_handles[6] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_SHORT, Type.SHORT, new Type[] { Type.OBJECT },
                Constants.INVOKEVIRTUAL));
        write_il.append(
                factory.createInvoke(
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_SHORT,
                        Type.VOID, new Type[] { Type.INT },
                        Constants.INVOKEVIRTUAL));
        write_il.append(gotos[6]);

        case_handles[7] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_BOOLEAN, Type.BOOLEAN, new Type[] { Type.OBJECT },
                Constants.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_BOOLEAN,
                Type.VOID, new Type[] { Type.BOOLEAN },
                Constants.INVOKEVIRTUAL));
        write_il.append(gotos[7]);

        InstructionHandle default_handle = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET, Type.OBJECT, new Type[] { Type.OBJECT },
                Constants.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                METHOD_WRITE_OBJECT, Type.VOID,
                new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
        InstructionHandle end_try = write_il.append(gotos[8]);

        write_il.insert(case_handles[0], new SWITCH(case_values,
                case_handles, default_handle));

        InstructionHandle handler = write_il.append(new ASTORE(6));
        write_il.append(factory.createNew(TYPE_JAVA_IO_IOEXCEPTION));
        write_il.append(new DUP());
        write_il.append(factory.createNew(TYPE_JAVA_LANG_STRING_BUFFER));
        write_il.append(new DUP());
        write_il.append(
                factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                        METHOD_INIT, Type.VOID, Type.NO_ARGS,
                        Constants.INVOKESPECIAL));
        write_il.append(new PUSH(constantpool, "Could not write field "));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_APPEND, Type.STRINGBUFFER, new Type[] { Type.STRING },
                Constants.INVOKEVIRTUAL));
        write_il.append(new ALOAD(3));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_APPEND, Type.STRINGBUFFER, new Type[] { Type.STRING },
                Constants.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_TO_STRING, Type.STRING, Type.NO_ARGS,
                Constants.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(TYPE_JAVA_IO_IOEXCEPTION,
                METHOD_INIT, Type.VOID, new Type[] { Type.STRING },
                Constants.INVOKESPECIAL));
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

        write_gen.addExceptionHandler(begin_try, end_try, handler,
                new ObjectType(TYPE_JAVA_LANG_EXCEPTION));

        return write_il;
    }

    private InstructionList generateDefaultWrites(MethodGen write_gen) {
        InstructionList write_il = new InstructionList();

        if (has_serial_persistent_fields) {
            return serialPersistentWrites(write_gen);
        }

        /* handle the primitive fields */

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];

            /* Don't send fields that are STATIC, or TRANSIENT */
            if (!(field.isStatic() || field.isTransient())) {
                Type field_type = Type.getType(field.getSignature());

                if (field_type instanceof BasicType) {
                    if (generator.isVerbose()) {
                        System.out.println("    writing basic field "
                                + field.getName() + " of type "
                                + field.getSignature());
                    }

                    write_il.append(writeInstructions(field));
                }
            }
        }

        /* handle the reference fields */

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];

            /* Don't send fields that are STATIC or TRANSIENT */
            if (!(field.isStatic() || field.isTransient())) {
                Type field_type = Type.getType(field.getSignature());

                if (field_type instanceof ReferenceType) {
                    if (generator.isVerbose()) {
                        System.out.println("    writing field "
                                + field.getName() + " of type "
                                + field.getSignature());
                    }
                    if (!field_type.equals(Type.STRING)
                            && !field_type.equals(java_lang_class_type)) {
                        write_il.append(writeReferenceField(field));
                    } else {
                        write_il.append(writeInstructions(field));
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

    private InstructionList readReferenceField(Field field,
            boolean from_constructor) {
        Type field_type = Type.getType(field.getSignature());
        InstructionList read_il = new InstructionList();

        boolean isfinal = false;
        boolean isarray = false;
        JavaClass field_class = null;
        String basicname = null;

        if (generator.isVerbose()) {
            System.out.println("    reading reference field "
                    + field.getName() + " of type "
                    + field_type.getSignature());
        }

        if (field_type instanceof ObjectType) {
            field_class = Repository.lookupClass(
                    ((ObjectType) field_type).getClassName());
            if (field_class != null && field_class.isFinal()) {
                isfinal = true;
            }
        } else if (field_type instanceof ArrayType) {
            isarray = true;
            Type el_type = ((ArrayType) field_type).getElementType();
            if (el_type instanceof ObjectType) {
                field_class = Repository.lookupClass(
                        ((ObjectType) el_type).getClassName());
                if (field_class != null && field_class.isFinal()) {
                    isfinal = true;
                }
            } else if (el_type instanceof BasicType) {
                basicname = el_type.toString();
            }
        }

        if ((basicname != null)
                || (isfinal
                        && (SerializationInfo.hasIbisConstructor(field_class)
                                || (SerializationInfo.isSerializable(field_class)
                                        && generator.forceGeneratedCalls())))) {
            read_il.append(new ALOAD(1));
            read_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ_KNOWN_TYPE_HEADER, Type.INT, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));
            read_il.append(new ISTORE(2));
            read_il.append(new ILOAD(2));
            read_il.append(new ICONST(-1));

            IF_ICMPNE ifcmp = new IF_ICMPNE(null);
            read_il.append(ifcmp);

            if (isarray) {
                if (basicname != null) {
                    String callname = readCallName(basicname);

                    read_il.append(new ALOAD(0));
                    read_il.append(new ALOAD(1));

                    read_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM, callname, field_type,
                            Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                    read_il.append(
                            factory.createFieldAccess(
                                    classname, field.getName(),
                                    field_type, Constants.PUTFIELD));
                } else {
                    Type el_type
                    = ((ArrayType) field_type).getElementType();
                    read_il.append(new ALOAD(0));
                    read_il.append(new ALOAD(1));
                    read_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM, METHOD_READ_INT, Type.INT,
                            Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                    read_il.append(new DUP());
                    read_il.append(new ISTORE(3));
                    read_il.append(factory.createNewArray(el_type,
                            (short) 1));
                    read_il.append(
                            factory.createFieldAccess(classname,
                                    field.getName(),
                                    field_type, Constants.PUTFIELD));
                    read_il.append(new ALOAD(1));
                    read_il.append(new ALOAD(0));
                    read_il.append(
                            factory.createFieldAccess(classname,
                                    field.getName(),
                                    field_type, Constants.GETFIELD));

                    read_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                            METHOD_ADD_OBJECT_TO_CYCLE_CHECK, Type.VOID,
                            new Type[] { Type.OBJECT },
                            Constants.INVOKEVIRTUAL));
                    read_il.append(new ICONST(0));
                    read_il.append(new ISTORE(4));
                    GOTO gto1 = new GOTO(null);
                    read_il.append(gto1);

                    InstructionHandle loop_body_start
                    = read_il.append(new ALOAD(1));
                    read_il.append(
                            factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                                    METHOD_READ_KNOWN_TYPE_HEADER, Type.INT,
                                    Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                    read_il.append(new ISTORE(2));
                    read_il.append(new ILOAD(2));
                    read_il.append(new ICONST(-1));

                    IF_ICMPNE ifcmp1 = new IF_ICMPNE(null);
                    read_il.append(ifcmp1);

                    read_il.append(new ALOAD(0));
                    read_il.append(
                            factory.createFieldAccess(classname,
                                    field.getName(),
                                    field_type, Constants.GETFIELD));
                    read_il.append(new ILOAD(4));

                    read_il.append(factory.createNew((ObjectType) el_type));
                    read_il.append(new DUP());
                    read_il.append(new ALOAD(1));
                    read_il.append(createInitInvocation(
                            field_class.getClassName(), factory));
                    read_il.append(new AASTORE());
                    GOTO gto2 = new GOTO(null);
                    read_il.append(gto2);
                    InstructionHandle cmp_goto2 = read_il.append(new ILOAD(
                            2));
                    ifcmp1.setTarget(cmp_goto2);
                    read_il.append(new ICONST(0));
                    IF_ICMPEQ ifcmpeq2 = new IF_ICMPEQ(null);
                    read_il.append(ifcmpeq2);

                    read_il.append(new ALOAD(0));
                    read_il.append(factory.createFieldAccess(classname,
                            field.getName(),
                            field_type, Constants.GETFIELD));
                    read_il.append(new ILOAD(4));

                    read_il.append(new ALOAD(1));
                    read_il.append(new ILOAD(2));
                    read_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                            METHOD_GET_OBJECT_FROM_CYCLE_CHECK, Type.OBJECT,
                            new Type[] { Type.INT },
                            Constants.INVOKEVIRTUAL));
                    read_il.append(
                            factory.createCheckCast(
                                    (ReferenceType) el_type));
                    read_il.append(new AASTORE());
                    InstructionHandle target2 = read_il.append(new NOP());
                    ifcmpeq2.setTarget(target2);
                    gto2.setTarget(target2);
                    read_il.append(new IINC(4, 1));
                    gto1.setTarget(read_il.append(new ILOAD(3)));
                    read_il.append(new ILOAD(4));
                    read_il.append(new IF_ICMPGT(loop_body_start));
                }
            } else {
                read_il.append(new ALOAD(0));

                read_il.append(factory.createNew((ObjectType) field_type));
                read_il.append(new DUP());
                read_il.append(new ALOAD(1));
                read_il.append(
                        createInitInvocation(field_class.getClassName(),
                                factory));
                read_il.append(
                        factory.createFieldAccess(classname,
                                field.getName(), field_type,
                                Constants.PUTFIELD));
            }

            GOTO gto = new GOTO(null);
            read_il.append(gto);

            InstructionHandle cmp_goto = read_il.append(new ILOAD(2));
            ifcmp.setTarget(cmp_goto);

            read_il.append(new ICONST(0));

            IF_ICMPEQ ifcmpeq = new IF_ICMPEQ(null);
            read_il.append(ifcmpeq);
            read_il.append(new ALOAD(0));
            read_il.append(new ALOAD(1));
            read_il.append(new ILOAD(2));
            read_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_GET_OBJECT_FROM_CYCLE_CHECK, Type.OBJECT,
                    new Type[] { Type.INT }, Constants.INVOKEVIRTUAL));

            read_il.append(
                    factory.createCheckCast((ReferenceType) field_type));
            read_il.append(
                    factory.createFieldAccess(classname, field.getName(),
                            field_type, Constants.PUTFIELD));

            InstructionHandle target = read_il.append(new NOP());
            ifcmpeq.setTarget(target);
            gto.setTarget(target);
        } else {
            read_il.append(readInstructions(field, from_constructor));
        }

        return read_il;
    }

    private InstructionHandle generateReadField(String tpname, Type tp,
            InstructionList read_il, GOTO gto, boolean from_constructor) {
        InstructionHandle h;

        if (from_constructor || !has_final_fields) {
            h = read_il.append(new ALOAD(4));
            read_il.append(new ALOAD(0));
            read_il.append(new ALOAD(1));
            if (tpname.equals("")) {
                read_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_READ_OBJECT, Type.OBJECT, Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));
            } else {
                read_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_READ + tpname, tp, Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));
            }
            read_il.append(
                    factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                            METHOD_SET + tpname, Type.VOID,
                            new Type[] { Type.OBJECT, tp },
                            Constants.INVOKEVIRTUAL));
            read_il.append(gto);

            return h;
        }

        h = read_il.append(new ILOAD(5));
        read_il.append(new PUSH(constantpool, Constants.ACC_FINAL));
        read_il.append(new IAND());
        IFEQ eq = new IFEQ(null);
        read_il.append(eq);
        read_il.append(new ALOAD(1));
        read_il.append(new ALOAD(0));
        read_il.append(new ALOAD(3));
        if (tpname.equals("")) {
            read_il.append(new ALOAD(4));
            read_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                    METHOD_GET_TYPE, new ObjectType(TYPE_JAVA_LANG_CLASS),
                    Type.NO_ARGS, Constants.INVOKEVIRTUAL));
            read_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS,
                    METHOD_GET_NAME, Type.STRING, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));

            read_il.append(factory.createInvoke(
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ_FIELD_OBJECT, Type.VOID, new Type[] { Type.OBJECT,
                            Type.STRING, Type.STRING },
                            Constants.INVOKEVIRTUAL));
        } else {
            read_il.append(factory.createInvoke(
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM, METHOD_READ_FIELD
                    + tpname, Type.VOID, new Type[] { Type.OBJECT,
                            Type.STRING }, Constants.INVOKEVIRTUAL));
        }
        GOTO gto2 = new GOTO(null);
        read_il.append(gto2);
        eq.setTarget(read_il.append(new ALOAD(4)));
        read_il.append(new ALOAD(0));
        read_il.append(new ALOAD(1));
        if (tpname.equals("")) {
            read_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ_OBJECT, tp, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));
        } else {
            read_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ + tpname, tp, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));
        }
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_SET + tpname, Type.VOID, new Type[] { Type.OBJECT, tp },
                Constants.INVOKEVIRTUAL));
        gto2.setTarget(read_il.append(gto));

        return h;
    }

    private InstructionList serialPersistentReads(boolean from_constructor,
            MethodGen read_gen) {
        Instruction persistent_field_access = factory.createFieldAccess(
                classname, FIELD_SERIAL_PERSISTENT_FIELDS, new ArrayType(
                        new ObjectType(TYPE_JAVA_IO_OBJECT_STREAM_FIELD), 1),
                        Constants.GETSTATIC);
        InstructionList read_il = new InstructionList();
        int[] case_values = new int[] { CASE_BOOLEAN, CASE_CHAR, CASE_DOUBLE, CASE_FLOAT, CASE_INT, CASE_LONG, CASE_SHORT,
                CASE_OBJECT };
        InstructionHandle[] case_handles
        = new InstructionHandle[case_values.length];
        GOTO[] gotos = new GOTO[case_values.length + 1];

        for (int i = 0; i < gotos.length; i++) {
            gotos[i] = new GOTO(null);
        }

        read_il.append(new SIPUSH((short) 0));
        read_il.append(new ISTORE(2));

        GOTO gto = new GOTO(null);
        read_il.append(gto);

        InstructionHandle loop_body_start
        = read_il.append(persistent_field_access);
        read_il.append(new ILOAD(2));
        read_il.append(new AALOAD());
        read_il.append(factory.createInvoke(TYPE_JAVA_IO_OBJECT_STREAM_FIELD,
                METHOD_GET_NAME, Type.STRING, Type.NO_ARGS,
                Constants.INVOKEVIRTUAL));
        read_il.append(new ASTORE(3));

        InstructionHandle begin_try = read_il.append(new PUSH(constantpool,
                classname));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS, METHOD_FOR_NAME,
                java_lang_class_type, new Type[] { Type.STRING },
                Constants.INVOKESTATIC));
        read_il.append(new ALOAD(3));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS, METHOD_GET_FIELD,
                new ObjectType(TYPE_JAVA_LANG_REFLECT_FIELD),
                new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
        read_il.append(new ASTORE(4));

        if (!from_constructor && has_final_fields) {
            read_il.append(new ALOAD(4));
            read_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                    METHOD_GET_MODIFIERS, Type.INT, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));
            read_il.append(new ISTORE(5));
        }

        read_il.append(persistent_field_access);
        read_il.append(new ILOAD(2));
        read_il.append(new AALOAD());
        read_il.append(factory.createInvoke(TYPE_JAVA_IO_OBJECT_STREAM_FIELD,
                METHOD_GET_TYPE_CODE, Type.CHAR, Type.NO_ARGS,
                Constants.INVOKEVIRTUAL));

        case_handles[0] = generateReadField(TYPE_BYTE, Type.BYTE, read_il,
                gotos[0], from_constructor);
        case_handles[1] = generateReadField(TYPE_CHAR, Type.CHAR, read_il,
                gotos[1], from_constructor);
        case_handles[2] = generateReadField(TYPE_DOUBLE, Type.DOUBLE, read_il,
                gotos[2], from_constructor);
        case_handles[3] = generateReadField(TYPE_FLOAT, Type.FLOAT, read_il,
                gotos[3], from_constructor);
        case_handles[4] = generateReadField(TYPE_INT, Type.INT, read_il,
                gotos[4], from_constructor);
        case_handles[5] = generateReadField(TYPE_LONG, Type.LONG, read_il,
                gotos[5], from_constructor);
        case_handles[6] = generateReadField(TYPE_SHORT, Type.SHORT, read_il,
                gotos[6], from_constructor);
        case_handles[7] = generateReadField(TYPE_BOOLEAN, Type.BOOLEAN,
                read_il, gotos[7], from_constructor);

        InstructionHandle default_handle = generateReadField("",
                Type.OBJECT, read_il, gotos[8], from_constructor);

        InstructionHandle end_try = read_il.getEnd();

        read_il.insert(case_handles[0], new SWITCH(case_values,
                case_handles, default_handle));

        InstructionHandle handler = read_il.append(new ASTORE(6));
        read_il.append(factory.createNew(TYPE_JAVA_IO_IOEXCEPTION));
        read_il.append(new DUP());
        read_il.append(factory.createNew(TYPE_JAVA_LANG_STRING_BUFFER));
        read_il.append(new DUP());
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_INIT, Type.VOID, Type.NO_ARGS,
                Constants.INVOKESPECIAL));
        read_il.append(new PUSH(constantpool, "Could not read field "));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_APPEND, Type.STRINGBUFFER, new Type[] { Type.STRING },
                Constants.INVOKEVIRTUAL));
        read_il.append(new ALOAD(3));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_APPEND, Type.STRINGBUFFER, new Type[] { Type.STRING },
                Constants.INVOKEVIRTUAL));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_TO_STRING, Type.STRING, Type.NO_ARGS,
                Constants.INVOKEVIRTUAL));
        read_il.append(factory.createInvoke(TYPE_JAVA_IO_IOEXCEPTION,
                METHOD_INIT, Type.VOID, new Type[] { Type.STRING },
                Constants.INVOKESPECIAL));
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

        read_gen.addExceptionHandler(begin_try, end_try, handler,
                new ObjectType(TYPE_JAVA_LANG_EXCEPTION));

        return read_il;
    }

    private InstructionList generateDefaultReads(boolean from_constructor,
            MethodGen read_gen) {
        InstructionList read_il = new InstructionList();

        if (has_serial_persistent_fields) {
            return serialPersistentReads(from_constructor, read_gen);
        }

        /* handle the primitive fields */

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];

            /* Don't send fields that are STATIC, or TRANSIENT */
            if (!(field.isStatic() || field.isTransient())) {
                Type field_type = Type.getType(field.getSignature());

                if (field_type instanceof BasicType) {
                    if (generator.isVerbose()) {
                        System.out.println("    writing basic field "
                                + field.getName() + " of type "
                                + field_type.getSignature());
                    }

                    read_il.append(readInstructions(field,
                            from_constructor));
                }
            }
        }

        /* handle the reference fields. */

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];

            /* Don't send fields that are STATIC or TRANSIENT */
            if (!(field.isStatic() || field.isTransient())) {
                Type field_type = Type.getType(field.getSignature());

                if (generator.isVerbose()) {
                    System.out.println("    writing field "
                            + field.getName() + " of type "
                            + field.getSignature());
                }

                if (field_type instanceof ReferenceType) {
                    if (!field_type.equals(Type.STRING)
                            && !field_type.equals(java_lang_class_type)) {
                        read_il.append(readReferenceField(field,
                                from_constructor));
                    } else {
                        read_il.append(readInstructions(field,
                                from_constructor));
                    }
                }
            }
        }

        return read_il;
    }

    private JavaClass generateInstanceGenerator() {

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
                    + classname);
        }

        String name = classname + METHOD_IBIS_IO_GENERATOR;

        ObjectType class_type = new ObjectType(classname);

        String classfilename = name.substring(name.lastIndexOf('.') + 1)
        + ".class";
        ClassGen iogenGen = new ClassGen(name, TYPE_IBIS_IO_GENERATOR,
                classfilename, Constants.ACC_FINAL | Constants.ACC_PUBLIC
                | Constants.ACC_SUPER, null);
        InstructionFactory iogenFactory = new InstructionFactory(iogenGen);

        InstructionList il = new InstructionList();

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
            il.append(new ALOAD(1));
            int ind = iogenGen.getConstantPool().addString(classname);
            il.append(new LDC(ind));
            il.append(iogenFactory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_CREATE_UNINITIALIZED_OBJECT, Type.OBJECT,
                    new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
            il.append(iogenFactory.createCheckCast(class_type));
            il.append(new ASTORE(2));

            /* Now read the superclass. */
            il.append(new ALOAD(1));
            il.append(new ALOAD(2));
            ind = iogenGen.getConstantPool().addString(super_classname);
            il.append(new LDC(ind));
            il.append(iogenFactory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                    METHOD_READ_SERIALIZABLE_OBJECT, Type.VOID, new Type[] {
                    Type.OBJECT, Type.STRING },
                    Constants.INVOKEVIRTUAL));

            /* Now, if the class has a readObject, call it. Otherwise,
             * read its fields, by calling generated_DefaultReadObject.
             */
            if (SerializationInfo.hasReadObject(methods)) {
                il.append(new ALOAD(2));
                il.append(new ALOAD(1));
                il.append(iogenFactory.createInvoke(classname,
                        METHOD_$READ_OBJECT_WRAPPER$, Type.VOID,
                        ibis_input_stream_arrtp, Constants.INVOKEVIRTUAL));
            } else {
                int dpth = getClassDepth(clazz);

                il.append(new ALOAD(2));
                il.append(new ALOAD(1));
                il.append(new SIPUSH((short) dpth));
                il.append(createGeneratedDefaultReadObjectInvocation(
                        classname, iogenFactory, Constants.INVOKEVIRTUAL));
            }
            il.append(new ALOAD(2));
        } else {
            il.append(iogenFactory.createNew(class_type));
            il.append(new DUP());
            il.append(new ALOAD(1));
            il.append(createInitInvocation(classname, iogenFactory));
        }
        il.append(new ARETURN());

        /*
         0       new DITree
         3       dup
         4       aload_1
         5       invokespecial DITree(ibis.io.IbisSerializationInputStream)
         8       areturn
         */

        MethodGen method = new MethodGen(
                Constants.ACC_FINAL | Constants.ACC_PUBLIC, Type.OBJECT,
                ibis_input_stream_arrtp, new String[] { VARIABLE_INPUT_STREAM },
                METHOD_GENERATED_NEW_INSTANCE, name, il,
                iogenGen.getConstantPool());

        method.setMaxStack(3);
        method.setMaxLocals();
        method.addException(TYPE_JAVA_IO_IOEXCEPTION);
        method.addException(TYPE_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
        iogenGen.addMethod(method.getMethod());

        il = new InstructionList();
        il.append(new ALOAD(0));
        il.append(iogenFactory.createInvoke(TYPE_IBIS_IO_GENERATOR, METHOD_INIT,
                Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
        il.append(new RETURN());

        method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID,
                Type.NO_ARGS, null, METHOD_INIT, name, il,
                iogenGen.getConstantPool());

        method.setMaxStack(1);
        method.setMaxLocals();
        iogenGen.addMethod(method.getMethod());

        return iogenGen.getJavaClass();
    }

    void generateCode() {
        /* Generate code inside the methods */
        if (generator.isVerbose()) {
            System.out.println("  Generating method code class for class : "
                    + classname);
            System.out.println("    Number of fields " + fields.length);
        }

        int dpth = getClassDepth(clazz);

        fillInGeneratedDefaultWriteObjectMethod(dpth);
        fillInGeneratedDefaultReadObjectMethod(dpth);
        fillInGeneratedWriteObjectMethod(dpth);
        fillInGeneratedReadObjectMethod(dpth);

        clazz = gen.getJavaClass();

        Repository.removeClass(classname);
        Repository.addClass(clazz);

        JavaClass instgen = generateInstanceGenerator();

        Repository.addClass(instgen);

        generator.markRewritten(clazz, instgen);
    }

    private void fillInGeneratedReadObjectMethod(int dpth) {
        /* Now, produce the read constructor. It only exists if the
         * superclass is not serializable, or if the superclass has an
         * ibis constructor, or is assumed to have one (-force option).
         */

        /* Now, do the same for the reading side. */
        MethodGen mgen = null;
        int index = -1;    
        InstructionList read_il = null;
        if (is_externalizable || super_has_ibis_constructor
                || !super_is_serializable || generator.forceGeneratedCalls()) {
            read_il = new InstructionList();
            if (is_externalizable) {
                read_il.append(new ALOAD(0));
                read_il.append(factory.createInvoke(classname, METHOD_INIT,
                        Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
            } else if (!super_is_serializable) {
                read_il.append(new ALOAD(0));
                read_il.append(factory.createInvoke(super_classname,
                        METHOD_INIT, Type.VOID, Type.NO_ARGS,
                        Constants.INVOKESPECIAL));
            } else {
                read_il.append(new ALOAD(0));
                read_il.append(new ALOAD(1));
                read_il.append(createInitInvocation(super_classname,
                        factory));
            }

            if (is_externalizable || !super_is_serializable) {
                read_il.append(new ALOAD(1));
                read_il.append(new ALOAD(0));
                read_il.append(
                        factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                                METHOD_ADD_OBJECT_TO_CYCLE_CHECK, Type.VOID,
                                new Type[] { Type.OBJECT },
                                Constants.INVOKEVIRTUAL));
            }

            int read_cons_index = SerializationInfo.findMethod(methods,
                    METHOD_INIT, 
                    SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V);
            mgen = new MethodGen(methods[read_cons_index], classname,
                    constantpool);
            index = read_cons_index;
        } else if (SerializationInfo.hasReadObject(methods)) {
            int read_wrapper_index = SerializationInfo.findMethod(methods,
                    METHOD_$READ_OBJECT_WRAPPER$, 
                    SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_V);
            mgen = new MethodGen(methods[read_wrapper_index], classname,
                    constantpool);
            read_il = new InstructionList();
            index = read_wrapper_index;
        }

        /* TODO: Shouldn't there be an else clause here that fills in a method?
         * Even an exception throwing method might be better?
         * There is almost certainly a problem here in some corner case.
         */

        if (read_il != null) {
            if (is_externalizable || SerializationInfo.hasReadObject(methods)) {
                /* First, get and set IbisSerializationInputStream's idea of the current object. */
                read_il.append(new ALOAD(1));
                read_il.append(new ALOAD(0));
                read_il.append(new SIPUSH((short) dpth));
                read_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_PUSH_CURRENT_OBJECT, Type.VOID, new Type[] {
                        Type.OBJECT, Type.INT },
                        Constants.INVOKEVIRTUAL));

                read_il.append(new ALOAD(0));
                read_il.append(new ALOAD(1));
                read_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_GET_JAVA_OBJECT_INPUT_STREAM,
                        sun_input_stream,
                        Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));
                if (is_externalizable) {
                    /* Invoke readExternal */
                    read_il.append(factory.createInvoke(classname,
                            METHOD_READ_EXTERNAL, Type.VOID,
                            new Type[] { new ObjectType(
                                    TYPE_JAVA_IO_OBJECT_INPUT) },
                                    Constants.INVOKEVIRTUAL));
                } else {
                    /* Invoke readObject. */
                    read_il.append(factory.createInvoke(classname,
                            METHOD_READ_OBJECT, Type.VOID,
                            new Type[] { sun_input_stream },
                            Constants.INVOKESPECIAL));
                }

                /* And then, restore IbisSerializationOutputStream's idea of the current object. */
                read_il.append(new ALOAD(1));
                read_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_POP_CURRENT_OBJECT, Type.VOID, Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));
            } else {
                read_il.append(generateDefaultReads(true, mgen));
            }

            read_il.append(mgen.getInstructionList());
            mgen.setInstructionList(read_il);

            mgen.setMaxStack(MethodGen.getMaxStack(constantpool, read_il,
                    mgen.getExceptionHandlers()));
            mgen.setMaxLocals();

            gen.setMethodAt(mgen.getMethod(), index);
        }
    }

    private void fillInGeneratedWriteObjectMethod(int dpth) {
        /* Now, produce generated_WriteObject. */
        int write_method_index = SerializationInfo.findMethod(methods,
                METHOD_GENERATED_WRITE_OBJECT, 
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_V);
        InstructionList write_il = new InstructionList();
        MethodGen write_gen = new MethodGen(methods[write_method_index], classname,
                constantpool);

        /* write the superclass if neccecary */
        if (is_externalizable) {
            /* Nothing to be done for the superclass. */
        } else if (super_is_ibis_serializable
                || (generator.forceGeneratedCalls() && super_is_serializable)) {
            write_il.append(new ALOAD(0));
            write_il.append(new ALOAD(1));
            write_il.append(createGeneratedWriteObjectInvocation(
                    super_classname, Constants.INVOKESPECIAL));

        } else if (super_is_serializable) {
            int ind = constantpool.addString(super_classname);
            write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(0));
            write_il.append(new LDC(ind));
            write_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                    METHOD_WRITE_SERIALIZABLE_OBJECT, Type.VOID, new Type[] {
                    Type.OBJECT, Type.STRING },
                    Constants.INVOKEVIRTUAL));
        }

        /* and now ... generated_WriteObject should either call the classes
         * writeObject, if it has one, or call generated_DefaultWriteObject.
         * The read constructor should either call readObject, or call
         * generated_DefaultReadObject.
         */
        if (is_externalizable || SerializationInfo.hasWriteObject(methods)) {
            /* First, get and set IbisSerializationOutputStream's idea of
             * the current object.
             */
            write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(0));
            write_il.append(new SIPUSH((short) dpth));
            write_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                    METHOD_PUSH_CURRENT_OBJECT, Type.VOID, new Type[] {
                    Type.OBJECT, Type.INT },
                    Constants.INVOKEVIRTUAL));

            write_il.append(new ALOAD(0));
            write_il.append(new ALOAD(1));
            write_il.append(factory.createInvoke(
                    TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                    METHOD_GET_JAVA_OBJECT_OUTPUT_STREAM,
                    sun_output_stream,
                    Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));
            if (is_externalizable) {
                /* Invoke writeExternal */
                write_il.append(
                        factory.createInvoke(classname, METHOD_WRITE_EXTERNAL,
                                Type.VOID, new Type[] { new ObjectType(
                                        TYPE_JAVA_IO_OBJECT_OUTPUT) },
                                        Constants.INVOKEVIRTUAL));
            } else {
                /* Invoke writeObject. */
                write_il.append(createWriteObjectInvocation());
            }

            /* And then, restore IbisSerializationOutputStream's idea of the current object. */
            write_il.append(new ALOAD(1));
            write_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                    METHOD_POP_CURRENT_OBJECT, Type.VOID, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));
        } else {
            write_il.append(generateDefaultWrites(write_gen));
        }

        write_gen = new MethodGen(methods[write_method_index], classname,
                constantpool);
        write_il.append(write_gen.getInstructionList());
        write_gen.setInstructionList(write_il);

        write_gen.setMaxStack(MethodGen.getMaxStack(constantpool, write_il,
                write_gen.getExceptionHandlers()));
        write_gen.setMaxLocals();

        gen.setMethodAt(write_gen.getMethod(), write_method_index);
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

        int default_write_method_index = SerializationInfo.findMethod(
                methods,
                METHOD_GENERATED_DEFAULT_WRITE_OBJECT, 
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM_I_V);
        MethodGen write_gen = new MethodGen(
                methods[default_write_method_index], classname,
                constantpool);

        InstructionList write_il = new InstructionList();
        InstructionHandle end = write_gen.getInstructionList().getStart();

        write_il.append(new ILOAD(2));
        write_il.append(new SIPUSH((short) dpth));
        IF_ICMPNE ifcmpne = new IF_ICMPNE(null);
        write_il.append(ifcmpne);
        write_il.append(generateDefaultWrites(write_gen));
        write_il.append(new GOTO(end));
        if (super_is_ibis_serializable || super_is_serializable) {
            InstructionHandle i = write_il.append(new ILOAD(2));
            ifcmpne.setTarget(i);
            write_il.append(new SIPUSH((short) dpth));
            write_il.append(new IF_ICMPGT(end));
            if (super_is_ibis_serializable || generator.forceGeneratedCalls()) {
                write_il.append(new ALOAD(0));
                write_il.append(new ALOAD(1));
                write_il.append(new ILOAD(2));
                write_il.append(
                        createGeneratedDefaultWriteObjectInvocation(
                                super_classname));
            } else {
                /*  Superclass is not rewritten.
                 */
                write_il.append(new ALOAD(1));
                write_il.append(new ALOAD(0));
                write_il.append(new ILOAD(2));
                write_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                        METHOD_DEFAULT_WRITE_SERIALIZABLE_OBJECT, Type.VOID,
                        new Type[] { Type.OBJECT, Type.INT },
                        Constants.INVOKEVIRTUAL));
            }
        } else {
            ifcmpne.setTarget(end);
        }
        write_il.append(write_gen.getInstructionList());

        write_gen.setInstructionList(write_il);
        write_gen.setMaxStack(MethodGen.getMaxStack(constantpool, write_il,
                write_gen.getExceptionHandlers()));
        write_gen.setMaxLocals();

        gen.setMethodAt(write_gen.getMethod(), default_write_method_index);
    }

    private void fillInGeneratedDefaultReadObjectMethod(int dpth) {
        InstructionHandle end;
        IF_ICMPNE ifcmpne;
        int default_read_method_index = SerializationInfo.findMethod(
                methods,
                METHOD_GENERATED_DEFAULT_READ_OBJECT, 
                SIGNATURE_LIBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM_I_V);
        MethodGen read_gen = new MethodGen(
                methods[default_read_method_index], classname,
                constantpool);

        InstructionList read_il = new InstructionList();
        end = read_gen.getInstructionList().getStart();

        read_il.append(new ILOAD(2));
        read_il.append(new SIPUSH((short) dpth));
        ifcmpne = new IF_ICMPNE(null);
        read_il.append(ifcmpne);
        read_il.append(generateDefaultReads(false, read_gen));
        read_il.append(new GOTO(end));

        if (super_is_ibis_serializable || super_is_serializable) {
            InstructionHandle i = read_il.append(new ILOAD(2));
            ifcmpne.setTarget(i);
            read_il.append(new SIPUSH((short) dpth));
            read_il.append(new IF_ICMPGT(end));
            if (super_is_ibis_serializable || generator.forceGeneratedCalls()) {
                read_il.append(new ALOAD(0));
                read_il.append(new ALOAD(1));
                read_il.append(new ILOAD(2));
                read_il.append(createGeneratedDefaultReadObjectInvocation(
                        super_classname, factory, Constants.INVOKESPECIAL));
            } else {
                /*  Superclass is not rewritten.
                 */
                read_il.append(new ALOAD(1));
                read_il.append(new ALOAD(0));
                read_il.append(new ILOAD(2));
                read_il.append(factory.createInvoke(TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_DEFAULT_READ_SERIALIZABLE_OBJECT, Type.VOID,
                        new Type[] { Type.OBJECT, Type.INT },
                        Constants.INVOKEVIRTUAL));
            }
        } else {
            ifcmpne.setTarget(end);
        }

        read_il.append(read_gen.getInstructionList());

        read_gen.setInstructionList(read_il);
        read_gen.setMaxStack(MethodGen.getMaxStack(constantpool, read_il,
                read_gen.getExceptionHandlers()));
        read_gen.setMaxLocals();

        gen.setMethodAt(read_gen.getMethod(), default_read_method_index);
    }
}