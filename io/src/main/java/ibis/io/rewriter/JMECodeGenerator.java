/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.io.rewriter;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.AALOAD;
import org.apache.bcel.generic.AASTORE;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ARRAYLENGTH;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DUP;
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
 * The CodeGenerator is responsible for generation of the actual bytecode used
 * at runtime to do serialization.
 *
 * @author Nick Palmer (npr200@few.vu.nl)
 *
 */
class JMECodeGenerator extends CodeGenerator
        implements JMERewriterConstants {

    boolean super_is_jme_serializable;

    boolean super_is_jme_special_case;

    boolean super_is_jme_rewritten;

    boolean super_has_jme_constructor;

    boolean is_jme_externalizable;

    boolean is_jme_abstract;

    boolean has_jme_serial_persistent_fields;

    public static JavaClass lookupClass(String name) {
        try {
            return Repository.lookupClass(name);
        } catch (ClassNotFoundException e) {
            System.err.println("Warning: class " + name + " not found");
            return null;
        }
    }

    JMECodeGenerator(IOGenerator generator, JavaClass cl) {
        super(generator, cl);

        super_is_jme_special_case = JMESerializationInfo
                .isJMESpecialCase(super_class);
        super_is_jme_serializable = JMESerializationInfo
                .isJMESerializable(super_class);
        super_is_jme_rewritten = JMESerializationInfo
                .isJMERewritten(super_class);
        is_jme_externalizable = JMESerializationInfo.isJMEExternalizable(cl);
        is_jme_abstract = cl.isAbstract();
        super_has_jme_constructor = JMESerializationInfo
                .hasJMEConstructor(super_class);
        has_jme_serial_persistent_fields = JMESerializationInfo
                .hasJMESerialPersistentFields(fields);
        if (generator.isVerbose()) {
            System.out.println("Code Generator: " + cl.getClassName()
                    + " sijs: " + super_is_jme_serializable + " sijr: "
                    + super_is_jme_rewritten);
        }
    }

    private Instruction createGeneratedWriteObjectInvocation(String name,
            short invmode) {
        return factory.createInvoke(name, METHOD_GENERATED_JME_WRITE_OBJECT,
                Type.VOID, jme_output_stream_arrtp, invmode);
    }

    private Instruction createGeneratedDefaultReadObjectInvocation(String name,
            InstructionFactory fac, short invmode) {
        return fac.createInvoke(name, METHOD_GENERATED_JME_DEFAULT_READ_OBJECT,
                Type.VOID, new Type[] { jme_input_stream, Type.INT }, invmode);
    }

    private Instruction createInitInvocation(String name,
            InstructionFactory f) {
        return f.createInvoke(name, METHOD_INIT, Type.VOID,
                jme_input_stream_arrtp, Const.INVOKESPECIAL);
    }

    private Instruction createGeneratedDefaultWriteObjectInvocation(
            String name) {
        return factory.createInvoke(name,
                METHOD_GENERATED_JME_DEFAULT_WRITE_OBJECT, Type.VOID,
                new Type[] { jme_output_stream, Type.INT },
                Const.INVOKESPECIAL);
    }

    private int getClassDepth(JavaClass cl) {
        if (cl.getClassName().equals("java.lang.Object")
                || !JMESerializationInfo.isJMESerializable(cl)) {
            return 0;
        }
        return 1 + getClassDepth(lookupClass(cl.getSuperclassName()));
    }

    @Override
    void generateEmptyMethods() {
        /* Generate the necessary (empty) methods. */

        if (generator.isVerbose()) {
            System.out.println(
                    "  Generating empty methods for class : " + classname);
            System.out.println("    " + classname
                    + " implements java.io.jme.Serializable -> adding "
                    + TYPE_IBIS_IO_JME_JMESERIALIZABLE);
        }

        /* add the ibis.io.JMESerializable interface to the class */
        gen.addInterface(TYPE_IBIS_IO_JME_JMESERIALIZABLE);

        /* Construct a write method */
        InstructionList il = new InstructionList();
        il.append(new RETURN());

        int flags = Const.ACC_PUBLIC
                | (gen.isFinal() ? Const.ACC_FINAL : 0);

        MethodGen write_method = new MethodGen(flags, Type.VOID,
                jme_output_stream_arrtp,
                new String[] { VARIABLE_OUTPUT_STREAM },
                METHOD_GENERATED_JME_WRITE_OBJECT, classname, il, constantpool);

        write_method.addException(TYPE_JAVA_IO_IOEXCEPTION);
        gen.addMethod(write_method.getMethod());

        /* ... and a default_write_method */
        il = new InstructionList();
        il.append(new RETURN());

        MethodGen default_write_method = new MethodGen(flags, Type.VOID,
                new Type[] { jme_output_stream, Type.INT },
                new String[] { VARIABLE_OUTPUT_STREAM, VARIABLE_LEVEL },
                METHOD_GENERATED_JME_DEFAULT_WRITE_OBJECT, classname, il,
                constantpool);

        default_write_method.addException(TYPE_JAVA_IO_IOEXCEPTION);
        gen.addMethod(default_write_method.getMethod());

        /* ... and a default_read_method */
        il = new InstructionList();
        il.append(new RETURN());

        MethodGen default_read_method = new MethodGen(flags, Type.VOID,
                new Type[] { jme_input_stream, Type.INT },
                new String[] { VARIABLE_OUTPUT_STREAM, VARIABLE_LEVEL },
                METHOD_GENERATED_JME_DEFAULT_READ_OBJECT, classname, il,
                constantpool);

        default_read_method.addException(TYPE_JAVA_IO_IOEXCEPTION);
        default_read_method
                .addException(TYPE_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
        gen.addMethod(default_read_method.getMethod());

        /*
         * Construct a read-of-the-stream constructor, but only when we can
         * actually use it.
         */
        if (is_jme_externalizable || !super_is_jme_serializable
                || generator.forceGeneratedCalls()
                || super_has_jme_constructor) {
            il = new InstructionList();
            il.append(new RETURN());

            MethodGen read_cons = new MethodGen(Const.ACC_PUBLIC, Type.VOID,
                    jme_input_stream_arrtp,
                    new String[] { VARIABLE_INPUT_STREAM }, METHOD_INIT,
                    classname, il, constantpool);
            read_cons.addException(TYPE_JAVA_IO_IOEXCEPTION);
            read_cons.addException(TYPE_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
            gen.addMethod(read_cons.getMethod());
        } else if (SerializationInfo.hasReadObject(methods)) {
            il = new InstructionList();
            il.append(new RETURN());
            MethodGen readobjectWrapper = new MethodGen(Const.ACC_PUBLIC,
                    Type.VOID, jme_input_stream_arrtp,
                    new String[] { VARIABLE_INPUT_STREAM },
                    METHOD_$READ_OBJECT_WRAPPER$, classname, il, constantpool);
            readobjectWrapper.addException(TYPE_JAVA_IO_IOEXCEPTION);
            readobjectWrapper
                    .addException(TYPE_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
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
        SerializationInfo info = SerializationInfo
                .getSerializationInfo(field_type);

        Type t = info.tp;
        InstructionList temp = new InstructionList();

        if (!info.primitive) {
            t = Type.getType(field_sig);
        }

        temp.append(new ALOAD(1));
        temp.append(new ALOAD(0));
        temp.append(factory.createFieldAccess(classname, field.getName(), t,
                Const.GETFIELD));
        temp.append(factory.createInvoke(TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM,
                info.write_name, Type.VOID, info.param_tp_arr,
                Const.INVOKEVIRTUAL));

        return temp;
    }

    private InstructionList readInstructions(Field field,
            boolean from_constructor) {
        String field_sig = field.getSignature();
        Type field_type = Type.getType(field_sig);
        SerializationInfo info = SerializationInfo
                .getSerializationInfo(field_type);

        Type t = info.tp;
        InstructionList temp = new InstructionList();

        if (!info.primitive) {
            t = Type.getType(field_sig);
        }

        if (from_constructor || !field.isFinal()) {
            temp.append(new ALOAD(0));
            temp.append(new ALOAD(1));
            temp.append(factory.createInvoke(
                    TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM, info.read_name,
                    info.tp, Type.NO_ARGS, Const.INVOKEVIRTUAL));

            if (!info.primitive) {
                temp.append(factory.createCheckCast((ReferenceType) t));
            }

            temp.append(factory.createFieldAccess(classname, field.getName(), t,
                    Const.PUTFIELD));
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
            temp.append(
                    factory.createInvoke(TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                            info.final_read_name, Type.VOID,
                            info.primitive
                                    ? new Type[] { Type.OBJECT, Type.STRING,
                                            Type.STRING }
                                    : new Type[] { Type.OBJECT, Type.STRING,
                                            Type.STRING, Type.STRING },
                            Const.INVOKEVIRTUAL));
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
            System.out.println("    writing reference field " + field.getName()
                    + " of type " + field_type.getSignature());
        }

        if (field_type instanceof ObjectType) {
            field_class = lookupClass(((ObjectType) field_type).getClassName());
            if (field_class != null && field_class.isFinal()) {
                isfinal = true;
            }
        } else if (field_type instanceof ArrayType) {
            isarray = true;
            Type el_type = ((ArrayType) field_type).getElementType();
            if (el_type instanceof ObjectType) {
                field_class = lookupClass(
                        ((ObjectType) el_type).getClassName());
                if (field_class != null && field_class.isFinal()) {
                    isfinal = true;
                }
            } else if (el_type instanceof BasicType) {
                basicname = el_type.toString();
            }
        }

        if ((basicname != null) || (isfinal
                && (JMESerializationInfo.hasJMEConstructor(field_class)
                        || (JMESerializationInfo.isJMESerializable(field_class)
                                && generator.forceGeneratedCalls())))) {
            // If there is an object replacer, we cannot do the
            // "fast" code.
            write_il.append(new ACONST_NULL());
            write_il.append(new ALOAD(1));
            write_il.append(factory.createFieldAccess(
                    TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM, VARIABLE_REPLACER,
                    new ObjectType(TYPE_IBIS_IO_REPLACER), Const.GETFIELD));
            IF_ACMPEQ replacertest = new IF_ACMPEQ(null);
            write_il.append(replacertest);
            write_il.append(writeInstructions(field));
            GOTO toEnd = new GOTO(null);
            write_il.append(toEnd);

            // "fast" code.
            replacertest.setTarget(write_il.append(new ALOAD(1)));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createFieldAccess(classname,
                    field.getName(), field_type, Const.GETFIELD));
            if (basicname != null) {
                write_il.append(
                        factory.createFieldAccess(TYPE_IBIS_IO_JME_CONSTANTS,
                                "TYPE_" + basicname.toUpperCase(), Type.INT,
                                Const.GETSTATIC));
                write_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM,
                        METHOD_WRITE_KNOWN_ARRAY_HEADER, Type.INT,
                        new Type[] { Type.OBJECT, Type.INT },
                        Const.INVOKEVIRTUAL));
            } else {
                write_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM,
                        METHOD_WRITE_KNOWN_OBJECT_HEADER, Type.INT,
                        new Type[] { Type.OBJECT }, Const.INVOKEVIRTUAL));
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
                        field.getName(), field_type, Const.GETFIELD));
                write_il.append(new ARRAYLENGTH());
                write_il.append(new DUP());
                write_il.append(new ISTORE(4));
                write_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM, METHOD_WRITE_INT,
                        Type.VOID, new Type[] { Type.INT },
                        Const.INVOKEVIRTUAL));
                if (basicname != null) {
                    write_il.append(new ALOAD(1));
                    write_il.append(new ALOAD(0));
                    write_il.append(factory.createFieldAccess(classname,
                            field.getName(), field_type, Const.GETFIELD));
                    write_il.append(new ICONST(0));
                    write_il.append(new ILOAD(4));
                    write_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM,
                            writeCallName(basicname), Type.VOID,
                            new Type[] { field_type, Type.INT, Type.INT },
                            Const.INVOKEVIRTUAL));
                } else {
                    write_il.append(new ICONST(0));
                    write_il.append(new ISTORE(3));
                    GOTO gto = new GOTO(null);
                    write_il.append(gto);

                    InstructionHandle loop_body_start = write_il
                            .append(new ALOAD(1));
                    write_il.append(new ALOAD(0));
                    write_il.append(factory.createFieldAccess(classname,
                            field.getName(), field_type, Const.GETFIELD));
                    write_il.append(new ILOAD(3));
                    write_il.append(new AALOAD());

                    write_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM,
                            METHOD_WRITE_KNOWN_OBJECT_HEADER, Type.INT,
                            new Type[] { Type.OBJECT },
                            Const.INVOKEVIRTUAL));
                    write_il.append(new ISTORE(2));
                    write_il.append(new ILOAD(2));
                    write_il.append(new ICONST(1));
                    IF_ICMPNE ifcmp1 = new IF_ICMPNE(null);
                    write_il.append(ifcmp1);

                    write_il.append(new ALOAD(0));
                    write_il.append(factory.createFieldAccess(classname,
                            field.getName(), field_type, Const.GETFIELD));
                    write_il.append(new ILOAD(3));
                    write_il.append(new AALOAD());
                    write_il.append(new ALOAD(1));
                    write_il.append(createGeneratedWriteObjectInvocation(
                            field_class.getClassName(),
                            Const.INVOKEVIRTUAL));

                    ifcmp1.setTarget(write_il.append(new IINC(3, 1)));
                    gto.setTarget(write_il.append(new ILOAD(4)));

                    write_il.append(new ILOAD(3));
                    write_il.append(new IF_ICMPGT(loop_body_start));
                }
            } else {
                write_il.append(new ALOAD(0));
                write_il.append(factory.createFieldAccess(classname,
                        field.getName(), field_type, Const.GETFIELD));
                write_il.append(new ALOAD(1));

                write_il.append(createGeneratedWriteObjectInvocation(
                        field_class.getClassName(), Const.INVOKEVIRTUAL));
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
                classname, FIELD_SERIAL_PERSISTENT_FIELDS,
                new ArrayType(
                        new ObjectType(TYPE_IBIS_IO_JME_OBJECT_STREAM_FIELD),
                        1),
                Const.GETSTATIC);
        InstructionList write_il = new InstructionList();
        int[] case_values = new int[] { CASE_BOOLEAN, CASE_CHAR, CASE_DOUBLE,
                CASE_FLOAT, CASE_INT, CASE_LONG, CASE_SHORT, CASE_OBJECT };
        InstructionHandle[] case_handles = new InstructionHandle[case_values.length];
        GOTO[] gotos = new GOTO[case_values.length + 1];

        for (int i = 0; i < gotos.length; i++) {
            gotos[i] = new GOTO(null);
        }

        write_il.append(new SIPUSH((short) 0));
        write_il.append(new ISTORE(2));

        GOTO gto = new GOTO(null);
        write_il.append(gto);

        InstructionHandle loop_body_start = write_il
                .append(persistent_field_access);
        write_il.append(new ILOAD(2));
        write_il.append(new AALOAD());
        write_il.append(factory.createInvoke(TYPE_JAVA_IO_OBJECT_STREAM_FIELD,
                METHOD_GET_NAME, Type.STRING, Type.NO_ARGS,
                Const.INVOKEVIRTUAL));
        write_il.append(new ASTORE(3));

        InstructionHandle begin_try = write_il
                .append(new PUSH(constantpool, classname));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS,
                METHOD_FOR_NAME, java_lang_class_type,
                new Type[] { Type.STRING }, Const.INVOKESTATIC));
        write_il.append(new ALOAD(3));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS,
                METHOD_GET_FIELD, new ObjectType(TYPE_JAVA_LANG_REFLECT_FIELD),
                new Type[] { Type.STRING }, Const.INVOKEVIRTUAL));
        write_il.append(new ASTORE(4));

        write_il.append(persistent_field_access);
        write_il.append(new ILOAD(2));
        write_il.append(new AALOAD());
        write_il.append(factory.createInvoke(TYPE_JAVA_IO_OBJECT_STREAM_FIELD,
                METHOD_GET_TYPE_CODE, Type.CHAR, Type.NO_ARGS,
                Const.INVOKEVIRTUAL));

        case_handles[0] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_BOOLEAN, Type.BOOLEAN, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                METHOD_WRITE_BOOLEAN, Type.VOID, new Type[] { Type.BOOLEAN },
                Const.INVOKEVIRTUAL));
        write_il.append(gotos[0]);

        case_handles[1] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_CHAR, Type.CHAR, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                METHOD_WRITE_CHAR, Type.VOID, new Type[] { Type.INT },
                Const.INVOKEVIRTUAL));
        write_il.append(gotos[1]);

        case_handles[2] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_DOUBLE, Type.DOUBLE, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                METHOD_WRITE_DOUBLE, Type.VOID, new Type[] { Type.DOUBLE },
                Const.INVOKEVIRTUAL));
        write_il.append(gotos[2]);

        case_handles[3] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_FLOAT, Type.FLOAT, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                METHOD_WRITE_FLOAT, Type.VOID, new Type[] { Type.FLOAT },
                Const.INVOKEVIRTUAL));
        write_il.append(gotos[3]);

        case_handles[4] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_INT, Type.INT, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM, METHOD_WRITE_INT,
                Type.VOID, new Type[] { Type.INT }, Const.INVOKEVIRTUAL));
        write_il.append(gotos[4]);

        case_handles[5] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_LONG, Type.LONG, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                METHOD_WRITE_LONG, Type.VOID, new Type[] { Type.LONG },
                Const.INVOKEVIRTUAL));
        write_il.append(gotos[5]);

        case_handles[6] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_SHORT, Type.SHORT, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                METHOD_WRITE_SHORT, Type.VOID, new Type[] { Type.INT },
                Const.INVOKEVIRTUAL));
        write_il.append(gotos[6]);

        case_handles[7] = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET_BOOLEAN, Type.BOOLEAN, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                METHOD_WRITE_BOOLEAN, Type.VOID, new Type[] { Type.BOOLEAN },
                Const.INVOKEVIRTUAL));
        write_il.append(gotos[7]);

        InstructionHandle default_handle = write_il.append(new ALOAD(1));
        write_il.append(new ALOAD(4));
        write_il.append(new ALOAD(0));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_GET, Type.OBJECT, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(
                TYPE_IBIS_IO_IBIS_SERIALIZATION_OUTPUT_STREAM,
                METHOD_WRITE_OBJECT, Type.VOID, new Type[] { Type.OBJECT },
                Const.INVOKEVIRTUAL));
        InstructionHandle end_try = write_il.append(gotos[8]);

        write_il.insert(case_handles[0],
                new SWITCH(case_values, case_handles, default_handle));

        InstructionHandle handler = write_il.append(new ASTORE(6));
        write_il.append(factory.createNew(TYPE_JAVA_IO_IOEXCEPTION));
        write_il.append(new DUP());
        write_il.append(factory.createNew(TYPE_JAVA_LANG_STRING_BUFFER));
        write_il.append(new DUP());
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_INIT, Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
        write_il.append(new PUSH(constantpool, "Could not write field "));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_APPEND, Type.STRINGBUFFER, new Type[] { Type.STRING },
                Const.INVOKEVIRTUAL));
        write_il.append(new ALOAD(3));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_APPEND, Type.STRINGBUFFER, new Type[] { Type.STRING },
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_TO_STRING, Type.STRING, Type.NO_ARGS,
                Const.INVOKEVIRTUAL));
        write_il.append(factory.createInvoke(TYPE_JAVA_IO_IOEXCEPTION,
                METHOD_INIT, Type.VOID, new Type[] { Type.STRING },
                Const.INVOKESPECIAL));
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

        if (has_jme_serial_persistent_fields) {
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
                        System.out.println(
                                "    writing basic field " + field.getName()
                                        + " of type " + field.getSignature());
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
                        System.out
                                .println("    writing field " + field.getName()
                                        + " of type " + field.getSignature());
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
            System.out.println("    reading reference field " + field.getName()
                    + " of type " + field_type.getSignature());
        }

        if (field_type instanceof ObjectType) {
            field_class = lookupClass(((ObjectType) field_type).getClassName());
            if (field_class != null && field_class.isFinal()) {
                isfinal = true;
            }
        } else if (field_type instanceof ArrayType) {
            isarray = true;
            Type el_type = ((ArrayType) field_type).getElementType();
            if (el_type instanceof ObjectType) {
                field_class = lookupClass(
                        ((ObjectType) el_type).getClassName());
                if (field_class != null && field_class.isFinal()) {
                    isfinal = true;
                }
            } else if (el_type instanceof BasicType) {
                basicname = el_type.toString();
            }
        }

        if ((basicname != null) || (isfinal
                && (JMESerializationInfo.hasJMEConstructor(field_class)
                        || (JMESerializationInfo.isJMESerializable(field_class)
                                && generator.forceGeneratedCalls())))) {
            read_il.append(new ALOAD(1));
            read_il.append(
                    factory.createInvoke(TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                            METHOD_READ_KNOWN_TYPE_HEADER, Type.INT,
                            Type.NO_ARGS, Const.INVOKEVIRTUAL));
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
                            TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM, callname,
                            field_type, Type.NO_ARGS, Const.INVOKEVIRTUAL));
                    read_il.append(factory.createFieldAccess(classname,
                            field.getName(), field_type, Const.PUTFIELD));
                } else {
                    Type el_type = ((ArrayType) field_type).getElementType();
                    read_il.append(new ALOAD(0));
                    read_il.append(new ALOAD(1));
                    read_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                            METHOD_READ_INT, Type.INT, Type.NO_ARGS,
                            Const.INVOKEVIRTUAL));
                    read_il.append(new DUP());
                    read_il.append(new ISTORE(3));
                    read_il.append(factory.createNewArray(el_type, (short) 1));
                    read_il.append(factory.createFieldAccess(classname,
                            field.getName(), field_type, Const.PUTFIELD));
                    read_il.append(new ALOAD(1));
                    read_il.append(new ALOAD(0));
                    read_il.append(factory.createFieldAccess(classname,
                            field.getName(), field_type, Const.GETFIELD));

                    read_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                            METHOD_ADD_OBJECT_TO_CYCLE_CHECK, Type.VOID,
                            new Type[] { Type.OBJECT },
                            Const.INVOKEVIRTUAL));
                    read_il.append(new ICONST(0));
                    read_il.append(new ISTORE(4));
                    GOTO gto1 = new GOTO(null);
                    read_il.append(gto1);

                    InstructionHandle loop_body_start = read_il
                            .append(new ALOAD(1));
                    read_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                            METHOD_READ_KNOWN_TYPE_HEADER, Type.INT,
                            Type.NO_ARGS, Const.INVOKEVIRTUAL));
                    read_il.append(new ISTORE(2));
                    read_il.append(new ILOAD(2));
                    read_il.append(new ICONST(-1));

                    IF_ICMPNE ifcmp1 = new IF_ICMPNE(null);
                    read_il.append(ifcmp1);

                    read_il.append(new ALOAD(0));
                    read_il.append(factory.createFieldAccess(classname,
                            field.getName(), field_type, Const.GETFIELD));
                    read_il.append(new ILOAD(4));

                    read_il.append(factory.createNew((ObjectType) el_type));
                    read_il.append(new DUP());
                    read_il.append(new ALOAD(1));
                    read_il.append(createInitInvocation(
                            field_class.getClassName(), factory));
                    read_il.append(new AASTORE());
                    GOTO gto2 = new GOTO(null);
                    read_il.append(gto2);
                    InstructionHandle cmp_goto2 = read_il.append(new ILOAD(2));
                    ifcmp1.setTarget(cmp_goto2);
                    read_il.append(new ICONST(0));
                    IF_ICMPEQ ifcmpeq2 = new IF_ICMPEQ(null);
                    read_il.append(ifcmpeq2);

                    read_il.append(new ALOAD(0));
                    read_il.append(factory.createFieldAccess(classname,
                            field.getName(), field_type, Const.GETFIELD));
                    read_il.append(new ILOAD(4));

                    read_il.append(new ALOAD(1));
                    read_il.append(new ILOAD(2));
                    read_il.append(factory.createInvoke(
                            TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                            METHOD_GET_OBJECT_FROM_CYCLE_CHECK, Type.OBJECT,
                            new Type[] { Type.INT }, Const.INVOKEVIRTUAL));
                    read_il.append(
                            factory.createCheckCast((ReferenceType) el_type));
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
                read_il.append(createInitInvocation(field_class.getClassName(),
                        factory));
                read_il.append(factory.createFieldAccess(classname,
                        field.getName(), field_type, Const.PUTFIELD));
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
            read_il.append(
                    factory.createInvoke(TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                            METHOD_GET_OBJECT_FROM_CYCLE_CHECK, Type.OBJECT,
                            new Type[] { Type.INT }, Const.INVOKEVIRTUAL));

            read_il.append(factory.createCheckCast((ReferenceType) field_type));
            read_il.append(factory.createFieldAccess(classname, field.getName(),
                    field_type, Const.PUTFIELD));

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
                read_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                        METHOD_READ_OBJECT, Type.OBJECT, Type.NO_ARGS,
                        Const.INVOKEVIRTUAL));
            } else {
                read_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                        METHOD_READ + tpname, tp, Type.NO_ARGS,
                        Const.INVOKEVIRTUAL));
            }
            read_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                    METHOD_SET + tpname, Type.VOID,
                    new Type[] { Type.OBJECT, tp }, Const.INVOKEVIRTUAL));
            read_il.append(gto);

            return h;
        }

        h = read_il.append(new ILOAD(5));
        read_il.append(new PUSH(constantpool, Const.ACC_FINAL));
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
                    Type.NO_ARGS, Const.INVOKEVIRTUAL));
            read_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS,
                    METHOD_GET_NAME, Type.STRING, Type.NO_ARGS,
                    Const.INVOKEVIRTUAL));

            read_il.append(
                    factory.createInvoke(TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                            METHOD_READ_FIELD_OBJECT, Type.VOID, new Type[] {
                                    Type.OBJECT, Type.STRING, Type.STRING },
                            Const.INVOKEVIRTUAL));
        } else {
            read_il.append(
                    factory.createInvoke(TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                            METHOD_READ_FIELD + tpname, Type.VOID,
                            new Type[] { Type.OBJECT, Type.STRING },
                            Const.INVOKEVIRTUAL));
        }
        GOTO gto2 = new GOTO(null);
        read_il.append(gto2);
        eq.setTarget(read_il.append(new ALOAD(4)));
        read_il.append(new ALOAD(0));
        read_il.append(new ALOAD(1));
        if (tpname.equals("")) {
            read_il.append(factory.createInvoke(
                    TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM, METHOD_READ_OBJECT,
                    tp, Type.NO_ARGS, Const.INVOKEVIRTUAL));
        } else {
            read_il.append(factory.createInvoke(
                    TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM, METHOD_READ + tpname,
                    tp, Type.NO_ARGS, Const.INVOKEVIRTUAL));
        }
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                METHOD_SET + tpname, Type.VOID, new Type[] { Type.OBJECT, tp },
                Const.INVOKEVIRTUAL));
        gto2.setTarget(read_il.append(gto));

        return h;
    }

    private InstructionList serialPersistentReads(boolean from_constructor,
            MethodGen read_gen) {
        Instruction persistent_field_access = factory
                .createFieldAccess(classname, FIELD_SERIAL_PERSISTENT_FIELDS,
                        new ArrayType(new ObjectType(
                                TYPE_JAVA_IO_OBJECT_STREAM_FIELD), 1),
                        Const.GETSTATIC);
        InstructionList read_il = new InstructionList();
        int[] case_values = new int[] { CASE_BOOLEAN, CASE_CHAR, CASE_DOUBLE,
                CASE_FLOAT, CASE_INT, CASE_LONG, CASE_SHORT, CASE_OBJECT };
        InstructionHandle[] case_handles = new InstructionHandle[case_values.length];
        GOTO[] gotos = new GOTO[case_values.length + 1];

        for (int i = 0; i < gotos.length; i++) {
            gotos[i] = new GOTO(null);
        }

        read_il.append(new SIPUSH((short) 0));
        read_il.append(new ISTORE(2));

        GOTO gto = new GOTO(null);
        read_il.append(gto);

        InstructionHandle loop_body_start = read_il
                .append(persistent_field_access);
        read_il.append(new ILOAD(2));
        read_il.append(new AALOAD());
        read_il.append(factory.createInvoke(TYPE_JAVA_IO_OBJECT_STREAM_FIELD,
                METHOD_GET_NAME, Type.STRING, Type.NO_ARGS,
                Const.INVOKEVIRTUAL));
        read_il.append(new ASTORE(3));

        InstructionHandle begin_try = read_il
                .append(new PUSH(constantpool, classname));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS,
                METHOD_FOR_NAME, java_lang_class_type,
                new Type[] { Type.STRING }, Const.INVOKESTATIC));
        read_il.append(new ALOAD(3));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_CLASS,
                METHOD_GET_FIELD, new ObjectType(TYPE_JAVA_LANG_REFLECT_FIELD),
                new Type[] { Type.STRING }, Const.INVOKEVIRTUAL));
        read_il.append(new ASTORE(4));

        if (!from_constructor && has_final_fields) {
            read_il.append(new ALOAD(4));
            read_il.append(factory.createInvoke(TYPE_JAVA_LANG_REFLECT_FIELD,
                    METHOD_GET_MODIFIERS, Type.INT, Type.NO_ARGS,
                    Const.INVOKEVIRTUAL));
            read_il.append(new ISTORE(5));
        }

        read_il.append(persistent_field_access);
        read_il.append(new ILOAD(2));
        read_il.append(new AALOAD());
        read_il.append(factory.createInvoke(TYPE_JAVA_IO_OBJECT_STREAM_FIELD,
                METHOD_GET_TYPE_CODE, Type.CHAR, Type.NO_ARGS,
                Const.INVOKEVIRTUAL));

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
        case_handles[7] = generateReadField(TYPE_BOOLEAN, Type.BOOLEAN, read_il,
                gotos[7], from_constructor);

        InstructionHandle default_handle = generateReadField("", Type.OBJECT,
                read_il, gotos[8], from_constructor);

        InstructionHandle end_try = read_il.getEnd();

        read_il.insert(case_handles[0],
                new SWITCH(case_values, case_handles, default_handle));

        InstructionHandle handler = read_il.append(new ASTORE(6));
        read_il.append(factory.createNew(TYPE_JAVA_IO_IOEXCEPTION));
        read_il.append(new DUP());
        read_il.append(factory.createNew(TYPE_JAVA_LANG_STRING_BUFFER));
        read_il.append(new DUP());
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_INIT, Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
        read_il.append(new PUSH(constantpool, "Could not read field "));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_APPEND, Type.STRINGBUFFER, new Type[] { Type.STRING },
                Const.INVOKEVIRTUAL));
        read_il.append(new ALOAD(3));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_APPEND, Type.STRINGBUFFER, new Type[] { Type.STRING },
                Const.INVOKEVIRTUAL));
        read_il.append(factory.createInvoke(TYPE_JAVA_LANG_STRING_BUFFER,
                METHOD_TO_STRING, Type.STRING, Type.NO_ARGS,
                Const.INVOKEVIRTUAL));
        read_il.append(factory.createInvoke(TYPE_JAVA_IO_IOEXCEPTION,
                METHOD_INIT, Type.VOID, new Type[] { Type.STRING },
                Const.INVOKESPECIAL));
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

        if (has_jme_serial_persistent_fields) {
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

                    read_il.append(readInstructions(field, from_constructor));
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
                    System.out.println("    writing field " + field.getName()
                            + " of type " + field.getSignature());
                }

                if (field_type instanceof ReferenceType) {
                    if (!field_type.equals(Type.STRING)
                            && !field_type.equals(java_lang_class_type)) {
                        read_il.append(
                                readReferenceField(field, from_constructor));
                    } else {
                        read_il.append(
                                readInstructions(field, from_constructor));
                    }
                }
            }
        }

        return read_il;
    }

    private JavaClass generateInstanceGenerator() {

        /*
         * Here we create a 'generator' object. We need this extra object for
         * three reasons: 1) Because the object is created from the 'ibis.io'
         * package (the Serialization code), we may not be allowed to create a
         * new instance of the object (due to inter-package access restrictions,
         * e.g. the object may not be public). Because the generator is in the
         * same package as the target object, it can create a new object for us.
         *
         * ?? How about totally private objects ?? can sun serialization handle
         * this ??
         *
         * 2) Using this generator object, we can do a normal 'new' of the
         * target type. This is important, because using 'newInstance' is 6
         * times more expensive than 'new'. 3) We do not want to invoke a
         * default constructor, but a special constructor that immediately reads
         * the object state from the stream. This cannot be done (efficiently)
         * with newInstance.
         */

        if (generator.isVerbose()) {
            System.out.println(
                    "  Generating InstanceGenerator class for " + classname);
        }

        String name = classname + METHOD_IBIS_IO_GENERATOR;

        ObjectType class_type = new ObjectType(classname);

        String classfilename = name.substring(name.lastIndexOf('.') + 1)
                + ".class";
        ClassGen iogenGen = new ClassGen(name, TYPE_IBIS_IO_JME_GENERATOR,
                classfilename, Const.ACC_FINAL | Const.ACC_PUBLIC
                        | Const.ACC_SUPER,
                null);
        InstructionFactory iogenFactory = new InstructionFactory(iogenGen);

        InstructionList il = new InstructionList();

        if (!is_jme_externalizable && super_is_jme_serializable
                && !super_has_jme_constructor
                && !generator.forceGeneratedCalls()) {
            /*
             * This is a difficult case. We cannot call a constructor, because
             * this constructor would be obliged to call a constructor for the
             * super-class. So, we do it differently: generate calls to
             * IbisSerializationInputStream methods which call native methods
             * ... I don't know another solution to this problem.
             */
            /*
             * First, create the object. Through a native call, because
             * otherwise the object would be marked uninitialized, and the code
             * would not pass bytecode verification. This native call also takes
             * care of calling the constructor of the first non-serializable
             * superclass.
             */
            il.append(new ALOAD(1));
            int ind = iogenGen.getConstantPool().addString(classname);
            il.append(new LDC(ind));
            il.append(iogenFactory.createInvoke(
                    TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                    METHOD_CREATE_UNINITIALIZED_OBJECT, Type.OBJECT,
                    new Type[] { Type.STRING }, Const.INVOKEVIRTUAL));
            il.append(iogenFactory.createCheckCast(class_type));
            il.append(new ASTORE(2));

            /* Now read the superclass. */
            il.append(new ALOAD(1));
            il.append(new ALOAD(2));
            ind = iogenGen.getConstantPool().addString(super_classname);
            il.append(new LDC(ind));
            il.append(iogenFactory.createInvoke(
                    TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                    METHOD_READ_SERIALIZABLE_OBJECT, Type.VOID,
                    new Type[] { Type.OBJECT, Type.STRING },
                    Const.INVOKEVIRTUAL));

            /*
             * Now, if the class has a readObject, call it. Otherwise, read its
             * fields, by calling generated_DefaultReadObject.
             */
            if (JMESerializationInfo.hasReadObject(methods)) {
                il.append(new ALOAD(2));
                il.append(new ALOAD(1));
                il.append(iogenFactory.createInvoke(classname,
                        METHOD_$READ_OBJECT_WRAPPER$, Type.VOID,
                        ibis_input_stream_arrtp, Const.INVOKEVIRTUAL));
            } else {
                int dpth = getClassDepth(clazz);

                il.append(new ALOAD(2));
                il.append(new ALOAD(1));
                il.append(new SIPUSH((short) dpth));
                il.append(createGeneratedDefaultReadObjectInvocation(classname,
                        iogenFactory, Const.INVOKEVIRTUAL));
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
         * 0 new DITree 3 dup 4 aload_1 5 invokespecial
         * DITree(ibis.io.IbisSerializationInputStream) 8 areturn
         */

        MethodGen method = new MethodGen(
                Const.ACC_FINAL | Const.ACC_PUBLIC, Type.OBJECT,
                jme_input_stream_arrtp, new String[] { VARIABLE_INPUT_STREAM },
                METHOD_GENERATED_NEW_INSTANCE, name, il,
                iogenGen.getConstantPool());

        method.setMaxStack(3);
        method.setMaxLocals();
        method.addException(TYPE_JAVA_IO_IOEXCEPTION);
        method.addException(TYPE_JAVA_LANG_CLASS_NOT_FOUND_EXCEPTION);
        iogenGen.addMethod(method.getMethod());

        il = new InstructionList();
        il.append(new ALOAD(0));
        il.append(iogenFactory.createInvoke(TYPE_IBIS_IO_JME_GENERATOR,
                METHOD_INIT, Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
        il.append(new RETURN());

        method = new MethodGen(Const.ACC_PUBLIC, Type.VOID, Type.NO_ARGS,
                null, METHOD_INIT, name, il, iogenGen.getConstantPool());

        method.setMaxStack(1);
        method.setMaxLocals();
        iogenGen.addMethod(method.getMethod());

        // Now we build the new_array function

        // Build the base of the function
        il = new InstructionList();
        il.append(new ACONST_NULL());
        il.append(new ASTORE(3));
        il.append(new ILOAD(2));
        InstructionHandle done = il.append(new ALOAD(3));
        il.append(new ARETURN());

        // Now build the switch
        ConstantPoolGen cp = iogenGen.getConstantPool();
        int index;
        InstructionHandle[] handles = new InstructionHandle[5];

        int i = 0;
        String arrayName = classname;
        do {
            handles[i++] = il.insert(done, new ILOAD(1));
            index = cp.addClass(arrayName);
            if (generator.isVerbose()) {
                System.out.println(
                        "Added : " + arrayName + " : " + index + " to cp.");
            }
            il.insert(done, new ANEWARRAY(index));
            il.insert(done, new ASTORE(3));
            if (handles[handles.length - 1] == null) {
                il.insert(done, new GOTO(done));
            }
            if (!arrayName.startsWith("[")) {
                arrayName = "L" + arrayName + ";";
            }
            arrayName = "[" + arrayName;
            if (generator.isVerbose()) {
                System.out.println("Generating " + arrayName);
            }
        } while (handles[handles.length - 1] == null);

        int values[] = { 1, 2, 3, 4, 5 };
        il.insert(handles[0], new SWITCH(values, handles, done));

        method = new MethodGen(Const.ACC_PUBLIC,
                Type.getType(Object[].class), new Type[] { Type.INT, Type.INT },
                new String[] { "len", "dimension" }, "new_array", name, il,
                iogenGen.getConstantPool());
        method.setMaxStack(1);
        method.setMaxLocals(4);
        iogenGen.addMethod(method.getMethod());

        return iogenGen.getJavaClass();
    }

    @Override
    void generateCode() {
        /* Generate code inside the methods */
        if (generator.isVerbose()) {
            System.out.println(
                    "  Generating method code class for class : " + classname);
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

        JavaClass instgen = null;

        if (!is_jme_abstract) {
            instgen = generateInstanceGenerator();
            Repository.addClass(instgen);
        }
        generator.markRewritten(clazz, instgen);
    }

    private void fillInGeneratedReadObjectMethod(int dpth) {
        /*
         * Now, produce the read constructor. It only exists if the superclass
         * is not serializable, or if the superclass has an ibis constructor, or
         * is assumed to have one (-force option).
         */

        /* Now, do the same for the reading side. */
        MethodGen mgen = null;
        int index = -1;
        InstructionList read_il = null;
        if (is_jme_externalizable || super_has_jme_constructor
                || !super_is_jme_serializable
                || generator.forceGeneratedCalls()) {
            read_il = new InstructionList();
            if (is_jme_externalizable) {
                read_il.append(new ALOAD(0));
                read_il.append(factory.createInvoke(classname, METHOD_INIT,
                        Type.VOID, Type.NO_ARGS, Const.INVOKESPECIAL));
            } else if (!super_is_jme_serializable) {
                read_il.append(new ALOAD(0));
                read_il.append(factory.createInvoke(super_classname,
                        METHOD_INIT, Type.VOID, Type.NO_ARGS,
                        Const.INVOKESPECIAL));
            } else {
                read_il.append(new ALOAD(0));
                read_il.append(new ALOAD(1));
                read_il.append(createInitInvocation(super_classname, factory));
            }

            if (is_jme_externalizable || !super_is_jme_serializable) {
                read_il.append(new ALOAD(1));
                read_il.append(new ALOAD(0));
                read_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                        METHOD_ADD_OBJECT_TO_CYCLE_CHECK, Type.VOID,
                        new Type[] { Type.OBJECT }, Const.INVOKEVIRTUAL));
            }

            int read_cons_index = JMESerializationInfo.findMethod(methods,
                    METHOD_INIT, SIGNATURE_LIBIS_IO_JME_OBJECT_INPUT_STREAM_V);
            mgen = new MethodGen(methods[read_cons_index], classname,
                    constantpool);
            index = read_cons_index;
        } else if (JMESerializationInfo.hasReadObject(methods)) {
            int read_wrapper_index = JMESerializationInfo.findMethod(methods,
                    METHOD_$READ_OBJECT_WRAPPER$,
                    SIGNATURE_LIBIS_IO_JME_OBJECT_INPUT_STREAM_V);
            mgen = new MethodGen(methods[read_wrapper_index], classname,
                    constantpool);
            read_il = new InstructionList();
            index = read_wrapper_index;
        }

        /*
         * TODO: Shouldn't there be an else clause here that fills in a method?
         * Even an exception throwing method might be better? There is almost
         * certainly a problem here in some corner case.
         */

        if (read_il != null) {
            if (is_jme_externalizable
                    || JMESerializationInfo.hasReadObject(methods)) {
                /*
                 * First, get and set IbisSerializationInputStream's idea of the
                 * current object.
                 */
                read_il.append(new ALOAD(1));
                read_il.append(new ALOAD(0));
                read_il.append(new SIPUSH((short) dpth));
                read_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                        METHOD_PUSH_CURRENT_OBJECT, Type.VOID,
                        new Type[] { Type.OBJECT, Type.INT },
                        Const.INVOKEVIRTUAL));

                read_il.append(new ALOAD(0));
                read_il.append(new ALOAD(1));
                read_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                        METHOD_GET_JAVA_OBJECT_INPUT_STREAM, sun_input_stream,
                        Type.NO_ARGS, Const.INVOKEVIRTUAL));
                if (is_jme_externalizable) {
                    /* Invoke readExternal */
                    read_il.append(factory.createInvoke(classname,
                            METHOD_READ_EXTERNAL, Type.VOID,
                            new Type[] {
                                    new ObjectType(TYPE_JAVA_IO_OBJECT_INPUT) },
                            Const.INVOKEVIRTUAL));
                } else {
                    /* Invoke readObject. */
                    read_il.append(
                            factory.createInvoke(classname, METHOD_READ_OBJECT,
                                    Type.VOID, new Type[] { sun_input_stream },
                                    Const.INVOKESPECIAL));
                }

                /*
                 * And then, restore IbisSerializationOutputStream's idea of the
                 * current object.
                 */
                read_il.append(new ALOAD(1));
                read_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_IBIS_SERIALIZATION_INPUT_STREAM,
                        METHOD_POP_CURRENT_OBJECT, Type.VOID, Type.NO_ARGS,
                        Const.INVOKEVIRTUAL));
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
        int write_method_index = JMESerializationInfo.findMethod(methods,
                METHOD_GENERATED_JME_WRITE_OBJECT,
                SIGNATURE_LIBIS_IO_JME_OBJECT_OUTPUT_STREAM_V);
        InstructionList write_il = new InstructionList();
        MethodGen write_gen = new MethodGen(methods[write_method_index],
                classname, constantpool);

        /* write the superclass if neccecary */
        if (is_jme_externalizable) {
            /* Nothing to be done for the superclass. */
        } else if (super_is_jme_serializable || (generator.forceGeneratedCalls()
                && super_is_jme_rewritten)) {
            write_il.append(new ALOAD(0));
            write_il.append(new ALOAD(1));
            write_il.append(createGeneratedWriteObjectInvocation(
                    super_classname, Const.INVOKESPECIAL));

        }

        /*
         * and now ... generated_WriteObject should either call the classes
         * writeObject, if it has one, or call generated_DefaultWriteObject. The
         * read constructor should either call readObject, or call
         * generated_DefaultReadObject.
         */
        if (is_jme_externalizable
                || JMESerializationInfo.hasWriteObject(methods)) {
            /*
             * First, get and set IbisSerializationOutputStream's idea of the
             * current object.
             */
            write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(0));
            write_il.append(new SIPUSH((short) dpth));
            write_il.append(
                    factory.createInvoke(TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM,
                            METHOD_PUSH_CURRENT_OBJECT, Type.VOID,
                            new Type[] { Type.OBJECT, Type.INT },
                            Const.INVOKEVIRTUAL));

            /* Invoke writeObject. */
            write_il.append(createGeneratedWriteObjectInvocation(
                    clazz.getClassName(), Const.INVOKEVIRTUAL));

            /*
             * And then, restore IbisSerializationOutputStream's idea of the
             * current object.
             */
            write_il.append(new ALOAD(1));
            write_il.append(
                    factory.createInvoke(TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM,
                            METHOD_POP_CURRENT_OBJECT, Type.VOID, Type.NO_ARGS,
                            Const.INVOKEVIRTUAL));
        } else {
            if (!super_is_jme_serializable && !super_is_jme_special_case) {
                /* TODO: Create a throws NotSerializable here */
                System.err.println("Class: " + clazz.getClassName()
                        + " marked ibis.io.jme.Serializable but superclass is not serializable! Use Externalizable if the superclass can not be marked the same.");
                write_il.append(new ALOAD(0));
                write_il.append(factory.createNew(
                        TYPE_IBIS_IO_JME_NOT_SERIALIZABLE_EXCEPTION));
                write_il.append(new DUP());
                write_il.append(
                        factory.createNew(TYPE_JAVA_LANG_STRING_BUFFER));
                write_il.append(new DUP());
                write_il.append(factory.createInvoke(
                        TYPE_JAVA_LANG_STRING_BUFFER, METHOD_INIT, Type.VOID,
                        Type.NO_ARGS, Const.INVOKESPECIAL));
                write_il.append(
                        new PUSH(constantpool, "Superclass Not Serializable: "
                                + clazz.getClassName()));
                write_il.append(factory.createInvoke(
                        TYPE_JAVA_LANG_STRING_BUFFER, METHOD_APPEND,
                        Type.STRINGBUFFER, new Type[] { Type.STRING },
                        Const.INVOKEVIRTUAL));
                write_il.append(factory.createInvoke(
                        TYPE_JAVA_LANG_STRING_BUFFER, METHOD_TO_STRING,
                        Type.STRING, Type.NO_ARGS, Const.INVOKEVIRTUAL));
                write_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_NOT_SERIALIZABLE_EXCEPTION,
                        METHOD_INIT, Type.VOID, new Type[] { Type.STRING },
                        Const.INVOKESPECIAL));
                write_il.append(new ATHROW());
            } else {
                write_il.append(generateDefaultWrites(write_gen));
            }
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
        /*
         * void generated_DefaultWriteObject( IbisSerializationOutputStream out,
         * int level) { if (level == dpth) { ... write fields ... (the code
         * resulting from the generateDefaultWrites() call). } else if (level <
         * dpth) { super.generated_DefaultWriteObject(out, level); } }
         */

        int default_write_method_index = JMESerializationInfo.findMethod(
                methods, METHOD_GENERATED_JME_DEFAULT_WRITE_OBJECT,
                SIGNATURE_LIBIS_IO_JME_OBJECT_OUTPUT_STREAM_I_V);
        MethodGen write_gen = new MethodGen(methods[default_write_method_index],
                classname, constantpool);

        InstructionList write_il = new InstructionList();
        InstructionHandle end = write_gen.getInstructionList().getStart();

        write_il.append(new ILOAD(2));
        write_il.append(new SIPUSH((short) dpth));
        IF_ICMPNE ifcmpne = new IF_ICMPNE(null);
        write_il.append(ifcmpne);
        write_il.append(generateDefaultWrites(write_gen));
        write_il.append(new GOTO(end));
        if (super_is_jme_serializable || super_is_jme_rewritten) {
            InstructionHandle i = write_il.append(new ILOAD(2));
            ifcmpne.setTarget(i);
            write_il.append(new SIPUSH((short) dpth));
            write_il.append(new IF_ICMPGT(end));
            if (super_is_jme_serializable || super_is_jme_rewritten
                    || generator.forceGeneratedCalls()) {
                write_il.append(new ALOAD(0));
                write_il.append(new ALOAD(1));
                write_il.append(new ILOAD(2));
                write_il.append(createGeneratedDefaultWriteObjectInvocation(
                        super_classname));
            } else {
                /*
                 * Superclass is not rewritten.
                 */
                write_il.append(new ALOAD(1));
                write_il.append(new ALOAD(0));
                write_il.append(new ILOAD(2));
                write_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_OUTPUT_STREAM,
                        METHOD_DEFAULT_WRITE_SERIALIZABLE_OBJECT, Type.VOID,
                        new Type[] { Type.OBJECT, Type.INT },
                        Const.INVOKEVIRTUAL));
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
        int default_read_method_index = JMESerializationInfo.findMethod(methods,
                METHOD_GENERATED_JME_DEFAULT_READ_OBJECT,
                SIGNATURE_LIBIS_IO_JME_OBJECT_INPUT_STREAM_I_V);
        MethodGen read_gen = new MethodGen(methods[default_read_method_index],
                classname, constantpool);

        InstructionList read_il = new InstructionList();
        end = read_gen.getInstructionList().getStart();

        read_il.append(new ILOAD(2));
        read_il.append(new SIPUSH((short) dpth));
        ifcmpne = new IF_ICMPNE(null);
        read_il.append(ifcmpne);
        read_il.append(generateDefaultReads(false, read_gen));
        read_il.append(new GOTO(end));

        if (super_is_jme_serializable || super_is_jme_rewritten) {
            InstructionHandle i = read_il.append(new ILOAD(2));
            ifcmpne.setTarget(i);
            read_il.append(new SIPUSH((short) dpth));
            read_il.append(new IF_ICMPGT(end));
            if (super_is_jme_serializable || generator.forceGeneratedCalls()) {
                read_il.append(new ALOAD(0));
                read_il.append(new ALOAD(1));
                read_il.append(new ILOAD(2));
                read_il.append(createGeneratedDefaultReadObjectInvocation(
                        super_classname, factory, Const.INVOKESPECIAL));
            } else {
                /*
                 * Superclass is not rewritten.
                 */
                read_il.append(new ALOAD(1));
                read_il.append(new ALOAD(0));
                read_il.append(new ILOAD(2));
                read_il.append(factory.createInvoke(
                        TYPE_IBIS_IO_JME_OBJECT_INPUT_STREAM,
                        METHOD_DEFAULT_READ_SERIALIZABLE_OBJECT, Type.VOID,
                        new Type[] { Type.OBJECT, Type.INT },
                        Const.INVOKEVIRTUAL));
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
