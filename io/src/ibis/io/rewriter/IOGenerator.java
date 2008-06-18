/* $Id$ */

package ibis.io.rewriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
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
import org.apache.bcel.generic.IF_ICMPEQ;
import org.apache.bcel.generic.IF_ICMPGT;
import org.apache.bcel.generic.IF_ICMPLT;
import org.apache.bcel.generic.IF_ICMPNE;
import org.apache.bcel.generic.IF_ACMPEQ;
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

/* TODO: docs.
 */

public class IOGenerator extends ibis.compile.IbiscComponent {
    private static final String ibis_input_stream_name
            = "ibis.io.IbisSerializationInputStream";

    private static final String ibis_output_stream_name
            = "ibis.io.IbisSerializationOutputStream";

    private static final String sun_input_stream_name
            = "java.io.ObjectInputStream";

    private static final String sun_output_stream_name
            = "java.io.ObjectOutputStream";

    static final ObjectType ibis_input_stream = new ObjectType(
            ibis_input_stream_name);

    static final ObjectType ibis_output_stream = new ObjectType(
            ibis_output_stream_name);

    static final ObjectType sun_input_stream = new ObjectType(
            sun_input_stream_name);

    static final ObjectType sun_output_stream = new ObjectType(
            sun_output_stream_name);

    static final Type[] ibis_input_stream_arrtp
            = new Type[] { ibis_input_stream };

    static final Type[] ibis_output_stream_arrtp
            = new Type[] { ibis_output_stream };

    static final Type java_lang_class_type = Type.getType("Ljava/lang/Class;");

    static HashMap<String, Long> serialversionids = new HashMap<String, Long>();

    private static class FieldComparator implements Comparator<Field> {
        public int compare(Field f1, Field f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }

    static FieldComparator fieldComparator = new FieldComparator();

    private static class SerializationInfo {

        String write_name;

        String read_name;

        String final_read_name;

        Type tp;

        Type[] param_tp_arr;

        boolean primitive;

        SerializationInfo(String wn, String rn, String frn, Type t,
                boolean primitive) {
            this.write_name = wn;
            this.read_name = rn;
            this.final_read_name = frn;
            this.tp = t;
            this.param_tp_arr = new Type[] { t };
            this.primitive = primitive;
        }
    }

    private class CodeGenerator {
        JavaClass clazz;

        ClassGen gen;

        String classname;

        String super_classname;

        JavaClass super_class;

        boolean super_is_serializable;

        boolean super_is_ibis_serializable;

        boolean super_has_ibis_constructor;

        boolean is_externalizable;

        boolean has_serial_persistent_fields;

        boolean final_fields;

        Field[] fields;

        Method[] methods;

        InstructionFactory factory;

        ConstantPoolGen constantpool;

        CodeGenerator(JavaClass cl) {
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
            Arrays.sort(fields, fieldComparator);

            super_is_serializable = isSerializable(super_class);
            is_externalizable = isExternalizable(cl);
            super_is_ibis_serializable = isIbisSerializable(super_class);
            super_has_ibis_constructor = hasIbisConstructor(super_class);
            has_serial_persistent_fields = hasSerialPersistentFields();
            final_fields = hasFinalFields();
        }

        /**
         * Computes the serial version UID value for the given class.
         */
        private long computeSUID() {
            if (!isSerializable(clazz)) {
                return 0L;
            }

            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);

                // 1. The class name written using UTF encoding.
                dout.writeUTF(clazz.getClassName());

                // 2. The class modifiers written as a 32-bit integer.
                int classModifiers = clazz.getModifiers()
                        & (Constants.ACC_PUBLIC | Constants.ACC_FINAL
                                | Constants.ACC_INTERFACE
                                | Constants.ACC_ABSTRACT);

                // Only set ABSTRACT for an interface when it has methods.
                Method[] cMethods = clazz.getMethods();
                if ((classModifiers & Constants.ACC_INTERFACE) != 0) {
                    if (cMethods.length > 0) {
                        classModifiers |= Constants.ACC_ABSTRACT;
                    } else {
                        classModifiers &= ~Constants.ACC_ABSTRACT;
                    }
                }
                dout.writeInt(classModifiers);

                // 3. The name of each interface sorted by name written using
                //    UTF encoding.
                String[] interfaceNames = clazz.getInterfaceNames();
                Arrays.sort(interfaceNames);
                for (int i = 0; i < interfaceNames.length; i++) {
                    dout.writeUTF(interfaceNames[i]);
                }

                // 4. For each field of the class sorted by field name (except
                //    private static and private transient fields):
                Field[] cFields = clazz.getFields();
                Arrays.sort(cFields, fieldComparator);
                for (int i = 0; i < cFields.length; i++) {
                    int mods = cFields[i].getModifiers();
                    if (((mods & Constants.ACC_PRIVATE) == 0)
                            || ((mods & (Constants.ACC_STATIC
                                        | Constants.ACC_TRANSIENT)) == 0)) {
                        // 4.1. The name of the field in UTF encoding.
                        dout.writeUTF(cFields[i].getName());
                        // 4.2. The modifiers of the field written as a
                        //      32-bit integer.
                        dout.writeInt(mods);
                        // 4.3. The descriptor of the field in UTF encoding
                        dout.writeUTF(cFields[i].getSignature());
                    }
                }

                // This is where the trouble starts for serialver.

                // 5. If a class initializer exists, write out the following:
                for (int i = 0; i < cMethods.length; i++) {
                    if (cMethods[i].getName().equals("<clinit>")) {
                        // 5.1. The name of the method, <clinit>, in UTF
                        //      encoding.
                        dout.writeUTF("<clinit>");
                        // 5.2. The modifier of the method,
                        //      java.lang.reflect.Modifier.STATIC, written as
                        //      a 32-bit integer.
                        dout.writeInt(Constants.ACC_STATIC);
                        // 5.3. The descriptor of the method, ()V, in UTF
                        //      encoding.
                        dout.writeUTF("()V");
                        break;
                    }
                }

                Arrays.sort(cMethods, new Comparator<Method>() {
                    public int compare(Method o1, Method o2) {
                        String name1 = o1.getName();
                        String name2 = o2.getName();
                        if (name1.equals(name2)) {
                            String sig1 = o1.getSignature();
                            String sig2 = o2.getSignature();
                            return sig1.compareTo(sig2);
                        }
                        return name1.compareTo(name2);
                    }
                });

                // 6. For each non-private constructor sorted by method name
                //    and signature:
                for (int i = 0; i < cMethods.length; i++) {
                    if (cMethods[i].getName().equals("<init>")) {
                        int mods = cMethods[i].getModifiers();
                        if ((mods & Constants.ACC_PRIVATE) == 0) {
                            // 6.1. The name of the method, <init>, in UTF
                            //      encoding.
                            dout.writeUTF("<init>");
                            // 6.2. The modifiers of the method written as a
                            //      32-bit integer.
                            dout.writeInt(mods);
                            // 6.3. The descriptor of the method in UTF
                            //      encoding.
                            dout.writeUTF(cMethods[i].getSignature().replace(
                                    '/', '.'));
                        }
                    }
                }

                // 7. For each non-private method sorted by method name and
                //    signature:
                for (int i = 0; i < cMethods.length; i++) {
                    if (!cMethods[i].getName().equals("<init>")
                            && !cMethods[i].getName().equals("<clinit>")) {
                        int mods = cMethods[i].getModifiers();
                        if ((mods & Constants.ACC_PRIVATE) == 0) {
                            // 7.1. The name of the method in UTF encoding.
                            dout.writeUTF(cMethods[i].getName());
                            // 7.2. The modifiers of the method written as a
                            //      32-bit integer.
                            dout.writeInt(mods);
                            // 7.3. The descriptor of the method in UTF
                            //      encoding.
                            dout.writeUTF(cMethods[i].getSignature().replace(
                                    '/', '.'));
                        }
                    }
                }

                dout.flush();

                // 8. The SHA-1 algorithm is executed on the stream of bytes
                //    produced by DataOutputStream and produces five 32-bit
                //    values sha[0..4].
                MessageDigest md = MessageDigest.getInstance("SHA");
                byte[] hashBytes = md.digest(bout.toByteArray());

                long hash = 0;
                // Use the first 8 bytes.
                for (int i = Math.min(hashBytes.length, 8) - 1; i >= 0; i--) {
                    hash = (hash << 8) | (hashBytes[i] & 0xFF);
                }
                return hash;
            } catch (Exception ex) {
                System.err.println("Warning: could not get serialVersionUID "
                        + "for class " + classname);
                return 0L;
            }
        }

        /**
         * Get the serialversionuid of a class that is about to be
         * rewritten. If necessary, a serialVersionUID field is added.
         */
        private void versionUID() {
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                if (f.getName().equals("serialVersionUID") && f.isFinal()
                        && f.isStatic()) {
                    /* Already present. Just return. */
                    return;
                }
            }

            long uid = 0;
            Long ui = serialversionids.get(classname);
            if (ui == null) {
                uid = computeSUID();
                serialversionids.put(classname, new Long(uid));
            } else {
                uid = ui.longValue();
            }

            if (uid != 0) {
                FieldGen f = new FieldGen(Constants.ACC_PRIVATE
                        | Constants.ACC_FINAL | Constants.ACC_STATIC,
                        Type.LONG, "serialVersionUID", constantpool);
                f.setInitValue(uid);
                gen.addField(f.getField());
                fields = gen.getFields();
            }
        }

        private boolean hasSerialPersistentFields() {
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                if (f.getName().equals("serialPersistentFields")
                        && f.isFinal()
                        && f.isStatic()
                        && f.isPrivate()
                        && f.getSignature().equals(
                                "[Ljava/io/ObjectStreamField;")) {
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
                if (methods[i].getName().equals(name)
                        && methods[i].getSignature().equals(signature)) {
                    return i;
                }
            }
            return -1;
        }

        private boolean hasWriteObject() {
            return findMethod("writeObject", "(Ljava/io/ObjectOutputStream;)V")
                    != -1;
        }

        private boolean hasReadObject() {
            return findMethod("readObject", "(Ljava/io/ObjectInputStream;)V")
                    != -1;
        }

        private boolean hasIbisConstructor(JavaClass cl) {
            Method[] clMethods = cl.getMethods();

            for (int i = 0; i < clMethods.length; i++) {
                if (clMethods[i].getName().equals("<init>")
                        && clMethods[i].getSignature().equals(
                                "(Libis/io/IbisSerializationInputStream;)V")) {
                    return true;
                }
            }
            return false;
        }

        private Instruction createGeneratedWriteObjectInvocation(String name,
                short invmode) {
            return factory.createInvoke(name, "generated_WriteObject",
                    Type.VOID, ibis_output_stream_arrtp, invmode);
        }

        private Instruction createGeneratedDefaultReadObjectInvocation(
                String name, InstructionFactory fac, short invmode) {
            return fac.createInvoke(name, "generated_DefaultReadObject",
                    Type.VOID, new Type[] { ibis_input_stream, Type.INT },
                    invmode);
        }

        private Instruction createInitInvocation(String name,
                InstructionFactory f) {
            return f.createInvoke(name, "<init>", Type.VOID,
                    ibis_input_stream_arrtp, Constants.INVOKESPECIAL);
        }

        private Instruction createGeneratedDefaultWriteObjectInvocation(
                String name) {
            return factory.createInvoke(name, "generated_DefaultWriteObject",
                    Type.VOID, new Type[] { ibis_output_stream, Type.INT },
                    Constants.INVOKESPECIAL);
        }

        private Instruction createWriteObjectInvocation() {
            return factory.createInvoke(classname, "writeObject", Type.VOID,
                    new Type[] { sun_output_stream }, Constants.INVOKESPECIAL);
        }

        private int getClassDepth(JavaClass cl) {
            if (!isSerializable(cl)) {
                return 0;
            }
            return 1 + getClassDepth(Repository.lookupClass(
                        cl.getSuperclassName()));
        }

        void generateMethods() {
            /* Generate the necessary (empty) methods. */

            if (verbose) {
                System.out.println("  Generating empty methods for class : "
                        + classname);
                System.out.println("    " + classname
                        + " implements java.io.Serializable -> adding "
                        + "ibis.io.Serializable");
            }

            /* add the ibis.io.Serializable interface to the class */
            gen.addInterface("ibis.io.Serializable");

            /* Construct a write method */
            InstructionList il = new InstructionList();
            il.append(new RETURN());

            int flags = Constants.ACC_PUBLIC
                    | (gen.isFinal() ? Constants.ACC_FINAL : 0);

            MethodGen write_method = new MethodGen(flags, Type.VOID,
                    ibis_output_stream_arrtp, new String[] { "os" },
                    "generated_WriteObject", classname, il, constantpool);

            write_method.addException("java.io.IOException");
            gen.addMethod(write_method.getMethod());

            /* ... and a default_write_method */
            il = new InstructionList();
            il.append(new RETURN());

            MethodGen default_write_method = new MethodGen(flags, Type.VOID,
                    new Type[] { ibis_output_stream, Type.INT }, new String[] {
                            "os", "lvl" }, "generated_DefaultWriteObject",
                    classname, il, constantpool);

            default_write_method.addException("java.io.IOException");
            gen.addMethod(default_write_method.getMethod());

            /* ... and a default_read_method */
            il = new InstructionList();
            il.append(new RETURN());

            MethodGen default_read_method = new MethodGen(flags, Type.VOID,
                    new Type[] { ibis_input_stream, Type.INT }, new String[] {
                            "os", "lvl" }, "generated_DefaultReadObject",
                    classname, il, constantpool);

            default_read_method.addException("java.io.IOException");
            default_read_method.addException(
                    "java.lang.ClassNotFoundException");
            gen.addMethod(default_read_method.getMethod());

            /* Construct a read-of-the-stream constructor, but only when we
             * can actually use it.
             */ 
            if (is_externalizable || !super_is_serializable
                    || force_generated_calls || super_has_ibis_constructor) {
                il = new InstructionList();
                il.append(new RETURN());

                MethodGen read_cons = new MethodGen(Constants.ACC_PUBLIC,
                        Type.VOID, ibis_input_stream_arrtp,
                        new String[] { "is" }, "<init>", classname, il,
                        constantpool);
                read_cons.addException("java.io.IOException");
                read_cons.addException("java.lang.ClassNotFoundException");
                gen.addMethod(read_cons.getMethod());
            } else if (hasReadObject()) {
                il = new InstructionList();
                il.append(new RETURN());
                MethodGen readobjectWrapper = new MethodGen(
                        Constants.ACC_PUBLIC, Type.VOID,
                        ibis_input_stream_arrtp, new String[] { "is" },
                        "$readObjectWrapper$", classname, il, constantpool);
                readobjectWrapper.addException("java.io.IOException");
                readobjectWrapper.addException(
                        "java.lang.ClassNotFoundException");
                gen.addMethod(readobjectWrapper.getMethod());
            }

            /* Now, create a new class structure, which has these methods. */
            JavaClass newclazz = gen.getJavaClass();

            if (target_classes.remove(clazz)) {
                Repository.removeClass(clazz);
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

            if (!info.primitive) {
                t = Type.getType(field_sig);
            }

            temp.append(new ALOAD(1));
            temp.append(new ALOAD(0));
            temp.append(factory.createFieldAccess(classname, field.getName(),
                    t, Constants.GETFIELD));
            temp.append(factory.createInvoke(ibis_output_stream_name,
                    info.write_name, Type.VOID, info.param_tp_arr,
                    Constants.INVOKEVIRTUAL));

            return temp;
        }

        private InstructionList readInstructions(Field field,
                boolean from_constructor) {
            String field_sig = field.getSignature();
            Type field_type = Type.getType(field_sig);
            SerializationInfo info = getSerializationInfo(field_type);

            Type t = info.tp;
            InstructionList temp = new InstructionList();

            if (!info.primitive) {
                t = Type.getType(field_sig);
            }

            if (from_constructor || !field.isFinal()) {
                temp.append(new ALOAD(0));
                temp.append(new ALOAD(1));
                temp.append(factory.createInvoke(ibis_input_stream_name,
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
                temp.append(factory.createInvoke(ibis_input_stream_name,
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
            return "writeArray" + name.substring(0, 1).toUpperCase()
                    + name.substring(1);
        }

        private InstructionList writeReferenceField(Field field) {
            Type field_type = Type.getType(field.getSignature());
            InstructionList write_il = new InstructionList();

            boolean isfinal = false;
            boolean isarray = false;
            JavaClass field_class = null;
            String basicname = null;

            if (verbose) {
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
                            && (hasIbisConstructor(field_class)
                                    || (isSerializable(field_class)
                                            && force_generated_calls)))) {
                // If there is an object replacer, we cannot do the
                // "fast" code.
                write_il.append(new ACONST_NULL());
                write_il.append(new ALOAD(1));
                write_il.append(factory.createFieldAccess(
                            ibis_output_stream_name, "replacer", 
                            new ObjectType("ibis.io.Replacer"),
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
                            "ibis.io.Constants",
                            "TYPE_" + basicname.toUpperCase(), Type.INT,
                            Constants.GETSTATIC));
                    write_il.append(factory.createInvoke(
                            ibis_output_stream_name, "writeKnownArrayHeader",
                            Type.INT, new Type[] { Type.OBJECT, Type.INT },
                            Constants.INVOKEVIRTUAL));
                } else {
                    write_il.append(factory.createInvoke(
                            ibis_output_stream_name, "writeKnownObjectHeader",
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
                                    "ibis.io.IbisSerializationOutputStream",
                                    "writeInt",
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
                                    "ibis.io.IbisSerializationOutputStream",
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
                                        ibis_output_stream_name,
                                        "writeKnownObjectHeader", Type.INT,
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
                    classname, "serialPersistentFields", new ArrayType(
                            new ObjectType("java.io.ObjectStreamField"), 1),
                    Constants.GETSTATIC);
            InstructionList write_il = new InstructionList();
            int[] case_values = new int[] { 'B', 'C', 'D', 'F', 'I', 'J', 'S',
                    'Z' };
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
                    factory.createInvoke("java.io.ObjectStreamField",
                            "getName", Type.STRING, Type.NO_ARGS,
                            Constants.INVOKEVIRTUAL));
            write_il.append(new ASTORE(3));

            InstructionHandle begin_try = write_il.append(new PUSH(
                    constantpool, classname));
            write_il.append(factory.createInvoke("java.lang.Class", "forName",
                    java_lang_class_type, new Type[] { Type.STRING },
                    Constants.INVOKESTATIC));
            write_il.append(new ALOAD(3));
            write_il.append(factory.createInvoke("java.lang.Class", "getField",
                    new ObjectType("java.lang.reflect.Field"),
                    new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
            write_il.append(new ASTORE(4));

            write_il.append(persistent_field_access);
            write_il.append(new ILOAD(2));
            write_il.append(new AALOAD());
            write_il.append(factory.createInvoke("java.io.ObjectStreamField",
                    "getTypeCode", Type.CHAR, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));

            case_handles[0] = write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(4));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "getBoolean", Type.BOOLEAN, new Type[] { Type.OBJECT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(factory.createInvoke(
                    "ibis.io.IbisSerializationOutputStream", "writeBoolean",
                    Type.VOID, new Type[] { Type.BOOLEAN },
                    Constants.INVOKEVIRTUAL));
            write_il.append(gotos[0]);

            case_handles[1] = write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(4));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "getChar", Type.CHAR, new Type[] { Type.OBJECT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(
                    factory.createInvoke(
                            "ibis.io.IbisSerializationOutputStream",
                            "writeChar", Type.VOID, new Type[] { Type.INT },
                            Constants.INVOKEVIRTUAL));
            write_il.append(gotos[1]);

            case_handles[2] = write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(4));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "getDouble", Type.DOUBLE, new Type[] { Type.OBJECT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(factory.createInvoke(
                    "ibis.io.IbisSerializationOutputStream", "writeDouble",
                    Type.VOID, new Type[] { Type.DOUBLE },
                    Constants.INVOKEVIRTUAL));
            write_il.append(gotos[2]);

            case_handles[3] = write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(4));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "getFloat", Type.FLOAT, new Type[] { Type.OBJECT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(factory.createInvoke(
                    "ibis.io.IbisSerializationOutputStream", "writeFloat",
                    Type.VOID, new Type[] { Type.FLOAT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(gotos[3]);

            case_handles[4] = write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(4));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "getInt", Type.INT, new Type[] { Type.OBJECT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(
                    factory.createInvoke(
                            "ibis.io.IbisSerializationOutputStream",
                            "writeInt", Type.VOID, new Type[] { Type.INT },
                            Constants.INVOKEVIRTUAL));
            write_il.append(gotos[4]);

            case_handles[5] = write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(4));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "getLong", Type.LONG, new Type[] { Type.OBJECT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(factory.createInvoke(
                    "ibis.io.IbisSerializationOutputStream", "writeLong",
                    Type.VOID, new Type[] { Type.LONG },
                    Constants.INVOKEVIRTUAL));
            write_il.append(gotos[5]);

            case_handles[6] = write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(4));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "getShort", Type.SHORT, new Type[] { Type.OBJECT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(
                    factory.createInvoke(
                        "ibis.io.IbisSerializationOutputStream", "writeShort",
                        Type.VOID, new Type[] { Type.INT },
                        Constants.INVOKEVIRTUAL));
            write_il.append(gotos[6]);

            case_handles[7] = write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(4));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "getBoolean", Type.BOOLEAN, new Type[] { Type.OBJECT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(factory.createInvoke(
                    "ibis.io.IbisSerializationOutputStream", "writeBoolean",
                    Type.VOID, new Type[] { Type.BOOLEAN },
                    Constants.INVOKEVIRTUAL));
            write_il.append(gotos[7]);

            InstructionHandle default_handle = write_il.append(new ALOAD(1));
            write_il.append(new ALOAD(4));
            write_il.append(new ALOAD(0));
            write_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "get", Type.OBJECT, new Type[] { Type.OBJECT },
                    Constants.INVOKEVIRTUAL));
            write_il.append(factory.createInvoke(
                    "ibis.io.IbisSerializationOutputStream",
                    "writeObject", Type.VOID,
                    new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
            InstructionHandle end_try = write_il.append(gotos[8]);

            write_il.insert(case_handles[0], new SWITCH(case_values,
                    case_handles, default_handle));

            InstructionHandle handler = write_il.append(new ASTORE(6));
            write_il.append(factory.createNew("java.io.IOException"));
            write_il.append(new DUP());
            write_il.append(factory.createNew("java.lang.StringBuffer"));
            write_il.append(new DUP());
            write_il.append(
                    factory.createInvoke("java.lang.StringBuffer",
                            "<init>", Type.VOID, Type.NO_ARGS,
                            Constants.INVOKESPECIAL));
            write_il.append(new PUSH(constantpool, "Could not write field "));
            write_il.append(factory.createInvoke("java.lang.StringBuffer",
                    "append", Type.STRINGBUFFER, new Type[] { Type.STRING },
                    Constants.INVOKEVIRTUAL));
            write_il.append(new ALOAD(3));
            write_il.append(factory.createInvoke("java.lang.StringBuffer",
                    "append", Type.STRINGBUFFER, new Type[] { Type.STRING },
                    Constants.INVOKEVIRTUAL));
            write_il.append(factory.createInvoke("java.lang.StringBuffer",
                    "toString", Type.STRING, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));
            write_il.append(factory.createInvoke("java.io.IOException",
                    "<init>", Type.VOID, new Type[] { Type.STRING },
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
                    new ObjectType("java.lang.Exception"));

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
                        if (verbose) {
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
                        if (verbose) {
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
            return "readArray" + name.substring(0, 1).toUpperCase()
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

            if (verbose) {
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
                            && (hasIbisConstructor(field_class)
                                    || (isSerializable(field_class)
                                            && force_generated_calls)))) {
                read_il.append(new ALOAD(1));
                read_il.append(factory.createInvoke(ibis_input_stream_name,
                        "readKnownTypeHeader", Type.INT, Type.NO_ARGS,
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
                                ibis_input_stream_name, callname, field_type,
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
                                ibis_input_stream_name, "readInt", Type.INT,
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
                                ibis_input_stream_name,
                                "addObjectToCycleCheck", Type.VOID,
                                new Type[] { Type.OBJECT },
                                Constants.INVOKEVIRTUAL));
                        read_il.append(new ICONST(0));
                        read_il.append(new ISTORE(4));
                        GOTO gto1 = new GOTO(null);
                        read_il.append(gto1);

                        InstructionHandle loop_body_start
                                = read_il.append(new ALOAD(1));
                        read_il.append(
                                factory.createInvoke(ibis_input_stream_name,
                                        "readKnownTypeHeader", Type.INT,
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
                                ibis_input_stream_name,
                                "getObjectFromCycleCheck", Type.OBJECT,
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
                read_il.append(factory.createInvoke(ibis_input_stream_name,
                        "getObjectFromCycleCheck", Type.OBJECT,
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

            if (from_constructor || !final_fields) {
                h = read_il.append(new ALOAD(4));
                read_il.append(new ALOAD(0));
                read_il.append(new ALOAD(1));
                if (tpname.equals("")) {
                    read_il.append(factory.createInvoke(ibis_input_stream_name,
                            "readObject", Type.OBJECT, Type.NO_ARGS,
                            Constants.INVOKEVIRTUAL));
                } else {
                    read_il.append(factory.createInvoke(ibis_input_stream_name,
                            "read" + tpname, tp, Type.NO_ARGS,
                            Constants.INVOKEVIRTUAL));
                }
                read_il.append(
                        factory.createInvoke("java.lang.reflect.Field",
                                "set" + tpname, Type.VOID,
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
                read_il.append(factory.createInvoke("java.lang.reflect.Field",
                        "getType", new ObjectType("java.lang.Class"),
                        Type.NO_ARGS, Constants.INVOKEVIRTUAL));
                read_il.append(factory.createInvoke("java.lang.Class",
                        "getName", Type.STRING, Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));

                read_il.append(factory.createInvoke(
                        "ibis.io.IbisSerializationInputStream",
                        "readFieldObject", Type.VOID, new Type[] { Type.OBJECT,
                                Type.STRING, Type.STRING },
                        Constants.INVOKEVIRTUAL));
            } else {
                read_il.append(factory.createInvoke(
                        "ibis.io.IbisSerializationInputStream", "readField"
                                + tpname, Type.VOID, new Type[] { Type.OBJECT,
                                Type.STRING }, Constants.INVOKEVIRTUAL));
            }
            GOTO gto2 = new GOTO(null);
            read_il.append(gto2);
            eq.setTarget(read_il.append(new ALOAD(4)));
            read_il.append(new ALOAD(0));
            read_il.append(new ALOAD(1));
            if (tpname.equals("")) {
                read_il.append(factory.createInvoke(ibis_input_stream_name,
                        "readObject", tp, Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));
            } else {
                read_il.append(factory.createInvoke(ibis_input_stream_name,
                        "read" + tpname, tp, Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));
            }
            read_il.append(factory.createInvoke("java.lang.reflect.Field",
                    "set" + tpname, Type.VOID, new Type[] { Type.OBJECT, tp },
                    Constants.INVOKEVIRTUAL));
            gto2.setTarget(read_il.append(gto));

            return h;
        }

        private InstructionList serialPersistentReads(boolean from_constructor,
                MethodGen read_gen) {
            Instruction persistent_field_access = factory.createFieldAccess(
                    classname, "serialPersistentFields", new ArrayType(
                            new ObjectType("java.io.ObjectStreamField"), 1),
                    Constants.GETSTATIC);
            InstructionList read_il = new InstructionList();
            int[] case_values = new int[] { 'B', 'C', 'D', 'F', 'I', 'J', 'S',
                    'Z' };
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
            read_il.append(factory.createInvoke("java.io.ObjectStreamField",
                    "getName", Type.STRING, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));
            read_il.append(new ASTORE(3));

            InstructionHandle begin_try = read_il.append(new PUSH(constantpool,
                    classname));
            read_il.append(factory.createInvoke("java.lang.Class", "forName",
                    java_lang_class_type, new Type[] { Type.STRING },
                    Constants.INVOKESTATIC));
            read_il.append(new ALOAD(3));
            read_il.append(factory.createInvoke("java.lang.Class", "getField",
                    new ObjectType("java.lang.reflect.Field"),
                    new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
            read_il.append(new ASTORE(4));

            if (!from_constructor && final_fields) {
                read_il.append(new ALOAD(4));
                read_il.append(factory.createInvoke("java.lang.reflect.Field",
                        "getModifiers", Type.INT, Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));
                read_il.append(new ISTORE(5));
            }

            read_il.append(persistent_field_access);
            read_il.append(new ILOAD(2));
            read_il.append(new AALOAD());
            read_il.append(factory.createInvoke("java.io.ObjectStreamField",
                    "getTypeCode", Type.CHAR, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));

            case_handles[0] = generateReadField("Byte", Type.BYTE, read_il,
                    gotos[0], from_constructor);
            case_handles[1] = generateReadField("Char", Type.CHAR, read_il,
                    gotos[1], from_constructor);
            case_handles[2] = generateReadField("Double", Type.DOUBLE, read_il,
                    gotos[2], from_constructor);
            case_handles[3] = generateReadField("Float", Type.FLOAT, read_il,
                    gotos[3], from_constructor);
            case_handles[4] = generateReadField("Int", Type.INT, read_il,
                    gotos[4], from_constructor);
            case_handles[5] = generateReadField("Long", Type.LONG, read_il,
                    gotos[5], from_constructor);
            case_handles[6] = generateReadField("Short", Type.SHORT, read_il,
                    gotos[6], from_constructor);
            case_handles[7] = generateReadField("Boolean", Type.BOOLEAN,
                    read_il, gotos[7], from_constructor);

            InstructionHandle default_handle = generateReadField("",
                    Type.OBJECT, read_il, gotos[8], from_constructor);

            InstructionHandle end_try = read_il.getEnd();

            read_il.insert(case_handles[0], new SWITCH(case_values,
                    case_handles, default_handle));

            InstructionHandle handler = read_il.append(new ASTORE(6));
            read_il.append(factory.createNew("java.io.IOException"));
            read_il.append(new DUP());
            read_il.append(factory.createNew("java.lang.StringBuffer"));
            read_il.append(new DUP());
            read_il.append(factory.createInvoke("java.lang.StringBuffer",
                    "<init>", Type.VOID, Type.NO_ARGS,
                    Constants.INVOKESPECIAL));
            read_il.append(new PUSH(constantpool, "Could not read field "));
            read_il.append(factory.createInvoke("java.lang.StringBuffer",
                    "append", Type.STRINGBUFFER, new Type[] { Type.STRING },
                    Constants.INVOKEVIRTUAL));
            read_il.append(new ALOAD(3));
            read_il.append(factory.createInvoke("java.lang.StringBuffer",
                    "append", Type.STRINGBUFFER, new Type[] { Type.STRING },
                    Constants.INVOKEVIRTUAL));
            read_il.append(factory.createInvoke("java.lang.StringBuffer",
                    "toString", Type.STRING, Type.NO_ARGS,
                    Constants.INVOKEVIRTUAL));
            read_il.append(factory.createInvoke("java.io.IOException",
                    "<init>", Type.VOID, new Type[] { Type.STRING },
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
                    new ObjectType("java.lang.Exception"));

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
                        if (verbose) {
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

                    if (verbose) {
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

            if (verbose) {
                System.out.println("  Generating InstanceGenerator class for "
                        + classname);
            }

            String name = classname + "_ibis_io_Generator";

            ObjectType class_type = new ObjectType(classname);

            String classfilename = name.substring(name.lastIndexOf('.') + 1)
                    + ".class";
            ClassGen iogenGen = new ClassGen(name, "ibis.io.Generator",
                    classfilename, Constants.ACC_FINAL | Constants.ACC_PUBLIC
                            | Constants.ACC_SUPER, null);
            InstructionFactory iogenFactory = new InstructionFactory(iogenGen);

            InstructionList il = new InstructionList();

            if (!is_externalizable && super_is_serializable
                    && !super_has_ibis_constructor && !force_generated_calls) {
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
                il.append(iogenFactory.createInvoke(ibis_input_stream_name,
                        "create_uninitialized_object", Type.OBJECT,
                        new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
                il.append(iogenFactory.createCheckCast(class_type));
                il.append(new ASTORE(2));

                /* Now read the superclass. */
                il.append(new ALOAD(1));
                il.append(new ALOAD(2));
                ind = iogenGen.getConstantPool().addString(super_classname);
                il.append(new LDC(ind));
                il.append(iogenFactory.createInvoke(ibis_input_stream_name,
                        "readSerializableObject", Type.VOID, new Type[] {
                                Type.OBJECT, Type.STRING },
                        Constants.INVOKEVIRTUAL));

                /* Now, if the class has a readObject, call it. Otherwise,
                 * read its fields, by calling generated_DefaultReadObject.
                 */
                if (hasReadObject()) {
                    il.append(new ALOAD(2));
                    il.append(new ALOAD(1));
                    il.append(iogenFactory.createInvoke(classname,
                            "$readObjectWrapper$", Type.VOID,
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
                    ibis_input_stream_arrtp, new String[] { "is" },
                    "generated_newInstance", name, il,
                    iogenGen.getConstantPool());

            method.setMaxStack(3);
            method.setMaxLocals();
            method.addException("java.io.IOException");
            method.addException("java.lang.ClassNotFoundException");
            iogenGen.addMethod(method.getMethod());

            il = new InstructionList();
            il.append(new ALOAD(0));
            il.append(iogenFactory.createInvoke("ibis.io.Generator", "<init>",
                    Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
            il.append(new RETURN());

            method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID,
                    Type.NO_ARGS, null, "<init>", name, il,
                    iogenGen.getConstantPool());

            method.setMaxStack(1);
            method.setMaxLocals();
            iogenGen.addMethod(method.getMethod());

            return iogenGen.getJavaClass();
        }

        void generateCode() {
            /* Generate code inside the methods */
            int write_method_index = findMethod("generated_WriteObject",
                    "(Libis/io/IbisSerializationOutputStream;)V");
            int default_write_method_index = findMethod(
                    "generated_DefaultWriteObject",
                    "(Libis/io/IbisSerializationOutputStream;I)V");
            int default_read_method_index = findMethod(
                    "generated_DefaultReadObject",
                    "(Libis/io/IbisSerializationInputStream;I)V");
            int read_cons_index = findMethod("<init>",
                    "(Libis/io/IbisSerializationInputStream;)V");
            int read_wrapper_index = findMethod("$readObjectWrapper$",
                    "(Libis/io/IbisSerializationInputStream;)V");

            if (verbose) {
                System.out.println("  Generating method code class for class : "
                                + classname);
                System.out.println("    Number of fields " + fields.length);
            }

            int dpth = getClassDepth(clazz);

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
                if (super_is_ibis_serializable || force_generated_calls) {
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
                            ibis_output_stream_name,
                            "defaultWriteSerializableObject", Type.VOID,
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
                if (super_is_ibis_serializable || force_generated_calls) {
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
                    read_il.append(factory.createInvoke(ibis_input_stream_name,
                            "defaultReadSerializableObject", Type.VOID,
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

            /* Now, produce the read constructor. It only exists if the
             * superclass is not serializable, or if the superclass has an
             * ibis constructor, or is assumed to have one (-force option).
             */

            read_il = null;
            if (is_externalizable || super_has_ibis_constructor
                    || !super_is_serializable || force_generated_calls) {
                read_il = new InstructionList();
                if (is_externalizable) {
                    read_il.append(new ALOAD(0));
                    read_il.append(factory.createInvoke(classname, "<init>",
                            Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
                } else if (!super_is_serializable) {
                    read_il.append(new ALOAD(0));
                    read_il.append(factory.createInvoke(super_classname,
                            "<init>", Type.VOID, Type.NO_ARGS,
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
                            factory.createInvoke(ibis_input_stream_name,
                                    "addObjectToCycleCheck", Type.VOID,
                                    new Type[] { Type.OBJECT },
                                    Constants.INVOKEVIRTUAL));
                }
            }

            /* Now, produce generated_WriteObject. */
            write_il = new InstructionList();
            write_gen = new MethodGen(methods[write_method_index], classname,
                    constantpool);

            /* write the superclass if neccecary */
            if (is_externalizable) {
                /* Nothing to be done for the superclass. */
            } else if (super_is_ibis_serializable
                    || (force_generated_calls && super_is_serializable)) {
                write_il.append(new ALOAD(0));
                write_il.append(new ALOAD(1));
                write_il.append(createGeneratedWriteObjectInvocation(
                        super_classname, Constants.INVOKESPECIAL));

            } else if (super_is_serializable) {
                int ind = constantpool.addString(super_classname);
                write_il.append(new ALOAD(1));
                write_il.append(new ALOAD(0));
                write_il.append(new LDC(ind));
                write_il.append(factory.createInvoke(ibis_output_stream_name,
                        "writeSerializableObject", Type.VOID, new Type[] {
                                Type.OBJECT, Type.STRING },
                        Constants.INVOKEVIRTUAL));
            }

            /* and now ... generated_WriteObject should either call the classes
             * writeObject, if it has one, or call generated_DefaultWriteObject.
             * The read constructor should either call readObject, or call
             * generated_DefaultReadObject.
             */
            if (is_externalizable || hasWriteObject()) {
                /* First, get and set IbisSerializationOutputStream's idea of
                 * the current object.
                 */
                write_il.append(new ALOAD(1));
                write_il.append(new ALOAD(0));
                write_il.append(new SIPUSH((short) dpth));
                write_il.append(factory.createInvoke(ibis_output_stream_name,
                        "push_current_object", Type.VOID, new Type[] {
                                Type.OBJECT, Type.INT },
                        Constants.INVOKEVIRTUAL));

                write_il.append(new ALOAD(0));
                write_il.append(new ALOAD(1));
                write_il.append(factory.createInvoke(
                            ibis_output_stream_name,
                            "getJavaObjectOutputStream",
                            sun_output_stream,
                            Type.NO_ARGS,
                            Constants.INVOKEVIRTUAL));
                if (is_externalizable) {
                    /* Invoke writeExternal */
                    write_il.append(
                            factory.createInvoke(classname, "writeExternal",
                                    Type.VOID, new Type[] { new ObjectType(
                                            "java.io.ObjectOutput") },
                                    Constants.INVOKEVIRTUAL));
                } else {
                    /* Invoke writeObject. */
                    write_il.append(createWriteObjectInvocation());
                }

                /* And then, restore IbisSerializationOutputStream's idea of the current object. */
                write_il.append(new ALOAD(1));
                write_il.append(factory.createInvoke(ibis_output_stream_name,
                        "pop_current_object", Type.VOID, Type.NO_ARGS,
                        Constants.INVOKEVIRTUAL));
            } else {
                write_il.append(generateDefaultWrites(write_gen));
            }

            /* Now, do the same for the reading side. */
            MethodGen mgen = null;
            int index = -1;
            if (read_il != null) {
                mgen = new MethodGen(methods[read_cons_index], classname,
                        constantpool);
                index = read_cons_index;
            } else if (hasReadObject()) {
                mgen = new MethodGen(methods[read_wrapper_index], classname,
                        constantpool);
                read_il = new InstructionList();
                index = read_wrapper_index;
            }

            if (read_il != null) {
                if (is_externalizable || hasReadObject()) {
                    /* First, get and set IbisSerializationInputStream's idea of the current object. */
                    read_il.append(new ALOAD(1));
                    read_il.append(new ALOAD(0));
                    read_il.append(new SIPUSH((short) dpth));
                    read_il.append(factory.createInvoke(ibis_input_stream_name,
                            "push_current_object", Type.VOID, new Type[] {
                                    Type.OBJECT, Type.INT },
                            Constants.INVOKEVIRTUAL));

                    read_il.append(new ALOAD(0));
                    read_il.append(new ALOAD(1));
                    read_il.append(factory.createInvoke(
                                ibis_input_stream_name,
                                "getJavaObjectInputStream",
                                sun_input_stream,
                                Type.NO_ARGS,
                                Constants.INVOKEVIRTUAL));
                    if (is_externalizable) {
                        /* Invoke readExternal */
                        read_il.append(factory.createInvoke(classname,
                                "readExternal", Type.VOID,
                                new Type[] { new ObjectType(
                                        "java.io.ObjectInput") },
                                Constants.INVOKEVIRTUAL));
                    } else {
                        /* Invoke readObject. */
                        read_il.append(factory.createInvoke(classname,
                                "readObject", Type.VOID,
                                new Type[] { sun_input_stream },
                                Constants.INVOKESPECIAL));
                    }

                    /* And then, restore IbisSerializationOutputStream's idea of the current object. */
                    read_il.append(new ALOAD(1));
                    read_il.append(factory.createInvoke(ibis_input_stream_name,
                            "pop_current_object", Type.VOID, Type.NO_ARGS,
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

            write_gen = new MethodGen(methods[write_method_index], classname,
                    constantpool);
            write_il.append(write_gen.getInstructionList());
            write_gen.setInstructionList(write_il);

            write_gen.setMaxStack(MethodGen.getMaxStack(constantpool, write_il,
                    write_gen.getExceptionHandlers()));
            write_gen.setMaxLocals();

            gen.setMethodAt(write_gen.getMethod(), write_method_index);

            clazz = gen.getJavaClass();

            Repository.removeClass(classname);
            Repository.addClass(clazz);

            JavaClass instgen = generateInstanceGenerator();

            Repository.addClass(instgen);

            if (fromIbisc) {
                setModified(wrapper.getInfo(clazz));
                addEntry(wrapper.getInfo(instgen), clazz.getClassName());
            }

            classes_to_save.add(clazz);
            classes_to_save.add(instgen);
        }
    }

    boolean local = true;

    boolean file = false;

    boolean force_generated_calls = false;

    boolean silent = false;

    HashMap<Type, SerializationInfo>  primitiveSerialization;

    SerializationInfo referenceSerialization;

    Vector<JavaClass> classes_to_rewrite, target_classes, classes_to_save;

    HashMap<String, JavaClass> arguments;

    boolean fromIbisc = false;

    public IOGenerator() {
        classes_to_rewrite = new Vector<JavaClass>();
        target_classes = new Vector<JavaClass>();
        classes_to_save = new Vector<JavaClass>();

        primitiveSerialization = new HashMap<Type, SerializationInfo> ();
        arguments = new HashMap<String, JavaClass>();

        primitiveSerialization.put(Type.BOOLEAN, new SerializationInfo(
                "writeBoolean", "readBoolean", "readFieldBoolean",
                Type.BOOLEAN, true));

        primitiveSerialization.put(Type.BYTE, new SerializationInfo(
                "writeByte", "readByte", "readFieldByte", Type.BYTE, true));

        primitiveSerialization.put(Type.SHORT, new SerializationInfo(
                "writeShort", "readShort", "readFieldShort", Type.SHORT, true));

        primitiveSerialization.put(Type.CHAR, new SerializationInfo(
                "writeChar", "readChar", "readFieldChar", Type.CHAR, true));

        primitiveSerialization.put(Type.INT, new SerializationInfo("writeInt",
                "readInt", "readFieldInt", Type.INT, true));
        primitiveSerialization.put(Type.LONG, new SerializationInfo(
                "writeLong", "readLong", "readFieldLong", Type.LONG, true));

        primitiveSerialization.put(Type.FLOAT, new SerializationInfo(
                "writeFloat", "readFloat", "readFieldFloat", Type.FLOAT, true));

        primitiveSerialization.put(Type.DOUBLE, new SerializationInfo(
                "writeDouble", "readDouble", "readFieldDouble", Type.DOUBLE,
                true));
        primitiveSerialization.put(Type.STRING, new SerializationInfo(
                "writeString", "readString", "readFieldString", Type.STRING,
                true));
        primitiveSerialization.put(java_lang_class_type, new SerializationInfo(
                "writeClass", "readClass", "readFieldClass",
                java_lang_class_type, true));

        referenceSerialization = new SerializationInfo("writeObject",
                "readObject", "readFieldObject", Type.OBJECT, false);

        silent = true;
    }

    public IOGenerator(boolean verbose, boolean local, boolean file,
            boolean force_generated_calls, boolean silent) {
        this();
        this.verbose = verbose;
        this.local = local;
        this.file = file;
        this.force_generated_calls = force_generated_calls;
        this.silent = silent;
    }

    public boolean processArgs(ArrayList<String> args) {
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("-iogen-force")) {
                force_generated_calls = true;
                args.remove(i);
                i--;
            }
        }
        return true;
    }

    public String getUsageString() {
        return "[-iogen-force]";
    }

    public void process(Iterator classes) {
        fromIbisc = true;
        arguments = new HashMap<String, JavaClass> ();
        for (Iterator<?> i = classes; i.hasNext();) {
            JavaClass cl = (JavaClass) i.next();
            arguments.put(cl.getClassName(), cl);
        }
	for (JavaClass cl : arguments.values()) {
            if (isSerializable(cl)) {
                if (! isIbisSerializable(cl)) {
                    addClass(cl);
                }
            }
        }
        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            JavaClass clazz = classes_to_rewrite.get(i);
            addReferencesToRewrite(clazz);
        }

        /* Sort class to rewrite. Super classes first.  */
        do_sort_classes(classes_to_rewrite);

        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            JavaClass clazz = (JavaClass) classes_to_rewrite.get(i);
            new CodeGenerator(clazz).generateMethods();
        }

        if (verbose) {
            System.out.println("Ibisc: IOGenerator rewriting classes");
        }

        /* Sort target_classes. Super classes first.  */
        do_sort_classes(target_classes);

        for (int i = 0; i < target_classes.size(); i++) {
            JavaClass clazz = target_classes.get(i);
            if (!clazz.isInterface()) {
                if (!silent) {
                    System.out.println("  Rewrite class : "
                            + clazz.getClassName());
                }
                new CodeGenerator(clazz).generateCode();
            }
        }
    }

    public String rewriterImpl() {
        return "BCEL";
    }

    SerializationInfo getSerializationInfo(Type tp) {
        SerializationInfo temp
                = primitiveSerialization.get(tp);
        return (temp == null ? referenceSerialization : temp);
    }

    boolean isSerializable(JavaClass clazz) {
        return Repository.implementationOf(clazz, "java.io.Serializable");
    }

    boolean isExternalizable(JavaClass clazz) {
        return Repository.implementationOf(clazz, "java.io.Externalizable");
    }

    boolean isIbisSerializable(JavaClass clazz) {
        return directImplementationOf(clazz, "ibis.io.Serializable");
    }

    private void addTargetClass(JavaClass clazz) {
        if (!target_classes.contains(clazz) && !isIbisSerializable(clazz)) {
            String nm = clazz.getClassName();
            if (arguments.containsKey(nm)) {
                target_classes.add(clazz);
                if (verbose) {
                    System.out.println("Adding target class : " + nm);
                }
            }
        }
    }

    private void addRewriteClass(Type t, JavaClass clazz) {
        if (t instanceof ArrayType) {
            addRewriteClass(((ArrayType) t).getBasicType(), clazz);
        } else if (t instanceof ObjectType) {
            String name = ((ObjectType) t).getClassName();
            JavaClass c = Repository.lookupClass(name);
            if (c != null) {
                if (!local
                        || clazz.getPackageName().equals(c.getPackageName())) {
                    addClass(c);
                }
            }
        }
    }

    private void addRewriteClass(JavaClass clazz) {
        if (!classes_to_rewrite.contains(clazz) && !isIbisSerializable(clazz)) {
            classes_to_rewrite.add(clazz);
            if (verbose) {
                System.out.println("Adding rewrite class : "
                        + clazz.getClassName());
            }
        }
    }

    private void addClass(JavaClass clazz) {
        boolean serializable = false;

        if (!clazz.isClass()) {
            return;
        }

        if (clazz.getClassName().equals("java.lang.Class")) {
            return;
        }

        if (clazz.getClassName().equals("java.lang.String")) {
            return;
        }

        try {
            if (Repository.instanceOf(clazz, "java.lang.Enum")) {
                return;
            }
        } catch(Exception e) {
            // Sigh: BCEL throws a NullPointerException if java.lang.Enum
            // does not exist
        }

        if (!classes_to_rewrite.contains(clazz)) {

            JavaClass super_classes[] = Repository.getSuperClasses(clazz);

            if (super_classes != null) {
                for (int i = 0; i < super_classes.length; i++) {
                    if (isSerializable(super_classes[i])) {
                        serializable = true;
                        if (!isIbisSerializable(super_classes[i])) {
                            if (!local
                                    || clazz.getPackageName().equals(
                                            super_classes[i].getPackageName())) {
                                addRewriteClass(super_classes[i]);
                            }
                        } else {
                            if (verbose) {
                                System.out.println(clazz.getClassName()
                                        + " already implements "
                                        + "ibis.io.Serializable");
                            }
                        }
                    }
                }
            }

            serializable |= isSerializable(clazz);
        } else {
            serializable = true;
        }

        if (serializable) {
            addRewriteClass(clazz);
            addTargetClass(clazz);
        }
    }

    private static boolean isFinal(Type t) {
        if (t instanceof BasicType) {
            return true;
        }
        if (t instanceof ArrayType) {
            return isFinal(((ArrayType) t).getBasicType());
        }
        if (t instanceof ObjectType) {
            String name = ((ObjectType) t).getClassName();
            JavaClass c = Repository.lookupClass(name);
            if (c == null) {
                return false;
            }
            return c.isFinal();
        }
        return false;
    }

    void addReferencesToRewrite(JavaClass clazz) {

        /* Find all references to final reference types and add these to the
         * rewrite list
         */
        Field[] fields = clazz.getFields();

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];

            /* Don't send fields that are STATIC or TRANSIENT */
            if (!(field.isStatic() || field.isTransient())) {
                Type field_type = Type.getType(field.getSignature());

                if (!(field_type instanceof BasicType)
                        && (field_type != Type.STRING) && isFinal(field_type)) {
                    addRewriteClass(field_type, clazz);
                }
            }
        }
    }

    private static boolean directImplementationOf(JavaClass clazz,
            String name) {
        String names[] = clazz.getInterfaceNames();
        String supername = clazz.getSuperclassName();

        if (supername.equals(name)) {
            return true;
        }

        if (names == null) {
            return false;
        }
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean predecessor(String c1, JavaClass c2) {
        String n = c2.getSuperclassName();

        // System.out.println("comparing " + c1 + ", " + n);
        if (n.equals(c1)) {
            return true;
        }
        if (n.equals("java.lang.Object")) {
            return false;
        }
        return predecessor(c1, Repository.lookupClass(n));
    }

    private void do_sort_classes(Vector<JavaClass> t) {
        int l = t.size();

        for (int i = 0; i < l; i++) {
            JavaClass clazz = (JavaClass) t.get(i);
            int sav_index = i;
            for (int j = i + 1; j < l; j++) {
                JavaClass clazz2 = (JavaClass) t.get(j);

                if (predecessor(clazz2.getClassName(), clazz)) {
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

    private void scanClass(Vector<String> classnames) {

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
        int lngth = classnames.size();
        Object[] names = classnames.toArray();

        java.util.Arrays.sort(names);

        for (int i = lngth - 1; i >= 0; i--) {
            String nm = (String) names[i];
            arguments.put(nm, null);
        }

        for (int i = lngth - 1; i >= 0; i--) {
            if (verbose) {
                System.out.println("  Loading class : " + (String) names[i]);
            }

            String className = (String) names[i];

            JavaClass clazz = null;
            if (!file) {
                clazz = Repository.lookupClass(className);
                if (clazz == null) {
                    System.err.println("Warning: could not load class "
                            + className + ". Please check your classpath.");
                }
            } else {

                System.err.println("class name = " + className);
                try {
                    ClassParser p = new ClassParser(className.replace('.',
                            java.io.File.separatorChar)
                            + ".class");
                    clazz = p.parse();
                    if (clazz != null) {
                        Repository.removeClass(className);
                        Repository.addClass(clazz);
                    }
                } catch (Exception e) {
                    System.err.println("got exception while loading class: "
                            + e);
                    System.exit(1);
                }
            }

            if (clazz != null) {
                if (isSerializable(clazz)) {
                    if (!isIbisSerializable(clazz)) {
                        addClass(clazz);
                    } else {
                        if (verbose) {
                            System.out.println(clazz.getClassName()
                                    + " already implements "
                                    + "ibis.io.Serializable");
                        }
                    }
                } else {
                    if (verbose) {
                        System.out.println(clazz.getClassName()
                                + " is not serializable");
                    }
                    Repository.removeClass(clazz);
                }
            }
        }

        if (verbose) {
            System.out.println("Preparing classes");
        }

        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            JavaClass clazz = classes_to_rewrite.get(i);
            addReferencesToRewrite(clazz);
        }

        Repository.clearCache();

        /* Sort class to rewrite. Super classes first.  */
        do_sort_classes(classes_to_rewrite);

        for (int i = 0; i < classes_to_rewrite.size(); i++) {
            JavaClass clazz = classes_to_rewrite.get(i);
            new CodeGenerator(clazz).generateMethods();
        }

        if (verbose) {
            System.out.println("Rewriting classes");
        }

        /* Sort target_classes. Super classes first.  */
        do_sort_classes(target_classes);

        for (int i = 0; i < target_classes.size(); i++) {
            JavaClass clazz = target_classes.get(i);
            if (!clazz.isInterface()) {
                if (!silent) {
                    System.out.println("  Rewrite class : "
                            + clazz.getClassName());
                }
                new CodeGenerator(clazz).generateCode();
            }
        }

        if (verbose) {
            System.out.println("Saving classes");
        }

        for (int i = 0; i < classes_to_save.size(); i++) {
            JavaClass clazz = classes_to_save.get(i);
            String cl = clazz.getClassName();
            String classfile = "";

            try {
                if (local) {
                    int index = cl.lastIndexOf('.');
                    classfile = cl.substring(index + 1) + ".class";
                } else {
                    classfile = cl.replace('.', java.io.File.separatorChar)
                            + ".class";
                }
                if (verbose) {
                    System.out.println("  Saving class : " + classfile);
                }
                clazz.dump(classfile);
            } catch (IOException e) {
                System.err.println("got exception while writing " + classfile
                        + ": " + e);
                System.exit(1);
            }
        }
    }

    private static void usage() {
        System.out.println("Usage : java IOGenerator [-dir|-local] "
                + "[-package <package>] [-v] "
                + "<fully qualified classname list | classfiles>");
        System.exit(1);
    }

    public static void main(String[] args) {
        boolean verbose = false;
        boolean local = true;
        boolean file = false;
        boolean force_generated_calls = false;
        boolean silent = false;
        Vector<String> files = new Vector<String>();
        String pack = null;

        if (args.length == 0) {
            usage();
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-v")) {
                verbose = true;
            } else if (!args[i].startsWith("-")) {
                files.add(args[i]);
            } else if (args[i].equals("-dir")) {
                local = false;
            } else if (args[i].equals("-local")) {
                local = true;
            } else if (args[i].equals("-file")) {
                file = true;
            } else if (args[i].equals("-silent")) {
                silent = true;
            } else if (args[i].equals("-force")) {
                force_generated_calls = true;
            } else if (args[i].equals("-package")) {
                pack = args[i + 1];
                i++; // skip arg
            } else {
                usage();
            }
        }

        Vector<String> newArgs = new Vector<String>();
        for (int i = 0; i < files.size(); i++) {
            String name = files.elementAt(i);

            int colon = name.indexOf(':');
            if (colon != -1) {
                name = name.substring(colon + 1);
            }

            int index = name.lastIndexOf(".class");

            if (index != -1) {
                name = name.substring(0, index);
                name = name.replace(java.io.File.separatorChar, '.');
                if (pack == null) {
                    newArgs.add(name);
                } else {
                    newArgs.add(pack + "." + name);
                }
            } else {
                File f = new File(name);

                name = name.replace(java.io.File.separatorChar, '.');

                if (f.isDirectory()) {
                    processDirectory(f, newArgs, name);
                    continue;
                }
                if (pack == null) {
                    newArgs.add(name);
                } else {
                    newArgs.add(pack + "." + name);
                }
            }
        }

        new IOGenerator(verbose, local, file, force_generated_calls, 
                silent).scanClass(newArgs);
    }

    private static void processDirectory(File f, Vector<String> args,
            String name) {
        File[] list = f.listFiles();
        String prefix = "";

        if (!name.equals(".")) {
            prefix = name + ".";
        }
        for (int i = 0; i < list.length; i++) {
            String fname = list[i].getName();
            if (list[i].isDirectory()) {
                processDirectory(list[i], args, prefix + fname);
            } else {
                int index = fname.lastIndexOf(".class");
                if (index != -1) {
                    args.add(prefix + fname.substring(0, index));
                }
            }
        }
    }
}
