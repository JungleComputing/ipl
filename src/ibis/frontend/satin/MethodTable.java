package ibis.frontend.satin;

import com.ibm.jikesbt.*;   
import ibis.util.BT_Analyzer;
import java.util.Vector;

final class MethodTable implements BT_Opcodes {
	int indexOf(BT_InsVector vec, BT_Ins ins) {
		return vec.indexOf(ins);
	}

	class SpawnTableEntry {
		BT_ExceptionTableEntryVector catchBlocks; /* indexed on spawnId */
		boolean hasInlet;
		boolean[] isLocalUsed;

		SpawnTableEntry() {}

		boolean[] analyseUsedLocals(BT_Method m, BT_ExceptionTableEntry catchBlock, int handlerIndex, 
					    boolean verbose) {
			int maxLocals = m.getCode().maxLocals;
			BT_InsVector ins = m.getCode().ins;
			int end = getEndOfCatchBlock(m, catchBlock, handlerIndex);

			// Start with all false.
			boolean[] used = new boolean[maxLocals];

			if(verbose) {
				System.out.println("analysing used locals for " + m + ", maxLocals = " + maxLocals);
			}

			BT_Ins endIns = ins.elementAt(end);
			if (!Satinc.isRetIns(endIns) && endIns.opcode != opc_athrow) {
				if(verbose) {
					System.out.println("no return at end of inlet, assuming all locals are used");
				}

				// They are all used.
				for(int j=0; j<maxLocals; j++) {
					used[j] = true;
				}
				return used;
			}

			for (int i=handlerIndex; i<ins.size(); i++) {
				BT_Ins curr = ins.elementAt(i);
				if(verbose) {
					System.out.println("ins: " + curr);
				}
				if(curr instanceof BT_JumpIns) {
					int dest = ins.indexOf(((BT_JumpIns)curr).target);
					if(dest < handlerIndex || // backjump out of handler
					   dest > end) { // forward jump beyond catch
						if(verbose) {
							System.out.println("inlet contains a jump to exit, assuming all locals are used");
						}

						// They are all used.
						for(int j=0; j<maxLocals; j++) {
							used[j] = true;
						}
						return used;
					}
				} else if (Satinc.isRetIns(curr) || curr.opcode == opc_athrow) {
					if(verbose) {
						System.out.println("return:");
						for(int k=0; k<used.length; k++) {
							if(!used[k]) {
								System.out.println("RET: local " + k + " is unused");
							}
						}
					}
//					System.out.println("inlet local opt triggered");
					return used;
				} else if (curr instanceof BT_LocalIns) {
					BT_LocalIns l = (BT_LocalIns) curr;
					used[l.localNr] = true;
					if(verbose) {
						System.out.println("just used local " + l.localNr);
					}
				}
			}

			System.out.println("HMM");
			return used;
		}

		SpawnTableEntry(SpawnTableEntry orig, BT_Method m, BT_Method origM) {
			BT_InsVector origVec = origM.getCode().ins;
			BT_InsVector newVec = m.getCode().ins;
			isLocalUsed = new boolean[origM.getCode().maxLocals];

			hasInlet = orig.hasInlet;
			if(hasInlet) {
				/* copy and rewite exception table */
				catchBlocks = new BT_ExceptionTableEntryVector();
				for(int i=0; i<orig.catchBlocks.size(); i++) {
					BT_ExceptionTableEntry origCatch = orig.catchBlocks.elementAt(i);
					int startIndex = origVec.indexOf(origCatch.startPCTarget);
					int endIndex = origVec.indexOf(origCatch.endPCTarget);
					int handlerIndex = origVec.indexOf(origCatch.handlerTarget);
					BT_ExceptionTableEntry newCatch = new BT_ExceptionTableEntry(
						newVec.elementAt(startIndex),
						newVec.elementAt(endIndex),
						newVec.elementAt(handlerIndex),
						origCatch.catchType);
					catchBlocks.addElement(newCatch);
				}
			}
		}
	}

	class MethodTableEntry {
		BT_Method m;
		boolean containsInlet;
		MethodTableEntry clone;
		int nrSpawns;
		SpawnTableEntry[] spawnTable;
		boolean isClone;
		int origNrLocals;
		String[] typesOfParams;
		String[] typesOfParamsNoThis;
		BT_Ins startLocalAlloc;

		MethodTableEntry() {}

		MethodTableEntry(MethodTableEntry orig, BT_Method m) {
			this.m = m;
			containsInlet = orig.containsInlet;
			clone = null;
			nrSpawns = orig.nrSpawns;
			isClone = true;
			origNrLocals = orig.origNrLocals;
			typesOfParams = orig.typesOfParams;
			typesOfParamsNoThis = orig.typesOfParamsNoThis;
			startLocalAlloc = orig.startLocalAlloc;

			spawnTable = new SpawnTableEntry[orig.spawnTable.length];
			for(int i=0; i<spawnTable.length; i++) {
				spawnTable[i] = new SpawnTableEntry(orig.spawnTable[i], m, orig.m);
			}
		}

		void print(java.io.PrintStream out) {
			out.println("Method: " + m);
			out.println("params(" + typesOfParams.length + "): ");
			for(int i=0; i<typesOfParams.length; i++) {
				out.println("    " + typesOfParams[i]);
			}

			out.println("This method contains " + nrSpawns + " spawn(s)");

			if(isClone) {
				out.println("This method is a clone of an inlet method");
			} else {
				out.println("This method is not a clone of an inlet method");
			}

			if(containsInlet) {
				out.println("This method contains an inlet");
			} else {
				out.println("This method does not contain an inlet");
			}
			out.println("---------------------");
		}
	}

	private Vector methodTable; /* a vector of MethodTableEntries */
	private BT_Analyzer analyzer;
	private BT_Class spawnableClass;
	private Satinc self;
	private boolean verbose;
	BT_Class c;

	MethodTable(BT_Class c, Satinc self, boolean verbose) {
		this.verbose = verbose;
		this.self = self;
		this.c = c;

		spawnableClass = BT_Class.forName("ibis.satin.Spawnable");
		analyzer = new BT_Analyzer(c, spawnableClass, verbose);
		analyzer.start();

		methodTable = new Vector();
		for(int i=0; i<c.methods.size(); i++) {
			BT_Method m = c.methods.elementAt(i);
			MethodTableEntry e = new MethodTableEntry();
			e.nrSpawns = calcNrSpawns(m);
			e.spawnTable = new SpawnTableEntry[e.nrSpawns];
			e.m = m;
			if(m.getCode() != null) {
				e.origNrLocals = m.getCode().maxLocals;
			}
			e.typesOfParams = getParamTypesThis(m);
			e.typesOfParamsNoThis = getParamTypesNoThis(m);
			if(m.getCode() != null) {
				fillSpawnTable(m, e);
			}
			methodTable.addElement(e);
		}
	}

	void print(java.io.PrintStream out) {
		out.print("---------------------");
		out.print("metod table of class " + c.useName());
		out.println("---------------------");

		for(int i=0; i<methodTable.size(); i++) {
			MethodTableEntry m = (MethodTableEntry)methodTable.elementAt(i);
			m.print(out);
		}
		out.println("---------------------");
	}

	private void fillSpawnTable(BT_Method m, MethodTableEntry me) {
		BT_CodeAttribute code = m.getCode();
		BT_InsVector ins = code.ins;
		int spawnId = 0;

		for(int k=0; k<ins.size(); k++) {
			int opcode = ins.elementAt(k).opcode;
			if (opcode == opc_invokevirtual) {
				BT_Method target = ins.elementAt(k).getMethodTarget();
				if(analyzer.isSpecial(target)) {
					// we have a spawn!
					analyzeSpawn(m, me, code, ins, ins.elementAt(k), spawnId);
					spawnId++;
				}
			}
		}
	}

	private void analyzeSpawn(BT_Method m, MethodTableEntry me, BT_CodeAttribute code, BT_InsVector ins, 
			  BT_Ins spawnIns, int spawnId) {
		SpawnTableEntry se = me.spawnTable[spawnId] = new SpawnTableEntry();
		se.isLocalUsed = new boolean[code.maxLocals];

                // We have a spawn. Is it in a try block?
		for(int j=0; j<code.exceptions.size(); j++) {
			BT_ExceptionTableEntry e = code.exceptions.elementAt(j);
			
			if(self.comesBefore(code.ins, e.startPCTarget, spawnIns) && 
			   self.comesBefore(code.ins, spawnIns, e.endPCTarget)) {
				/* ok, we have an inlet, add try-catch block info to table */
				me.containsInlet = true;
				se.hasInlet = true;
				se.catchBlocks = new BT_ExceptionTableEntryVector();

				se.catchBlocks.addElement(e);
				if(verbose) {
					System.out.println("spawn " + spawnId + 
							   " with inlet, type = " + e.catchType);
				}

				boolean[] used = se.analyseUsedLocals(m, e, ins.indexOf(e.handlerTarget), verbose);
				for(int k=0; k<used.length; k++) {
					if(used[k]) {
						se.isLocalUsed[k] = true;
					}
				}
			}
		}

		if(verbose) {
			System.out.println(m + ": unused locals in all inlets: ");
			for(int k=0; k<se.isLocalUsed.length; k++) {
				if(!se.isLocalUsed[k]) System.out.println("local " + k + " is unused");
				else System.out.println("local " + k + " is used");
			}
		}
	}

	static String[] getParamTypesThis(BT_Method m) {
		int startIndex = m.useName().indexOf("(") + 1;
		int endIndex = m.useName().indexOf(")");

		String params = m.useName().substring(startIndex, endIndex);
		Vector res = new Vector();
		int end = -1;
		int start = 0;
		if(startIndex != endIndex) {
			while(end != params.length()) {
				int index = params.indexOf(",", start);
				
				if(index > 0) { // more params to follow 
					end = index;
				} else {
					end = params.length();
				}
				
				res.add(params.substring(start, end));
				start = end+1;
			}
		}

		int size = res.size();
		int startPos = 0;
		if(!m.isClassMethod()) {
			size++;
			startPos = 1;
		}

		String[] result = new String[size];
		if(!m.isClassMethod()) {
			result[0] = m.cls.useName();
		}

		for(int i=0; i<res.size(); i++) {
			result[i+startPos] = (String) res.elementAt(i);
		}

		return result;
	}

	static String[] getParamTypesNoThis(BT_Method m) {
		int startIndex = m.useName().indexOf("(") + 1;
		int endIndex = m.useName().indexOf(")");
		if(startIndex == endIndex) {
			return new String[0];
		}
		String params = m.useName().substring(startIndex, endIndex);
		Vector res = new Vector();
		int end = -1;
		int start = 0;

		while(end != params.length()) {
			int index = params.indexOf(",", start);

			if(index > 0) { // more params to follow 
				end = index;
			} else {
				end = params.length();
			}

			res.add(params.substring(start, end));
			start = end+1;
		}

		String[] result = new String[res.size()];
		for(int i=0; i<res.size(); i++) {
			result[i] = (String) res.elementAt(i);
		}

		return result;
	}


	private int calcNrSpawns(BT_Method m) {
		BT_CodeAttribute code = m.getCode();
		if(code == null) return 0;
		BT_InsVector ins = code.ins;
		if(ins == null) return 0;
		int count = 0;

		for(int i=0; i<ins.size(); i++) {
			int opcode = ins.elementAt(i).opcode;
			if(opcode == opc_invokevirtual) {
				BT_Method target = ins.elementAt(i).getMethodTarget();
				if(analyzer.isSpecial(target)) count++;
			}
		}

		return count;
	}

	boolean isSpawnable(BT_Method m) {
		return analyzer.isSpecial(m);
	}

	void addCloneToInletTable(BT_Method mOrig, BT_Method m) {
		for(int i=0; i<methodTable.size(); i++) {
			MethodTableEntry e = (MethodTableEntry) methodTable.elementAt(i);
			if(e.m.equals(mOrig)) {
				MethodTableEntry newE = new MethodTableEntry(e, m);
				methodTable.addElement(newE);
				e.clone = newE;
				return;
			}
		}
		System.err.println("illegal method in addCloneToInletTable: " + mOrig);

		System.exit(1);
	}

	private MethodTableEntry findMethod (BT_Method m) {
		for(int i=0; i<methodTable.size(); i++) {
			MethodTableEntry e = (MethodTableEntry) methodTable.elementAt(i);
			if(e.m.equals(m)) {
				return e;
			}
		}

		System.err.println("Unable to find method " + m);
		new Exception().printStackTrace();
		System.exit(1);

		return null;
	}

	boolean hasInlet(BT_Method m, int spawnId) {
		MethodTableEntry e = findMethod(m);
		return e.spawnTable[spawnId].hasInlet;
	}

	boolean isLocalUsedInInlet(BT_Method m, int localNr) {
		MethodTableEntry e = findMethod(m);

		if(localNr >= m.getCode().maxLocals) {
			System.out.println("eek, local nr too large: " + localNr + ", max: " + m.getCode().maxLocals);
		}

		for(int i=0; i<e.spawnTable.length; i++) {
			if(e.spawnTable[i].isLocalUsed[localNr]) return true;
		}

		return false;
	}

	int origNrLocals(BT_Method m) {
		MethodTableEntry e = findMethod(m);
		return e.origNrLocals;
	}

	String[] typesOfParams(BT_Method m) {
		MethodTableEntry e = findMethod(m);
		return e.typesOfParams;
	}

	String[] typesOfParamsNoThis(BT_Method m) {
		MethodTableEntry e = findMethod(m);
		return e.typesOfParamsNoThis;
	}

	BT_ExceptionTableEntryVector getCatchTypes(BT_Method m, int spawnId) {
		MethodTableEntry e = findMethod(m);
		return e.spawnTable[spawnId].catchBlocks;
	}

	BT_Method getExcpetionHandlingClone(BT_Method m) {
		return findMethod(m).clone.m;
	}

	boolean containsInlet(BT_Method m) {
		return findMethod(m).containsInlet;
	}

	boolean isClone(BT_Method m) {
		return findMethod(m).isClone;
	}

	int nrSpawns(BT_Method m) {
		return findMethod(m).nrSpawns;
	}

	void setStartLocalAlloc(BT_Method m, BT_Ins i) {
		findMethod(m).startLocalAlloc = i;
	}

	BT_Ins getStartLocalAlloc(BT_Method m) {
		return findMethod(m).startLocalAlloc;
	}

	int getEndOfCatchBlock(BT_Method m, BT_ExceptionTableEntry catchBlock, int handlerIndex) {
		BT_InsVector ins = m.getCode().ins;
		BT_LocalVariableAttribute a = getLocalAttribute(m);

		for(int i=0; i<a.localVariables.length; i++) {
			BT_LocalVariableAttribute.LV l = a.localVariables[i];
			int start = ins.indexOf(l.startIns);

			// dangerous, javac is one instruction further...
			if(start == handlerIndex || start == handlerIndex + 1 || start == handlerIndex + 2 && 
			   l.descriptorC.equals(catchBlock.catchType)) {
				int end = ins.indexOf(l.beyondIns) - 1;
//				System.out.println("found end of catch block: " + handlerIndex + " - " + end);
				return end;
			}
		}

		System.err.println("Could not find end of catch block, did you compile with the '-g' option?");
		System.exit(1);
		return -1;
	}

	private static BT_LocalVariableAttribute getLocalAttribute(BT_Method m) {
		BT_AttributeVector av = m.getCode().attributes;
		BT_Attribute a = av.getAttribute("LocalVariableTable");

		BT_LocalVariableAttribute res = (BT_LocalVariableAttribute) a;

		if(res == null) {
			System.err.println("Could not get local variable table, did you compile with the '-g' option?");
			System.exit(1);
		}

		return res;
	}

	static String getParamName(BT_Method m, int paramNr) {
		BT_LocalVariableAttribute a = getLocalAttribute(m);
		BT_InsVector ins = m.getCode().ins;

		int minPos = Integer.MAX_VALUE;
		String res = null;

		for(int i=0; i<a.localVariables.length; i++) {
			BT_LocalVariableAttribute.LV l = a.localVariables[i];
			if(l.localIndex == paramNr) {
				int startPos = ins.indexOf(l.startIns);
				if(startPos < 0) startPos = 0;

				if(startPos < minPos) {
					minPos = startPos;
					res = l.nameS;
				}
			}
		}

		if(res != null) {
			return res;
		}

		System.out.println("code for " + m);
		for(int i=0; i<ins.size(); i++) {
			System.out.println("ins [" + i + "]:   " + ins.elementAt(i));
		}
		System.out.println();

		System.err.println("getParamName: could not find name of param " + paramNr);
		System.exit(1);

		return null;
	}

	static String getParamType(BT_Method m, int paramNr) {
		BT_LocalVariableAttribute a = getLocalAttribute(m);
		BT_InsVector ins = m.getCode().ins;

		int minPos = Integer.MAX_VALUE;
		String res = null;

		for(int i=0; i<a.localVariables.length; i++) {
			BT_LocalVariableAttribute.LV l = a.localVariables[i];
			if(l.localIndex == paramNr) {
				int startPos = ins.indexOf(l.startIns);
				if(startPos < 0) startPos = 0;

				if(startPos < minPos) {
					minPos = startPos;
					res = l.descriptorC.useName();
				}
			}
		}

		if(res != null) {
			return res;
		}

		System.err.println("getParamType: could not find type of param " + paramNr);
		new Exception().printStackTrace();
		System.exit(1);

		return null;
	}

	static String getLocalName(BT_Method m, BT_Ins curr) {
		BT_LocalVariableAttribute a = getLocalAttribute(m);
		int localNr;
		if(curr instanceof BT_LocalIns) {
			localNr = ((BT_LocalIns) curr).localNr;
		} else {
			localNr = ((BT_IIncIns) curr).localNr;
		}
		BT_InsVector ins = m.getCode().ins;
		int currPos = ins.indexOf(curr);

		for(int i=0; i<a.localVariables.length; i++) {
			BT_LocalVariableAttribute.LV l = a.localVariables[i];
			if(l.localIndex == localNr) {
				int startPos = ins.indexOf(l.startIns)-1;
				int beyondPos = ins.indexOf(l.beyondIns);
				if(beyondPos < 0) beyondPos = ins.size();
				if(beyondPos == ins.size()-1) beyondPos++;

				if(startPos <= currPos && beyondPos > currPos) {
					return l.nameS;
				}
			}
		}

		new Exception().printStackTrace();
		System.err.println("getLocalName: could not find name of local " + localNr);
		System.err.println("Maybe you need to initialize the variable");
		System.exit(1);
		return null;
	}

	static BT_Class getLocalBTType(BT_Method m, BT_Ins curr) {
		BT_LocalVariableAttribute a = getLocalAttribute(m);
		int localNr;
		if(curr instanceof BT_LocalIns) {
			localNr = ((BT_LocalIns) curr).localNr;
		} else {
			localNr = ((BT_IIncIns) curr).localNr;
		}
		BT_InsVector ins = m.getCode().ins;
		int currPos = ins.indexOf(curr);
//		System.out.println("getLocalType START: localNr = " + localNr + ", currPos = " + currPos);

		for(int i=0; i<a.localVariables.length; i++) {
			BT_LocalVariableAttribute.LV l = a.localVariables[i];
//			System.out.println("getLocalType: found data for local " + l.localIndex);

			if(l.localIndex == localNr) {
				int startPos = ins.indexOf(l.startIns)-1;
				int beyondPos = ins.indexOf(l.beyondIns);
				if(beyondPos < 0) beyondPos = ins.size();
				if(beyondPos == ins.size()-1) beyondPos++;

//				System.out.println("local " + localNr + "(" +
//						   l.nameS + "): start = " + startPos + ", end = " + beyondPos +
//						   ", myPos = " + currPos + " size = " + ins.size());

				if(startPos <= currPos && beyondPos > currPos) {
					return l.descriptorC;
				}
			}
		}

		System.out.println("code for " + m);
		for(int i=0; i<ins.size(); i++) {
			System.out.println("ins [" + i + "]:   " + ins.elementAt(i));
		}
		System.out.println();

		new Exception().printStackTrace();
		System.err.println("getLocalType: could not find type of local " + localNr + ", method = " + m);
		System.err.println("Maybe you need to initialize the variable");
		System.exit(1);
		return null;
	}

	static String getLocalType(BT_Method m, BT_Ins curr) {
		return getLocalBTType(m, curr).useName();
	}

	static String generatedLocalName(BT_Class type, String name) {
		return name + "_" + type.useName().replace('.', '_').replace('[', '_').replace(']', '_');
	}

	static String generatedLocalName(String type, String name) {
		return name + "_" + type.replace('.', '_').replace('[', '_').replace(']', '_');
	}

	static String[] getAllLocalDecls(BT_Method m) {
		BT_LocalVariableAttribute a = getLocalAttribute(m);
		Vector v = new Vector();

		for(int i=0; i<a.localVariables.length; i++) {
			BT_LocalVariableAttribute.LV l = a.localVariables[i];
			String e = l.descriptorC.useName() + " " + generatedLocalName(l.descriptorC, l.nameS) + ";";
			if(!v.contains(e)) {
				v.addElement(e);
			}
		}

		String[] result = new String[v.size()];
		for(int i=0; i<v.size(); i++) {
			result[i] = (String) v.elementAt(i);
//			System.out.println("localdecls for " + m + ": " + result[i]);
		}

		return result;
	}

	static String[] getAllLocalTypes(BT_Method m) {
		BT_LocalVariableAttribute a = getLocalAttribute(m);
		Vector v = new Vector();

		for(int i=0; i<a.localVariables.length; i++) {
			BT_LocalVariableAttribute.LV l = a.localVariables[i];
			String e = l.descriptorC.useName();
//			if(!v.contains(e)) {
				v.addElement(e);
//			}
		}

		String[] result = new String[v.size()];
		for(int i=0; i<v.size(); i++) {
			result[i] = (String) v.elementAt(i);
		}

		return result;
	}

	static String[] getAllLocalNames(BT_Method m) {
		BT_LocalVariableAttribute a = getLocalAttribute(m);
		Vector v = new Vector();

		for(int i=0; i<a.localVariables.length; i++) {
			BT_LocalVariableAttribute.LV l = a.localVariables[i];
			String e = generatedLocalName(l.descriptorC, l.nameS);
//			if(!v.contains(e)) {
				v.addElement(e);
//			}
		}

		String[] result = new String[v.size()];
		for(int i=0; i<v.size(); i++) {
			result[i] = (String) v.elementAt(i);
		}

		return result;
	}

	boolean containsSpawnedCall(BT_Method m) {
		BT_CodeAttribute code = m.getCode();
		if(code == null) return false;
		BT_InsVector ins = code.ins;

		for(int i=0; i<ins.size(); i++) {
			int opcode = ins.elementAt(i).opcode;
			if(opcode == opc_invokevirtual) {
				BT_Method target = ins.elementAt(i).getMethodTarget();
				if(analyzer.isSpecial(target)) return true;
			}
		}

		return false;
	}

	static int realMaxLocals(BT_Method m) {
		BT_CodeAttribute code = m.getCode();
		int maxLocals = 0;

		for(int i=0; i<code.ins.size(); i++) {
			if(code.ins.elementAt(i) instanceof BT_LocalIns) {
				BT_LocalIns ins = (BT_LocalIns) code.ins.elementAt(i);
				if (ins.localNr > maxLocals) maxLocals = ins.localNr;
			}
		}

		return maxLocals + 1;
	}
}
