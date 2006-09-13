/* $Id$ */

package ibis.frontend.satin;

import ibis.frontend.generic.BT_Analyzer;
import ibis.frontend.ibis.IbiscComponent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ArrayInstruction;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BIPUSH;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.BranchHandle;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DCONST;
import org.apache.bcel.generic.DLOAD;
import org.apache.bcel.generic.DSTORE;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.DUP_X2;
import org.apache.bcel.generic.FCONST;
import org.apache.bcel.generic.FLOAD;
import org.apache.bcel.generic.FSTORE;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.IADD;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.IFNE;
import org.apache.bcel.generic.IFNONNULL;
import org.apache.bcel.generic.IFNULL;
import org.apache.bcel.generic.IF_ICMPNE;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.INSTANCEOF;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.ISTORE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LCONST;
import org.apache.bcel.generic.LLOAD;
import org.apache.bcel.generic.LSTORE;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.POP;
import org.apache.bcel.generic.POP2;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.SWAP;
import org.apache.bcel.generic.StackConsumer;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;

// A method that contains a spawn is rewritten like this:
// maxlocals = spawnCounter
// maxlocals+1 = outstandingSpawns
// maxLocals+2 = curr, the current invocation record we are syncing on
// maxLocals+3 = temp invocationrecord, cast to correct invocationRecord type

// @@@ optimizations still to do:
//     If there is only one spawn in a method, no need to test for ids.
//     (o.a. for inlets).
//     exception handler in non-clone is unreachable, delete.
//     initialisations of locals. delete if not needed.

public final class Satinc extends IbiscComponent {
    private JavaClass satinObjectClass;

    private JavaClass writeMethodsInterface;

    private ObjectType spawnCounterType;

    private ObjectType irType;

    private ObjectType satinType;

    private boolean local = true;

    private boolean invocationRecordCache = true;

    private boolean inletOpt = true;

    private boolean spawnCounterOpt = true;

    private JavaClass c; // the class we are rewriting 

    private ClassGen gen_c;

    private ConstantPoolGen cpg;

    private InstructionFactory ins_f;

    private ArrayList idTable = new ArrayList();

    private String className;

    private String classNameNoPackage;

    private String packageName;

    private boolean errors = false;

    private MethodTable mtab;

    // for ibisc
    private HashSet satinSet = new HashSet();

    /**
     * Nested classes have '$'-signs in their bytecode names, but
     * these are not legal in java code, so we filter them out,
     * by replacing them with a '.'.
     */
    private static class DollarFilter extends FilterOutputStream {
        DollarFilter(OutputStream out) {
            super(out);
        }

        public void write(int b) throws IOException {
            if (b == '$') {
                super.write('.');
            } else {
                super.write(b);
            }
        }
    }

    private static class StoreClass {
        private InstructionList store;

        private Method target;

        private String className;

        private String packageName;

        private String classNameNoPackage;

        StoreClass(InstructionList store, Method target, JavaClass cl) {
            this.store = store;
            this.target = target;
            className = cl.getClassName();
            packageName = cl.getPackageName();

            if (packageName != null && !packageName.equals("")) {
                classNameNoPackage = className.substring(className
                    .lastIndexOf('.') + 1, className.length());
            } else {
                classNameNoPackage = className;
            }
        }

        public InstructionList getStoreIns() {
            return store;
        }

        public Method getMethod() {
            return target;
        }

        public String getClassName() {
            return className;
        }

        public String getClassNameNoPackage() {
            return classNameNoPackage;
        }

        public String getPackageName() {
            return packageName;
        }

        public boolean equals(Object o) {
            if (o == null && store == null) {
                return true;
            }
            if (o == null) {
                return false;
            }
            if (store == null) {
                return false;
            }

            if (!(o instanceof StoreClass)) {
                return false;
            }

            StoreClass c = (StoreClass) o;

            if (!(target.equals(c.target))) {
                return false;
            }

            if (!store.equals(c.store)) {
                return false;
            }

            return true;
        }

        /**
         * If you redefine equals(), you should redefine hashCode() as well.
         */
        public int hashCode() {
            return target.hashCode();
        }
    }

    public Satinc() {
        satinObjectClass = Repository.lookupClass("ibis.satin.SatinObject");
        spawnCounterType = new ObjectType(
                "ibis.satin.impl.spawnSync.SpawnCounter");
        irType = new ObjectType("ibis.satin.impl.spawnSync.InvocationRecord");
        satinType = new ObjectType("ibis.satin.impl.Satin");
        writeMethodsInterface = Repository.lookupClass(
                "ibis.satin.WriteMethodsInterface");
    }

    public Satinc(boolean verbose, boolean local, boolean keep,
        boolean invocationRecordCache, boolean inletOpt,
        boolean spawnCounterOpt) {

        this();

        this.verbose = verbose;
        this.keep = keep;
        this.local = local;
        this.invocationRecordCache = invocationRecordCache;
        this.inletOpt = inletOpt;
        this.spawnCounterOpt = spawnCounterOpt;
    }

    public String getFileBase(String pkg, String name, String pre, String post) {
        name = pre + name + post;
        if (!local && pkg != null && !pkg.equals("")) {
            return pkg.replace('.', File.separatorChar) + File.separator
                + name;
        }
        if (fromIbisc) {
            String dir = getDirectory(className);
            if (dir != null) {
                return dir + File.separator + name;
            }
        }
        return name;
    }

    boolean isSatin() {
        return Repository.instanceOf(c, satinObjectClass);
    }

    boolean isRewritten() {
        Field[] fields = c.getFields();
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equals("$rewritten$")) {
                return true;
            }
        }
        return false;
    }

    static boolean isRetIns(Instruction i) {
        return i instanceof ReturnInstruction;
    }

    static String getInitVal(Type s) {
        if (s instanceof BasicType) {
            return "0";
        }
        return "null";
    }

    static boolean isRefType(Type s) {
        return (s instanceof ReferenceType);
    }

    CodeExceptionGen getExceptionHandler(MethodGen m, InstructionHandle self) {
        CodeExceptionGen exc[] = m.getExceptionHandlers();

        for (int j = 0; j < exc.length; j++) {
            InstructionHandle h = exc[j].getStartPC();
            InstructionHandle h2 = exc[j].getEndPC();
            do {
                if (h == self) {
                    return exc[j];
                }
                h = h.getNext();
            } while (h != h2);
            if (h == self) {
                return exc[j];
            }
        }

        return null;
    }

    Instruction getSatin(InstructionFactory insf) {
        return insf.createInvoke("ibis.satin.impl.Satin", "getSatin",
            satinType, Type.NO_ARGS, Constants.INVOKESTATIC);
    }

    void removeLocalTypeTables(MethodGen mg) {
        ConstantPoolGen cpg = mg.getConstantPool();
        Attribute[] a = mg.getCodeAttributes();
        for (int i = 0; i < a.length; i++) {
            String attribName = ((ConstantUtf8) cpg.getConstant(a[i]
                .getNameIndex())).getBytes();
            if (attribName.equals("LocalVariableTypeTable")) {
                mg.removeCodeAttribute(a[i]);
            }
        }
    }

    void generateMain(ClassGen clg, Method origMain) {

        InstructionList il = new InstructionList();

        MethodGen new_main = new MethodGen(Constants.ACC_STATIC
            | Constants.ACC_PUBLIC, Type.VOID, new Type[] { new ArrayType(
            Type.STRING, 1) }, new String[] { "argv" }, "main", clg
            .getClassName(), il, clg.getConstantPool());

        il.append(ins_f.createNew(satinType));
        il.append(new DUP());
        il.append(ins_f.createInvoke("ibis.satin.impl.Satin", "<init>",
            Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
        il.append(ins_f.createInvoke("ibis.satin.impl.Satin", "isMaster",
            Type.BOOLEAN, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        BranchHandle ifcmp = il.append(new IFEQ(null));

        InstructionHandle origMain_handle = il.append(new ALOAD(0));
        
        InstructionHandle try_start = il.append(ins_f.createInvoke(clg
            .getClassName(), origMain.getName(), Type.VOID,
            new Type[] { new ArrayType(Type.STRING, 1) },
            Constants.INVOKESTATIC));

        BranchHandle try_end = il.append(new GOTO(null));

        InstructionHandle e_handler = il.append(new ASTORE(1));
        il.append(ins_f.createFieldAccess("java.lang.System", "out",
            new ObjectType("java.io.PrintStream"), Constants.GETSTATIC));
        il.append(ins_f.createNew("java.lang.StringBuffer"));
        il.append(new DUP());
        il.append(new PUSH(gen_c.getConstantPool(), "Exception in main: "));
        il.append(ins_f.createInvoke("java.lang.StringBuffer", "<init>",
            Type.VOID, new Type[] { Type.STRING }, Constants.INVOKESPECIAL));
        il.append(new ALOAD(1));
        il.append(ins_f.createInvoke("java.lang.StringBuffer", "append",
            new ObjectType("java.lang.StringBuffer"),
            new Type[] { Type.OBJECT }, Constants.INVOKEVIRTUAL));
        il.append(ins_f.createInvoke("java.lang.StringBuffer", "toString",
            Type.STRING, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        il.append(ins_f.createInvoke("java.io.PrintStream", "println",
            Type.VOID, new Type[] { Type.STRING }, Constants.INVOKEVIRTUAL));
        il.append(new ALOAD(1));
        il.append(ins_f.createInvoke("java.lang.Throwable", "printStackTrace",
            Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

        BranchHandle gto2 = il.append(new GOTO(null));

        InstructionHandle ifeq_target = il.append(getSatin(ins_f));
        ifcmp.setTarget(ifeq_target);
        il.append(ins_f.createInvoke("ibis.satin.impl.Satin", "client",
            Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

        il.append(getSatin(ins_f));
        il.append(ins_f.createInvoke("ibis.satin.impl.Satin", "isMaster",
            Type.BOOLEAN, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        il.append(new IFNE(origMain_handle));

        InstructionHandle gto_target = il.append(getSatin(ins_f));
        try_end.setTarget(gto_target);
        gto2.setTarget(gto_target);

        il.append(ins_f.createInvoke("ibis.satin.impl.Satin", "exit",
            Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
        il.append(new RETURN());

        new_main.addExceptionHandler(try_start, try_end, e_handler,
            new ObjectType("java.lang.Throwable"));
        new_main.setMaxStack();
        new_main.setMaxLocals();

        new_main.addLocalVariable("argv", new ArrayType(Type.STRING, 1), 0,
            origMain_handle, null);

        removeLocalTypeTables(new_main);

        Method main = new_main.getMethod();
        gen_c.addMethod(main);
    }

    static void printMethod(Method m) {
        System.out.println("code for method " + m + ":");
        System.out.println(m.getCode());
        System.out.println("*******************************************");

    }

    static String do_mangle(StringBuffer s) {
        // OK, now sanitize parameters
        int i = 0;
        while (i < s.length()) {
            switch (s.charAt(i)) {
            case '$':
            case '.':
            case '/':
                s.setCharAt(i, '_');
                break;

            case '_':
                s.replace(i, i + 1, "_1");
                break;

            case ';':
                s.replace(i, i + 1, "_2");
                break;

            case '[':
                s.replace(i, i + 1, "_3");
                break;

            default:
                break;
            }
            i++;
        }
        return s.toString();
    }

    static String do_mangle(String name, String sig) {
        StringBuffer s = new StringBuffer(sig);
        name = do_mangle(new StringBuffer(name));

        int open = sig.indexOf("(");
        if (open == -1) {
            return name;
        }
        s.delete(0, open + 1);

        sig = s.toString();

        int close = sig.indexOf(")");
        if (close == -1) {
            return name;
        }
        s.delete(close, s.length());

        return name + "__" + do_mangle(s);
    }

    static String do_mangle(Method m) {
        return do_mangle(m.getName(), m.getSignature());
    }

    static String do_mangle(MethodGen m) {
        return do_mangle(m.getName(), m.getSignature());
    }

    String invocationRecordName(Method m, String clnam, String pnam) {
        if (pnam != null && !pnam.equals("")) {
            return pnam + ".Satin_" + clnam + "_" + do_mangle(m)
                + "_InvocationRecord";
        }
        return "Satin_" + clnam + "_" + do_mangle(m) + "_InvocationRecord";
    }

    String invocationRecordFileBase(Method m, String clnam, String pnam) {
        return getFileBase(pnam, clnam + "_" + do_mangle(m), "Satin_",
            "_InvocationRecord");
    }

    String invocationRecordClassName(Method m, String clnam) {
        return invocationRecordName(m, clnam, null);
    }

    String localRecordName(Method m) {
        if (packageName != null && !packageName.equals("")) {
            return packageName + ".Satin_" + classNameNoPackage + "_"
                + do_mangle(m) + "_LocalRecord";
        }
        return "Satin_" + classNameNoPackage + "_" + do_mangle(m)
            + "_LocalRecord";
    }

    String localRecordFileBase(Method m) {
        return getFileBase(packageName,
            classNameNoPackage + "_" + do_mangle(m), "Satin_", "_LocalRecord");
    }

    String localRecordClassName(Method m) {
        return "Satin_" + classNameNoPackage + "_" + do_mangle(m)
            + "_LocalRecord";
    }

    String localRecordName(MethodGen m) {
        if (packageName != null && !packageName.equals("")) {
            return packageName + ".Satin_" + classNameNoPackage + "_"
                + do_mangle(m) + "_LocalRecord";
        }
        return "Satin_" + classNameNoPackage + "_" + do_mangle(m)
            + "_LocalRecord";
    }

    String returnRecordName(Method m, String clnam, String pnam) {
        if (pnam != null && !pnam.equals("")) {
            return pnam + ".Satin_" + clnam + "_" + do_mangle(m)
                + "_ReturnRecord";
        }
        return "Satin_" + clnam + "_" + do_mangle(m) + "_ReturnRecord";
    }

    String returnRecordFileBase(Method m, String clnam, String pnam) {
        return getFileBase(pnam, clnam + "_" + do_mangle(m), "Satin_",
            "_ReturnRecord");
    }

    String returnRecordClassName(Method m, String clnam) {
        return returnRecordName(m, clnam, null);
    }

    void insertAllDeleteLocalRecords(MethodGen m) {
        int maxLocals = m.getMaxLocals();
        InstructionList il = m.getInstructionList();

        for (InstructionHandle i = il.getStart(); i != null; i = i.getNext()) {
            Instruction ins = i.getInstruction();
            if (ins instanceof ReturnInstruction) {
                i = insertDeleteLocalRecord(m, il, i, maxLocals);
            }
        }
    }

    InstructionHandle insertDeleteLocalRecord(MethodGen m, InstructionList il,
        InstructionHandle i, int maxLocals) {
        String local_record_name = localRecordName(m);

        // Note: maxLocals has been recomputed at this point.
        il.insert(i, new ALOAD(maxLocals - 5));
        il.insert(i, ins_f.createInvoke(local_record_name, "delete", Type.VOID,
            new Type[] { new ObjectType(local_record_name) },
            Constants.INVOKESTATIC));

        return i;
    }

    InstructionHandle insertDeleteSpawncounter(InstructionList il,
        InstructionHandle i, int maxLocals) {
        // In this case, jumps to the return must in fact jump to
        // the new instruction sequence! So, we change the instruction
        // at the handle.

        // First, save the return instruction.
        Instruction r = i.getInstruction();

        i.setInstruction(new ALOAD(maxLocals));
        i = il
            .append(i, ins_f.createInvoke(
                "ibis.satin.impl.spawnSync.SpawnCounter", "deleteSpawnCounter",
                Type.VOID, new Type[] { spawnCounterType },
                Constants.INVOKESTATIC));
        i = il.append(i, r);

        return i;
    }

    int allocateId(InstructionList storeIns, Method target, JavaClass cl) {
        StoreClass s = new StoreClass(storeIns, target, cl);

        int id = idTable.indexOf(s);
        if (id < 0) {
            idTable.add(s);
            id = idTable.size() - 1;
        }

        return id;
    }

    InstructionList getStoreIns(int id) {
        return ((StoreClass) idTable.get(id)).getStoreIns();
    }

    Method getStoreTarget(int id) {
        return ((StoreClass) idTable.get(id)).getMethod();
    }

    String getStoreClass(int id) {
        return ((StoreClass) idTable.get(id)).getClassNameNoPackage();
    }

    String getStorePackage(int id) {
        return ((StoreClass) idTable.get(id)).getPackageName();
    }

    void clearIdTable() {
        idTable.clear();
    }

    void rewriteAbort(MethodGen m, InstructionList il, InstructionHandle i,
        int maxLocals) {
        // in a clone, we have to abort two lists: the outstanding spawns of
        // the parent, and the outstanding spawns of the clone.
        Instruction fa = getSatin(ins_f);
        Instruction ab = ins_f.createInvoke("ibis.satin.impl.Satin", "abort",
            Type.VOID, new Type[] { irType, irType }, Constants.INVOKEVIRTUAL);
        if (mtab.isClone(m)) {
            int parentPos = 3;

            if (!m.isStatic()) { // we have an additional 'this' param
                parentPos++;
            }

            i.getPrev().getPrev().setInstruction(fa);
            // push outstanding spawns
            i.getPrev().setInstruction(new ALOAD(maxLocals - 3));

            // push parent invocationrecord
            i.setInstruction(new ALOAD(parentPos));
            i = i.getNext();
        } else if (mtab.containsInlet(m)) {
            i.getPrev().getPrev().setInstruction(fa);

            // push outstanding spawns
            i.getPrev().setInstruction(new ALOAD(maxLocals - 3));
            i.setInstruction(new ACONST_NULL());
            i = i.getNext();
        } else {
            i.getPrev().setInstruction(fa);
            // push outstanding spawns
            i.setInstruction(new ALOAD(maxLocals - 3));
            i = i.getNext();
            il.insert(i, new ACONST_NULL());
        }

        // and call Satin.abort
        il.insert(i, ab);

        // all jobs were killed, set outstandingSpawns to null
        il.insert(i, new ACONST_NULL()); // push null
        il.insert(i, new ASTORE(maxLocals - 3)); // write
    }

    void rewriteSync(MethodGen m, InstructionList il, InstructionHandle i,
        int maxLocals) {
        BranchHandle firstJumpPos = null;
        InstructionHandle pos = null;
        if (verbose) {
            System.out.println("rewriting sync, class = " + c);
        }

        /*
         this is allowed, sync is poll operation. --Rob
         if (idTable.size() == 0) {
         System.err.println("Error: sync without spawn");
         System.exit(1);
         }
         */
        Instruction sync_invocation = ins_f.createInvoke(
            "ibis.satin.impl.Satin", "sync", Type.VOID,
            new Type[] { spawnCounterType }, Constants.INVOKEVIRTUAL);
        Instruction satin_field_access = getSatin(ins_f);

        // Now find the push-sequence of the sync parameter (the object).
        InstructionHandle par = i;
        int stackincr = 0;
        do {
            par = par.getPrev();
            stackincr += par.getInstruction().produceStack(cpg)
                - par.getInstruction().consumeStack(cpg);
        } while (stackincr != 1);

        if (par != i.getPrev()) {
            deleteIns(il, par, i.getPrev());
        }

        i.getPrev().setInstruction(satin_field_access);
        i.setInstruction(new ALOAD(maxLocals));

        i = i.getNext(); // so that we can insert ...

        // and call Satin.sync 
        il.insert(i, sync_invocation);

        firstJumpPos = il.insert(i, new GOTO(null));
        pos = i;

        //*** Loop code. ***

        // Push curr = outstandingSpawns. 
        il.insert(pos, new ALOAD(maxLocals + 1));
        il.insert(pos, new DUP());
        il.insert(pos, new ASTORE(maxLocals + 2));

        il.insert(pos, ins_f.createFieldAccess(
            "ibis.satin.impl.spawnSync.InvocationRecord", "cacheNext", irType,
            Constants.GETFIELD));
        il.insert(pos, new ASTORE(maxLocals + 1));

        InstructionHandle[] jumpTargets = new InstructionHandle[idTable.size()];
        BranchHandle[] ifcmps = new BranchHandle[idTable.size()];
        BranchHandle[] gotos = new BranchHandle[idTable.size()];

        // loop over all ids handed out in this method 
        for (int k = 0; k < idTable.size(); k++) {
            String invClass = invocationRecordName(getStoreTarget(k),
                getStoreClass(k), getStorePackage(k));
            Type target_returntype = getStoreTarget(k).getReturnType();

            // Now generate code to test the id, and do the assignment to the
            // result variable. The previous ifnull jumps here.
            if (idTable.size() > 1) {
                il.insert(pos, new ALOAD(maxLocals + 2));

                if (k > 0) {
                    jumpTargets[k - 1] = pos.getPrev();
                }

                il.insert(pos, ins_f.createFieldAccess(
                    "ibis.satin.impl.spawnSync.InvocationRecord", "storeId",
                    Type.INT, Constants.GETFIELD));

                // push id value 
                il.insert(pos, new BIPUSH((byte) k));

                // do compare 
                ifcmps[k] = il.insert(pos, new IF_ICMPNE(null));
            }

            // assign result

            il.insert(pos, new ALOAD(maxLocals + 2));
            il.insert(pos, ins_f.createCheckCast(new ObjectType(invClass)));
            // store to variable that is supposed to contain result 
            InstructionList storeIns = getStoreIns(k);

            if (storeIns != null) {
                il.insert(pos, new DUP());
                if (isArrayStore(storeIns.getStart())) {
                    // array, maxLocals+3 = temp, cast to correct
                    // invocationRecord type
                    il.insert(pos, new DUP());
                    il.insert(pos, new ASTORE(maxLocals + 3));
                    il.insert(pos, ins_f
                        .createFieldAccess(invClass, "array", new ArrayType(
                            target_returntype, 1), Constants.GETFIELD));

                    il.insert(pos, new ALOAD(maxLocals + 3));
                    il.insert(pos, ins_f.createFieldAccess(invClass, "index",
                        Type.INT, Constants.GETFIELD));
                    il.insert(pos, new ALOAD(maxLocals + 3));
                } else if (isPutField(storeIns.getStart())) {
                    PUTFIELD pf = (PUTFIELD) storeIns.getStart()
                        .getInstruction();
                    // we have a putfield, maxLocals+3 = temp, cast to
                    // correct invocationRecord type
                    il.insert(pos, new DUP());
                    il.insert(pos, new ASTORE(maxLocals + 3));
                    il
                        .insert(pos, ins_f.createFieldAccess(invClass, "ref",
                            new ObjectType("java.lang.Object"),
                            Constants.GETFIELD));
                    il.insert(pos, ins_f.createCheckCast(pf.getClassType(cpg)));
                    il.insert(pos, new ALOAD(maxLocals + 3));
                }
                il.insert(pos, ins_f.createFieldAccess(invClass, "result",
                    target_returntype, Constants.GETFIELD));
                il.insert(pos, storeIns.copy());
            }

            il.insert(pos, ins_f
                .createInvoke(invClass, "delete", Type.VOID,
                    new Type[] { new ObjectType(
                        invocationRecordName(getStoreTarget(k),
                            getStoreClass(k), getStorePackage(k))) },
                    Constants.INVOKESTATIC));

            if (k != idTable.size() - 1) {
                gotos[k] = il.insert(pos, new GOTO(pos));
            }
        }

        // Outer loop test, the first goto jumps here. 
        // The previous if_icmp_ne also jumps here. 
        il.insert(pos, new ALOAD(maxLocals + 1));

        firstJumpPos.setTarget(pos.getPrev());
        if (jumpTargets.length > 0) {
            jumpTargets[idTable.size() - 1] = pos.getPrev();

            if (idTable.size() > 1) {
                for (int k = 0; k < idTable.size(); k++) {
                    ifcmps[k].setTarget(jumpTargets[k]);
                    if (k != idTable.size() - 1) {
                        gotos[k].setTarget(pos.getPrev());
                    }
                }
            }
        }

        // jump back to start op loop 
        il.insert(pos, new IFNONNULL(firstJumpPos.getNext()));

        if (verbose) {
            System.out.println("outputting post-sync aborted check for " + m);
        }
        insertAbortedCheck(m, il, pos);
    }

    void insertAbortedCheck(MethodGen m, InstructionList il,
        InstructionHandle pos) {
        // Generates:
        //   if (Config.FAULT_TOLERANCE || Config.ABORTS) {
        //       if (satin.getParent() != null && satin.getParent().aborted) {
        //           throw new ibis.satin.impl.aborts.AbortException();
        //       }
        //   }
        InstructionHandle abo = insertThrowAbort(m, il, pos);

        // Fault tolerance and aborts are always on --Rob
        //        il.insert(abo, ins_f.createFieldAccess("ibis.satin.impl.Config",
        //                "FAULT_TOLERANCE", Type.BOOLEAN, Constants.GETSTATIC));
        //        il.insert(abo, ins_f.createFieldAccess("ibis.satin.impl.Config",
        //                "ABORTS", Type.BOOLEAN, Constants.GETSTATIC));
        //        il.insert(abo, new IOR());
        //        il.insert(abo, new IFEQ(pos));
        il.insert(abo, getSatin(ins_f));
        il.insert(abo, ins_f.createInvoke("ibis.satin.impl.Satin", "getParent",
            irType, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

        // test for null (root job)
        il.insert(abo, new IFNULL(pos));

        il.insert(abo, getSatin(ins_f));
        il.insert(abo, ins_f.createInvoke("ibis.satin.impl.Satin", "getParent",
            irType, Type.NO_ARGS, Constants.INVOKEVIRTUAL));

        il.insert(abo, ins_f.createFieldAccess(
            "ibis.satin.impl.spawnSync.InvocationRecord", "aborted",
            Type.BOOLEAN, Constants.GETFIELD));
        il.insert(abo, new IFEQ(pos));

        /*
         ////@@@@@@@@@@2 this needs fixing :-(
         // Test for parent.eek, if non-null, throw it (exception in inlet).
         il.insert(abo, getSatin(ins_f));
         il.insert(abo, ins_f.createInvoke("ibis.satin.impl.Satin", "getParent",
         irType, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
         il.insert(abo, ins_f.createFieldAccess(
         "ibis.satin.impl.spawnSync.InvocationRecord", "eek",
         new ObjectType("java.lang.Throwable"), Constants.GETFIELD));
         il.insert(abo, new IFNULL(abo));
         il.insert(abo, getSatin(ins_f));
         il.insert(abo, ins_f.createInvoke("ibis.satin.impl.Satin", "getParent",
         irType, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
         il.insert(abo, ins_f.createFieldAccess(
         "ibis.satin.impl.spawnSync.InvocationRecord", "eek",
         new ObjectType("java.lang.Throwable"), Constants.GETFIELD));

         il.insert(abo, new ATHROW());
         */
    }

    InstructionHandle insertThrowAbort(MethodGen m, InstructionList il,
        InstructionHandle pos) {
        InstructionHandle retval;

        retval = il.insert(pos, ins_f
            .createNew("ibis.satin.impl.aborts.AbortException"));
        il.insert(pos, new DUP());
        il.insert(pos, ins_f.createInvoke(
            "ibis.satin.impl.aborts.AbortException", "<init>", Type.VOID,
            Type.NO_ARGS, Constants.INVOKESPECIAL));
        il.insert(pos, new ATHROW());

        return retval;
    }

    InstructionHandle getFirstParamPushPos(InstructionHandle i) {
        int paramsOnStack = i.getInstruction().consumeStack(cpg)
            - i.getInstruction().produceStack(cpg);

        if (verbose) {
            System.out.println("Expected params for " + i.getInstruction()
                + " is " + paramsOnStack);
        }

        InstructionHandle k = i.getPrev();
        int pushed = 0;

        do {
            pushed += k.getInstruction().produceStack(cpg)
                - k.getInstruction().consumeStack(cpg);
            k = k.getPrev();
        } while (pushed < paramsOnStack);

        return k;
    }

    static private void deleteIns(InstructionList il, InstructionHandle ih,
        InstructionHandle new_target) {
        // System.out.println("deleteIns: instructionList = " + il);
        // System.out.println("   handle = " + ih);
        try {
            il.delete(ih);
        } catch (TargetLostException e) {
            InstructionHandle[] targets = e.getTargets();
            for (int i = 0; i < targets.length; i++) {
                InstructionTargeter[] targeters = targets[i].getTargeters();

                for (int j = 0; j < targeters.length; j++) {
                    targeters[j].updateTarget(targets[i], new_target);
                }
            }
        }
    }

    static private void deleteIns(InstructionList il, InstructionHandle a,
        InstructionHandle b, InstructionHandle new_target) {
        try {
            il.delete(a, b);
        } catch (TargetLostException e) {
            InstructionHandle[] targets = e.getTargets();
            for (int i = 0; i < targets.length; i++) {
                InstructionTargeter[] targeters = targets[i].getTargeters();

                for (int j = 0; j < targeters.length; j++) {
                    targeters[j].updateTarget(targets[i], new_target);
                }
            }
        }
    }

    InstructionList getAndRemoveStoreIns(InstructionList il, InstructionHandle i) {

        int netto_stack_inc = 0;
        InstructionHandle storeStart = i;

        do {
            int inc = i.getInstruction().produceStack(cpg)
                - i.getInstruction().consumeStack(cpg);
            netto_stack_inc += inc;
            i = i.getNext();
        } while (netto_stack_inc >= 0);

        InstructionList result = new InstructionList();
        InstructionHandle ip = storeStart;

        do {
            result.append(ip.getInstruction());
            ip = ip.getNext();
        } while (ip != i);

        deleteIns(il, storeStart, ip.getPrev(), ip);

        return result;
    }

    boolean isArrayStore(InstructionHandle ins) {
        if (ins == null) {
            return false;
        }
        Instruction i = ins.getInstruction();
        return (i instanceof ArrayInstruction && i instanceof StackConsumer);
    }

    boolean isPutField(InstructionHandle ins) {
        if (ins == null) {
            return false;
        }
        Instruction i = ins.getInstruction();
        return i instanceof PUTFIELD;
    }

    void rewriteSpawn(MethodGen m, InstructionList il, Method target,
        InstructionHandle i, int maxLocals, int spawnId, JavaClass cl) {
        String clname = cl.getClassName();
        String pnam = cl.getPackageName();
        if (pnam != null && !pnam.equals("")) {
            clname = clname.substring(clname.lastIndexOf('.') + 1, clname
                .length());
        }

        if (verbose) {
            System.out.println("rewriting spawn, target = " + target.getName()
                + ", sig = " + target.getSignature());
        }

        Instruction store = null;
        InstructionList storeIns = null;

        // A spawned method invocation. Target and parameters are already on
        // the stack.
        // Push spawnCounter, outstandingSpawns, and the id for the result. 
        // Then call getNewInvocationRecord 
        // Finally we call Satin.spawn(outstandingSpawns) 
        // Also remove the original invocation and the store of the result. 
        // Keep the store instruction, and remove it from the instruction
        // vector. 
        // We must give this store instruction an method-unique id. 

        Type[] params = target.getArgumentTypes();
        Type returnType = target.getReturnType();

        if (!returnType.equals(Type.VOID)) {
            store = i.getNext().getInstruction();
            storeIns = getAndRemoveStoreIns(il, i.getNext());
            if (store instanceof ReturnInstruction) {
                System.err.println("\"return <spawnable method>\" is not "
                    + "allowed");
                System.exit(1);
            }
        }

        int storeId = allocateId(storeIns, target, cl);

        // push spawn counter 
        i.setInstruction(new ALOAD(maxLocals));

        // push outstandingSpawns 
        InstructionHandle ih = il.append(i, new ALOAD(maxLocals + 1));

        // push storeId 
        ih = il.append(ih, new BIPUSH((byte) storeId));

        // push spawnId 
        ih = il.append(ih, new BIPUSH((byte) spawnId));

        // push parentLocals 
        if (getExceptionHandler(m, i) != null) {
            ih = il.append(ih, new ALOAD(maxLocals - 1));
        } else {
            ih = il.append(ih, new ACONST_NULL());
        }

        // Call getNewInvocationRecord 
        String methodName;
        Type parameters[];
        int ix = 0;

        if (storeIns != null && isArrayStore(storeIns.getStart())) {
            methodName = "getNewArray";
            parameters = new Type[params.length + 8];
            parameters[ix++] = new ArrayType(returnType, 1);
            parameters[ix++] = Type.INT;
        } else if (storeIns != null && isPutField(storeIns.getStart())) {
            methodName = "getNewRef";
            parameters = new Type[params.length + 7];
            parameters[ix++] = new ObjectType("java.lang.Object");
        } else {
            methodName = "getNew";
            parameters = new Type[params.length + 6];
        }

        parameters[ix++] = new ObjectType(cl.getClassName());
        for (int j = 0; j < params.length; j++) {
            parameters[ix++] = params[j];
        }
        parameters[ix++] = spawnCounterType;
        parameters[ix++] = irType;
        parameters[ix++] = Type.INT;
        parameters[ix++] = Type.INT;
        parameters[ix++] = new ObjectType("ibis.satin.impl.aborts.LocalRecord");

        ih = il.append(ih, ins_f.createInvoke(invocationRecordName(target,
            clname, pnam), methodName, new ObjectType(invocationRecordName(
            target, clname, pnam)), parameters, Constants.INVOKESTATIC));

        // Store result in outstandingSpawns 
        ih = il.append(ih, new ASTORE(maxLocals + 1));

        // Now, we call Satin.spawn(outstandingSpawns) 

        // push s 
        ih = il.append(ih, getSatin(ins_f));

        // push outstandingSpawns 
        ih = il.append(ih, new ALOAD(maxLocals + 1));

        // and call Satin.spawn 
        ih = il.append(ih, ins_f.createInvoke("ibis.satin.impl.Satin", "spawn",
            Type.VOID, new Type[] { irType }, Constants.INVOKEVIRTUAL));
        if (verbose) {
            System.out.println("outputting post-spawn aborted check for " + m);
        }
        insertAbortedCheck(m, il, ih.getNext());
    }

    /* replace store by pop, load by const push */
    void removeUnusedLocals(Method mOrig, MethodGen m) {
        InstructionList il = m.getInstructionList();
        InstructionHandle[] ins = il.getInstructionHandles();
        for (int i = 0; i < ins.length; i++) {
            Instruction in = ins[i].getInstruction();

            if (in instanceof LocalVariableInstruction) {
                LocalVariableInstruction curr = (LocalVariableInstruction) in;
                if (mtab.getLocal(m, curr, ins[i].getPosition()) != null
                    && curr.getIndex() < m.getMaxLocals() - 5
                    && !mtab.isLocalUsedInInlet(mOrig, curr.getIndex())) {
                    if (curr instanceof IINC) {
                        ins[i].setInstruction(new NOP());
                    } else if (curr instanceof LSTORE || curr instanceof DSTORE) {
                        ins[i].setInstruction(new POP2());
                    } else if (curr instanceof StoreInstruction) {
                        ins[i].setInstruction(new POP());
                    } else if (curr instanceof ALOAD) {
                        ins[i].setInstruction(new ACONST_NULL());
                    } else if (curr instanceof FLOAD) {
                        ins[i].setInstruction(new FCONST((float) 0.0));
                    } else if (curr instanceof ILOAD) {
                        ins[i].setInstruction(new ICONST(0));
                    } else if (curr instanceof DLOAD) {
                        ins[i].setInstruction(new DCONST(0.0));
                    } else if (curr instanceof LLOAD) {
                        ins[i].setInstruction(new LCONST(0L));
                    } else {
                        System.out.println("unhandled ins in "
                            + "removeUnusedLocals: " + curr);
                        System.exit(1);
                    }
                }
            }
        }
    }

    void initSpawnTargets(InstructionList il) {
        for (int i = 0; i < idTable.size(); i++) {
            InstructionList storeIns = getStoreIns(i);

            if (storeIns == null) {
                continue;
            }

            if (isArrayStore(storeIns.getStart())) {
                continue;
            }

            Instruction store = storeIns.getStart().getInstruction();

            if (store instanceof LSTORE) {
                il.insert(new LCONST(0));
                il.append(il.getStart(), store);
            } else if (store instanceof ISTORE) {
                il.insert(new ICONST(0));
                il.append(il.getStart(), store);
            } else if (store instanceof FSTORE) {
                il.insert(new FCONST((float) 0.0));
                il.append(il.getStart(), store);
            } else if (store instanceof DSTORE) {
                il.insert(new DCONST(0.0));
                il.append(il.getStart(), store);
            } else if (store instanceof ASTORE) {
                il.insert(new ACONST_NULL());
                il.append(il.getStart(), store);
            } else if (store instanceof PUTFIELD) {
                // no need to init.
            } else if (store instanceof PUTSTATIC) {
                // no need to init.
            } else if (store instanceof ALOAD) {
                // no need to init.
            } else {
                System.err.println("WARNING: Unhandled store instruction in "
                    + "initSpawnTargets, opcode = " + store.getOpcode()
                    + " ins = " + store);
                // System.exit(1);
            }
        }
    }

    public Method findMethod(InvokeInstruction ins) {
        String name = ins.getMethodName(cpg);
        String sig = ins.getSignature(cpg);
        String cls = ins.getClassName(cpg);

        if (cls.startsWith("[")) {
            cls = "java.lang.Object";
        }

        JavaClass cl = Repository.lookupClass(cls);

        if (cl == null) {
            System.out.println("findMethod: could not find class " + cls);
            return null;
        }

        while (cl != null) {
            Method[] methods = cl.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals(name)
                    && methods[i].getSignature().equals(sig)) {
                    return methods[i];
                }
            }
            cls = cl.getSuperclassName();
            if (cls != null) {
                cl = Repository.lookupClass(cls);
            } else {
                cl = null;
            }
        }
        System.out.println("findMethod: could not find method " + name + sig);
        return null;
    }

    public JavaClass findMethodClass(InvokeInstruction ins) {
        String name = ins.getMethodName(cpg);
        String sig = ins.getSignature(cpg);
        String cls = ins.getClassName(cpg);

        if (cls.startsWith("[")) {
            cls = "java.lang.Object";
        }
        JavaClass cl = Repository.lookupClass(cls);

        if (cl == null) {
            System.out.println("findMethod: could not find class " + cls);
            return null;
        }

        while (cl != null) {
            Method[] methods = cl.getMethods();
            for (int i = 0; i < methods.length; i++) {
                    if (methods[i].getName().equals(name)
                    && methods[i].getSignature().equals(sig)) {
                    return cl;
                }
            }
            cls = cl.getSuperclassName();
            if (cls != null) {
                cl = Repository.lookupClass(cls);
            } else {
                cl = null;
            }
        }
        System.out.println("findMethod: could not find method " + name + sig);
        return null;
    }

    void rewriteMethod(Method mOrig, MethodGen m) {
        int spawnId = 0;

        if (verbose) {
            System.out.println("method " + mOrig
                + " contains a spawned call, rewriting");
        }

        clearIdTable();

        InstructionList il = m.getInstructionList();
        int maxLocals = m.getMaxLocals();
        InstructionHandle insertAllocPos = null;
        InstructionHandle[] ih = il.getInstructionHandles();

        if (verbose) {
            System.out.println("maxLocals = " + maxLocals);
        }
        // optimization:
        // find first spawn, then look if there is a jump before the spawn
        // that jumps over it...
        // this avoids alloccing and deleting spawn counters before a spawn
        // happens (e.g. with thresholds).
        // this can only be done when the method does not contain an inlet.
        // Otherwise, the clone jumps to the inlet without creating a
        // spawncounter. --Rob
        if (!mtab.containsInlet(m) && spawnCounterOpt) {
            CodeExceptionGen[] ceg = m.getExceptionHandlers();
            for (int i = 0; i < ih.length; i++) {
                if (ih[i].getInstruction() instanceof INVOKEVIRTUAL) {
                    Method target = findMethod((INVOKEVIRTUAL) (ih[i]
                        .getInstruction()));
                    JavaClass cl = findMethodClass((INVOKEVIRTUAL) (ih[i]
                        .getInstruction()));
                    if (mtab.isSpawnable(target, cl)) {
                        for (int j = 0; j < i; j++) {
                            for (int k = 0; k < ceg.length; k++) {
                                if (ih[j] == ceg[k].getStartPC()) {
                                    // Give up the optimization for now.
                                    insertAllocPos = il.getStart();
                                    break;
                                }
                            }
                            if (insertAllocPos != null) {
                                break;
                            }
                            if (ih[j] instanceof BranchHandle) {
                                InstructionHandle jumpTarget = ((BranchHandle) (ih[j]))
                                    .getTarget();
                                boolean found = false;
                                for (int k = 0; k < i; k++) {
                                    if (ih[k] == jumpTarget) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    insertAllocPos = ih[0];
                                }
                            }
                        }

                        if (insertAllocPos == null) { // no jumps
                            insertAllocPos = ih[i];
                        }
                        break;
                    }
                }
            }
            if (insertAllocPos == null) {
                insertAllocPos = il.getStart();
            }
        } else {
            insertAllocPos = il.getStart();
        }

        // Allocate a spawn counter at the start of the method,
        // local slot is maxLocals 
        il.insert(insertAllocPos, ins_f.createInvoke(
            "ibis.satin.impl.spawnSync.SpawnCounter", "newSpawnCounter",
            spawnCounterType, Type.NO_ARGS, Constants.INVOKESTATIC));

        // Allocate and initialize spawn_counter at slot maxLocals.
        il.insert(insertAllocPos, new ASTORE(maxLocals));
        InstructionHandle sp_h = il.insert(insertAllocPos, new ACONST_NULL());
        // Allocate and initialize outstandingSpawns at slot maxLocals+1
        il.insert(insertAllocPos, new ASTORE(maxLocals + 1));
        InstructionHandle os_h = il.insert(insertAllocPos, new ACONST_NULL());
        // Allocate and initialize curr at slot maxLocals+2 
        il.insert(insertAllocPos, new ASTORE(maxLocals + 2));
        InstructionHandle curr_h = il.insert(insertAllocPos, new ACONST_NULL());
        // Allocate and initialize localRecord at slot maxLocals+3 
        il.insert(insertAllocPos, new ASTORE(maxLocals + 3));
        InstructionHandle lr_h = il.insert(insertAllocPos, new NOP());

        m.addLocalVariable("spawn_counter", spawnCounterType, maxLocals, sp_h,
            null);
        m.addLocalVariable("outstanding_spawns", irType, maxLocals + 1, os_h,
            null);
        m.addLocalVariable("current", irType, maxLocals + 2, curr_h, null);
        m.addLocalVariable("local_record", new ObjectType(
            "ibis.satin.impl.aborts.LocalRecord"), maxLocals + 3, lr_h, null);

        for (InstructionHandle i = insertAllocPos; i != null; i = i.getNext()) {
            if (i.getInstruction() instanceof ReturnInstruction) {
                i = insertDeleteSpawncounter(il, i, maxLocals);
            } else if (i.getInstruction() instanceof INVOKEVIRTUAL) {
                INVOKEVIRTUAL ins = (INVOKEVIRTUAL) (i.getInstruction());
                Method target = findMethod(ins);
                JavaClass cl = findMethodClass(ins);
                boolean rewritten = false;

                // Rewrite the sync statement. 
                if (target.getName().equals("sync")
                    && target.getSignature().equals("()V")) {
                    if (cl != null && cl.equals(satinObjectClass)) {
                        rewriteSync(m, il, i, maxLocals);
                        rewritten = true;
                    }
                }
                if (!rewritten && mtab.isSpawnable(target, cl)) {
                    rewriteSpawn(m, il, target, i, maxLocals, spawnId, cl);
                    spawnId++;
                }

            }
        }

        if (!mtab.containsInlet(m)) {
            initSpawnTargets(il);
        }
    }

    InstructionHandle pushParams(InstructionList il, Method m) {
        Type[] params = mtab.typesOfParams(m);
        InstructionHandle pos = il.getStart();

        for (int i = 0, param = 0; i < params.length; i++, param++) {
            if (params[i].equals(Type.BOOLEAN) || params[i].equals(Type.BYTE)
                || params[i].equals(Type.SHORT) || params[i].equals(Type.CHAR)
                || params[i].equals(Type.INT)) {
                il.insert(pos, new ILOAD(param));
            } else if (params[i].equals(Type.FLOAT)) {
                il.insert(pos, new FLOAD(param));
            } else if (params[i].equals(Type.LONG)) {
                il.insert(pos, new LLOAD(param));
                param++;
            } else if (params[i].equals(Type.DOUBLE)) {
                il.insert(pos, new DLOAD(param));
                param++;
            } else {
                il.insert(pos, new ALOAD(param));
            }
        }

        return pos;
    }

    InstructionHandle rewriteStore(MethodGen m, InstructionList il,
        InstructionHandle i, int maxLocals, String localClassName) {
        LocalVariableInstruction curr = (LocalVariableInstruction) (i
            .getInstruction());
        Type type = mtab.getLocalType(m, curr, i.getPosition());
        if (type == null) {
            return i;
        }
        String name = mtab.getLocalName(m, curr, i.getPosition());
        String fieldName = MethodTable.generatedLocalName(type, name);

        i.setInstruction(new ALOAD(maxLocals));
        i = i.getNext();

        if (type.equals(Type.LONG) || type.equals(Type.DOUBLE)) {
            il.insert(i, new DUP_X2());
            il.insert(i, new POP());
        } else {
            il.insert(i, new SWAP());
        }

        i = il.insert(i, ins_f.createFieldAccess(localClassName, fieldName,
            type, Constants.PUTFIELD));
        return i;
    }

    InstructionHandle rewriteLoad(MethodGen m, InstructionList il,
        InstructionHandle i, int maxLocals, String localClassName) {
        LocalVariableInstruction curr = (LocalVariableInstruction) (i
            .getInstruction());
        Type type = mtab.getLocalType(m, curr, i.getPosition());
        if (type == null) {
            return i;
        }
        String name = mtab.getLocalName(m, curr, i.getPosition());
        String fieldName = MethodTable.generatedLocalName(type, name);

        i.setInstruction(new ALOAD(maxLocals));
        i = i.getNext();
        i = il.insert(i, ins_f.createFieldAccess(localClassName, fieldName,
            type, Constants.GETFIELD));

        return i;
    }

    void shiftLocals(InstructionList il, int shift) {
        InstructionHandle[] ih = il.getInstructionHandles();
        for (int i = 0; i < ih.length; i++) {
            Instruction ins = ih[i].getInstruction();
            if (ins instanceof LocalVariableInstruction) {
                LocalVariableInstruction l = (LocalVariableInstruction) ins;
                l.setIndex(l.getIndex() + shift);
            }
        }

    }

    InstructionHandle insertTypecheckCode(MethodGen m, InstructionList il,
        InstructionHandle pos, int spawnId, int exceptionPos) {
        ArrayList catches = mtab.getCatchTypes(m, spawnId);

        InstructionHandle[] jumpTargets = new InstructionHandle[catches.size() + 1];

        BranchHandle[] jumps = new BranchHandle[catches.size()];

        for (int i = 0; i < catches.size(); i++) {
            CodeExceptionGen e = (CodeExceptionGen) (catches.get(i));
            ObjectType type = e.getCatchType();
            InstructionHandle catchTarget = e.getHandlerPC();

            jumpTargets[i] = il.insert(pos, new ALOAD(exceptionPos));
            il.insert(pos, new INSTANCEOF(cpg.addClass(type)));
            il.insert(pos, new BIPUSH((byte) 1));
            jumps[i] = il.insert(pos, new IF_ICMPNE(null));
            il.insert(pos, new ALOAD(exceptionPos));
            il.insert(pos, ins_f.createCheckCast(type));
            il.insert(pos, new GOTO(catchTarget));
        }

        InstructionHandle t = il.insert(pos, new ALOAD(exceptionPos));
        il.insert(pos, new ATHROW());

        jumpTargets[catches.size()] = t;

        for (int i = 0; i < catches.size(); i++) {
            jumps[i].setTarget(jumpTargets[i + 1]);
        }

        return pos;
    }

    void generateExceptionHandlingClone(Method mOrig) {
        int localsShift = 4; // we have 4 params
        int spawnIdPos = 0;
        int localRecordPos = 1;
        int exceptionPos = 2;
        int parentPos = 3;

        if (!mOrig.isStatic()) { // we have an additional 'this' param
            localsShift++;
            spawnIdPos++;
            localRecordPos++;
            exceptionPos++;
            parentPos++;
        }

        MethodGen m = new MethodGen(mOrig, className, cpg);
        m.setArgumentTypes(new Type[] { Type.INT,
            new ObjectType(localRecordName(mOrig)),
            new ObjectType("java.lang.Throwable"), irType });

        m.setName("exceptionHandlingClone_" + mOrig.getName());
        m
            .setAccessFlags(Constants.ACC_PUBLIC
                | (m.getAccessFlags() & ~(Constants.ACC_PRIVATE | Constants.ACC_PROTECTED)));

        InstructionList il = m.getInstructionList();
        il.setPositions();

        InstructionHandle startLocalPos = il.findHandle(mtab
            .getStartLocalAlloc(mOrig));

        mtab.addCloneToInletTable(mOrig, m);

        if (inletOpt) {
            removeUnusedLocals(mOrig, m);
        }

        // Now generate code to restore locals, push the exception and jump
        // to the correct catch block.
        shiftLocals(il, localsShift); // add localsShift to all locals 
        // (we have localsShift parameters...)

        LocalVariableGen[] lv = m.getLocalVariables();
        for (int i = 0; i < lv.length; i++) {
            lv[i].setIndex(lv[i].getIndex() + localsShift);
        }

        m.addLocalVariable("spawn_id", Type.INT, spawnIdPos, startLocalPos,
            null);
        m.addLocalVariable("local_record", new ObjectType(
            localRecordName(mOrig)), localRecordPos, startLocalPos, null);
        m.addLocalVariable("excpt", new ObjectType("java.lang.Throwable"),
            exceptionPos, startLocalPos, null);
        m.addLocalVariable("parent", irType, parentPos, startLocalPos, null);

        // At pos 'startPos', the new of the local record starts.
        // Delete it, and replace with assignment from param

        // Load local record
        startLocalPos.setInstruction(new ALOAD(localRecordPos));
        startLocalPos = startLocalPos.getNext();

        // Save record
        startLocalPos.setInstruction(new ASTORE(m.getMaxLocals() - 5
            + localsShift));
        startLocalPos = startLocalPos.getNext();

        // Remove allocation of LocalRecord.
        // The nr of instructions to be removed depends on the number of
        // locals used.

        InstructionHandle x = startLocalPos;

        int insCount = mtab.typesOfParams(mOrig).length;
        for (int i = 1; i < insCount; i++) {
            x = x.getNext();
        }
        InstructionHandle pos = x.getNext();

        deleteIns(il, startLocalPos, x, pos);

        // okidoki, now jump 
        int nrSpawns = mtab.nrSpawns(mOrig);
        InstructionHandle[] spawnIdTable = new InstructionHandle[nrSpawns + 1];
        BranchHandle[] jumps = new BranchHandle[nrSpawns + 1];
        int nrInlets = 0;
        for (int i = 0; i < nrSpawns; i++) {
            // loop over all spawnIds in method (i)
            if (mtab.hasInlet(mOrig, i)) {
                spawnIdTable[nrInlets] = il.insert(pos, new ILOAD(spawnIdPos));
                il.insert(pos, new BIPUSH((byte) i));
                jumps[nrInlets] = il.insert(pos, new IF_ICMPNE(null));
                pos = insertTypecheckCode(m, il, pos, i, exceptionPos);
                nrInlets++;
            }
        }
        spawnIdTable[nrInlets] = pos;

        for (int i = 0; i < nrInlets; i++) {
            jumps[i].setTarget(spawnIdTable[i + 1]);
        }

        m.setMaxLocals();
        m.setMaxStack();
        removeLocalTypeTables(m);
        m.stripAttributes(true);

        Method newm = m.getMethod();
        gen_c.addMethod(newm);

        mtab.setMethod(m, newm);
    }

    static boolean isLocalStore(Instruction ins) {
        return (ins instanceof StoreInstruction);
    }

    static boolean isLocalLoad(Instruction ins) {
        return (ins instanceof LoadInstruction);
    }

    void rewriteInletMethod(Method mOrig, MethodGen m) {
        if (verbose) {
            System.out.println("method " + mOrig
                + " contains a spawned call and inlet, rewriting to local "
                + "record");
        }

        String localClassName = localRecordName(m);
        Type local_record_type = new ObjectType(localClassName);
        InstructionList il = m.getInstructionList();
        int maxLocals = m.getMaxLocals();

        if (verbose) {
            System.out.println("maxLocals = " + maxLocals);
        }

        // Allocate Local class.
        InstructionHandle pos = pushParams(il, mOrig);

        Type[] paramtypes = mtab.typesOfParams(mOrig);
        il.insert(pos, ins_f.createInvoke(localClassName, "getNew",
            local_record_type, paramtypes, Constants.INVOKESTATIC));

        il.insert(pos, new ASTORE(maxLocals));
        m.addLocalVariable("local_record", local_record_type, maxLocals, il
            .insert(pos, new NOP()), null);

        for (InstructionHandle i = pos; i != null; i = i.getNext()) {

            Instruction ins = i.getInstruction();

            if (isLocalStore(ins)) {
                LocalVariableInstruction curr = (LocalVariableInstruction) ins;

                if (!inletOpt
                    || mtab.isLocalUsedInInlet(mOrig, curr.getIndex())) {
                    if (verbose) {
                        System.out.println(m + ": rewriting local "
                            + curr.getIndex());
                    }
                    i = rewriteStore(m, il, i, maxLocals, localClassName);
                } else {
                    if (verbose) {
                        System.out.println(m + ": NOT rewriting local "
                            + curr.getIndex());
                    }
                }
            } else if (isLocalLoad(ins)) {
                LocalVariableInstruction curr = (LocalVariableInstruction) ins;

                if (!inletOpt
                    || mtab.isLocalUsedInInlet(mOrig, curr.getIndex())) {
                    if (verbose) {
                        System.out.println(m + ": rewriting local "
                            + curr.getIndex());
                    }
                    i = rewriteLoad(m, il, i, maxLocals, localClassName);
                } else {
                    if (verbose) {
                        System.out.println(m + ": NOT rewriting local "
                            + curr.getIndex());
                    }
                }
            } else if (ins instanceof IINC) {
                IINC curr = (IINC) ins;
                if (!inletOpt
                    || mtab.isLocalUsedInInlet(mOrig, curr.getIndex())) {
                    if (verbose) {
                        System.out.println(m + ": rewriting local "
                            + curr.getIndex());
                    }

                    int val = curr.getIncrement();
                    String fieldName = mtab.getLocalName(m, curr, i
                        .getPosition());
                    Type fieldType = mtab
                        .getLocalType(m, curr, i.getPosition());
                    if (fieldType == null) {
                        continue;
                    }

                    i.setInstruction(new ALOAD(maxLocals));
                    i = i.getNext();
                    il.insert(i, new DUP());

                    il.insert(i, ins_f.createFieldAccess(localClassName,
                        MethodTable.generatedLocalName(fieldType, fieldName),
                        fieldType, Constants.GETFIELD));
                    il.insert(i, new BIPUSH((byte) val));
                    il.insert(i, new IADD());
                    i = il.insert(i, ins_f.createFieldAccess(localClassName,
                        MethodTable.generatedLocalName(fieldType, fieldName),
                        fieldType, Constants.PUTFIELD));
                } else {
                    if (verbose) {
                        System.out.println(m + ": NOT rewriting local "
                            + curr.getIndex());
                    }
                }
            }
        }
        m.setMaxLocals();
    }

    // rewrite method invocations to spawned method invocations 
    void rewriteMethods() {
        Method[] methods = gen_c.getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];

            if (mtab.containsSpawnedCall(m)) {
                InstructionHandle l = null;

                MethodGen mg = mtab.getMethodGen(m);

                if (mtab.containsInlet(m)) {
                    rewriteInletMethod(m, mg);
                    l = mg.getInstructionList().getStart();
                }

                rewriteMethod(m, mg);

                mg.setMaxLocals();
                mg.setMaxStack();
                removeLocalTypeTables(mg);

                Method newm = mg.getMethod();
                mtab.replace(m, newm);

                if (mtab.containsInlet(newm)) {
                    mtab.setStartLocalAlloc(mg, l);
                    generateExceptionHandlingClone(newm);
                    insertAllDeleteLocalRecords(mg);
                    mg.setMaxLocals();
                    mg.setMaxStack();
                    Method newm2 = mg.getMethod();
                    mtab.replace(newm, newm2);
                    newm = newm2;
                }

                gen_c.removeMethod(m);
                gen_c.addMethod(newm);
            }
        }

        methods = gen_c.getMethods();

        // now rewrite the aborts (also in generated clones)
        for (int j = 0; j < methods.length; j++) {
            Method m = methods[j];
            boolean rewritten = false;

            MethodGen mg = mtab.getMethodGen(m);

            InstructionList il = mg.getInstructionList();

            if (il == null) {
                continue;
            }

            int maxLocals = mg.getMaxLocals();

            for (InstructionHandle i = il.getStart(); i != null; i = i
                .getNext()) {
                Instruction ins = i.getInstruction();
                if (ins instanceof INVOKEVIRTUAL) {
                    String targetname = ((InvokeInstruction) ins)
                        .getMethodName(cpg);
                    String sig = ((InvokeInstruction) ins).getSignature(cpg);

                    if (targetname.equals("abort") && sig.equals("()V")) {
                        JavaClass cl = findMethodClass((INVOKEVIRTUAL) ins);

                        if (cl != null && cl.equals(satinObjectClass)) {
                            // Rewrite the abort statement. 
                            rewriteAbort(mg, il, i, maxLocals);
                            rewritten = true;
                        }
                    }
                }
            }

            if (rewritten) {
                mg.setMaxLocals();
                mg.setMaxStack();
                removeLocalTypeTables(mg);

                Method newm = mg.getMethod();
                mtab.replace(m, newm);
                gen_c.setMethodAt(newm, j);
            }

        }
    }

    // Rewrite method invocations to spawned method invocations.
    // There is a chicken-and-egg problem here. The generated LocalRecord
    // depends on the rewritten bytecode, and vice versa. The solution is to
    // first generate a dummy method in the localRecord, and in the second
    // pass write the real method, and compile the whole class again.
    void generateInvocationRecords() throws IOException {
        Method[] methods = gen_c.getMethods();
        ArrayList toCompiler = new ArrayList();

        for (int i = 0; i < methods.length; i++) {
            if (mtab.containsInlet(methods[i])) {
                if (verbose) {
                    System.out.println(methods[i] + ": code contains an inlet");
                }

                writeLocalRecord(methods[i]);
                toCompiler.add(localRecordFileBase(methods[i]) + ".java");
            }

            if (mtab.isSpawnable(methods[i], c)) {
                writeInvocationRecord(methods[i], classNameNoPackage,
                    packageName);
                writeReturnRecord(methods[i], classNameNoPackage, packageName);

                toCompiler.add(invocationRecordFileBase(methods[i],
                    classNameNoPackage, packageName) + ".java");
                toCompiler.add(returnRecordFileBase(methods[i],
                    classNameNoPackage, packageName) + ".java");
            }
        }
        if (toCompiler.size() > 0) {
            compile(toCompiler, className);
            if (! fromIbisc) {
                for (int i = 0; i < methods.length; i++) {
                    if (mtab.containsInlet(methods[i])) {
                        Repository.lookupClass(localRecordName(methods[i]));
                    }

                    if (mtab.isSpawnable(methods[i], c)) {
                        Repository.lookupClass(invocationRecordName(methods[i],
                            classNameNoPackage, packageName));
                        Repository.lookupClass(returnRecordName(methods[i],
                            classNameNoPackage, packageName));
                    }
                }
            }
        }
    }

    void insertReturnPop(Method m, InstructionList il) {
        Type returnType = m.getReturnType();

        if (returnType.equals(Type.DOUBLE) || returnType.equals(Type.LONG)) {
            il.append(new POP2());
        } else if (returnType.equals(Type.VOID)) {
            // do nothing
        } else {
            il.append(new POP());
        }
    }

    void regenerateLocalRecord() {
        Method[] methods = gen_c.getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];

            if (!mtab.isClone(m) && mtab.containsInlet(m)) {
                String local_record_name = localRecordName(m);

                Method clone = mtab.getExceptionHandlingClone(m);
                JavaClass localRec = Repository.lookupClass(local_record_name);

                ClassGen recgen = new ClassGen(localRec);

                Method exceptionHandler = recgen.containsMethod(
                    "handleException", "(ILjava/lang/Throwable;"
                        + "Libis/satin/impl/spawnSync/InvocationRecord;)V");
                MethodGen handler_g = new MethodGen(exceptionHandler,
                    local_record_name, recgen.getConstantPool());

                InstructionList il = handler_g.getInstructionList();
                InstructionFactory insf = new InstructionFactory(recgen);

                if (verbose) {
                    System.out.println(m
                        + ":code contained an inlet, Rewriting localrecord,"
                        + " clone = " + clone);
                }

                InstructionHandle old_end = il.getEnd();

                if (!clone.isStatic()) {
                    // push this
                    String thisName = MethodTable.getParamName(m, 0);
                    Type thisType = mtab.getParamType(m, 0);
                    String thisFieldName = MethodTable.generatedLocalName(
                        thisType, thisName);

                    il.append(new ALOAD(0));
                    il.append(insf.createFieldAccess(local_record_name,
                        thisFieldName, thisType, Constants.GETFIELD));
                }

                // push spawnId, push localrecord, push exception, push parent
                // then invoke static/virtual
                il.append(new ILOAD(1));
                il.append(new ALOAD(0));
                il.append(new ALOAD(2));
                il.append(new ALOAD(3));

                il.append(insf.createInvoke(className, clone.getName(), clone
                    .getReturnType(), clone.getArgumentTypes(), clone
                    .isStatic() ? Constants.INVOKESTATIC
                    : Constants.INVOKEVIRTUAL));
                insertReturnPop(m, il);
                il.append(new RETURN());

                deleteIns(il, old_end, old_end.getNext()); // remove return

                handler_g.setMaxLocals();
                handler_g.setMaxStack();

                removeLocalTypeTables(handler_g);

                Method newHandler = handler_g.getMethod();
                recgen.replaceMethod(exceptionHandler, newHandler);

                JavaClass newclass = recgen.getJavaClass();

                Repository.removeClass(localRec);
                Repository.addClass(newclass);

                if (fromIbisc) {
                    setModified(wrapper.getInfo(newclass));
                } else {
                    String clnam = newclass.getClassName();
                    String dst;
                    if (local) {
                        dst = clnam.substring(clnam.lastIndexOf('.') + 1);
                    } else {
                        dst = clnam.replace('.', java.io.File.separatorChar);
                    }
                    dst = dst + ".class";

                    try {
                        newclass.dump(dst);
                    } catch (IOException e) {
                        System.out.println("error writing " + dst);
                        System.exit(1);
                    }
                }
            }
        }
    }

    void writeLocalRecord(Method m) throws IOException {
        String name = localRecordFileBase(m);
        if (verbose) {
            System.out.println("writing localrecord code to " + name + ".java");
        }

        FileOutputStream f = new FileOutputStream(name + ".java");
        BufferedOutputStream b = new BufferedOutputStream(f);
        DollarFilter b2 = new DollarFilter(b);
        PrintStream out = new PrintStream(b2);

        try {
            if (packageName != null && !packageName.equals("")) {
                out.println("package " + packageName + ";");
            }

            name = localRecordClassName(m);

            out.println("public final class " + name
                + " extends ibis.satin.impl.aborts.LocalRecord {");
            out.println("    private static " + name + " cache;");
            out.println("    private " + name + " cacheNext;");

            String[] allLvs = MethodTable.getAllLocalDecls(m);

            for (int i = 0; i < allLvs.length; i++) {
                out.println("    " + allLvs[i]);
            }
            out.println();

            // generate constructor, all parameters to the call must be copied.
            // locals are not initialized yet, so no need to copy them.
            Type[] params = mtab.typesOfParams(m);

            // ctor 
            out.print("    public " + name + "(");

            for (int i = 0; i < params.length; i++) {
                String paramName = MethodTable.getParamName(m, i);

                out.print(params[i] + " "
                    + MethodTable.generatedLocalName(params[i], paramName));
                if (i != params.length - 1) {
                    out.print(", ");
                }
            }
            out.println(") {");

            for (int i = 0; i < params.length; i++) {
                String paramName = MethodTable.getParamName(m, i);

                out.println("        this."
                    + MethodTable.generatedLocalName(params[i], paramName)
                    + " = "
                    + MethodTable.generatedLocalName(params[i], paramName)
                    + ";");
            }

            out.println("    }\n");

            // cache
            out.print("    public static " + name + " getNew(");

            for (int i = 0; i < params.length; i++) {
                String paramName = MethodTable.getParamName(m, i);

                out.print(params[i] + " "
                    + MethodTable.generatedLocalName(params[i], paramName));
                if (i != params.length - 1) {
                    out.print(", ");
                }
            }
            out.println(") {");

            out.println("        if (cache == null) {");
            out.print("            return new " + name + "(");
            for (int i = 0; i < params.length; i++) {
                String paramName = MethodTable.getParamName(m, i);

                out.print(MethodTable.generatedLocalName(params[i], paramName));
                if (i != params.length - 1) {
                    out.print(", ");
                }
            }
            out.println(");");
            out.println("        }");

            out.println("        " + name + " result = cache;");
            out.println("        cache = cache.cacheNext;");

            for (int i = 0; i < params.length; i++) {
                String paramName = MethodTable.getParamName(m, i);

                out.println("        result."
                    + MethodTable.generatedLocalName(params[i], paramName)
                    + " = "
                    + MethodTable.generatedLocalName(params[i], paramName)
                    + ";");
            }

            out.println("        result.cacheNext = null;");
            out.println("        return result;");
            out.println("    }\n");

            // delete
            out.println("    public static void delete(" + name + " curr) {");

            // wipe fields for gc
            Type[] ltypes = MethodTable.getAllLocalTypes(m);
            String[] lnames = MethodTable.getAllLocalNames(m);

            for (int i = 0; i < ltypes.length; i++) {
                if (ltypes[i] instanceof ReferenceType) {
                    out.println("        curr." + lnames[i] + " = null;");
                }
            }

            out.println("        curr.cacheNext = cache;");
            out.println("        cache = curr;");
            out.println("    }\n");

            // generate a method that runs the clone in case of exceptions 
            out
                .println("    public void handleException(int spawnId, "
                    + "Throwable t, ibis.satin.impl.spawnSync.InvocationRecord parent) "
                    + "throws Throwable {");
            out
                .println("        if (ibis.satin.impl.Config.inletLogger.isDebugEnabled()) {");
            out.println("            ibis.satin.impl.Config.inletLogger.debug("
                + "\"handleE: spawnId = \" + spawnId + "
                + "\", t = \" + t + \", parent = \" + parent + \", "
                + "this = \" + this);");
            out.println("        }");
            // This will later be replaced with call to exception handler
            out.println("    }");

            out.println("}");
        } finally {
            out.close();
        }
    }

    private boolean isSharedObject(Type t) {
        if (t instanceof ObjectType) {
            ObjectType typ = (ObjectType) t;
            if (Repository.instanceOf(typ.getClassName(),
                "ibis.satin.SharedObject")) {
                return true;
            }
        }
        return false;
    }

    void writeInvocationRecord(Method m, String clname, String pnam)
        throws IOException {
        String name = invocationRecordFileBase(m, clname, pnam);

        if (verbose) {
            System.out.println("writing invocationrecord code to " + name
                + ".java");
        }

        FileOutputStream f = new FileOutputStream(name + ".java");
        BufferedOutputStream b = new BufferedOutputStream(f);
        DollarFilter b2 = new DollarFilter(b);
        PrintStream out = new PrintStream(b2);
        // PrintStream out = System.err;
        try {
            Type[] params = mtab.typesOfParamsNoThis(m);
            String[] params_types_as_names = new String[params.length];

            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof ObjectType) {
                    String clnam = ((ObjectType) params[i]).getClassName();
                    if (!Repository.implementationOf(clnam,
                        "java.io.Serializable")) {
                        System.err.println(clname
                            + ": parameter of spawnable method " + m.getName()
                            + " with non-serializable type " + clnam);
                        System.err.println(clname
                            + ": all parameters of a spawnable method must"
                            + " be serializable.");
                        errors = true;
                    }
                }
                params_types_as_names[i] = params[i].toString();
            }

            Type returnType = m.getReturnType();
            if (returnType instanceof ObjectType) {
                String onam = ((ObjectType) returnType).getClassName();
                if (!Repository.implementationOf(onam, "java.io.Serializable")) {
                    System.err.println(clname + ": spawnable method "
                        + m.getName() + " has non-serializable return type: "
                        + onam + ".");
                    System.err.println(clname
                        + ": the return type of a spawnable method must be "
                        + "serializable.");
                    errors = true;
                }
            }

            if (pnam != null && !pnam.equals("")) {
                out.println("package " + pnam + ";");
            }

            name = invocationRecordClassName(m, clname);

            out.println("public final class " + name
                + " extends ibis.satin.impl.spawnSync.InvocationRecord {");

            // fields 
            out.println("    public " + clname + " self;");
            for (int i = 0; i < params_types_as_names.length; i++) {
                if (!isSharedObject(params[i])) {
                    out.println("    public " + params_types_as_names[i]
                        + " param" + i + ";");
                } else {
                    /*out.println("    transient " + params_types_as_names[i]
                     + " param" + i + ";");*/
                    out.println("    public String satin_so_reference_param"
                        + i + ";");
                }
            }

            // result 
            if (!returnType.equals(Type.VOID)) {
                out.println("    public transient " + returnType + " result;");
                out.println("    public transient int index;");
                out.println("    public transient " + returnType + "[] array;");

                // For putfield:
                out.println("    public transient Object ref;");
            }

            if (invocationRecordCache) {
                out.println("    static " + name + " invocationRecordCache;");
            }
            out.println();

            // ctor 
            out.print("    public " + name + "(");
            out.print(clname + " self, ");
            for (int i = 0; i < params_types_as_names.length; i++) {
                out.print(params_types_as_names[i] + " param" + i + ", ");
            }
            out
                .println("ibis.satin.impl.spawnSync.SpawnCounter s, "
                    + "ibis.satin.impl.spawnSync.InvocationRecord next, int storeId, "
                    + "int spawnId, ibis.satin.impl.aborts.LocalRecord parentLocals) {");
            out.println("        super(s, next, storeId, spawnId, "
                + "parentLocals);");
            out.println("        this.self = self;");

            for (int i = 0; i < params_types_as_names.length; i++) {
                if (!isSharedObject(params[i])) {
                    out
                        .println("        this.param" + i + " = param" + i
                            + ";");
                } else {
                    out.println("        this.satin_so_reference_param" + i
                        + " = param" + i + ".objectId;");
                }
            }

            out.println("    }\n");

            // getNew method 
            out.print("    public static " + name + " getNew(");
            out.print(clname + " self, ");
            for (int i = 0; i < params_types_as_names.length; i++) {
                out.print(params_types_as_names[i] + " param" + i + ", ");
            }
            out
                .println("ibis.satin.impl.spawnSync.SpawnCounter s, "
                    + "ibis.satin.impl.spawnSync.InvocationRecord next, int storeId, "
                    + "int spawnId, ibis.satin.impl.aborts.LocalRecord parentLocals) {");

            if (invocationRecordCache) {
                out.println("        if (invocationRecordCache == null) {");
            }
            out.print("            return new " + name + "(self, ");
            for (int i = 0; i < params_types_as_names.length; i++) {
                out.print(" param" + i + ", ");
            }
            out.println("s, next, storeId, spawnId, parentLocals);");
            if (invocationRecordCache) {
                out.println("        }\n");

                out
                    .println("        " + name
                        + " res = invocationRecordCache;");
                out.println("        invocationRecordCache = (" + name
                    + ") res.cacheNext;\n");

                out.println("        res.self = self;");
                
                for (int i = 0; i < params_types_as_names.length; i++) {
                    if (!isSharedObject(params[i])) {
                        out.println("        res.param" + i + " = param" + i
                                + ";");
                    } else {
                        out.println("        res.satin_so_reference_param" + i
                            + " = param" + i + ".objectId;");
                    }
                }

                out.println("        res.init(s, next, storeId, spawnId, parentLocals);");
                out.println("        return res;");
            }
            out.println("    }\n");

            // getNew method for arrays 
            if (!returnType.equals(Type.VOID)) {
                out.print("    public static " + name + " getNewArray(");
                out.print(returnType + "[] array, int index, ");
                out.print(clname + " self, ");
                for (int i = 0; i < params_types_as_names.length; i++) {
                    out.print(params_types_as_names[i] + " param" + i + ", ");
                }
                out.println("ibis.satin.impl.spawnSync.SpawnCounter s, "
                    + "ibis.satin.impl.spawnSync.InvocationRecord next, "
                    + "int storeId, int spawnId, "
                    + "ibis.satin.impl.aborts.LocalRecord parentLocals) {");
                out.print("            " + name + " res = getNew(self, ");
                for (int i = 0; i < params_types_as_names.length; i++) {
                    out.print(" param" + i + ", ");
                }
                out.println("s, next, storeId, spawnId, parentLocals);");

                out.println("        res.index = index;");
                out.println("        res.array = array;");
                out.println("        return res;");
                out.println("    }\n");
            }

            // getNew method for fields 
            if (!returnType.equals(Type.VOID)) {
                out.print("    public static " + name + " getNewRef(");
                out.print("Object ref, ");
                out.print(clname + " self, ");
                for (int i = 0; i < params_types_as_names.length; i++) {
                    out.print(params_types_as_names[i] + " param" + i + ", ");
                }
                out.println("ibis.satin.impl.spawnSync.SpawnCounter s, "
                    + "ibis.satin.impl.spawnSync.InvocationRecord next, "
                    + "int storeId, int spawnId, "
                    + "ibis.satin.impl.aborts.LocalRecord parentLocals) {");
                out.print("            " + name + " res = getNew(self, ");
                for (int i = 0; i < params_types_as_names.length; i++) {
                    out.print(" param" + i + ", ");
                }
                out.println("s, next, storeId, spawnId, parentLocals);");

                out.println("        res.ref = ref;");
                out.println("        return res;");
                out.println("    }\n");
            }

            // static delete method 
            out.println("    public static void delete(" + name + " w) {");
            if (invocationRecordCache) {
                if (!returnType.equals(Type.VOID)) {
                    out.println("        w.array = null;");
                    out.println("        w.ref = null;");
                }
                // Set everything to null, don't keep references live for gc. 
                out.println("        w.clear();");
                out.println("        w.self = null;");

                for (int i = 0; i < params.length; i++) {
                    if (isRefType(params[i])) {
                        if (!isSharedObject(params[i])) {
                            out.println("        w.param" + i + " = null;");
                        } else {
                            out.println("        w.satin_so_reference_param" + i
                                + " = null;");
                        }
                    }
                }
                out.println("        w.cacheNext = invocationRecordCache;");
                out.println("        invocationRecordCache = w;");
            }
            out.println("    }\n");

            // runLocal method 
            out.println("    public void runLocal() throws Throwable {");
            out.println("            try {");

            if (!returnType.equals(Type.VOID)) {
                out.print("            result = ");
            }
            out.print("            self." + m.getName() + "(");
            for (int i = 0; i < params.length; i++) {
                if (isSharedObject(params[i])) {
                    out
                        .print("("
                            + params_types_as_names[i]
                            + ")"
                            + "ibis.satin.impl.Satin.getSatin().so.getSOReference(satin_so_reference_param"
                            + i + ")");
                } else {
                    out.print("param" + i);
                }
                if (i != params.length - 1) {
                    out.print(", ");
                }
            }
            out.println(");");
            out
                .println("            } catch (Throwable e) {\n"
                    + "                if (ibis.satin.impl.Config.inletLogger.isDebugEnabled()) {\n"
                    + "                    ibis.satin.impl.Config.inletLogger.debug("
                    + "\"caught exception in runlocal: "
                    + "\" + e, e);\n"
                    + "                }\n"
                    + "                if (! (e instanceof ibis.satin.impl.aborts.AbortException)) {\n"
                    + "                    eek = e;\n"
                    + "                } else if (ibis.satin.impl.Config.abortLogger.isDebugEnabled()) {\n"
                    + "                    ibis.satin.impl.Config.abortLogger.debug("
                    + "\"got AbortException \" + e, e);\n"
                    + "                }\n"
                    + "            }\n"
                    + "            if (eek != null && !inletExecuted) {\n"
                    + "                if (ibis.satin.impl.Config.inletLogger.isDebugEnabled()) {\n"
                    + "                    ibis.satin.impl.Config.inletLogger.debug("
                    + "\"runlocal: calling inlet for: "
                    + "\" + this);\n"
                    + "                }\n"
                    + "                if (parentLocals != null)\n"
                    + "                    parentLocals.handleException(spawnId, eek, this);\n"
                    + "                if (ibis.satin.impl.Config.inletLogger.isDebugEnabled()) {\n"
                    + "                    ibis.satin.impl.Config.inletLogger.debug(\"runlocal:"
                    + " calling inlet for: \" + this + \" DONE\");\n"
                    + "                }\n"
                    + "                if (parentLocals == null)\n"
                    + "                    throw eek;\n" + "            }\n"
                    + "\n");
            out.println("    }\n");

            // runRemote method 
            out.println("    public ibis.satin.impl.spawnSync.ReturnRecord "
                + "runRemote() {");
            out.println("        try {");
            out.print("            ");
            if (!returnType.equals(Type.VOID)) {
                out.print("result = ");
            }
            out.print("self." + m.getName() + "(");

            for (int i = 0; i < params.length; i++) {
                if (isSharedObject(params[i])) {
                    out
                        .print("("
                            + params_types_as_names[i]
                            + ")"
                            + "ibis.satin.impl.Satin.getSatin().so.getSOReference(satin_so_reference_param"
                            + i + ")");
                } else {
                    out.print("param" + i);
                }
                if (i != params.length - 1) {
                    out.print(", ");
                }
            }
            out.println(");");
            out.println("        } catch (Throwable e) {\n"
                      + "            if (ibis.satin.impl.Config.inletLogger.isDebugEnabled()) {\n"
                      + "                ibis.satin.impl.Config.inletLogger.debug("
                      +                        "\"caught exception in runremote: "
                      +                        "\" + e, e);\n"
                      + "            }\n");
            out
                .println("                if (! (e instanceof ibis.satin.impl.aborts.AbortException)) {");
            out.println("                    eek = e;");
            out
                .println("                } else if (ibis.satin.impl.Config.abortLogger.isDebugEnabled()) {");
            out
                .println("                    ibis.satin.impl.Config.abortLogger.debug("
                    + "\"got AbortException \" + e, e);");
            out.println("                }");
            out.println("        }");
            out
                .print("        return new "
                    + returnRecordName(m, clname, pnam));
            if (!returnType.equals(Type.VOID)) {
                out.println("(result, eek, getStamp());");
            } else {
                out.println("(eek, getStamp());");
            }
            out.println("    }");

            //assignTo method
            out.print("    public void assignTo(Throwable eek");
            if (!returnType.equals(Type.VOID)) {
                out.println(", " + returnType + " result");
            }
            out.println(") {");
            out.println("        this.eek = eek;");
            if (!returnType.equals(Type.VOID)) {
                out.println("        this.result = result;");
            }
            out.println("    }");

            //getReturnRecord method
            if (returnType.equals(Type.VOID)) {
                out
                    .println("    public ibis.satin.impl.spawnSync.ReturnRecord "
                        + "getReturnRecord() {");
                out
                    .println("        return new "
                        + returnRecordName(m, clname, pnam)
                        + "(null, getStamp());");
                out.println("    }\n");
            } else {
                out
                    .println("    public ibis.satin.impl.spawnSync.ReturnRecord "
                        + "getReturnRecord() {");
                out.println("        return new "
                    + returnRecordName(m, clname, pnam)
                    + "(result, null, getStamp());");
                out.println("    }\n");
            }

            //setSOReferences method
            out.println("    public void setSOReferences()");
            out
                .println("        throws ibis.satin.impl.sharedObjects.SOReferenceSourceCrashedException {");
            for (int i = 0; i < params.length; i++) {
                if (isSharedObject(params[i])) {
                    /*		    out.println("        param" + i + " = "
                     + "(" + params_types_as_names[i] + ")"
                     + "satin.getSOReference(satin_so_reference_param"
                     + i + ", owner);");*/
                    out
                        .println("        ibis.satin.impl.Satin.getSatin().so.setSOReference(satin_so_reference_param"
                            + i + ", owner);");
                }
            }
            out.println("    }\n");

            //getSOReferences method
            out.println("    public java.util.Vector getSOReferences() {");
            out
                .println("        java.util.Vector refs = new java.util.Vector();");
            for (int i = 0; i < params.length; i++) {
                if (isSharedObject(params[i])) {
                    out.println("        refs.add(satin_so_reference_param" + i
                        + ");");
                }
            }
            out.println("        return refs;");
            out.println("    }\n");

            //guard method
            //generate a guard method only if the programmer defined a
            // guard_method_name method
            String signature = m.getSignature();
            String guardSignature = signature.substring(0, signature
                .indexOf(")") + 1)
                + "Z";
            String guardName = "guard_" + m.getName();

            if (gen_c.containsMethod(guardName, guardSignature) != null) {
                if (verbose) {
                    System.out.println("Generating a guard function for "
                        + m.getName());
                }
                out.println("    public boolean guard() {");
                out.print("        return self.guard_" + m.getName() + "(");
                for (int i = 0; i < params.length; i++) {
                    if (isSharedObject(params[i])) {
                        out
                            .print("("
                                + params_types_as_names[i]
                                + ")"
                                + "ibis.satin.impl.Satin.getSatin().so.getSOReference(satin_so_reference_param"
                                + i + ")");
                    } else {
                        out.print("param" + i);
                    }
                    if (i != params.length - 1) {
                        out.print(", ");
                    }
                }
                out.println(");");
                out.println("    }\n");
            } else if (verbose) {
                //check if there are some misformed guards
                Method[] meth = gen_c.getMethods();
                for (int i = 0; i < meth.length; i++) {
                    if (meth[i].getName().startsWith("guard_")) {
                        System.out.println("Possibly invalid guard function");
                    }
                }
            }

            // method to clear object params to allow th e GC to clean them up --Rob
            out.println("    public void clearParams() {");
            for (int i = 0; i < params.length; i++) {
                if (!isSharedObject(params[i])) {
                    if (params[i] instanceof ObjectType
                        || params[i] instanceof ArrayType) {
                        out.println("        param" + i + " = null;");
                    }
                }
            }
            out.println("    }");

            out.println("}");
        } finally {
            out.close();
        }
    }

    void writeReturnRecord(Method m, String clname, String pnam)
        throws IOException {
        String name = returnRecordFileBase(m, clname, pnam);
        if (verbose) {
            System.out
                .println("writing returnrecord code to " + name + ".java");
        }

        FileOutputStream f = new FileOutputStream(name + ".java");
        BufferedOutputStream b = new BufferedOutputStream(f);
        DollarFilter b2 = new DollarFilter(b);
        PrintStream out = new PrintStream(b2);

        name = returnRecordClassName(m, clname);

        try {
            Type returnType = m.getReturnType();

            if (pnam != null && !pnam.equals("")) {
                out.println("package " + pnam + ";");
            }
            out.println("public final class " + name
                + " extends ibis.satin.impl.spawnSync.ReturnRecord {");
            if (!returnType.equals(Type.VOID)) {
                out.println("    " + returnType + " result;\n");
            }

            // ctor 
            out.print("    public " + name + "(");
            if (!returnType.equals(Type.VOID)) {
                out
                    .println(returnType
                        + " result, Throwable eek, ibis.satin.impl.spawnSync.Stamp stamp) {");
            } else {
                out
                    .println(" Throwable eek, ibis.satin.impl.spawnSync.Stamp stamp) {");
            }

            out.println("        super(eek);");
            if (!returnType.equals(Type.VOID)) {
                out.println("        this.result = result;");
            }
            out.println("        this.stamp = stamp;");
            out.println("    }\n");

            out
                .println("    public void assignTo(ibis.satin.impl.spawnSync.InvocationRecord rin) {");
            out.println("        " + invocationRecordName(m, clname, pnam)
                + " r = (" + invocationRecordName(m, clname, pnam) + ") rin;");
            out.print("	r.assignTo(eek");
            if (!returnType.equals(Type.VOID)) {
                out.print(", result");
            }
            out.println(");");
            out.println("    }");
            out.println("}");
        } finally {
            out.close();
        }
    }


    /* Start of SO rewriter. */

    String SOInvocationRecordName(Method m, String clnam, String pnam) {
        if (pnam != null && !pnam.equals("")) {
            return pnam + ".Satin_" + clnam + "_" + do_mangle(m)
                + "_SOInvocationRecord";
        }
        return "Satin_" + clnam + "_" + do_mangle(m) + "_SOInvocationRecord";
    }

    String SOInvocationRecordFileBase(Method m, String clnam, String pnam) {
        return getFileBase(pnam, clnam + "_" + do_mangle(m), "Satin_",
            "_SOInvocationRecord");
    }

    String SOInvocationRecordClassName(Method m, String clnam) {
        return SOInvocationRecordName(m, clnam, null);
    }

    private void writeSOInvocationRecord(Method m, String clname, String pnam)
        throws java.io.IOException {
        String name = SOInvocationRecordFileBase(m, clname, pnam);
        int i;

        FileOutputStream f = new FileOutputStream(name + ".java");
        BufferedOutputStream b = new BufferedOutputStream(f);
        DollarFilter b2 = new DollarFilter(b);
        PrintStream out = new PrintStream(b2);

        if (verbose) {
            System.err.println("Generating inv rec for method: " + m.getName()
                + " with signature: " + m.getSignature());
        }

        /* Copied from MethodTable.java; I have no clue why
         m.getArgumentTypes is not used*/
        Type[] params = Type.getArgumentTypes(m.getSignature());
        String[] params_types_as_names = new String[params.length];

        for (i = 0; i < params.length; i++) {
            if (params[i] instanceof ObjectType) {
                String clnam = ((ObjectType) params[i]).getClassName();
                if (!Repository.implementationOf(clnam, "java.io.Serializable")) {
                    System.err.println(clname + ": write method"
                        + " with non-serializable parameter type " + clnam);
                    System.err.println(clname
                        + ": all parameters of a write method"
                        + " must be serializable.");
                    System.exit(1);
                }
            }
            params_types_as_names[i] = params[i].toString();
        }

        if (pnam != null && !pnam.equals("")) {
            out.println("package " + pnam + ";");
        }

        name = SOInvocationRecordClassName(m, clname);

        out.println("public final class " + name
            + " extends ibis.satin.impl.sharedObjects.SOInvocationRecord {");

        //fields
        for (i = 0; i < params_types_as_names.length; i++) {
            out.println("\t" + params_types_as_names[i] + " param" + i + ";");
        }
        out.println();

        //constructor
        out.print("\tpublic " + name + "(String objectId, ");
        for (i = 0; i < params_types_as_names.length - 1; i++) {
            out.print(params_types_as_names[i] + " param" + i + ", ");
        }
        out.println(params_types_as_names[i] + " param" + i + ") {");
        out.println("\t\tsuper(objectId);");
        for (i = 0; i < params_types_as_names.length; i++) {
            //		if (params[i] instanceof BasicType) {
            out.println("\t\tthis.param" + i + " = param" + i + ";");
            /*		} else {
             //copy the parameter
             out.println("\t\tthis.param" + i + " = (" + params_types_as_names[i]
             + ") cloneObject(param" + i + ");");
             }*/
        }
        out.println("\t}\n");

        //invoke method
        out.println("\tpublic void invoke(ibis.satin.SharedObject object) {");
        out.println("\t\t" + clname + " obj = (" + clname + ") object;");
        out.println("\t\ttry{");
        out.print("\t\t\tobj.so_local_" + m.getName() + "(");
        for (i = 0; i < params_types_as_names.length - 1; i++) {
            out.print("param" + i + ", ");
        }
        out.println("param" + i + ");");
        out.println("\t\t} catch (Throwable t) {");
        out
            .println("\t\t\t/* exceptions will be only thrown at the originating node*/");
        out.println("\t\t}");
        out.println("\t}");
        out.println();
        out.println("}");
        out.close();
    }

    void rewriteSOMethod(Method m, String clnam, String pnam) {
        MethodGen origMethodGen;
        MethodGen newMethodGen;
        InstructionList newMethodInsList;
        Type[] arguments;
        Type[] objectId_and_arguments;
        Type returnType;
        int oldAccessFlags;
        int monitorVarAddr;
        String classname = clnam;

        if (pnam != null && !pnam.equals("")) {
            classname = pnam + "." + clnam;
        }

        if (verbose) {
            System.err.println("Rewriting method: " + m.getName()
                + " with signature: " + m.getSignature());
        }

        //prefix the original method name with so_local
        origMethodGen = new MethodGen(m, classname, cpg);
        origMethodGen.setName("so_local_" + m.getName());
        origMethodGen.setMaxStack();
        origMethodGen.setMaxLocals();
        oldAccessFlags = origMethodGen.getAccessFlags();
        origMethodGen.setAccessFlags(0x0001); //public
        gen_c.removeMethod(m);
        removeLocalTypeTables(origMethodGen);
        gen_c.addMethod(origMethodGen.getMethod());

        //create the new method with the following body:
        //Satin.getSatin().
        //broadcastSOInvocation(new Satin_methodname_SOInvocationRecord(params))
        //synchronized(Satin.getSatin()) {
        //return so_local_methodname(params);
        //}

        newMethodInsList = new InstructionList();
        //broadcast 
        newMethodInsList.append(ins_f.createInvoke("ibis.satin.impl.Satin",
            "getSatin", new ObjectType("ibis.satin.impl.Satin"), new Type[] {},
            Constants.INVOKESTATIC));
        newMethodInsList.append(ins_f.createNew(SOInvocationRecordName(m,
            clnam, pnam)));
        newMethodInsList.append(InstructionFactory.createDup(1));
        newMethodInsList.append(new ALOAD(0));
        newMethodInsList.append(ins_f.createGetField(classname, "objectId",
            Type.STRING));
        arguments = m.getArgumentTypes();
        int k1 = 0;
        for (int k = 0; k < arguments.length; k++) {
            newMethodInsList.append(InstructionFactory.createLoad(arguments[k],
                k1 + 1));
            k1 += arguments[k].getSize();
        }

        objectId_and_arguments = new Type[arguments.length + 1];
        objectId_and_arguments[0] = Type.STRING;
        for (int k = 0; k < arguments.length; k++) {
            objectId_and_arguments[k + 1] = arguments[k];
        }

        newMethodInsList.append(ins_f.createInvoke(SOInvocationRecordName(m,
            clnam, pnam), "<init>", Type.VOID, objectId_and_arguments,
            Constants.INVOKESPECIAL));
        newMethodInsList.append(ins_f.createInvoke("ibis.satin.impl.Satin",
            "broadcastSOInvocation", Type.VOID, new Type[] { new ObjectType(
                "ibis.satin.impl.sharedObjects.SOInvocationRecord") },
            Constants.INVOKEVIRTUAL));
        //enter the monitor	
        monitorVarAddr = 1;
        for (int k = 0; k < arguments.length; k++) {
            monitorVarAddr += arguments[k].getSize();
        }
        newMethodInsList.append(ins_f.createInvoke("ibis.satin.impl.Satin",
            "getSatin", new ObjectType("ibis.satin.impl.Satin"), new Type[] {},
            Constants.INVOKESTATIC));
        newMethodInsList.append(InstructionFactory.createDup(1));
        newMethodInsList.append(new ASTORE(monitorVarAddr));
        newMethodInsList.append(new MONITORENTER());
        //call the object method
        InstructionHandle from1 = newMethodInsList.append(new ALOAD(0));
        k1 = 0;
        for (int k = 0; k < arguments.length; k++) {
            newMethodInsList.append(InstructionFactory.createLoad(arguments[k],
                k1 + 1));
            k1 += arguments[k].getSize();
        }
        returnType = m.getReturnType();
        newMethodInsList.append(ins_f.createInvoke(classname, "so_local_"
            + m.getName(), returnType, arguments, Constants.INVOKEVIRTUAL));
        //exit the monitor
        newMethodInsList.append(new ALOAD(monitorVarAddr));
        newMethodInsList.append(new MONITOREXIT());
        //return statement
        InstructionHandle to1 = newMethodInsList.append(InstructionFactory
            .createReturn(returnType));
        //exception handlers
        InstructionHandle from2 = newMethodInsList.append(new ASTORE(
            monitorVarAddr + 1));
        newMethodInsList.append(new ALOAD(monitorVarAddr));
        newMethodInsList.append(new MONITOREXIT());
        InstructionHandle to2 = newMethodInsList.append(new ALOAD(
            monitorVarAddr + 1));
        newMethodInsList.append(new ATHROW());

        newMethodGen = new MethodGen(oldAccessFlags, returnType, arguments,
            origMethodGen.getArgumentNames(), m.getName(), classname,
            newMethodInsList, cpg);

        newMethodGen.addExceptionHandler(from1, to1, from2, null);
        newMethodGen.addExceptionHandler(from2, to2, from2, null);
        newMethodGen.setMaxStack();
        newMethodGen.setMaxLocals();
        removeLocalTypeTables(newMethodGen);

        gen_c.addMethod(newMethodGen.getMethod());

    }

    void doSORewrite() throws IOException {
        BT_Analyzer a = new BT_Analyzer(c, writeMethodsInterface, verbose);
        a.start(false);

        //		Vector methods = a.subjectSpecialMethods;
        Method[] methods = c.getMethods();

        if (methods == null) {
            return;
        }

        //add the $SOrewritten$ field
        gen_c.addField(new FieldGen(Constants.ACC_STATIC, Type.BOOLEAN,
            "$SOrewritten$", cpg).getField());

        //add things to the constant pool
        /*		int getSatin = cpg.addMethodref("ibis.satin.impl.Satin",
         "getSatin",
         "()Libis/satin/impl/Satin");
         int broadcastSOInvocation = 
         cpg.addMethodref("ibis.satin.impl.Satin",
         "broadcastSOInvocation",
         "(Libis/satin/impl/SOInvocationRecord)V");*/

        //rewrite methods
        for (int j = 0; j < methods.length; j++) {
            Method method = methods[j];
            //		    System.err.println(method.toString());
            if (a.isSpecial(method)) {
                //change the name of the method to so_local_methodName
                //create a new methodName with the following body
                //Satin.getSatin().broadcastSOInvocation(new SOInvRecord())
                //return so_local_methodName
                rewriteSOMethod(method, classNameNoPackage, packageName);

            }
        }

        JavaClass newSubjectClass = gen_c.getJavaClass();
        Repository.removeClass(c);
        Repository.addClass(newSubjectClass);

        if (! fromIbisc) {
            //dump the class
            String dst;
            if (local) {
                dst = className.substring(className.lastIndexOf('.') + 1);
            } else {
                dst = className.replace('.', java.io.File.separatorChar);
            }
            dst = dst + ".class";
            newSubjectClass.dump(dst);
        } else {
            setModified(wrapper.getInfo(newSubjectClass));
            writeAll();
        }

        ArrayList toCompiler = new ArrayList();
        //generate so invocation records
        for (int j = 0; j < methods.length; j++) {
            Method method = methods[j];
            if (a.isSpecial(method)) {
                //generate an SOInvocationRecord for this method
                writeSOInvocationRecord(method, classNameNoPackage, packageName);

                toCompiler.add(SOInvocationRecordFileBase(method,
                    classNameNoPackage, packageName) + ".java");
            }
        }
        if (toCompiler.size() > 0) {
            compile(toCompiler, className);
            if (! fromIbisc) {
                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];
                    if (a.isSpecial(method)) {
                        Repository.lookupClass(SOInvocationRecordName(method,
                            classNameNoPackage, packageName));
                    }
                }
            }
        }
    }

    /* End of SO rewriter. */

    private static JavaClass lookup(String className) {
        JavaClass c = Repository.lookupClass(className);

        if (c == null) {
            System.out.println("class " + className + " not found");
            System.exit(1);
        }

        return c;
    }

    public void start(JavaClass clazz) {

        c = clazz;

        this.className = c.getClassName();
        packageName = c.getPackageName();

        if (packageName != null && !packageName.equals("")) {
            classNameNoPackage = this.className.substring(this.className
                .lastIndexOf('.') + 1, this.className.length());
        } else {
            classNameNoPackage = this.className;
        }

        gen_c = new ClassGen(c);
        cpg = gen_c.getConstantPool();
        ins_f = new InstructionFactory(gen_c);

        if (isSatin()) {
            if (verbose) {
                System.out.println(className + " is a satin class");
            }
        }

        if (isRewritten()) {
            if (verbose) {
                System.out.println(className + " is already rewritten");
            }
            return;
        }

        if (c.isInterface()) {
            if (verbose) {
                System.out.println(className + " is an interface");
            }
            return;
        }

        if (Repository.instanceOf(c, "ibis.satin.SharedObject")) {

            if (gen_c.containsField("$SOrewritten$") != null) {
                if (verbose) {
                    System.out.println(className + " is already rewritten");
                }
            } else {
                try {
                    doSORewrite();
                } catch (IOException e) {
                    System.out.println("IO error: " + e);
                    System.exit(1);
                }
            }
            return;
        }

        // If we have the main method, rename it to origMain. 
        Method main = gen_c.containsMethod("main", "([Ljava/lang/String;)V");

        if (main != null) {
            MethodGen m = new MethodGen(main, className, gen_c
                .getConstantPool());

            if (verbose) {
                System.out.println("the class has main, renaming to "
                    + "$origMain$");
            }

            m.setName("$origMain$");
            m.setMaxStack();
            m.setMaxLocals();
            m.setAccessFlags((m.getAccessFlags() & ~Constants.ACC_PUBLIC)
                | Constants.ACC_PRIVATE);

            removeLocalTypeTables(m);

            gen_c.removeMethod(main);

            main = m.getMethod();

            gen_c.addMethod(main);

            // FieldGen f = new FieldGen(Constants.ACC_STATIC, satinType,
            //         satinFieldName, gen_c.getConstantPool());

            // satinField = f.getField();
            // gen_c.addField(satinField);

            generateMain(gen_c, main);
        }

        mtab = new MethodTable(c, gen_c, this, verbose);

        if (verbose) {
            mtab.print(System.out);
        }

        try {
            generateInvocationRecords();
        } catch (IOException e) {
            System.out.println("IO error: " + e);
            System.exit(1);
        }

        rewriteMethods();

        Repository.removeClass(c);

        gen_c.addField(new FieldGen(Constants.ACC_STATIC, Type.BOOLEAN,
            "$rewritten$", cpg).getField());

        c = gen_c.getJavaClass();

        Repository.addClass(c);

        if (fromIbisc) {
            setModified(wrapper.getInfo(c));
        } else {
            // now overwrite the classfile 
            String dst;
            String clnam = className;
            if (local) {
                dst = clnam.substring(clnam.lastIndexOf('.') + 1);
            } else {
                dst = clnam.replace('.', java.io.File.separatorChar);
            }
            dst = dst + ".class";
            try {
                c.dump(dst);
            } catch (IOException e) {
                System.out.println("Error writing " + dst);
                System.exit(1);
            }
        }

        regenerateLocalRecord();

        if (errors) {
            System.exit(1);
        }
    }

    public static void usage() {
        System.err.println("Usage : java Satinc [[-no]-verbose] [[-no]-keep] "
            + "[-dir|-local] [[-no]-irc] [[-no]-sc-opt] "
            + "[[-no]-inlet-opt] [[-no]-so] " + "<classname>*");
        System.exit(1);
    }

    private void addToSatinSet(String list) {
        StringTokenizer st = new StringTokenizer(list, ", ");
        while (st.hasMoreTokens()) {
            satinSet.add(st.nextToken());
        }
    }

    public boolean processArgs(ArrayList args) {
        boolean retval = false;
        for (int i = 0; i < args.size(); i++) {
            String arg = (String) args.get(i);
            if (false) {
                // nothing
            } else if (arg.equals("-satin-no-irc")) {
                invocationRecordCache = false;
                args.remove(i--);
            } else if (arg.equals("-satin-irc")) {
                invocationRecordCache = true;
                args.remove(i--);
            } else if (arg.equals("-satin-no-inlet-opt")) {
                inletOpt = false;
                args.remove(i--);
            } else if (arg.equals("-satin-inlet-opt")) {
                inletOpt = true;
                args.remove(i--);
            } else if (arg.equals("-satin-no-sc-opt")) {
                spawnCounterOpt = false;
                args.remove(i--);
            } else if (arg.equals("-satin-sc-opt")) {
                spawnCounterOpt = true;
                args.remove(i--);
            } else if (arg.equals("-satin")) {
                args.remove(i);
                retval = true;
                if (i >= args.size()) {
                    throw new IllegalArgumentException("-satin needs classlist");
                }
                addToSatinSet((String) args.get(i));
                args.remove(i--);
            }
        }
        return retval;
    }

    public String rewriterImpl() {
        return "BCEL";
    }

    public void process(Iterator classes) {
        if (satinSet.size() == 0) {
            return;
        }
        while (classes.hasNext()) {
            JavaClass clazz = (JavaClass) classes.next();
            if (satinSet.contains(clazz.getClassName())) {
                start(clazz);
            }
        }
    }

    public static void main(String[] args) {
        boolean verbose = false;
        boolean keep = false;
        boolean local = true;
        boolean invocationRecordCache = true;
        boolean inletOpt = true;
        boolean spawnCounterOpt = true;
        ArrayList list = new ArrayList();

        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("-")) {
                list.add(args[i]);
            } else if (args[i].equals("-v")) {
                verbose = true;
            } else if (args[i].equals("-verbose")) {
                verbose = true;
            } else if (args[i].equals("-no-verbose")) {
                verbose = false;
            } else if (args[i].equals("-keep")) {
                keep = true;
            } else if (args[i].equals("-no-keep")) {
                keep = false;
            } else if (args[i].equals("-dir")) {
                local = false;
            } else if (args[i].equals("-local")) {
                local = true;
            } else if (args[i].equals("-irc-off")) {
                invocationRecordCache = false;
            } else if (args[i].equals("-no-irc")) {
                invocationRecordCache = false;
            } else if (args[i].equals("-irc")) {
                invocationRecordCache = true;
            } else if (args[i].equals("-no-inlet-opt")) {
                inletOpt = false;
            } else if (args[i].equals("-inlet-opt")) {
                inletOpt = true;
            } else if (args[i].equals("-no-sc-opt")) {
                spawnCounterOpt = false;
            } else if (args[i].equals("-sc-opt")) {
                spawnCounterOpt = true;
            } else {
                usage();
            }
        }

        if (list.size() == 0) {
            usage();
        }

        Satinc satinc = new Satinc(verbose, local, keep, invocationRecordCache,
                inletOpt, spawnCounterOpt);

        for (int i = 0; i < list.size(); i++) {
            satinc.start(lookup((String) list.get(i)));
        }
    }
}
