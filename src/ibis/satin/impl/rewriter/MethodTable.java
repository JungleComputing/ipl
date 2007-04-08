/* $Id$ */

package ibis.satin.impl.rewriter;

import ibis.frontend.util.BT_Analyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Type;

final class MethodTable {

    static class SpawnTableEntry {

        ArrayList<CodeExceptionGen> catchBlocks; /* indexed on spawnId */

        boolean hasInlet;

        boolean[] isLocalUsed;

        SpawnTableEntry() {
            // do nothing
        }

        boolean[] analyseUsedLocals(MethodGen mg, CodeExceptionGen catchBlock,
            boolean verbose) {
            int maxLocals = mg.getMaxLocals();
            InstructionHandle end = getEndOfCatchBlock(mg, catchBlock);

            // Start with all false.
            boolean[] used = new boolean[maxLocals];

            if (verbose) {
                System.out.println("analysing used locals for " + mg
                    + ", maxLocals = " + maxLocals);
            }

            if (end == null) {
                if (verbose) {
                    System.out.println("finally clause; assuming all locals "
                        + "are used");
                }
                for (int j = 0; j < maxLocals; j++) {
                    used[j] = true;
                }
                return used;
            }

            Instruction endIns = end.getInstruction();
            if (!(endIns instanceof ReturnInstruction)
                && !(endIns instanceof ATHROW)) {
                if (verbose) {
                    System.out.println("no return at end of inlet, assuming "
                        + "all locals are used");
                }

                // They are all used.
                for (int j = 0; j < maxLocals; j++) {
                    used[j] = true;
                }
                return used;
            }

            for (InstructionHandle i = catchBlock.getHandlerPC(); i != end
                && i != null; i = i.getNext()) {
                Instruction curr = i.getInstruction();
                if (verbose) {
                    System.out.println("ins: " + curr);
                }
                if (curr instanceof BranchInstruction) {
                    InstructionHandle dest = ((BranchInstruction) curr)
                        .getTarget();

                    if (dest.getPosition() < catchBlock.getHandlerPC()
                        .getPosition()
                        || // backjump out of handler
                        dest.getPosition() > end.getPosition()) {
                        // forward jump beyond catch
                        if (verbose) {
                            System.out
                                .println("inlet contains a jump to exit, "
                                    + "assuming all locals are used");
                        }

                        // They are all used.
                        for (int j = 0; j < maxLocals; j++) {
                            used[j] = true;
                        }
                        return used;
                    }
                    /* No, not good, could be inside a conditional.
                     } else if (curr instanceof ReturnInstruction
                     || curr instanceof ATHROW) {
                     if (verbose) {
                     System.out.println("return:");
                     for (int k = 0; k < used.length; k++) {
                     if (!used[k]) {
                     System.out.println("RET: local " + k
                     + " is unused");
                     }
                     }
                     }
                     // System.out.println("inlet local opt triggered");
                     return used;
                     */
                } else if (curr instanceof LocalVariableInstruction) {
                    LocalVariableInstruction l = (LocalVariableInstruction) curr;
                    used[l.getIndex()] = true;
                    if (verbose) {
                        System.out.println("just used local " + l.getIndex());
                    }
                }
            }

            return used;
        }

        SpawnTableEntry(SpawnTableEntry orig, MethodGen mg, MethodGen origM) {
            isLocalUsed = new boolean[origM.getMaxLocals()];

            hasInlet = orig.hasInlet;
            if (hasInlet) {
                /* copy and rewite exception table */
                CodeExceptionGen origE[] = origM.getExceptionHandlers();
                CodeExceptionGen newE[] = mg.getExceptionHandlers();

                catchBlocks = new ArrayList<CodeExceptionGen>();

                for (int i = 0; i < orig.catchBlocks.size(); i++) {
                    CodeExceptionGen origCatch = orig.catchBlocks
                        .get(i);
                    for (int j = 0; j < origE.length; j++) {
                        if (origCatch == origE[j]) {
                            catchBlocks.add(newE[j]);
                            break;
                        }
                    }
                }
            }
        }
    }

    static class MethodTableEntry {

        Method m;

        MethodGen mg;

        boolean containsInlet;

        MethodTableEntry clone;

        int nrSpawns;

        SpawnTableEntry[] spawnTable;

        boolean isClone;

        Type[] typesOfParams;

        Type[] typesOfParamsNoThis;

        int startLocalAlloc;

        MethodTableEntry() {
            /* do nothing */
        }

        MethodTableEntry(MethodTableEntry orig, MethodGen mg) {
            this.mg = mg;
            containsInlet = orig.containsInlet;
            clone = null;
            nrSpawns = orig.nrSpawns;
            isClone = true;
            typesOfParams = orig.typesOfParams;
            typesOfParamsNoThis = orig.typesOfParamsNoThis;

            startLocalAlloc = orig.startLocalAlloc;

            spawnTable = new SpawnTableEntry[orig.spawnTable.length];
            for (int i = 0; i < spawnTable.length; i++) {
                spawnTable[i] = new SpawnTableEntry(orig.spawnTable[i], mg,
                    orig.mg);
            }
        }

        void print(java.io.PrintStream out) {
            out.println("Method: " + m);
            out.println("params(" + typesOfParams.length + "): ");
            for (int i = 0; i < typesOfParams.length; i++) {
                out.println("    " + typesOfParams[i]);
            }

            out.println("This method contains " + nrSpawns + " spawn(s)");

            if (isClone) {
                out.println("This method is a clone of an inlet method");
            } else {
                out.println("This method is not a clone of an inlet method");
            }

            if (containsInlet) {
                out.println("This method contains an inlet");
            } else {
                out.println("This method does not contain an inlet");
            }
            out.println("---------------------");
        }
    }

    private Vector<MethodTableEntry> methodTable; /* a vector of MethodTableEntries */

    private JavaClass spawnableClass;

    private Satinc self;

    private boolean verbose;

    JavaClass c;

    private static HashMap<JavaClass, BT_Analyzer> analyzers
            = new HashMap<JavaClass, BT_Analyzer>();

    private boolean available_slot(Type[] types, int ind, Type t) {
        Type tp = types[ind];
        if (tp != null) {
            if (t.equals(tp)) {
                return true;
            }
            return false;
        }
        if (t.equals(Type.LONG) || t.equals(Type.DOUBLE)) {
            if (types[ind + 1] != null) {
                return false;
            }
            types[ind + 1] = Type.VOID;
        }
        types[ind] = t;
        return true;
    }

    // Make each stack position indicate a single type of variable.
    private void rewriteLocals(MethodGen m, ConstantPoolGen cpg) {
        // First, analyze how many stack positions are for parameters.
        // We won't touch those.
        int parameterpos = m.isStatic() ? 0 : 1;

        Type[] parameters = m.getArgumentTypes();

        parameterpos += parameters.length;
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].equals(Type.LONG)
                || parameters[i].equals(Type.DOUBLE)) {
                parameterpos++;
            }
        }

        LocalVariableGen[] locals = m.getLocalVariables();
        int maxpos = m.getMaxLocals();

        Type[] types = new Type[2 * locals.length + maxpos];

        for (int i = 0; i < locals.length; i++) {
            int ind = locals[i].getIndex();
            if (ind < parameterpos) {
                continue;
            }
            Type t = locals[i].getType();
            if (!available_slot(types, ind, t)) {
                // Give this local a new stack position.
                locals[i].setIndex(maxpos);
                types[maxpos] = t;
                rewriteLocal(locals[i].getStart(), locals[i].getEnd(), ind,
                    maxpos);
                // And give all other locals that have the same old stack
                // position and the same type this new stack position.
                for (int j = i + 1; j < locals.length; j++) {
                    if (locals[j].getIndex() == ind
                        && locals[j].getType().equals(t)) {
                        locals[j].setIndex(maxpos);
                        rewriteLocal(locals[j].getStart(), locals[j].getEnd(),
                            ind, maxpos);
                    }
                }
                maxpos++;
                if (t.equals(Type.LONG) || t.equals(Type.DOUBLE)) {
                    // longs and doubles need two stack positions.
                    types[maxpos] = Type.VOID;
                    maxpos++;
                }
            }
        }

        // Rewrite all locals that are not in the LocalVariableTable.
        // Javac sometimes generates these for synchronized blocks.
        // Here, we give them a new index, so that they at least do not
        // collide with locals in the LocalVariableTable.
        // For now, if they occur in an inlet handler, they will not be
        // rewritten!
        ArrayList<Integer> newLocals = new ArrayList<Integer>();
        for (InstructionHandle h = m.getInstructionList().getStart(); h != null;
                h = h.getNext()) {
            Instruction ins = h.getInstruction();
            if (ins instanceof LocalVariableInstruction) {
                LocalVariableInstruction lins = (LocalVariableInstruction) ins;
                int pos = lins.getIndex();
                if (pos >= parameterpos) {
                    // Stay away from the method parameters.
                    LocalVariableGen lg = getLocal(m, lins, h.getPosition());
                    if (lg == null) {
                        int local = lins.getIndex();
                        Type tp = lins.getType(cpg);
                        while (local >= newLocals.size()) {
                            newLocals.add(null);
                        }
                        if (newLocals.get(local) == null) {
                            newLocals.set(local, new Integer(maxpos));
                            maxpos++;
                            if (tp.equals(Type.LONG) || tp.equals(Type.DOUBLE)) {
                                maxpos++;
                            }
                        }
                        Integer i = newLocals.get(local);
                        lins.setIndex(i.intValue());
                    }
                }
            }
        }
    }

    // Rewrite instructions where local is used.
    void rewriteLocal(InstructionHandle fromH, InstructionHandle toH,
        int oldindex, int newindex) {

        InstructionHandle h = fromH;
        if (h.getPrev() != null) {
            h = h.getPrev();
            // This instruction should contain the store.
        }
        while (h != null && h != toH) {
            Instruction ins = h.getInstruction();
            if (ins instanceof LocalVariableInstruction) {
                LocalVariableInstruction lins = (LocalVariableInstruction) ins;
                if (lins.getIndex() == oldindex) {
                    lins.setIndex(newindex);
                }
            }
            h = h.getNext();
        }
    }

    MethodTable(JavaClass c, ClassGen gen_c, Satinc self, boolean verbose) {
        this.verbose = verbose;
        this.self = self;
        this.c = c;

        spawnableClass = Repository.lookupClass("ibis.satin.Spawnable");

        methodTable = new Vector<MethodTableEntry>();

        Method[] methods = gen_c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            MethodGen mg = new MethodGen(m, c.getClassName(), gen_c
                .getConstantPool());
            rewriteLocals(mg, gen_c.getConstantPool());
            mg.setMaxLocals();
            mg.setMaxStack();
            gen_c.removeMethod(m);
            m = mg.getMethod();
            gen_c.addMethod(m);
            // mg = new MethodGen(m, c.getClassName(), gen_c.getConstantPool());
            //
            if (mg.getName().equals("<clinit>")) {
                /* MethodGen clinit = */new MethodGen(m, c.getClassName(),
                    gen_c.getConstantPool());
            }
            MethodTableEntry e = new MethodTableEntry();

            e.nrSpawns = calcNrSpawns(mg);
            e.spawnTable = new SpawnTableEntry[e.nrSpawns];
            e.m = m;
            e.mg = mg;
            e.typesOfParams = this.getParamTypesThis(m);
            e.typesOfParamsNoThis = this.getParamTypesNoThis(m);

            if (mg.getInstructionList() != null) {
                fillSpawnTable(e);
            }
            methodTable.addElement(e);
        }
    }

    void print(java.io.PrintStream out) {
        out.print("---------------------");
        out.print("method table of class " + c.getClassName());
        out.println("---------------------");

        for (int i = 0; i < methodTable.size(); i++) {
            MethodTableEntry m = methodTable.elementAt(i);
            m.print(out);
        }
        out.println("---------------------");
    }

    private void fillSpawnTable(MethodTableEntry me) {
        InstructionList il = me.mg.getInstructionList();
        InstructionHandle[] ins = il.getInstructionHandles();

        il.setPositions();

        int spawnId = 0;

        for (int k = 0; k < ins.length; k++) {
            if (ins[k].getInstruction() instanceof INVOKEVIRTUAL) {
                Method target = self.findMethod((INVOKEVIRTUAL) (ins[k]
                    .getInstruction()));
                JavaClass cl = self.findMethodClass((INVOKEVIRTUAL) (ins[k]
                    .getInstruction()));
                if (isSpawnable(target, cl)) {
                    // we have a spawn!
                    analyzeSpawn(me, ins[k], spawnId);
                    spawnId++;
                }
            }
        }
    }

    private void analyzeSpawn(MethodTableEntry me, InstructionHandle spawnIns,
        int spawnId) {
        SpawnTableEntry se = me.spawnTable[spawnId] = new SpawnTableEntry();
        se.isLocalUsed = new boolean[me.mg.getMaxLocals()];

        CodeExceptionGen[] exceptions = me.mg.getExceptionHandlers();

        // We have a spawn. Is it in a try block?
        for (int j = 0; j < exceptions.length; j++) {
            CodeExceptionGen e = exceptions[j];
            int startPC = e.getStartPC().getPosition();
            int endPC = e.getEndPC().getPosition();
            int PC = spawnIns.getPosition();

            if (verbose) {
                System.err.println("startPC = " + startPC + ", endPC = "
                    + endPC + ", PC = " + PC);
            }

            if (PC >= startPC && PC <= endPC) {
                /* ok, we have an inlet, add try-catch block info to table */
                me.containsInlet = true;
                se.hasInlet = true;

                if (se.catchBlocks == null) {
                    se.catchBlocks = new ArrayList<CodeExceptionGen>();
                }

                se.catchBlocks.add(e);

                if (verbose) {
                    System.out.println("spawn " + spawnId + " with inlet " + e);
                }

                boolean[] used = se.analyseUsedLocals(me.mg, e, verbose);
                for (int k = 0; k < used.length; k++) {
                    if (used[k]) {
                        se.isLocalUsed[k] = true;
                    }
                }
            }
        }

        if (verbose) {
            System.out.println(me.m + ": unused locals in all inlets: ");
            for (int k = 0; k < se.isLocalUsed.length; k++) {
                if (!se.isLocalUsed[k]) {
                    System.out.println("local " + k + " is unused");
                } else {
                    System.out.println("local " + k + " is used");
                }
            }
        }
    }

    private Type[] getParamTypesNoThis(Method m) {
        return Type.getArgumentTypes(m.getSignature());
    }

    private Type[] getParamTypesThis(Method m) {
        Type[] params = getParamTypesNoThis(m);

        if (m.isStatic()) {
            return params;
        }

        Type[] newparams = new Type[1 + params.length];
        newparams[0] = new ObjectType(c.getClassName());
        for (int i = 0; i < params.length; i++) {
            newparams[i + 1] = params[i];
        }
        return newparams;
    }

    private int calcNrSpawns(MethodGen mg) {
        InstructionList il = mg.getInstructionList();
        if (il == null) {
            return 0;
        }

        InstructionHandle[] ins = il.getInstructionHandles();
        int count = 0;

        for (int i = 0; i < ins.length; i++) {
            if (ins[i].getInstruction() instanceof INVOKEVIRTUAL) {
                Method target = self.findMethod((INVOKEVIRTUAL) (ins[i]
                    .getInstruction()));
                JavaClass cl = self.findMethodClass((INVOKEVIRTUAL) (ins[i]
                    .getInstruction()));
                if (isSpawnable(target, cl)) {
                    count++;
                }
            }
        }

        return count;
    }

    private BT_Analyzer getAnalyzer(JavaClass cl) {
        BT_Analyzer b = analyzers.get(cl);
        if (b == null) {
            b = new BT_Analyzer(cl, spawnableClass, verbose);
            b.start(false);
            analyzers.put(cl, b);
        }
        return b;
    }

    boolean isSpawnable(Method m, JavaClass cl) {
        BT_Analyzer analyzer = getAnalyzer(cl);
        // System.out.println("isSpawnable: method = " + m + ", class = " +
        // cl.getClassName());
        if (! analyzer.hasSpecialMethods()) {
            return false;
        }
        return analyzer.isSpecial(m);
    }

    void addCloneToInletTable(Method mOrig, MethodGen mg) {
        for (int i = 0; i < methodTable.size(); i++) {
            MethodTableEntry e = methodTable.elementAt(i);
            if (e.m.equals(mOrig)) {
                MethodTableEntry newE = new MethodTableEntry(e, mg);
                methodTable.addElement(newE);
                e.clone = newE;
                return;
            }
        }
        System.err.println("illegal method in addCloneToInletTable: " + mOrig);

        System.exit(1);
    }

    private MethodTableEntry findMethod(MethodGen mg) {
        for (int i = 0; i < methodTable.size(); i++) {
            MethodTableEntry e = methodTable.elementAt(i);
            if (e.mg == mg) {
                return e;
            }
        }

        System.err.println("Unable to find method " + mg);
        new Exception().printStackTrace();
        System.exit(1);

        return null;
    }

    private MethodTableEntry findMethod(Method m) {
        for (int i = 0; i < methodTable.size(); i++) {
            MethodTableEntry e = methodTable.elementAt(i);
            if (e.m == m) {
                return e;
            }
        }

        System.err.println("Unable to find method " + m);
        new Exception().printStackTrace();
        System.exit(1);

        return null;
    }

    boolean hasInlet(Method m, int spawnId) {
        MethodTableEntry e = findMethod(m);
        return e.spawnTable[spawnId].hasInlet;
    }

    boolean isLocalUsedInInlet(Method m, int localNr) {
        MethodTableEntry e = findMethod(m);

        if (localNr >= e.mg.getMaxLocals()) {
            System.out.println("eek, local nr too large: " + localNr
                + ", max: " + e.mg.getMaxLocals());
        }

        for (int i = 0; i < e.spawnTable.length; i++) {
            if (e.spawnTable[i].isLocalUsed[localNr]) {
                return true;
            }
        }

        return false;
    }

    Type[] typesOfParams(Method m) {
        MethodTableEntry e = findMethod(m);
        return e.typesOfParams;
    }

    Type[] typesOfParamsNoThis(Method m) {
        MethodTableEntry e = findMethod(m);
        return e.typesOfParamsNoThis;
    }

    ArrayList<CodeExceptionGen> getCatchTypes(MethodGen m, int spawnId) {
        MethodTableEntry e = findMethod(m);
        return e.spawnTable[spawnId].catchBlocks;
    }

    Method getExceptionHandlingClone(Method m) {
        return findMethod(m).clone.m;
    }

    boolean containsInlet(MethodGen m) {
        return findMethod(m).containsInlet;
    }

    boolean containsInlet(Method m) {
        return findMethod(m).containsInlet;
    }

    boolean isClone(MethodGen m) {
        return findMethod(m).isClone;
    }

    boolean isClone(Method m) {
        return findMethod(m).isClone;
    }

    int nrSpawns(Method m) {
        return findMethod(m).nrSpawns;
    }

    void setStartLocalAlloc(MethodGen m, InstructionHandle i) {
        m.getInstructionList().setPositions();
        findMethod(m).startLocalAlloc = i.getPosition();
    }

    int getStartLocalAlloc(Method m) {
        return findMethod(m).startLocalAlloc;
    }

    static InstructionHandle getEndOfCatchBlock(MethodGen m,
        CodeExceptionGen catchBlock) {

        if (catchBlock.getCatchType() == null) {
            // finally clause, no local variable!
            return null;
        }

        LocalVariableGen[] lt = m.getLocalVariables();

        InstructionHandle handler = catchBlock.getHandlerPC();

        for (int i = 0; i < lt.length; i++) {
            InstructionHandle start = lt[i].getStart();
            InstructionHandle end = lt[i].getEnd();

            // dangerous, javac is one instruction further...
            if ((start == handler || start == handler.getNext() || start == handler
                .getNext().getNext())
                && lt[i].getType().equals(catchBlock.getCatchType())) {
                // System.out.println("found range of catch block: "
                //         + handler + " - " + end);
                return end.getPrev();
            }
        }

        System.err
            .println("Could not find end of catch block, did you compile "
                + "with the '-g' option?");
        System.exit(1);
        return null;
    }

    private static LocalVariableTable getLocalTable(Method m) {
        LocalVariableTable lt = m.getLocalVariableTable();

        if (lt == null) {
            System.err.println("Could not get local variable table, did you "
                + "compile with the '-g' option?");
            System.exit(1);
        }

        return lt;
    }

    private final LocalVariableGen[] getLocalTable(MethodGen m) {
        LocalVariableGen[] lt = m.getLocalVariables();

        if (lt == null) {
            System.err.println("Could not get local variable table, did you "
                + "compile with the '-g' option?");
            System.exit(1);
        }

        return lt;
    }

    static String getParamName(Method m, int paramNr) {
        LocalVariable[] lt = getLocalTable(m).getLocalVariableTable();

        int minPos = Integer.MAX_VALUE;
        String res = null;

        for (int i = 0; i < lt.length; i++) {
            LocalVariable l = lt[i];
            if (l.getIndex() == paramNr) {
                int startPos = l.getStartPC();

                if (startPos < minPos) {
                    minPos = startPos;
                    res = l.getName();
                }
            }
        }

        if (res != null) {
            return res;
        }

        System.err.println("getParamName: could not find name of param "
            + paramNr);
        System.exit(1);

        return null;
    }

    static String getParamNameNoThis(Method m, int paramNr) {
        if (!m.isStatic()) {
            paramNr++;
        }
        return getParamName(m, paramNr);
    }

    Type getParamType(Method m, int paramNr) {
        return findMethod(m).typesOfParams[paramNr];
    }

    final LocalVariableGen getLocal(MethodGen m, LocalVariableInstruction curr,
        int pos) {
        int localNr = curr.getIndex();
        LocalVariableGen[] lt = getLocalTable(m);

        for (int i = 0; i < lt.length; i++) {
            // Watch out. The first initialization seems not to be included in
            // the range given in the local variable table!

            if (localNr == lt[i].getIndex()) {
                //                System.err.println("Looking for local " + localNr
                //                        + " on position " + pos);
                //                System.err.println("found one with range "
                //                        + lt[i].getStart().getPrev().getPosition() + ", "
                //                        + lt[i].getEnd().getPosition());

                if (pos >= lt[i].getStart().getPrev().getPosition()
                    && pos < (lt[i].getEnd().getPosition())) {
                    return lt[i];
                }
            }
        }

        return null;
    }

    String getLocalName(MethodGen m, LocalVariableInstruction curr, int pos) {
        LocalVariableGen a = getLocal(m, curr, pos);

        if (a == null) {
            return null;
        }

        return a.getName();
    }

    Type getLocalType(MethodGen m, LocalVariableInstruction curr, int pos) {
        LocalVariableGen a = getLocal(m, curr, pos);

        if (a == null) {
            return null;
        }

        return a.getType();
    }

    static String generatedLocalName(Type type, String name) {
        return name
            + "_"
            + type.toString().replace('.', '_').replace('[', '_').replace(']',
                '_').replace('$', '_');
    }

    static String[] getAllLocalDecls(Method m) {
        LocalVariable[] lt = getLocalTable(m).getLocalVariableTable();
        Vector<String> v = new Vector<String>();

        for (int i = 0; i < lt.length; i++) {
            LocalVariable l = lt[i];
            Type tp = Type.getType(l.getSignature());
            String e = tp.toString() + " "
                + generatedLocalName(tp, l.getName()) + ";";
            if (!v.contains(e)) {
                v.addElement(e);
            }
        }

        String[] result = new String[v.size()];
        result = v.toArray(result);
        // for (int i = 0; i < v.size(); i++) {
        //     result[i] = (String) v.elementAt(i);
            // System.out.println("localdecls for " + m + ": " + result[i]);
        // }

        return result;
    }

    static Type[] getAllLocalTypes(Method m) {
        LocalVariable[] lt = getLocalTable(m).getLocalVariableTable();
        Type[] result = new Type[lt.length];

        for (int i = 0; i < lt.length; i++) {
            LocalVariable l = lt[i];
            Type tp = Type.getType(l.getSignature());
            result[i] = tp;
        }

        return result;
    }

    static String[] getAllLocalNames(Method m) {
        LocalVariable[] lt = getLocalTable(m).getLocalVariableTable();
        String[] result = new String[lt.length];

        for (int i = 0; i < lt.length; i++) {
            LocalVariable l = lt[i];
            Type tp = Type.getType(l.getSignature());
            String e = generatedLocalName(tp, l.getName());
            result[i] = e;
        }
        return result;
    }

    boolean containsSpawnedCall(MethodGen m) {
        InstructionList code = m.getInstructionList();

        if (code == null) {
            return false;
        }

        InstructionHandle ih[] = code.getInstructionHandles();

        for (int i = 0; i < ih.length; i++) {
            Instruction ins = ih[i].getInstruction();
            if (ins instanceof INVOKEVIRTUAL) {
                Method target = self.findMethod((INVOKEVIRTUAL) (ins));
                JavaClass cl = self.findMethodClass((INVOKEVIRTUAL) (ins));
                if (isSpawnable(target, cl)) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean containsSpawnedCall(Method m) {
        MethodGen mg = getMethodGen(m);

        return containsSpawnedCall(mg);
    }

    static int realMaxLocals(MethodGen m) {
        m.setMaxLocals();
        return m.getMaxLocals();
    }

    MethodGen getMethodGen(Method m) {
        MethodTableEntry e = findMethod(m);
        if (e == null) {
            return null;
        }
        return e.mg;
    }

    void replace(Method orig, Method newm) {
        MethodTableEntry e = findMethod(orig);
        e.m = newm;
    }

    void setMethod(MethodGen mg, Method m) {
        MethodTableEntry e = findMethod(mg);
        e.m = m;
    }
}
