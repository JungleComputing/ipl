package ibis.frontend.satin;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.verifier.*;

import java.util.Vector;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.PrintStream;
import java.io.File;

// A method that contains a spawn is rewritten like this:
// maxlocals = spawnCounter
// maxlocals+1 = outstandingSpawns
// maxLocals+2 = curr, the current invocation record we are syncing on
// maxLocals+3 = temp invocationrecord, cast to correct invocationRecord type

// @@@ optimizations TODO:
//     If there is only one spawn in a method, no need to test for ids. (o.a. for inlets).
//     exception handler in non-clone is unreachable, delete.
//     initialisations of locals. delete if not needed.

public final class Satinc {
    JavaClass satinObjectClass;
    JavaClass mainClass;
    ObjectType spawnCounterType;
    ObjectType irType;
    ObjectType satinType;

    JavaClass		c; // the class we are rewriting 
    ClassGen		gen_c;
    ConstantPoolGen	cpg;
    InstructionFactory	ins_f;

    Vector idTable = new Vector();
    boolean verbose;
    boolean keep;
    boolean local;
    boolean print;
    boolean invocationRecordCache;
    Field satinField;
    String classname;
    String mainClassname;
    String compiler = "javac";
    boolean supportAborts;
    boolean inletOpt;
    boolean spawnCounterOpt;
    MethodTable mtab;
    boolean failed_verification = false;

    private class StoreClass {
	Instruction store;
	Instruction load; // for putfield 
	Method target;

	StoreClass(Instruction store, Instruction load, Method target) {
	    this.store = store;
	    this.target = target;
	    this.load = load;
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

	    if (!store.equals(c.store)) {
		return false;
	    }

	    // stores are equal, are loads? 
	    if (c.load == null && load == null) {
		return true;
	    }
	    if (c.load == null) {
		return false;
	    }
	    if (load == null) {
		return false;
	    }

	    return load.equals(c.load);
	}
    }

    public Satinc(boolean verbose, boolean local, boolean keep, boolean print, boolean invocationRecordCache,
           String classname, String mainClassname, String compiler, boolean supportAborts, boolean inletOpt, boolean spawnCounterOpt) {
	this.verbose = verbose;
	this.keep = keep;
	this.print = print;
	this.local = local;
	this.invocationRecordCache = invocationRecordCache;
	this.classname = classname;
	this.mainClassname = mainClassname;
	this.compiler = compiler;
	this.supportAborts = supportAborts;
	this.inletOpt = inletOpt;
	this.spawnCounterOpt = spawnCounterOpt;

	c = Repository.lookupClass(classname);

	if (c == null) {
	    System.out.println("class " + classname + " not found");
	    System.exit(1);
	}

	gen_c = new ClassGen(c);
	cpg = gen_c.getConstantPool();
	ins_f = new InstructionFactory(gen_c);

	if (classname.equals(mainClassname)) {
	    mainClass = c;
	} else {
	    mainClass = Repository.lookupClass(mainClassname);
	}

	satinObjectClass = Repository.lookupClass("ibis.satin.SatinObject");
	spawnCounterType = new ObjectType("ibis.satin.SpawnCounter");
	irType = new ObjectType("ibis.satin.InvocationRecord");
	satinType = new ObjectType("ibis.satin.Satin");
    }

    boolean isSatin() {
	return Repository.instanceOf(c, satinObjectClass);
    }

    boolean isRewritten() {
	return Repository.implementationOf(c, "ibis.satin.SatinRewritten");
    }

    static boolean isRetIns(Instruction i) {
	return i instanceof ReturnInstruction;
    }

    static String getInitVal(Type s) {
	if (s instanceof BasicType) return "0";
	return "null";
    }

    static boolean isRefType(Type s) {
	return (s instanceof ReferenceType);
    }

    private static Type getReturnType(Method m) {
	return Type.getReturnType(m.getSignature());
    }

    private static Type[] getArgumentTypes(Method m) {
	return Type.getArgumentTypes(m.getSignature());
    }

    CodeExceptionGen getExceptionHandler(MethodGen m, InstructionHandle self) {
	CodeExceptionGen exc[] = m.getExceptionHandlers();

	for (int j=0; j<exc.length; j++) {
	    if (exc[j].containsTarget(self)) {
		return exc[j];
	    }
	}

	return null;
    }

    void generateMain(ClassGen c, Method origMain) {

	InstructionList il = new InstructionList();

	MethodGen new_main = new MethodGen(Constants.ACC_STATIC | Constants.ACC_PUBLIC,
					   Type.VOID,
					   new Type[] {new ArrayType(Type.STRING, 1)},
					   new String[] { "argv" },
					   "main",
					   c.getClassName(),
					   il,
					   c.getConstantPool());

	il.append(ins_f.createNew(satinType));
	il.append(new DUP());
	il.append(new ALOAD(0));
	il.append(ins_f.createInvoke("ibis.satin.Satin",
				     "<init>",
				     Type.VOID,
				     new Type[] {new ArrayType(Type.STRING, 1)},
				     Constants.INVOKESPECIAL));
	il.append(new DUP());
	il.append(ins_f.createFieldAccess(mainClassname,
					  "satinClass",
					  satinType,
					  Constants.PUTSTATIC));
	il.append(ins_f.createFieldAccess("ibis.satin.Satin",
					  "master",
					  Type.BOOLEAN,
					  Constants.GETFIELD));
	BranchHandle ifcmp = il.append(new IFEQ(null));
	il.append(ins_f.createFieldAccess(mainClassname,
					  "satinClass",
					  satinType,
					  Constants.GETSTATIC));
	il.append(ins_f.createFieldAccess("ibis.satin.Satin",
					  "mainArgs",
					  new ArrayType(Type.STRING, 1),
					  Constants.GETFIELD));
	InstructionHandle try_start =
	    il.append(ins_f.createInvoke(c.getClassName(),
				     origMain.getName(),
				     Type.VOID,
				     new Type[] {new ArrayType(Type.STRING, 1)},
				     Constants.INVOKESTATIC));

	BranchHandle try_end = il.append(new GOTO(null));

	InstructionHandle e_handler = il.append(new ASTORE(1));
	il.append(ins_f.createFieldAccess("java.lang.System",
					  "out",
					  new ObjectType("java.io.PrintStream"),
					  Constants.GETSTATIC));
	il.append(ins_f.createNew("java.lang.StringBuffer"));
	il.append(new DUP());
	il.append(new PUSH(gen_c.getConstantPool(), "Exception in main: "));
	il.append(ins_f.createInvoke("java.lang.StringBuffer",
				     "<init>",
				     Type.VOID,
				     new Type[] {Type.STRING},
				     Constants.INVOKESPECIAL));
	il.append(new ALOAD(1));
	il.append(ins_f.createInvoke("java.lang.StringBuffer",
				     "append",
				     new ObjectType("java.lang.StringBuffer"),
				     new Type[] {Type.OBJECT},
				     Constants.INVOKEVIRTUAL));
	il.append(ins_f.createInvoke("java.lang.StringBuffer",
				     "toString",
				     Type.STRING,
				     Type.NO_ARGS,
				     Constants.INVOKEVIRTUAL));
	il.append(ins_f.createInvoke("java.io.PrintStream",
				     "println",
				     Type.VOID,
				     new Type[] {Type.STRING},
				     Constants.INVOKEVIRTUAL));
	il.append(new ALOAD(1));
	il.append(ins_f.createInvoke("java.lang.Throwable",
				     "printStackTrace",
				     Type.VOID,
				     Type.NO_ARGS,
				     Constants.INVOKEVIRTUAL));

	BranchHandle gto2 = il.append(new GOTO(null));

	InstructionHandle ifeq_target = 
	     il.append(ins_f.createFieldAccess(mainClassname,
					       "satinClass",
					       satinType,
					       Constants.GETSTATIC));
	ifcmp.setTarget(ifeq_target);
	il.append(ins_f.createInvoke("ibis.satin.Satin",
				     "client",
				     Type.VOID,
				     Type.NO_ARGS,
				     Constants.INVOKEVIRTUAL));

	InstructionHandle gto_target = 
	     il.append(ins_f.createFieldAccess(mainClassname,
					       "satinClass",
					       satinType,
					       Constants.GETSTATIC));
	try_end.setTarget(gto_target);
	gto2.setTarget(gto_target);

	il.append(ins_f.createInvoke("ibis.satin.Satin",
				     "exit",
				     Type.VOID,
				     Type.NO_ARGS,
				     Constants.INVOKEVIRTUAL));
	il.append(new RETURN());

	new_main.addExceptionHandler(try_start, try_end, e_handler, new ObjectType("java.lang.Throwable"));
	new_main.setMaxStack();
	new_main.setMaxLocals();

	Method main = new_main.getMethod();
	gen_c.addMethod(main);
    }

    static void printMethod(Method m) {
	System.out.println("code for method " + m + ":");
	System.out.println(m.getCode());
	System.out.println("*******************************************");
	
    }

    String invocationRecordName(Method m) {
	return ("Satin_" + c.getClassName() + "_" + m.getName() + "_InvocationRecord").replace('.', '_');
    }

    String localRecordName(Method m) {
	return ("Satin_" + c.getClassName() + "_" + m.getName() + "_LocalRecord").replace('.', '_');
    }

    String localRecordName(MethodGen m) {
	return ("Satin_" + c.getClassName() + "_" + m.getName() + "_LocalRecord").replace('.', '_');
    }

    String returnRecordName(Method m) {
	return ("Satin_" + c.getClassName() + "_" + m.getName() + "_ReturnRecord").replace('.', '_');
    }

    void insertAllDeleteLocalRecords(MethodGen m) {
	int maxLocals = m.getMaxLocals();
	InstructionList il = m.getInstructionList();

	for (InstructionHandle i=il.getStart(); i != null; i = i.getNext()) {
	    Instruction ins = i.getInstruction();
	    if (ins instanceof ReturnInstruction) {
		i = insertDeleteLocalRecord(m, il, i, maxLocals);
	    }
	}
    }

    InstructionHandle insertDeleteLocalRecord(MethodGen m, InstructionList il, InstructionHandle i, int maxLocals) {
	String local_record_name = localRecordName(m);

	// Note: maxLocals has been recomputed at this point.
	il.insert(i, new ALOAD(maxLocals-5));
	il.insert(i, ins_f.createInvoke(local_record_name,
				     "delete",
				     Type.VOID,
				     new Type[] { new ObjectType(local_record_name) },
				     Constants.INVOKESTATIC));

	return i;
    }

    InstructionHandle insertDeleteSpawncounter(InstructionList il, InstructionHandle i, int maxLocals) {
	il.insert(i, new ALOAD(maxLocals));
	il.insert(i, ins_f.createInvoke("ibis.satin.Satin",
				     "deleteSpawnCounter",
				     Type.VOID,
				     new Type[] { spawnCounterType },
				     Constants.INVOKESTATIC));
	return i;
    }

    int allocateId(Instruction storeIns, Instruction loadIns, Method target) {
	StoreClass s = new StoreClass(storeIns, loadIns, target);

	int id = idTable.indexOf(s);
	if (id < 0) {
	    idTable.add(s);
	    id = idTable.size() - 1;
	}

	return id;
    }

    Instruction getStoreIns(int id) {
	return ((StoreClass)idTable.get(id)).store;
    }

    Instruction getLoadIns(int id) {
	return ((StoreClass) idTable.get(id)).load;
    }

    Method getStoreTarget(int id) {
	return ((StoreClass) idTable.get(id)).target;
    }

    void clearIdTable() {
	idTable.clear();
    }

    void rewriteAbort(MethodGen m, InstructionList il, InstructionHandle i, int maxLocals) {
	// in a clone, we have to abort two lists: the outstanding spawns of the parent, and the outstanding
	// spawns of the clone.
	Instruction fa = ins_f.createFieldAccess(mainClassname,
						 "satinClass",
						 satinType,
						 Constants.GETSTATIC);
	Instruction ab = ins_f.createInvoke("ibis.satin.Satin",
					    "abort",
					    Type.VOID,
					    new Type[] { irType,
							 irType },
					    Constants.INVOKEVIRTUAL);
	if (mtab.isClone(m)) {
	    int parentPos = 3;

	    if (!m.isStatic()) { // we have an additional 'this' param
		parentPos++;
	    }

	    i.getPrev().getPrev().setInstruction(fa);
	    i.getPrev().setInstruction(new ALOAD(maxLocals-3));		// push outstanding spawns
	    i.setInstruction(new ALOAD(parentPos));			// push parent invocationrecord
	    i = i.getNext();
	} else if (mtab.containsInlet(m)) {
	    i.getPrev().getPrev().setInstruction(fa);
	    i.getPrev().setInstruction(new ALOAD(maxLocals-3));		// push outstanding spawns
	    i.setInstruction(new ACONST_NULL());			// push null
	    i = i.getNext();
	} else {
	    i.getPrev().setInstruction(fa);
	    i.setInstruction(new ALOAD(maxLocals-3));			// push outstanding spawns
	    i = i.getNext();
	    il.insert(i, new ACONST_NULL());				// push null
	}

	// and call Satin.abort
	il.insert(i, ab);

	// all jobs were killed, set outstandingSpawns to null
	il.insert(i, new ACONST_NULL());				// push null
	il.insert(i, new ASTORE(maxLocals-3));				// write
    }

    void rewriteSync(Method m, InstructionList il, InstructionHandle i, int maxLocals) {
	int ifnullPos = -1;
	BranchHandle firstJumpPos = null;
	InstructionHandle pos = null;
	Type returnType = getReturnType(m);

	if (verbose) {
	    System.out.println("rewriting sync, class = " + c);
	}

	if (idTable.size() == 0) {
	    System.err.println("Error: sync without spawn");
	    System.exit(1);
	}

	Instruction sync_invocation = ins_f.createInvoke("ibis.satin.Satin",
							 "sync",
							 Type.VOID,
							 new Type[] { spawnCounterType },
							 Constants.INVOKEVIRTUAL);
	Instruction satin_field_access = ins_f.createFieldAccess(mainClassname,
								 "satinClass",
								 satinType,
								 Constants.GETSTATIC);

	if (mtab.containsInlet(m)) {
	    deleteIns(il, i.getPrev().getPrev(), i.getPrev());
	}

	i.getPrev().setInstruction(satin_field_access);
	i.setInstruction(new ALOAD(maxLocals));

	i = i.getNext();							// so that we can insert ...

	// and call Satin.sync 
	il.insert(i, sync_invocation);

	firstJumpPos = il.insert(i, new GOTO(null));
	pos = i;

	//*** Loop code. ***

	// Push curr = outstandingSpawns. 
	il.insert(pos, new ALOAD(maxLocals+1));
	il.insert(pos, new DUP());
	il.insert(pos, new ASTORE(maxLocals+2));

	// outstandingSpawns = outstandingSpawns.next 
	il.insert(pos, ins_f.createFieldAccess("ibis.satin.InvocationRecord",
					       "cacheNext",
					       irType,
					       Constants.GETFIELD));
	il.insert(pos, new ASTORE(maxLocals+1));

	InstructionHandle[] jumpTargets = new InstructionHandle[idTable.size()];
	BranchHandle[] ifcmps = new BranchHandle[idTable.size()];
	BranchHandle[] gotos = new BranchHandle[idTable.size()];

	// loop over all ids handed out in this method 
	for (int k=0; k<idTable.size(); k++) {
	    String invClass = invocationRecordName(getStoreTarget(k));
	    Type target_returntype = getReturnType(getStoreTarget(k));

	    // Now generate code to test the id, and do the assignment to the result variable. 
	    // The previous ifnull jumps here.
	    if (idTable.size() > 1) {
		il.insert(pos, new ALOAD(maxLocals+2));
		
		if (k > 0) {
		    jumpTargets[k-1] = pos.getPrev();
		}

		il.insert(pos, ins_f.createFieldAccess("ibis.satin.InvocationRecord",
						       "storeId",
						       Type.INT,
						       Constants.GETFIELD));

		// push id value 
		il.insert(pos, new BIPUSH((byte)k));

		// do compare 
		ifcmps[k] = il.insert(pos, new IF_ICMPNE(null));
	    }

	    // assign result

	    il.insert(pos, new ALOAD(maxLocals+2));
	    il.insert(pos, ins_f.createCheckCast(new ObjectType(invClass)));
	    // store to variable that is supposed to contain result 
	    if (isArrayStore(getStoreIns(k))) { // array, maxLocals+3 = temp, cast to correct invocationRecord type
		il.insert(pos, new DUP());
		il.insert(pos, new DUP());
		il.insert(pos, new ASTORE(maxLocals+3));
		il.insert(pos, ins_f.createFieldAccess(invClass,
						       "array",
						       new ArrayType(returnType, 1),
						       Constants.GETFIELD));
		
		il.insert(pos, new ALOAD(maxLocals+3));
		il.insert(pos, ins_f.createFieldAccess(invClass,
						       "index",
						       Type.INT,
						       Constants.GETFIELD));
		il.insert(pos, new ALOAD(maxLocals+3));
		il.insert(pos, ins_f.createFieldAccess(invClass,
						       "result",
						       target_returntype,
						       Constants.GETFIELD));
		il.insert(pos, getStoreIns(k));
	    } else { // not an array. field or local.
		if (getStoreIns(k) != null) { // local
		    if (getLoadIns(k) == null) {
			il.insert(pos, new DUP());
			il.insert(pos, ins_f.createFieldAccess(invClass,
							       "result",
							       target_returntype,
							       Constants.GETFIELD));
			il.insert(pos, getStoreIns(k));
		    } else { // we have a putfield, maxLocals+3 = temp, cast to correct invocationRecord type
			il.insert(pos, new ASTORE(maxLocals+3));
			il.insert(pos, getLoadIns(k));
			il.insert(pos, new ALOAD(maxLocals+3));
			il.insert(pos, ins_f.createFieldAccess(invClass,
							       "result",
							       target_returntype,
							       Constants.GETFIELD));
			il.insert(pos, getStoreIns(k));
			il.insert(pos, new ALOAD(maxLocals+3));
		    }
		}
	    }

	    il.insert(pos, ins_f.createInvoke(invClass,
					    "delete",
					    Type.VOID,
					    new Type[] { new ObjectType(invocationRecordName(getStoreTarget(k))) },
					    Constants.INVOKESTATIC));

	    if (k != idTable.size()-1) {
		gotos[k] = il.insert(pos, new GOTO(pos));
	    }
	}

	// Outer loop test, the first goto jumps here. 
	// The previous if_icmp_ne also jumps here. 
	il.insert(pos, new ALOAD(maxLocals+1));

	firstJumpPos.setTarget(pos.getPrev());
	jumpTargets[idTable.size()-1] = pos.getPrev();

	if (idTable.size() > 1) {
	    for (int k=0; k<idTable.size(); k++) {
		ifcmps[k].setTarget(jumpTargets[k]);
		if (k != idTable.size()-1) {
		    gotos[k].setTarget(pos.getPrev());
		}
	    }
	}

	// jump back to start op loop 
	il.insert(pos, new IFNONNULL(firstJumpPos.getNext()));

	// only for aborts: add an if when this job may be aborted
	if (supportAborts) {
	    if (verbose) {
		System.out.println("outputting post-sync aborted check for " + m);
	    }

	    InstructionHandle abo = insertNullReturn(m, il, pos);

	    il.insert(abo, ins_f.createFieldAccess(mainClassname,
						   "satinClass",
						   satinType,
						   Constants.GETSTATIC));
	    il.insert(abo, ins_f.createFieldAccess("ibis.satin.Satin",
						   "parent",
						   irType,
						   Constants.GETFIELD));

	    // test for null (root job)
	    il.insert(abo, new IFNULL(pos));

	    il.insert(abo, ins_f.createFieldAccess(mainClassname,
						   "satinClass",
						   satinType,
						   Constants.GETSTATIC));
	    il.insert(abo, ins_f.createFieldAccess("ibis.satin.Satin",
						   "parent",
						   irType,
						   Constants.GETFIELD));

	    il.insert(abo, ins_f.createFieldAccess("ibis.satin.InvocationRecord",
						   "aborted",
						   Type.BOOLEAN,
						   Constants.GETFIELD));
	    il.insert(abo, new IFEQ(pos));

/*
////@@@@@@@@@@2 this needs fixing :-(
	    // Test for parent.eek, if non-null, throw it (exception in inlet).
	    il.insert(abo, ins_f.createFieldAccess(mainClassname,
						   "satinClass",
						   satinType,
						   Constants.GETSTATIC));
	    il.insert(abo, ins_f.createFieldAccess("ibis.satin.Satin",
						   "parent",
						   irType,
						   Constants.GETFIELD));
	    il.insert(abo, ins_f.createFieldAccess("ibis.satin.InvocationRecord",
						   "eek",
						   new ObjectType("java.lang.Throwable"),
						   Constants.GETFIELD));
	    il.insert(abo, new IFNULL(abo));
	    il.insert(abo, ins_f.createFieldAccess(mainClassname,
						   "satinClass",
						   satinType,
						   Constants.GETSTATIC));
	    il.insert(abo, ins_f.createFieldAccess("ibis.satin.Satin",
						   "parent",
						   irType,
						   Constants.GETFIELD));
	    il.insert(abo, ins_f.createFieldAccess("ibis.satin.InvocationRecord",
						   "eek",
						   new ObjectType("java.lang.Throwable"),
						   Constants.GETFIELD));

	    il.insert(abo, new ATHROW());
*/

	}
    }

    InstructionHandle insertNullReturn(Method m, InstructionList il, InstructionHandle pos) {
	Type returnType = getReturnType(m);
	InstructionHandle retval;

	if (returnType instanceof ReferenceType) {
	    // a reference type
	    retval = il.insert(pos, new ACONST_NULL());
	    il.insert(pos, new ARETURN());
	} else if (returnType.equals(Type.VOID)) {
	    retval = il.insert(pos, new RETURN());
	} else if (returnType.equals(Type.FLOAT)) {
	    retval = il.insert(pos, new FCONST((float)0));
	    il.insert(pos, new FRETURN());
	} else if (returnType.equals(Type.DOUBLE)) {
	    retval = il.insert(pos, new DCONST(0.0));
	    il.insert(pos, new DRETURN());
	} else if (returnType.equals(Type.LONG)) {
	    retval = il.insert(pos, new LCONST(0));
	    il.insert(pos, new LRETURN());
	} else { // boolean, byte, char short or int
	    retval = il.insert(pos, new ICONST(0));
	    il.insert(pos, new IRETURN());
	}
	return retval;
    }


    InstructionHandle getFirstParamPushPos(InstructionList il, InstructionHandle i) {
	int paramsOnStack = i.getInstruction().consumeStack(cpg) - i.getInstruction().produceStack(cpg);

	if (verbose) {
	    System.out.println("Expected params for " + i.getInstruction() + " is " + paramsOnStack);
	}
	
	InstructionHandle k = i.getPrev();
	int pushed = 0;

	do {
	    pushed += k.getInstruction().produceStack(cpg) - k.getInstruction().consumeStack(cpg);
	    k = k.getPrev();
	} while (pushed < paramsOnStack);
	
	return k;
    }

    static private void deleteIns(InstructionList il, InstructionHandle ih, InstructionHandle new_target) {
// System.out.println("deleteIns: instructionList = " + il);
// System.out.println("   handle = " + ih);
	try {
	    il.delete(ih);
	}
	catch (TargetLostException e) {
	    InstructionHandle[] targets = e.getTargets();
	    for (int i=0; i < targets.length; i++) {
		InstructionTargeter[] targeters = targets[i].getTargeters();
     
		for(int j=0; j < targeters.length; j++) {
		    targeters[j].updateTarget(targets[i], new_target);
		}
            }
	}
    }

    static private void deleteIns(InstructionList il, InstructionHandle a, InstructionHandle b, InstructionHandle new_target) {
	try {
	    il.delete(a, b);
	}
	catch (TargetLostException e) {
	    InstructionHandle[] targets = e.getTargets();
	    for (int i=0; i < targets.length; i++) {
		InstructionTargeter[] targeters = targets[i].getTargeters();
     
		for(int j=0; j < targeters.length; j++) {
		    targeters[j].updateTarget(targets[i], new_target);
		}
            }
	}
    }

    Instruction getAndRemoveLoadIns(InstructionList il, InstructionHandle i) {
	InstructionHandle loadPos = getFirstParamPushPos(il, i).getPrev();

	Instruction res = loadPos.getInstruction();
	deleteIns(il, loadPos, loadPos.getNext());

	return res;
    }

    boolean isArrayStore(Instruction ins) {
	if (ins == null) {
	    return false;
	}
	if (ins instanceof ArrayInstruction && ins instanceof StackConsumer) {
	    return true;
	}
	
	return false;
    }


    void rewriteSpawn(MethodGen m, InstructionList il, Method target, InstructionHandle i, int maxLocals, int spawnId) {

	if (verbose) {
	    System.out.println("rewriting spawn, target = " + target.getName() + ", sig = " + target.getSignature());
	}

	Instruction storeIns = null;
	Instruction loadIns = null;

	// A spawned method invocation. Target and parameters are already on the stack.
	// Push spawnCounter, outstandingSpawns, and the id for the result. 
	// Then call getNewInvocationRecord 
	// Remove the original invocation and the store of the result. 
	
	// Keep the store instruction, and remove it from the instruction vector. 
	// We must give this store instruction an method-unique id. 
	
	Type[] params = getArgumentTypes(target);
	Type returnType = getReturnType(target);
	
	if (! returnType.equals(Type.VOID)) {
	    storeIns = i.getNext().getInstruction();
	    if (storeIns instanceof PUTFIELD) {
		loadIns = getAndRemoveLoadIns(il, i);
	    }
	    deleteIns(il, i.getNext(), i.getNext().getNext());
	}

	int storeId = allocateId(storeIns, loadIns, target);

	// push spawn counter 
	il.insert(i, new ALOAD(maxLocals));

	// push outstandingSpawns 
	il.insert(i, new ALOAD(maxLocals+1));

	// push storeId 
	il.insert(i, new BIPUSH((byte)storeId));

	// push spawnId 
	il.insert(i, new BIPUSH((byte)spawnId));

	// push parentLocals 
	if (getExceptionHandler(m, i) != null) {
	    il.insert(i, new ALOAD(maxLocals-1));
	} else {
	    il.insert(i, new ACONST_NULL());
	}

	// Call getNewInvocationRecord 
	String methodName;
	Type parameters[];
	int ix = 0;

	if (storeIns != null && isArrayStore(storeIns)) {
	    methodName = "getNewArray";
	    parameters = new Type[params.length+8];
	    parameters[ix++] = new ArrayType(returnType, 1);
	    parameters[ix++] = Type.INT;
	}
	else {
	    methodName = "getNew";
	    parameters = new Type[params.length+6];
	}

	parameters[ix++] = new ObjectType(c.getClassName());
	for (int j = 0; j < params.length; j++) {
	    parameters[ix++] = params[j];
	}
	parameters[ix++] = spawnCounterType;
	parameters[ix++] = irType;
	parameters[ix++] = Type.INT;
	parameters[ix++] = Type.INT;
	parameters[ix++] = new ObjectType("ibis.satin.LocalRecord");
	
	i.setInstruction(ins_f.createInvoke(invocationRecordName(target),
				     methodName,
				     new ObjectType(invocationRecordName(target)),
				     parameters,
				     Constants.INVOKESTATIC));

	// Store result in outstandingSpawns 
	i = il.append(i, new ASTORE(maxLocals+1));

	// Now, we call Satin.spawn(outstandingSpawns) 
	
	// push s 
	i = il.append(i, ins_f.createFieldAccess(mainClassname,
					  "satinClass",
					  satinType,
					  Constants.GETSTATIC));
	
	// push outstandingSpawns 
	i = il.append(i, new ALOAD(maxLocals+1));
	
	// and call Satin.spawn 
	i = il.append(i, ins_f.createInvoke("ibis.satin.Satin",
				     "spawn",
				     Type.VOID,
				     new Type[] { irType },
				     Constants.INVOKEVIRTUAL));
    }

    /* replace store by pop, load by const push */
    void removeUnusedLocals(Method mOrig, MethodGen m) {
	InstructionList il = m.getInstructionList();
	InstructionHandle[] ins = il.getInstructionHandles();
	for (int i=0; i<ins.length; i++) {
	    Instruction in = ins[i].getInstruction();

	    if (in instanceof LocalVariableInstruction) {
		LocalVariableInstruction curr = (LocalVariableInstruction) in;
		if (curr.getIndex() < m.getMaxLocals()-5 && !mtab.isLocalUsedInInlet(mOrig, curr.getIndex())) {
		    if (curr instanceof IINC) {
		        ins[i].setInstruction(new NOP());
		    }
		    else if (curr instanceof LSTORE || curr instanceof DSTORE) {
			ins[i].setInstruction(new POP2());
		    }
		    else if (curr instanceof StoreInstruction) {
			ins[i].setInstruction(new POP());
		    }
		    else if (curr instanceof ALOAD) {
			ins[i].setInstruction(new ACONST_NULL());
		    }
		    else if (curr instanceof FLOAD) {
			ins[i].setInstruction(new FCONST((float) 0.0));
		    }
		    else if (curr instanceof ILOAD) {
			ins[i].setInstruction(new ICONST(0));
		    }
		    else if (curr instanceof DLOAD) {
			ins[i].setInstruction(new DCONST(0.0));
		    }
		    else if (curr instanceof LLOAD) {
			ins[i].setInstruction(new LCONST(0L));
		    }
		    else {
			System.out.println("unhandled ins in removeUnusedLocals: " + curr);
			System.exit(1);
		    }
		}
	    }
	}
    }

    void initSpawnTargets(InstructionList il) {
	for (int i=0; i<idTable.size(); i++) {
	    Instruction store = getStoreIns(i);

	    if (store == null) {
		continue;
	    }

	    if (isArrayStore(store)) {
		continue;
	    }

	    if (store instanceof LSTORE) {
		il.insert(new LCONST(0));
		il.append(il.getStart(), store);
	    }
	    else if (store instanceof ISTORE) {
		il.insert(new ICONST(0));
		il.append(il.getStart(), store);
	    }
	    else if (store instanceof FSTORE) {
		il.insert(new FCONST((float) 0.0));
		il.append(il.getStart(), store);
	    }
	    else if (store instanceof DSTORE) {
		il.insert(new DCONST(0.0));
		il.append(il.getStart(), store);
	    }
	    else if (store instanceof ASTORE) {
		il.insert(new ACONST_NULL());
		il.append(il.getStart(), store);
	    }
	    else if (store instanceof PUTFIELD) {
		// no need to init.
	    }
	    else {
		System.err.println("Unhandled store instruction in initSpawnTargets, opcode = " + store.getOpcode());
		//				System.exit(1);
	    }
	}
    }

    public Method findMethod(InvokeInstruction ins) {
	String name = ins.getMethodName(cpg);
	String sig  = ins.getSignature(cpg);
	String cls  = ins.getClassName(cpg);

	JavaClass cl = Repository.lookupClass(cls);
	
	if (cl == null) {
System.out.println("findMethod: could not find class " + cls);
	    return null;
	}

	while (cl != null) {
	    Method[] methods = cl.getMethods();
	    for (int i = 0; i < methods.length; i++) {
		if (methods[i].getName().equals(name) &&
		    methods[i].getSignature().equals(sig)) {
		    return methods[i];
		}
	    }
	    cls = cl.getSuperclassName();
	    if (cls != null) {
		cl = Repository.lookupClass(cls);
	    }
	    else cl = null;
	}
System.out.println("findMethod: could not find method " + name + sig);
	return null;
    }

    public JavaClass findMethodClass(InvokeInstruction ins) {
	String name = ins.getMethodName(cpg);
	String sig  = ins.getSignature(cpg);
	String cls  = ins.getClassName(cpg);

	JavaClass cl = Repository.lookupClass(cls);
	
	if (cl == null) {
System.out.println("findMethod: could not find class " + cls);
	    return null;
	}

	while (cl != null) {
	    Method[] methods = cl.getMethods();
	    for (int i = 0; i < methods.length; i++) {
		if (methods[i].getName().equals(name) &&
		    methods[i].getSignature().equals(sig)) {
		    return cl;
		}
	    }
	    cls = cl.getSuperclassName();
	    if (cls != null) {
		cl = Repository.lookupClass(cls);
	    }
	    else cl = null;
	}
System.out.println("findMethod: could not find method " + name + sig);
	return null;
    }

    void rewriteMethod(Method mOrig, MethodGen m) {
	int spawnId = 0;

	if (verbose) {
	    System.out.println("method " + mOrig + " contains a spawned call, rewriting");
	}

	clearIdTable();

	InstructionList il = m.getInstructionList();
	int maxLocals = m.getMaxLocals();
	InstructionHandle  insertAllocPos = null;
	InstructionHandle[] ih = il.getInstructionHandles();

	if (verbose) {
	    System.out.println("maxLocals = " + maxLocals);
	}
	// optimization:
	// find first spawn, then look if there is a jump before the spawn that jumps over it...
        // this avoids alloccing and deleting spawn counters before a spawn hapens (e.g. with thresholds)
	if (spawnCounterOpt) {
	    for (int i = 0; i < ih.length; i++) {
		if (ih[i].getInstruction() instanceof INVOKEVIRTUAL) {
		    Method target = findMethod((INVOKEVIRTUAL) (ih[i].getInstruction()));
		    if (mtab.isSpawnable(target)) {
			for (int j = 0; j < i; j++) {
			    if (ih[j] instanceof BranchHandle) {
				InstructionHandle jumpTarget = ((BranchHandle)(ih[j])).getTarget();
				boolean found = false;
				for (int k = 0; k < i; k++) {
				    if (ih[k] == jumpTarget) {
					found = true;
					break;
				    }
				}
				if (! found) {
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

	// Allocate a spawn counter at the start of the method, local slot is maxLocals 
	il.insert(insertAllocPos, ins_f.createInvoke("ibis.satin.Satin",
				     "newSpawnCounter",
				     spawnCounterType,
				     Type.NO_ARGS,
				     Constants.INVOKESTATIC));
	il.insert(insertAllocPos, new ASTORE(maxLocals));

	// Allocate and init outstandingSpawns at slot maxLocals+1 
	il.insert(insertAllocPos, new ACONST_NULL());
	il.insert(insertAllocPos, new ASTORE(maxLocals+1));

	// Allocate and init curr at slot maxLocals+2 
	il.insert(insertAllocPos, new ACONST_NULL());
	il.insert(insertAllocPos, new ASTORE(maxLocals+2));

	// Allocate and init curr at slot maxLocals+3 
	il.insert(insertAllocPos, new ACONST_NULL());
	il.insert(insertAllocPos, new ASTORE(maxLocals+3));

	for (InstructionHandle i = insertAllocPos; i != null; i = i.getNext()) {
	    if (i.getInstruction() instanceof ReturnInstruction) {
		i = insertDeleteSpawncounter(il, i, maxLocals);
	    }
	    else if (i.getInstruction() instanceof INVOKEVIRTUAL) {
		INVOKEVIRTUAL ins = (INVOKEVIRTUAL)(i.getInstruction());
		Method target = findMethod(ins);
		boolean rewritten = false;

		// Rewrite the sync statement. 
		if (target.getName().equals("sync") &&
		    target.getSignature().equals("()V")) {
		    JavaClass cl = findMethodClass(ins);
		    if (cl != null && cl.equals(satinObjectClass)) {
		        rewriteSync(mOrig, il, i, maxLocals);
			rewritten = true;
		    }
		} 
		if (! rewritten && mtab.isSpawnable(target)) {
		    rewriteSpawn(m, il, target, i, maxLocals, spawnId);
		    spawnId++;
		}
	    }
	}

	initSpawnTargets(il);
    }

    InstructionHandle  pushParams(InstructionList il, Method m) {
	Type[] params = mtab.typesOfParams(m);
	InstructionHandle pos = il.getStart();

	for (int i=0, param=0; i<params.length; i++,param++) {
	    if (params[i].equals(Type.BOOLEAN) ||
	       params[i].equals(Type.BYTE) ||
	       params[i].equals(Type.SHORT) ||
	       params[i].equals(Type.CHAR) ||
	       params[i].equals(Type.INT)) {
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

    InstructionHandle rewriteStore(MethodGen m, InstructionList il, InstructionHandle i, int maxLocals, String localClassName) {
	LocalVariableInstruction curr = (LocalVariableInstruction)(i.getInstruction());
	Type type = mtab.getLocalType(m, curr, i.getPosition());
	String name = mtab.getLocalName(m, curr, i.getPosition());
	String fieldName = mtab.generatedLocalName(type, name);

	i.setInstruction(new ALOAD(maxLocals));
	i = i.getNext();

	if (type.equals(Type.LONG) || type.equals(Type.DOUBLE)) {
	    il.insert(i, new DUP_X2());
	    il.insert(i, new POP());
	} else {
	    il.insert(i, new SWAP());
	}

	i = il.insert(i, ins_f.createFieldAccess(localClassName,
					  fieldName,
					  type,
					  Constants.PUTFIELD));
	return i;
    }

    InstructionHandle rewriteLoad(MethodGen m, InstructionList il, InstructionHandle i, int maxLocals, String localClassName) {
	LocalVariableInstruction curr = (LocalVariableInstruction)(i.getInstruction());
	Type type = mtab.getLocalType(m, curr, i.getPosition());
	String name = mtab.getLocalName(m, curr, i.getPosition());
	String fieldName = mtab.generatedLocalName(type, name);

	i.setInstruction(new ALOAD(maxLocals));
	i = i.getNext();
	i = il.insert(i, ins_f.createFieldAccess(localClassName,
					  fieldName,
					  type,
					  Constants.GETFIELD));

	return i;
    }

    void shiftLocals(InstructionList il, int shift) {
	InstructionHandle[] ih = il.getInstructionHandles();
	for (int i=0; i<ih.length; i++) {
	    Instruction ins = ih[i].getInstruction();
	    if (ins instanceof LocalVariableInstruction) {
		LocalVariableInstruction l = (LocalVariableInstruction) ins;
		l.setIndex(l.getIndex() + shift);
	    }
	}
    }

    InstructionHandle insertTypecheckCode(MethodGen m, InstructionList il, InstructionHandle pos, int spawnId, int exceptionPos) {
	Vector catches = mtab.getCatchTypes(m, spawnId);

	InstructionHandle startPos = pos;
	InstructionHandle[] jumpTargets = new InstructionHandle[catches.size()+1];

	// optimization: if there is only one type of exception, no need to test.
	if (catches.size() == 1) {
	    CodeExceptionGen e = (CodeExceptionGen) (catches.elementAt(0));
	    ObjectType type = e.getCatchType();
	    InstructionHandle catchTarget = e.getHandlerPC();

	    il.insert(pos, new ALOAD(exceptionPos));
	    il.insert(pos, ins_f.createCheckCast(type));
	    il.insert(pos, new GOTO(catchTarget));
	    return pos;
	}

	BranchHandle[] jumps = new BranchHandle[catches.size()];

	for (int i=0; i<catches.size(); i++) {
	    CodeExceptionGen e = (CodeExceptionGen) (catches.elementAt(i));
	    ObjectType type = e.getCatchType();
	    InstructionHandle catchTarget = e.getHandlerPC();
	    
	    jumpTargets[i] = il.insert(pos, new ALOAD(exceptionPos));
	    il.insert(pos, new INSTANCEOF(cpg.addClass(type)));
	    il.insert(pos, new BIPUSH((byte)1));
	    jumps[i] = il.insert(pos, new IF_ICMPNE(null));
	    il.insert(pos, new ALOAD(exceptionPos));
	    il.insert(pos, ins_f.createCheckCast(type));
	    il.insert(pos, new GOTO(catchTarget));
	}

	jumpTargets[catches.size()] = pos;

	for (int i=0; i<catches.size(); i++) {
	    jumps[i].setTarget(jumpTargets[i+1]);
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

	MethodGen m = new MethodGen(mOrig, c.getClassName(), cpg);
	m.setArgumentTypes(new Type[] { Type.INT,
					new ObjectType(localRecordName(mOrig)),
					new ObjectType("java.lang.Throwable"),
					irType } );

	m.setName("exceptionHandlingClone_" + mOrig.getName());

	InstructionList il = m.getInstructionList();
	il.setPositions();

	InstructionHandle startLocalPos = il.findHandle(mtab.getStartLocalAlloc(mOrig));

	mtab.addCloneToInletTable(mOrig, m);

	if (inletOpt) {
	    removeUnusedLocals(mOrig, m);
	}

	// Now generate code to restore locals, push the exception and jump
	// to the correct catch block.
	shiftLocals(il, localsShift); // add localsShift to all locals 
				      // (we have localsShift parameters...)

	// At pos 'startPos', the new of the local record starts.
	// Delete it, and replace with assignment from param

	// Load local record
	startLocalPos.setInstruction(new ALOAD(localRecordPos));
	startLocalPos = startLocalPos.getNext();

	// Save record
	startLocalPos.setInstruction(new ASTORE(m.getMaxLocals()-5+localsShift));
	startLocalPos = startLocalPos.getNext();

	// Remove allocation of LocalRecord.
	// The nr of instructions to be removed depends on the number of locals used.

	InstructionHandle x = startLocalPos;

	int insCount = mtab.typesOfParams(mOrig).length;
	for (int i=1; i<insCount; i++) {
	    x = x.getNext();
	}
	InstructionHandle pos = x.getNext();

	deleteIns(il, startLocalPos, x, pos);

	// okidoki, now jump 
	int nrSpawns = mtab.nrSpawns(mOrig);
	InstructionHandle[] spawnIdTable = new InstructionHandle[nrSpawns+1];
	BranchHandle[] jumps = new BranchHandle[nrSpawns+1];
	int nrInlets = 0;
	InstructionHandle startPos = pos;
	for (int i=0; i<nrSpawns; i++) { // loop over all spawnIds in method (i)
	    if (mtab.hasInlet(mOrig, i)) {
		spawnIdTable[nrInlets] = il.insert(pos, new ILOAD(spawnIdPos));
		il.insert(pos, new BIPUSH((byte)i));
		jumps[nrInlets] = il.insert(pos, new IF_ICMPNE(null));
		pos = insertTypecheckCode(m, il, pos, i, exceptionPos);
		nrInlets++;
	    }
	}
	spawnIdTable[nrInlets] = pos;

	for (int i=0; i<nrInlets; i++) {
	    jumps[i].setTarget(spawnIdTable[i+1]);
	}

	m.setMaxLocals();
	m.setMaxStack();
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
	    System.out.println("method " + mOrig + " contains a spawned call and inlet, rewriting to local record");
	}

	String localClassName = localRecordName(m);
	InstructionList il = m.getInstructionList();
	int maxLocals = m.getMaxLocals();

	if (verbose) {
	    System.out.println("maxLocals = " + maxLocals);
	}

	// Allocate Local class.
	InstructionHandle pos = pushParams(il, mOrig);

	Type[] paramtypes = mtab.typesOfParams(mOrig);
	il.insert(pos, ins_f.createInvoke(localClassName,
				     "getNew",
				     new ObjectType(localRecordName(m)),
				     paramtypes,
				     Constants.INVOKESTATIC));

	il.insert(pos, new ASTORE(maxLocals));

	for (InstructionHandle i=pos; i != null; i = i.getNext()) {

	    Instruction ins = i.getInstruction();

	    if (isLocalStore(ins)) {
		LocalVariableInstruction curr = (LocalVariableInstruction) ins;

		if (!inletOpt || mtab.isLocalUsedInInlet(mOrig, curr.getIndex())) {
		    if (verbose) {
			System.out.println(m + ": rewriting local " + curr.getIndex());
		    }
		    i = rewriteStore(m, il, i, maxLocals, localClassName);
		} else {
		    if (verbose) {
			System.out.println(m + ": NOT rewriting local " + curr.getIndex());
		    }
		}
	    }
	    else if (isLocalLoad(ins)) {
		LocalVariableInstruction curr = (LocalVariableInstruction) ins;

		if (!inletOpt || mtab.isLocalUsedInInlet(mOrig, curr.getIndex())) {
		    if (verbose) {
			System.out.println(m + ": rewriting local " + curr.getIndex());
		    }
		    i = rewriteLoad(m, il, i, maxLocals, localClassName);
		} else {
		    if (verbose) {
			System.out.println(m + ": NOT rewriting local " + curr.getIndex());
		    }
		}
	    }
	    else if (ins instanceof IINC) {
		IINC curr = (IINC) ins;
		if (!inletOpt || mtab.isLocalUsedInInlet(mOrig, curr.getIndex())) {
		    if (verbose) {
			System.out.println(m + ": rewriting local " + curr.getIndex());
		    }

		    int val = curr.getIncrement();
		    String fieldName = mtab.getLocalName(m, curr, i.getPosition());
		    Type fieldType = mtab.getLocalType(m, curr, i.getPosition());

		    i.setInstruction(new ALOAD(maxLocals));
		    i = i.getNext();
		    il.insert(i, new DUP());

		    il.insert(i, ins_f.createFieldAccess(localClassName,
							 mtab.generatedLocalName(fieldType, fieldName),
							 fieldType,
							 Constants.GETFIELD));
		    il.insert(i, new BIPUSH((byte)val));
		    il.insert(i, new IADD());
		    i = il.insert(i, ins_f.createFieldAccess(localClassName,
							 mtab.generatedLocalName(fieldType, fieldName),
							 fieldType,
							 Constants.PUTFIELD));
		} else {
		    if (verbose) {
			System.out.println(m + ": NOT rewriting local " + curr.getIndex());
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

	    if (il == null) continue;

	    int maxLocals = mg.getMaxLocals();

	    for (InstructionHandle i=il.getStart(); i != null; i=i.getNext()) {
		Instruction ins = i.getInstruction();
		if (ins instanceof INVOKEVIRTUAL) {
		    String targetname = ((InvokeInstruction)ins).getMethodName(cpg);
		    String sig = ((InvokeInstruction)ins).getSignature(cpg);

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

		Method newm = mg.getMethod();
		mtab.replace(m, newm);
		gen_c.setMethodAt(newm, j);
	    }

	}
    }

    void removeFile(String name) {
	if (verbose) {
	    System.out.println("removing " + name);
	}

	try {
	    File f = new File(name);
	    f.delete();
	} catch (Exception e) {
	    System.err.println("Warning: could not remove " + name);
	}
    }

    void compileGenerated(String className) {
	try {
	    String command = compiler + " " + className + ".java";
	    if (verbose) {
		System.out.println("Running: " + command);
	    }
	
	    Runtime r = Runtime.getRuntime();
	    Process p = r.exec(command);
	    int res = p.waitFor();
	    if (res != 0) {
		System.err.println("Error compiling generated code (" + className + ").");
		System.exit(1);
	    }
	    if (verbose) {
		System.out.println("Done");
	    }
	    Repository.lookupClass(className);
	} catch (Exception e) {
	    System.err.println("IO error: " + e);
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    // Rewrite method invocations to spawned method invocations.
    // There is a chicken-and-egg problem here. The generated LocalRecord depends on
    // the rewritten bytecode, and vice versa. The solution is to first generate a dummy
    // method in the localRecord, and in the second pass write the real method, and
    // compile the whole class again.
    void generateInvocationRecords(String base) throws IOException {
	Method[] methods = gen_c.getMethods();

	for (int i=0; i<methods.length; i++) {
	    if (mtab.containsInlet(methods[i])) {
		if (verbose) {
		    System.out.println(methods[i] + ": code contains an inlet");
		}
		
		writeLocalRecord(methods[i], base, false);
		compileGenerated(localRecordName(methods[i]));
		
		if (!keep) { // remove generated files 
		    removeFile(invocationRecordName(methods[i]) + ".java");
		}
	    }
	    
	    if (mtab.isSpawnable(methods[i])) {
		writeInvocationRecord(methods[i], base);
		writeReturnRecord(methods[i], base);
		
		compileGenerated(invocationRecordName(methods[i]));
		if (!keep) { // remove generated files 
		    removeFile(invocationRecordName(methods[i]) + ".java");
		}
		
		compileGenerated(returnRecordName(methods[i]));
		if (!keep) { // remove generated files 
		    removeFile(returnRecordName(methods[i]) + ".java");
		}
	    }
	}
    }

    void insertReturnPop(Method m, InstructionList il) {
	Type returnType = getReturnType(m);

	if (returnType.equals(Type.DOUBLE) || returnType.equals(Type.LONG)) {
	    il.append(new POP2());
	} else if (returnType.equals(Type.VOID)) {
	    // do nothing
	} else { 
	    il.append(new POP());
	}
    }

    void regenerateLocalRecord(String base) {
	Method[] methods = gen_c.getMethods();

	for (int i = 0; i < methods.length; i++) {
	    Method m = methods[i];

	    if (!mtab.isClone(m) && mtab.containsInlet(m)) {
		String local_record_name = localRecordName(m);

		Method clone = mtab.getExceptionHandlingClone(m);
		JavaClass localRec = Repository.lookupClass(local_record_name);

		ClassGen recgen = new ClassGen(localRec);

		Method exceptionHandler = recgen.containsMethod("handleException", "(ILjava/lang/Throwable;Libis/satin/InvocationRecord;)V");
		MethodGen handler_g = new MethodGen(exceptionHandler, local_record_name, recgen.getConstantPool());

		InstructionList il = handler_g.getInstructionList();
		InstructionFactory ins_f = new InstructionFactory(recgen);

		if (verbose) {
		    System.out.println(m + ":code contained an inlet, REwriting localrecord, clone = " + clone);
		}

		InstructionHandle old_end = il.getEnd();

		if (! clone.isStatic()) {
		    // push this
		    String thisName = mtab.getParamName(m, 0);
		    Type thisType = mtab.getParamType(m, 0);
		    String thisFieldName = mtab.generatedLocalName(thisType, thisName);

		    il.append(new ALOAD(0));
		    il.append(ins_f.createFieldAccess(local_record_name,
						   thisFieldName,
						   thisType,
						   Constants.GETFIELD));
	 	}

		// push spawnId, push localrecord, push exception, push parent then invoke static/virtual
		il.append(new ILOAD(1));
		il.append(new ALOAD(0));
		il.append(new ALOAD(2));
		il.append(new ALOAD(3));

		il.append(ins_f.createInvoke(c.getClassName(),
					  clone.getName(),
					  getReturnType(clone),
					  getArgumentTypes(clone),
					  clone.isStatic() ? Constants.INVOKESTATIC : Constants.INVOKEVIRTUAL));
		insertReturnPop(m, il);
		il.append(new RETURN());

		deleteIns(il, old_end, old_end.getNext());	// remove return

		handler_g.setMaxLocals();
		handler_g.setMaxStack();
		
		Method newHandler = handler_g.getMethod();
		recgen.replaceMethod(exceptionHandler, newHandler);

		JavaClass newclass = recgen.getJavaClass();

		Repository.removeClass(localRec);
		Repository.addClass(newclass);
		
		String dst = "";

		try {
		    if (local) {
			String src = newclass.getSourceFileName();
			dst = src.substring(0, src.indexOf(".")) + ".class";
		    } else {
			String classname = newclass.getClassName();
			dst = classname.replace('.', '/') + ".class";
		    }
		    newclass.dump(dst);
		} catch (IOException e) {
		    System.out.println("error writing " + dst);
		    System.exit(1);
		}

	        if (! do_verify(newclass)) failed_verification = true;
	    }
	}
    }

    void writeLocalRecord(Method m, String basename, boolean finalPass) throws IOException {
	String name = localRecordName(m);
	if (verbose) {
	    System.out.println("writing localrecord code to " + name + ".java");
	}

	FileOutputStream f = new FileOutputStream(name + ".java");
	BufferedOutputStream b = new BufferedOutputStream(f);
	PrintStream out = new PrintStream(b);

	out.println("final class " + name + " extends ibis.satin.LocalRecord {");
	out.println("    static " + name + " cache;");

	String[] allLvs = mtab.getAllLocalDecls(m);

	for (int i=0; i<allLvs.length; i++) {
		out.println("    " + allLvs[i]);
	}
	out.println();

        // generate constructor, all parameters to the call must be copied.
	// locals are not initialized yet, so no need to copy them.
	Type[] params = mtab.typesOfParams(m);

	// ctor 
	out.print("    " + name + "(");

	for (int i=0; i<params.length; i++) {
	    String paramName = mtab.getParamName(m, i);

	    out.print(params[i] + " " + mtab.generatedLocalName(params[i], paramName));
	    if (i != params.length-1) {
		out.print(", ");
	    }
	}
	out.println(") {");

	for (int i=0; i<params.length; i++) {
		String paramName = mtab.getParamName(m, i);

		out.println("        this." + mtab.generatedLocalName(params[i], paramName) + 
		        " = " + mtab.generatedLocalName(params[i], paramName) + ";");
	}

	out.println("    }\n");

	// cache
	out.print("    static " + name + " getNew(");

	for (int i=0; i<params.length; i++) {
	    String paramName = mtab.getParamName(m, i);

	    out.print(params[i] + " " + mtab.generatedLocalName(params[i], paramName));
	    if (i != params.length-1) {
		out.print(", ");
	    }
	}
	out.println(") {");

	out.println("        if (cache == null) {");
	out.print("            return new " + name + "(");
	for (int i=0; i<params.length; i++) {
	    String paramName = mtab.getParamName(m, i);

	    out.print( mtab.generatedLocalName(params[i], paramName));
	    if (i != params.length-1) {
		out.print(", ");
	    }
	}
	out.println(");");
	out.println("        }");

	out.println("        " + name + " result = cache;");
	out.println("        cache = (" + name + ") cache.next;");

	for (int i=0; i<params.length; i++) {
		String paramName = mtab.getParamName(m, i);

		out.println("        result." + mtab.generatedLocalName(params[i], paramName) + 
		        " = " + mtab.generatedLocalName(params[i], paramName) + ";");
	}

	out.println("        result.next = null;");
	out.println("        return result;");
	out.println("    }\n");

	// delete
	out.println("    static void delete(" + name + " curr) {");

	// wipe fields for gc
	Type[] ltypes = mtab.getAllLocalTypes(m);
	String[] lnames = mtab.getAllLocalNames(m);

	for (int i=0; i<ltypes.length; i++) {
		if (ltypes[i] instanceof ReferenceType) {
		    out.println("        curr." + lnames[i] + " = null;");
		}
	}

	out.println("        curr.next = cache;");
	out.println("        cache = curr;");
	out.println("    }\n");

	// generate a method that runs the clone in case of exceptions 
	out.println("    public void handleException(int spawnId, Throwable t, ibis.satin.InvocationRecord parent) {");
	out.println("        if (ibis.satin.Config.INLET_DEBUG) System.out.println(\"handleE: spawnId = \" + spawnId + \", t = \" + t + \", parent = \" + parent + \", this = \" + this);");
	// This will later be replaced with call to exception handler
	out.println("    }");

	out.println("}");
	out.close();
    }

    void writeInvocationRecord(Method m, String basename) throws IOException {
	String name = invocationRecordName(m);
	if (verbose) {
	    System.out.println("writing invocationrecord code to " + name + ".java");
	}

	FileOutputStream f = new FileOutputStream(name + ".java");
	BufferedOutputStream b = new BufferedOutputStream(f);
	PrintStream out = new PrintStream(b);
	//		PrintStream out = System.err;
	Type[] params = mtab.typesOfParamsNoThis(m);

	Type returnType = getReturnType(m);

	out.println("import ibis.satin.*;\n");
	out.println("final class " + name + " extends InvocationRecord {");

	// fields 
	out.println("    " + c.getClassName() + " self;");
	for (int i=0; i<params.length; i++) {
	    out.println("    " + params[i] + " param" + i + ";");
	}

	// result 
	if (! returnType.equals(Type.VOID)) {
	    out.println("    transient " + returnType + " result;");
	    out.println("    transient int index;");
	    out.println("    transient " + returnType + "[] array;");
	}

	if (invocationRecordCache) {
	    out.println("    static " + name + " invocationRecordCache;");
	}
	out.println();
	
	// ctor 
	out.print("    " + name + "(");
	out.print(c.getClassName() + " self, ");
	for (int i=0; i<params.length; i++) {
	    out.print(params[i] + " param" + i + ", ");
	}
	out.println("SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {");
	out.println("        super(s, next, storeId, spawnId, parentLocals);");
	out.println("        this.self = self;");

	for (int i=0; i<params.length; i++) {
	    out.println("        this.param" + i + " = param" + i + ";");
	}

	out.println("    }\n");

	// getNew method 
	out.print("    static " + name + " getNew(");
	out.print(c.getClassName() + " self, ");
	for (int i=0; i<params.length; i++) {
	    out.print(params[i] + " param" + i + ", ");
	}
	out.println("SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {");

	if (invocationRecordCache) {
	    out.println("        if (invocationRecordCache == null) {");
	}
	out.print("            return new " + name + "(self, ");
	for (int i=0; i<params.length; i++) {
	    out.print(" param" + i + ", ");
	}
	out.println("s, next, storeId, spawnId, parentLocals);");
	if (invocationRecordCache) {
	    out.println("        }\n");

	    out.println("        " + name + " res = invocationRecordCache;");
	    out.println("        invocationRecordCache = (" + name + ") res.cacheNext;");
	    out.println("        res.self = self;");
	    for (int i=0; i<params.length; i++) {
		out.println("        res.param" + i + " = param" + i + ";");
	    }
	    out.println("        res.spawnCounter = s;");
	    out.println("        res.cacheNext = next;");
	    out.println("        res.storeId = storeId;");

	    out.println("        if (ibis.satin.Config.ABORTS) {");
	    out.println("                res.spawnId = spawnId;");
	    out.println("                res.parentLocals = parentLocals;");
	    out.println("        }");

	    out.println("        return res;");
	}
	out.println("    }\n");

	// getNew method for arrays 
	if (! returnType.equals(Type.VOID)) {
	    out.print("    static " + name + " getNewArray(");
	    out.print(returnType + "[] array, int index, ");
	    out.print(c.getClassName() + " self, ");
	    for (int i=0; i<params.length; i++) {
		out.print(params[i] + " param" + i + ", ");
	    }
	    out.println("SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {");
	    out.print("            " + name + " res = getNew(self, ");
	    for (int i=0; i<params.length; i++) {
		out.print(" param" + i + ", ");
	    }
	    out.println("s, next, storeId, spawnId, parentLocals);");
	    
	    out.println("        res.index = index;");
	    out.println("        res.array = array;");
	    out.println("        return res;");
	    out.println("    }\n");
	}

	// static delete method 
	out.println("    static void delete(" + name + " w) {");
	if (invocationRecordCache) {
	    if (! returnType.equals(Type.VOID)) {
		out.println("        w.array = null;");
	    }
	    // Set everything to null, don't keep references live for gc. 
	    out.println("        w.clear();");
	    out.println("        w.self = null;");
	    
	    for (int i=0; i<params.length; i++) {
		if (isRefType(params[i])) {
		    out.println("        w.param" + i + " = null;");
		}
	    }
	    out.println("        w.cacheNext = invocationRecordCache;");
	    out.println("        invocationRecordCache = w;");
	}
	out.println("    }\n");

/*
        // unused ...
        // delete method (for abort)
        out.println("    public void delete() {");
        if (invocationRecordCache) {
            if (! returnType.equals(Type.VOID)) {
                out.println("        array = null;");
            }
            // Set everything to null, don't keep references live for gc. 
            out.println("        clear();");
            out.println("        self = null;");

            for (int i=0; i<params.length; i++) {
                if (isRefType(params[i])) {
                    out.println("        param" + i + " = null;");
                }
            }
            out.println("        cacheNext = invocationRecordCache;");
            out.println("        invocationRecordCache = this;");
        }
        out.println("    }\n");
*/


	// runLocal method 
	out.println("    public void runLocal() {");
	if (supportAborts) {
	    out.println("        try {");
	}
	if (! returnType.equals(Type.VOID)) {
	    out.print("            result = ");
	}
	out.print("            self." + m.getName() + "(");
	for (int i=0; i<params.length; i++) {
	    out.print("param" + i);
	    if (i != params.length-1) {
		out.print(", ");
	    }
	}
	out.println(");");
	if (supportAborts) {
	    out.println("        } catch (Throwable e) {");
	    out.println("            if (ibis.satin.Config.INLET_DEBUG) System.out.println(\"caught exception in runlocal: \" + e);");
	    out.println("                eek = e;");
	    out.println("        }");

	    out.println("        if (eek != null) {");
	    out.println("            if (ibis.satin.Config.INLET_DEBUG) System.out.println(\"runlocal: calling inlet for: \" + this);");
	    out.println("            parentLocals.handleException(spawnId, eek, this);");
	    out.println("            if (ibis.satin.Config.INLET_DEBUG) System.out.println(\"runlocal: calling inlet for: \" + this + \" DONE\");");
	    out.println("        }");
	}
	out.println("    }\n");

	// runRemote method 
	out.println("    public ibis.satin.ReturnRecord runRemote() {");
	if (supportAborts) {
	    out.println("        Throwable eek = null;");
	}
	if (supportAborts) {
	    if (! returnType.equals(Type.VOID)) {
		out.print("        " + returnType + " result = ");
		out.print(getInitVal(returnType));
		out.println(";");
	    }
	} else {
	    if (! returnType.equals(Type.VOID)) {
		out.println("        " + returnType + " result;");
	    }
	}

	if (supportAborts) {
	    out.println("        try {");
	}
	if (! returnType.equals(Type.VOID)) {
	    out.print("            result = ");
	}
	out.print("            self." + m.getName() + "(");

	for (int i=0; i<params.length; i++) {
	    out.print("param" + i);
	    if (i != params.length-1) {
		out.print(", ");
	    }
	}
	out.println(");");
	if (supportAborts) {
	    out.println("        } catch (Throwable e) {");
	    out.println("            eek = e;");
	    out.println("        }");
	}
	out.print("        return new " + returnRecordName(m));
	if (! returnType.equals(Type.VOID)) {
	    out.println("(result, eek, stamp);");
	} else {
	    out.println("(eek, stamp);");
	}
	out.println("    }");
	out.println("}");

	out.close();
    }

    void writeReturnRecord(Method m, String basename) throws IOException {
	String name = returnRecordName(m);
	if (verbose) {
	    System.out.println("writing returnrecord code to " + name + ".java");
	}

	FileOutputStream f = new FileOutputStream(name + ".java");
	BufferedOutputStream b = new BufferedOutputStream(f);
	PrintStream out = new PrintStream(b);

	Type returnType = getReturnType(m);

	out.println("import ibis.satin.*;\n");
	out.println("final class " + name + " extends ReturnRecord {");
	if (! returnType.equals(Type.VOID)) {
	    out.println("    " + returnType + " result;\n");
	}

	// ctor 
	out.print("    " + name + "(");
	if (! returnType.equals(Type.VOID)) {
	    out.println(returnType + " result, Throwable eek, int stamp) {");
	} else {
	    out.println(" Throwable eek, int stamp) {");
	}

	out.println("        super(eek);");
	if (! returnType.equals(Type.VOID)) {
	    out.println("        this.result = result;");
	}
	out.println("        this.stamp = stamp;");
	out.println("    }\n");

	out.println("    public void assignTo(InvocationRecord rin) {");
	out.println("        " + invocationRecordName(m) + " r = (" +
	        invocationRecordName(m) + ") rin;");
	if (! returnType.equals(Type.VOID)) {
	    out.println("        r.result = result;");
	}
	out.println("        r.eek = eek;");
	out.println("    }");
	out.println("}");

	out.close();
    }

    public void start() {
	if (isSatin()) {
	    if (verbose) {
		System.out.println(c.getClassName() + " is a satin class");
	    }
	}

	if (isRewritten()) {
	    System.out.println(c.getClassName() + " is already rewritten");
	    return;
	}

	// If we have the main method, rename it to origMain. 
	Method main = gen_c.containsMethod("main","([Ljava/lang/String;)V");

	if (main != null) {
	    MethodGen m = new MethodGen(main, c.getClassName(), gen_c.getConstantPool());

	    if (verbose) {
		System.out.println("the class has main, renaming to origMain");
	    }

	    m.setName("origMain");
	    m.setMaxStack();
	    m.setMaxLocals();

	    gen_c.removeMethod(main);

	    main = m.getMethod();

	    gen_c.addMethod(main);

	    FieldGen f = new FieldGen(Constants.ACC_STATIC, satinType, "satinClass", gen_c.getConstantPool());

	    satinField = f.getField();
	    gen_c.addField(satinField);

	    generateMain(gen_c, main);
	} else {
	    satinField = null;
	    Field f[] = mainClass.getFields();
	    for (int i = 0; i < f.length; i++) {
		if (f[i].getName().equals("satinClass")) {
		    satinField = f[i];
		    break;
		}
	    }
	    if (satinField == null) {
		// no main field!
		// we are probably compiling a non-satin application/library then
		if (verbose) {
		    System.err.println("no mainclass, returning");
		}
		return;
	    }
	}

	String src = c.getSourceFileName();
	int index = src.indexOf(".");
	String base = src.substring(0, index);
	String dst = base + ".class";

	mtab = new MethodTable(c, gen_c, this, verbose);

	if (verbose) {
	    mtab.print(System.out);
	}

	try {
	    generateInvocationRecords(base);
	} catch (IOException e) {
	    System.out.println("IO error: " + e);
	    System.exit(1);
	}

	rewriteMethods();

	Repository.removeClass(c);

	gen_c.addInterface("ibis.satin.SatinRewritten");

	c = gen_c.getJavaClass();

	Repository.addClass(c);

	// now overwrite the classfile 
	try {
	    if (! local) {
		dst = c.getPackageName().replace('.', '/') + "/" + dst;
	    }
	    c.dump(dst);
	} catch (IOException e) {
	    System.out.println("Error writing " + dst);
	    System.exit(1);
	}

	regenerateLocalRecord(base);

	Method[] methods = c.getMethods();
	// cleanup
	for (int i=0; i<methods.length; i++) {
	    if (!keep) { // remove generated files 
		if (mtab.containsInlet(methods[i])) {
		    removeFile(localRecordName(methods[i]) + ".java");
		}
	    }
	}

	if (! do_verify(c)) failed_verification = true;

	if (print) {
	    System.out.println(c);
	}
	if (failed_verification) {
	    System.out.println("Verification failed!");
	    System.exit(1);
	}
    }

    public static void usage() {
	System.err.println("Usage : java Satinc [-v] [-keep] [-dir|-local] [-print] [-irc-off] [-no-sc-opt]" +
		   "[-compiler \"your compile command\" ] [-no-aborts] [-no-inlet-opt] <classname> [mainClass]");
	System.exit(1);
    }

    private static boolean do_verify(JavaClass c) {
	Verifier verf = VerifierFactory.getVerifier(c.getClassName());
	boolean verification_failed = false;

// System.out.println("Verifying " + c.getClassName());

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

    public static void main(String[] args) {
	String target = null;
	String mainClass = null;
	boolean verbose = false;
	boolean keep = false;
	boolean local = true;
	boolean print = false;
	boolean invocationRecordCache = true;
	boolean supportAborts = true;
	String compiler = "javac";
	boolean inletOpt = true;
	boolean spawnCounterOpt = true;

	for (int i=0; i<args.length; i++) {
	    if (args[i].equals("-v")) {
		verbose = true;
	    } else if (!args[i].startsWith("-")) {
		if (target == null) {
		    target = args[i];
		} else if (mainClass == null) {
		    mainClass = args[i];
		} else {
		    usage();
		}
	    } else if (args[i].equals("-compiler")) {
		compiler = args[i+1];
		i++;
	    } else if (args[i].equals("-keep")) {
		keep = true;
	    } else if (args[i].equals("-local")) {
		local = false;
	    } else if (args[i].equals("-dir")) {
		local = false;
	    } else if (args[i].equals("-local")) {
		local = true;
	    } else if (args[i].equals("-print")) {
		print = true;
	    } else if (args[i].equals("-irc-off")) {
		invocationRecordCache = false;
	    } else if (args[i].equals("-no-aborts")) {
		supportAborts = false;
	    } else if (args[i].equals("-no-inlet-opt")) {
		inletOpt = false;
	    } else if (args[i].equals("-no-sc-opt")) {
		spawnCounterOpt = false;
	    } else {
		usage();
	    }
	}

	if (target == null) {
	    usage();
	}

	if (mainClass == null) {
	    mainClass = target;
	}

	new Satinc(verbose, local, keep, print, invocationRecordCache, target, mainClass, compiler, supportAborts, inletOpt, spawnCounterOpt).start();
    }
}
