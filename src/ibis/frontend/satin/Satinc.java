package ibis.frontend.satin;

import com.ibm.jikesbt.*;   
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

public final class Satinc implements BT_Opcodes {
	BT_Class satinObjectClass;
	BT_Class satinClass;
	BT_Class mainClass;
	BT_Class spawnCounterClass;
	BT_Class irClass;

	BT_Class c; // the class we are rewriting 
	Vector idTable = new Vector();
	boolean verbose;
	boolean keep;
	boolean print;
	boolean invocationRecordCache;
	BT_Field satinField;
	String classname;
	String mainClassname;
	String compiler = "jikes";
	boolean supportAborts;
	boolean inletOpt;
	boolean spawnCounterOpt;
	MethodTable mtab;

	private class StoreClass {
		BT_Ins store;
		BT_Ins load; // for putfield 
		BT_Method target;

		StoreClass(BT_Ins store, BT_Ins load, BT_Method target) {
			this.store = store;
			this.target = target;
			this.load = load;
		}

		public boolean equals(Object o) {
			if (o == null && store == null) {
				return true;
			}
			if(o == null) {
				return false;
			}
			if(store == null) {
				return false;
			}

			if (!(o instanceof StoreClass)) {
				return false;
			}

			StoreClass c = (StoreClass) o;

			if(!store.equals(c.store)) {
				return false;
			}

			// stores are equal, are loads? 
			if(c.load == null && load == null) {
				return true;
			}
			if(c.load == null) {
				return false;
			}
			if(load == null) {
				return false;
			}

			return load.equals(c.load);
		}
	}

	public Satinc(boolean verbose, boolean keep, boolean print, boolean invocationRecordCache,
	       String classname, String mainClassname, String compiler, boolean supportAborts, boolean inletOpt, boolean spawnCounterOpt, boolean createFactory) {
		this.verbose = verbose;
		this.keep = keep;
		this.print = print;
		this.invocationRecordCache = invocationRecordCache;
		this.classname = classname;
		this.mainClassname = mainClassname;
		this.compiler = compiler;
		this.supportAborts = supportAborts;
		this.inletOpt = inletOpt;
		this.spawnCounterOpt = spawnCounterOpt;

		if(createFactory) {
			BT_Factory.factory = new SatinFactory(classname);
		}

		c = BT_Class.forName(classname);

		if(classname.equals(mainClassname)) {
			mainClass = c;
		} else {
			mainClass = BT_Class.forName(mainClassname);
		}

		satinObjectClass = BT_Class.forName("ibis.satin.SatinObject");
		satinClass = BT_Class.forName("ibis.satin.Satin");
		spawnCounterClass = BT_Class.forName("ibis.satin.SpawnCounter");
		irClass = BT_Class.forName("ibis.satin.InvocationRecord");
	}

	boolean isSatin(BT_Class c) {
		if (c.fullName().equals(satinObjectClass.fullName())) { // SatinObject 
			return true;
		}

		return c.isDescendentOf(satinObjectClass);
	}

	static boolean isRetIns(BT_Ins i) {
		switch(i.opcode) {
			case opc_areturn:
			case opc_return:
			case opc_dreturn:
			case opc_freturn:
			case opc_ireturn:
			case opc_lreturn:
				return true;
		}
		return false;
	}

	static String getInitVal(String s) {
		if(s.equals("byte")) {
			return "0";
		} else if(s.equals("char")) {
			return "0";
		} else if(s.equals("short")) {
			return "0";
		} else if(s.equals("int")) {
			return "0";
		} else if(s.equals("float")) {
			return "0";
		} else if(s.equals("long")) {
			return "0";
		} else if(s.equals("double")) {
			return "0";
		} else {
			return "null";
		}
	}

	static boolean isRefType(String s) {
		if(s.equals("byte")) {
			return false;
		} else if(s.equals("boolean")) {
			return false;
		} else if(s.equals("char")) {
			return false;
		} else if(s.equals("short")) {
			return false;
		} else if(s.equals("int")) {
			return false;
		} else if(s.equals("float")) {
			return false;
		} else if(s.equals("long")) {
			return false;
		} else if(s.equals("double")) {
			return false;
		} else {
			return true;
		}
	}

	BT_ExceptionTableEntry getExceptionHandler(BT_CodeAttribute code, BT_Ins self) {
		for(int j=0; j<code.exceptions.size(); j++) {
			BT_ExceptionTableEntry e = code.exceptions.elementAt(j);
			
			if(comesBefore(code.ins, e.startPCTarget, self) && comesBefore(code.ins, self, e.endPCTarget)) {
				return e;
			}
		}

		return null;
	}

	void generateMain(BT_Class c, BT_Method origMain) {
		BT_MethodSignature sig = origMain.getSignature();
		BT_CodeAttribute code;

		BT_Method satinCtor = satinClass.findMethod("<init>", "(java.lang.String[])");

		BT_Ins mainCode[] = {
			/*  0 */ BT_Ins.make(opc_new, satinClass), 
			/*  1 */ BT_Ins.make(opc_dup),
			/*  2 */ BT_Ins.make(opc_aload_0),
			/*  3 */ BT_Ins.make(opc_invokespecial, satinCtor),
			/*  4 */ BT_Ins.make(opc_putstatic, satinField),
			/*  5 */ BT_Ins.make(opc_getstatic, satinField),
			/*  6 */ BT_Ins.make(opc_getfield, satinClass.findField("master")), 
			/*  7 */ null, /* ifeq 25 */
			/*  8 */ BT_Ins.make(opc_getstatic, satinField),
			/*  9 */ BT_Ins.make(opc_getfield, satinClass.findField("mainArgs")), 
			/* 10 */ BT_Ins.make(opc_invokestatic, origMain),
			/* 11 */ null, /* goto 27 */
			/* 12 */ BT_Ins.make(opc_astore_1),
			/* 13 */ BT_Ins.make(opc_getstatic, BT_Repository.findField("java.lang.System", "out", "java.io.PrintStream")),
			/* 14 */ BT_Ins.make(opc_new, BT_Class.forName("java.lang.StringBuffer")), 
			/* 15 */ BT_Ins.make(opc_dup),
			/* 16 */ BT_Ins.make(opc_ldc, "Exception in main: "),
			/* 17 */ BT_Ins.make(opc_invokespecial, BT_Class.forName("java.lang.StringBuffer").
					     findMethod("<init>", "(java.lang.String)")),
			/* 18 */ BT_Ins.make(opc_aload_1),
			/* 19 */ BT_Ins.make(opc_invokevirtual, BT_Class.forName("java.lang.StringBuffer").
					     findMethod("append", "(java.lang.Object)")),
			/* 20 */ BT_Ins.make(opc_invokevirtual, BT_Class.forName("java.lang.StringBuffer").
					     findMethod("toString", "()")),
			/* 21 */ BT_Ins.make(opc_invokevirtual, BT_Class.forName("java.io.PrintStream").
					     findMethod("println", "(java.lang.String)")),
			/* 22 */ BT_Ins.make(opc_aload_1),
			/* 23 */ BT_Ins.make(opc_invokevirtual, BT_Class.forName("java.lang.Throwable").
					     findMethod("printStackTrace", "()")),
			/* 24 */ null, /* goto 27 */
			/* 25 */ BT_Ins.make(opc_getstatic, satinField),
			/* 26 */ BT_Ins.make(opc_invokevirtual, satinClass.findMethod("client", "()")),
			/* 27 */ BT_Ins.make(opc_getstatic, satinField),
			/* 28 */ BT_Ins.make(opc_invokevirtual, satinClass.findMethod("exit", "()")),
			/* 29 */ BT_Ins.make(opc_return)
		};

		//		mainCode[7] = BT_Ins.make(opc_if_icmpeq, mainCode[25]);
		mainCode[7] = new BT_JumpOffsetIns(opc_ifeq, -1, mainCode[25]);
		mainCode[11] = BT_Ins.make(opc_goto, mainCode[27]);
		mainCode[24] = BT_Ins.make(opc_goto, mainCode[27]);

		code = new BT_CodeAttribute(mainCode);
		code.setExceptionHandler(10, 11, 12, BT_Class.forName("java.lang.Throwable"));

		BT_Method m = new BT_Method(c, "void", "main", "(java.lang.String[])", code);
		m.becomePublic();
		m.becomeStatic();

		try {
			code.verify();
		} catch (Exception e) {
			System.err.println("verify error: " + e);
			e.printStackTrace();

			for(int i=0; i<code.ins.size(); i++) {
				System.out.println("ins[" + i + "]: " + code.ins.elementAt(i));
			}

			System.exit(1);
		}
	}

	static void printMethod(BT_Method m) {
		System.out.println("code for method " + m + ":");
		BT_InsVector ins = m.getCode().ins;

		for(int i=0; i<ins.size(); i++) {
			System.out.println(i + ": " + ins.elementAt(i));
		}
		System.out.println("*******************************************");
		
	}

	String invocationRecordName(BT_Method m) {
		return ("Satin_" + m.fullName() + "_InvocationRecord").replace('.', '_');
	}

	String localRecordName(BT_Method m) {
		return ("Satin_" + m.fullName() + "_LocalRecord").replace('.', '_');
	}

	String returnRecordName(BT_Method m) {
		return ("Satin_" + m.fullName() + "_ReturnRecord").replace('.', '_');
	}

	void insertAllDeleteLocalRecords(BT_Class c, BT_Method m) {
		BT_InsVector ins = m.getCode().ins;
		int maxLocals = m.getCode().maxLocals;

		for(int i=0; i<ins.size(); i++) {
			int opcode = ins.elementAt(i).opcode;
			switch(opcode) {
			case opc_areturn:
			case opc_athrow:
			case opc_return:
			case opc_dreturn:
			case opc_freturn:
			case opc_ireturn:
			case opc_lreturn:
				// Delete the spawn counter before all returns 
				i += insertDeleteLocalRecord(c, m, ins, i, maxLocals);
				break;
			}
		}
	}

	int insertDeleteLocalRecord(BT_Class c, BT_Method m, BT_InsVector ins, int i, int maxLocals) {
		BT_Class localClass = BT_Class.forName(localRecordName(m));
		BT_Method localDeleter = localClass.findMethod("delete", "(" + localRecordName(m) + ")");

		ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals-1), i);
		ins.insertElementAt(BT_Ins.make(opc_invokestatic, localDeleter), i+1);

		return 2; // 2 instructions inserted 
	}

	int insertDeleteSpawncounter(BT_Class c, BT_Method m, BT_InsVector ins, int i, int maxLocals) {
		ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals), i);
		ins.insertElementAt(BT_Ins.make(opc_invokestatic, 
						satinClass.
						findMethod("deleteSpawnCounter", 
							   "(ibis.satin.SpawnCounter)")), i+1);

		rewriteJumps(ins, i+2, i);

		return 2; // 2 instructions inserted 
	}

	void rewriteJumps(BT_InsVector ins, int from, int to) {
		BT_Ins fromIns = ins.elementAt(from);
		BT_Ins toIns = ins.elementAt(to);
		rewriteJumps(ins, fromIns, toIns);
	}

	void rewriteJumps(BT_InsVector ins, BT_Ins fromIns, BT_Ins toIns) {
		for(int i=0; i<ins.size(); i++) {
			BT_Ins curr = ins.elementAt(i);
			if(curr instanceof BT_JumpIns) {
				BT_JumpIns j = (BT_JumpIns) curr;
				j.changeReferencesFromTo(fromIns, toIns);
			}
		}

	}

	int allocateId(BT_Ins storeIns, BT_Ins loadIns, BT_Method target) {
		StoreClass s = new StoreClass(storeIns, loadIns, target);

		int id = idTable.indexOf(s);
		if(id < 0) {
			idTable.add(s);
			id = idTable.size() - 1;
		}

		return id;
	}

	BT_Ins getStoreIns(int id) {
		return ((StoreClass) idTable.get(id)).store;
	}

	BT_Ins getLoadIns(int id) {
		return ((StoreClass) idTable.get(id)).load;
	}

	BT_Method getStoreTarget(int id) {
		return ((StoreClass) idTable.get(id)).target;
	}

	void clearIdTable() {
		idTable.clear();
	}

	void printCode(BT_Method m, BT_CodeAttribute code) {
		System.out.println("Code for method: " + m);
		for(int i=0; i<code.ins.size(); i++) {
			System.out.println("ins[" + i + "] = " + code.ins.elementAt(i));
		}
		System.out.println("End of code for method: " + m);
	}	

	void overwriteIns(BT_InsVector ins, BT_Ins newIns, int pos) {
		for(int i=0; i<ins.size(); i++) {
			if(ins.elementAt(i) instanceof BT_JumpIns) {
				BT_JumpIns curr = (BT_JumpIns)ins.elementAt(i);
				int index = indexOfIns(ins, curr.target);
				if(index == pos) {
//					System.out.println("overwriting jump: ins = " + curr + ", index = " + index);
					curr.changeReferencesFromTo(curr.target, newIns);
				}
			}
		}
		ins.setElementAt(newIns, pos);
	}

	void rewriteAbort(BT_Class c, BT_Method m, BT_CodeAttribute code, BT_InsVector ins, int i, int maxLocals) {
		// in a clone, we have to abort two lists: the outstanding spawns of the parent, and the outstanding
		// spawns of the clone.
		if(mtab.isClone(m)) {
			int parentPos = 3;

			if(!m.isClassMethod()) { // we have an additional 'this' param
				parentPos++;
			}

			overwriteIns(ins, BT_Ins.make(opc_getstatic, satinField), i-2);
			overwriteIns(ins, BT_Ins.make(opc_aload, mtab.realMaxLocals(m)-3), i-1); // push outstanding spawns
			overwriteIns(ins, BT_Ins.make(opc_aload, parentPos), i); // push parent Invocationrecord

			// and call Satin.abort
			ins.insertElementAt(BT_Ins.make(opc_invokevirtual, 
			     satinClass.findMethod("abort", "(ibis.satin.InvocationRecord,ibis.satin.InvocationRecord)")), i+1);
			
			// all jobs were killed, set outstandingSpawns to null
			ins.insertElementAt(BT_Ins.make(opc_aconst_null), i+2); // push null
			ins.insertElementAt(BT_Ins.make(opc_astore, mtab.realMaxLocals(m)-3), i+3); // write
		} else {
			if(mtab.containsInlet(m)) {
				overwriteIns(ins, BT_Ins.make(opc_getstatic, satinField), i-2);
				overwriteIns(ins, BT_Ins.make(opc_aload, maxLocals+1), i-1); // push outstanding spawns
				overwriteIns(ins, BT_Ins.make(opc_aconst_null), i); // push null

				// and call Satin.abort
				ins.insertElementAt(BT_Ins.make(opc_invokevirtual, 
				   satinClass.findMethod("abort", 
				   "(ibis.satin.InvocationRecord,ibis.satin.InvocationRecord)")), i+1);

				// all jobs were killed, set outstandingSpawns to null
				ins.insertElementAt(BT_Ins.make(opc_aconst_null), i+2); // push null
				ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+1), i+3); // write
			} else {
				overwriteIns(ins, BT_Ins.make(opc_getstatic, satinField), i-1);
				overwriteIns(ins, BT_Ins.make(opc_aload, maxLocals+1), i); // push outstanding spawns
				ins.insertElementAt(BT_Ins.make(opc_aconst_null), i+1); // push null

				// and call Satin.abort
				ins.insertElementAt(BT_Ins.make(opc_invokevirtual, 
				   satinClass.findMethod("abort", 
				   "(ibis.satin.InvocationRecord,ibis.satin.InvocationRecord)")), i+2);

				// all jobs were killed, set outstandingSpawns to null
				ins.insertElementAt(BT_Ins.make(opc_aconst_null), i+3); // push null
				ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+1), i+4); // write
			}
		}
	}

	void rewriteSync(BT_Class c, BT_Method m, BT_CodeAttribute code, BT_InsVector ins, int i, int maxLocals) {
		int ifnullPos = -1;
		int firstJumpPos = -1;
		int pos = -1;

		if(verbose) {
			System.out.println("rewriting sync, class = " + c);
		}

		if(idTable.size() == 0) {
			System.err.println("Error: sync without spawn");
			System.exit(1);
		}

		if(!mtab.containsInlet(m)) {
			overwriteIns(ins, BT_Ins.make(opc_getstatic, satinField), i-1);
			overwriteIns(ins, BT_Ins.make(opc_aload, maxLocals), i);

			// and call Satin.sync 
			ins.insertElementAt(BT_Ins.make(opc_invokevirtual, 
						     satinClass.findMethod("sync", "(ibis.satin.SpawnCounter)")), i+1);
			
			ins.insertElementAt(BT_Ins.make(opc_nop), i+2); // replace later by goto 
			firstJumpPos = i+2;
			pos = i+3;
		} else {
			overwriteIns(ins, BT_Ins.make(opc_getstatic, satinField), i-2);
			overwriteIns(ins, BT_Ins.make(opc_aload, maxLocals), i-1);

			// and call Satin.sync 
			overwriteIns(ins, BT_Ins.make(opc_invokevirtual, 
						     satinClass.findMethod("sync", "(ibis.satin.SpawnCounter)")), i);
			
			ins.insertElementAt(BT_Ins.make(opc_nop), i+1); // replace later by goto 
			firstJumpPos = i+1;
			pos = i+2;
		}

		//*** Loop code. ***

		// Push curr = outstandingSpawns. 
		ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+1), pos);
		ins.insertElementAt(BT_Ins.make(opc_dup), pos+1);
		ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+2), pos+2);

		// outstandingSpawns = outstandingSpawns.next 
		ins.insertElementAt(BT_Ins.make(opc_getfield, 
						BT_Class.forName("ibis.satin.InvocationRecord").findField("cacheNext")), pos+3);
		ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+1), pos+4);
		pos += 5;

		int startLoopPos = pos;

		BT_Ins[] jumpTargets = new BT_Ins[idTable.size()];

		// loop over all ids handed out in this method 
		for(int k=0; k<idTable.size(); k++) {
			BT_Class invClass = BT_Class.forName(invocationRecordName(getStoreTarget(k)));

			// Now generate code to test the id, and do the assignment to the result variable. 
			// The previous ifnull jumps here.
			if(idTable.size() > 1) {
				ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+2), pos);
				if(k > 0) {
					jumpTargets[k-1] = ins.elementAt(pos);
				}

				ins.insertElementAt(BT_Ins.make(opc_getfield, 
								BT_Class.forName("ibis.satin.InvocationRecord").
								findField("storeId")), pos+1);

				// push id value 
				ins.insertElementAt(BT_Ins.make(opc_bipush, k), pos+2);

				// do compare 
				// replace later by if_icmp_ne 
				ins.insertElementAt(BT_Ins.make(opc_nop), pos+3);
				pos += 4;
			}

			// assign result

			// store to variable that is supposed to contain result 
			if(isArrayStore(getStoreIns(k))) { // array, maxLocals+3 = temp, cast to correct invocationRecord type
				ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+2), pos);
				ins.insertElementAt(BT_Ins.make(opc_checkcast, invClass), pos+1);
				ins.insertElementAt(BT_Ins.make(opc_dup), pos+2);
				ins.insertElementAt(BT_Ins.make(opc_dup), pos+3);
				ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+3), pos+4);
				ins.insertElementAt(BT_Ins.make(opc_getfield, invClass.findField("array")), pos+5);

				ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+3), pos+6);
				ins.insertElementAt(BT_Ins.make(opc_getfield, invClass.findField("index")), pos+7);

				ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+3), pos+8);
				ins.insertElementAt(BT_Ins.make(opc_getfield, invClass.findField("result")), pos+9);

				ins.insertElementAt(getStoreIns(k), pos+10);

				ins.insertElementAt(BT_Ins.make(opc_invokestatic,
				    invClass.findMethod("delete", 
				        "(" + invocationRecordName(getStoreTarget(k)) + ")")), pos+11);

				if(k != idTable.size()-1) {
					ins.insertElementAt(BT_Ins.make(opc_xxxunusedxxx), pos+12);
					pos++;
				}
				pos += 12;
			} else { // not an array. field or local.
				if(getStoreIns(k) != null) { // local
					if(getLoadIns(k) == null) {
						ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+2), pos);
						ins.insertElementAt(BT_Ins.make(opc_checkcast, invClass), pos+1);

						ins.insertElementAt(BT_Ins.make(opc_dup), pos+2);
						ins.insertElementAt(BT_Ins.make(opc_getfield, invClass.findField("result")), pos+3);
						ins.insertElementAt(getStoreIns(k), pos+4);
						pos += 5;
					} else { // we have a putfield, maxLocals+3 = temp, cast to correct invocationRecord type
						ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+2), pos);
						ins.insertElementAt(BT_Ins.make(opc_checkcast, invClass), pos+1);
						ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+3), pos+2);
                                                ins.insertElementAt(getLoadIns(k), pos+3);
                                                ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+3), pos+4);
                                                ins.insertElementAt(BT_Ins.make(opc_getfield, invClass.findField("result")), pos+5);
                                                ins.insertElementAt(getStoreIns(k), pos+6);
						ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+3), pos+7);
						pos += 8;
					}
				} else {
					ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+2), pos);
					ins.insertElementAt(BT_Ins.make(opc_checkcast, invClass), pos+1);
					pos += 2;
				}

				ins.insertElementAt(BT_Ins.make(opc_invokestatic, // pop curr
								invClass.findMethod("delete", 
								  "(" + invocationRecordName(getStoreTarget(k)) + ")")), pos);

				if(k != idTable.size()-1) {
					ins.insertElementAt(BT_Ins.make(opc_xxxunusedxxx), pos+1);
					pos++;
				}
				pos++;
			}
		}

		// Outer loop test, the first goto jumps here. 
		// The previous if_icmp_ne also jumps here. 
		ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+1), pos);
		overwriteIns(ins, BT_Ins.make(opc_goto, ins.elementAt(pos)), firstJumpPos);
		jumpTargets[idTable.size()-1] = ins.elementAt(pos);
		pos++;

		for(int j=startLoopPos; j<pos; j++) {
			if(ins.elementAt(j).opcode == opc_xxxunusedxxx) {
					overwriteIns(ins, BT_Ins.make(opc_goto, ins.elementAt(pos-1)), j);
			}
		}

		int k = 0;
		for(int j=startLoopPos; j<pos; j++) {
			if(ins.elementAt(j).opcode == opc_nop) {
				overwriteIns(ins, new BT_JumpOffsetIns(opc_if_icmpne, -1, 
								      jumpTargets[k]), j);
				k++;
			}
		}

		// jump back to start op loop 
		ins.insertElementAt(new BT_JumpOffsetIns(opc_ifnonnull, -1, ins.elementAt(firstJumpPos+1)), pos);

		// only for aborts: add an if when this job may be aborted
		if(supportAborts) {
			if(verbose) {
				System.out.println("outputting post-sync aborted check for " + m);
			}

			ins.insertElementAt(BT_Ins.make(opc_getstatic, satinField), pos+1);
			ins.insertElementAt(BT_Ins.make(opc_getfield, satinClass.findField("parent")), pos+2);

			// test for null (root job)
			ins.insertElementAt(new BT_JumpOffsetIns(opc_ifnull, -1, 
								 ins.elementAt(pos+3)), pos+3);

			ins.insertElementAt(BT_Ins.make(opc_getstatic, satinField), pos+4);
			ins.insertElementAt(BT_Ins.make(opc_getfield, satinClass.findField("parent")), pos+5);

			ins.insertElementAt(BT_Ins.make(opc_getfield, irClass.findField("aborted")), pos+6);
			ins.insertElementAt(new BT_JumpOffsetIns(opc_ifeq, -1, 
								 ins.elementAt(pos+7)), pos+7);
/*
////@@@@@@@@@@2 this needs fixing :-(
			// Test for parent.eek, if non-null, throw it (exception in inlet).
			ins.insertElementAt(BT_Ins.make(opc_getstatic, satinField), pos+8);
			ins.insertElementAt(BT_Ins.make(opc_getfield, satinClass.findField("parent")), pos+9);
			ins.insertElementAt(BT_Ins.make(opc_getfield, irClass.findField("eek")), pos+10);
			int jmpPos = pos+11;
			ins.insertElementAt(new BT_JumpOffsetIns(opc_ifnull, -1, 
								 ins.elementAt(pos+11)), pos+11);
			ins.insertElementAt(BT_Ins.make(opc_getstatic, satinField), pos+12);
			ins.insertElementAt(BT_Ins.make(opc_getfield, satinClass.findField("parent")), pos+13);
			ins.insertElementAt(BT_Ins.make(opc_getfield, irClass.findField("eek")), pos+14);
			ins.insertElementAt(BT_Ins.make(opc_athrow), pos+15);

			int targetPos = pos+16;
			pos += 16;

*/
			pos += 8;

			pos += insertNullReturn(c, m, ins, pos);


			// rewrite previous jump
//			overwriteIns(ins, new BT_JumpOffsetIns(opc_ifnull, -1, 
//								 ins.elementAt(targetPos)), jmpPos);
		}
	}

	int insertNullReturn(BT_Class c, BT_Method m, BT_InsVector ins, int pos) {
		BT_MethodSignature sig = m.getSignature();
		BT_Class returnType = sig.returnType;
		String s = returnType.useName();

		if(!returnType.isPrimitive()) {
			// a reference type
			ins.insertElementAt(BT_Ins.make(opc_aconst_null), pos);
			ins.insertElementAt(BT_Ins.make(opc_areturn), pos+1);
			return 2;
		} else if(s.equals("void")) {
			ins.insertElementAt(BT_Ins.make(opc_return), pos);
			return 1;
		} else if(s.equals("float")) {
			ins.insertElementAt(BT_Ins.make(opc_fconst_0), pos);
			ins.insertElementAt(BT_Ins.make(opc_freturn), pos+1);
			return 2;
		} else if(s.equals("double")) {
			ins.insertElementAt(BT_Ins.make(opc_dconst_0), pos);
			ins.insertElementAt(BT_Ins.make(opc_dreturn), pos+1);
			return 2;
		} else if(s.equals("long")) {
			ins.insertElementAt(BT_Ins.make(opc_lconst_0), pos);
			ins.insertElementAt(BT_Ins.make(opc_lreturn), pos+1);
			return 2;
		} else { // boolean, byte, char short or int
			ins.insertElementAt(BT_Ins.make(opc_iconst_0), pos);
			ins.insertElementAt(BT_Ins.make(opc_ireturn), pos+1);
			return 2;
		}
	}


	int getFirstParamPushPos(BT_InsVector ins, int i) {
		int paramsOnStack = -ins.elementAt(i).getStackDiff();

		if(verbose) {
			System.out.println("Expected params for " + ins.elementAt(i).getMethodTarget() + " is " + paramsOnStack);
		}
		
		int k = i-1;
		int pushed = 0;

		do {
			pushed += ins.elementAt(k).getStackDiff();
			k--;
		} while (pushed < paramsOnStack);
		
		return k;
	}

	BT_Ins getAndRemoveLoadIns(BT_InsVector ins, int i) {
		int loadPos = getFirstParamPushPos(ins, i) - 1;

		BT_Ins res = ins.elementAt(loadPos);
		ins.removeElementAt(loadPos);

		return res;
	}

	boolean isArrayStore(BT_Ins ins) {
		if(ins == null) {
			return false;
		}
		switch(ins.opcode) {
		case opc_lastore:
		case opc_iastore:
		case opc_fastore:
		case opc_aastore:
		case opc_dastore:
			return true;
		}
		
		return false;
	}

	boolean comesBefore(BT_InsVector ins, BT_Ins ins1, BT_Ins ins2) {
		return ins.indexOf(ins1) < ins.indexOf(ins2);
	}

	void rewriteSpawn(BT_Class c, BT_Method self, BT_CodeAttribute code, 
			  BT_InsVector ins, BT_Method target, int i, int maxLocals, int spawnId) {
		if(verbose) {
			System.out.println("rewriting spawn, target = " + target.fullName());
		}

		BT_Ins storeIns = null;
		BT_Ins loadIns = null;

		// A spawned method invocation. Target and parameters are already on the stack.
		// Push spawnCounter, outstandingSpawns, and the id for the result. 
		// Then call getNewInvocationRecord 
		// Remove the original invocation and the store of the result. 
		
		// Keep the store instruction, and remove it from the instruction vector. 
		// We must give this store instruction an method-unique id. 
		
		if(!target.isVoidMethod()) {
			storeIns = ins.elementAt(i+1);
			if(storeIns.opcode == opc_putfield) {
				loadIns = getAndRemoveLoadIns(ins, i);
				i--;
			}
			ins.removeElementAt(i+1);
		}
		int storeId = allocateId(storeIns, loadIns, target);

		// remove invoke 
		ins.removeElementAt(i);

		// push spawn counter 
		ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals), i);

		// push outstandingSpawns 
		ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+1), i+1);

		// push storeId 
		ins.insertElementAt(BT_Ins.make(opc_bipush, storeId), i+2);

		// push spawnId 
		ins.insertElementAt(BT_Ins.make(opc_bipush, spawnId), i+3);

		// push parentLocals 
		if(getExceptionHandler(code, ins.elementAt(i)) != null) {
			ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals-1), i+4);
		} else {
			ins.insertElementAt(BT_Ins.make(opc_aconst_null), i+4);
		}

		int startIndex = target.useName().indexOf("(") + 1;
		int endIndex = target.useName().indexOf(")");
		String params = target.useName().substring(startIndex, endIndex);

		// Call getNewInvocationRecord 
		String methodName;
		String paramsString;
		if(storeIns != null && isArrayStore(storeIns)) {
			String sig = target.getSignature().toString();
			int start = sig.indexOf(')') + 1;
			StringBuffer returnType = new StringBuffer();
			descriptorToType(sig.substring(start, sig.length()), returnType);
			
			methodName = "getNewArray";
			paramsString = "(" + returnType + "[], int, " + c.fullName() + ", " + params + ", " + 
			  "ibis.satin.SpawnCounter, ibis.satin.InvocationRecord, int, int, ibis.satin.LocalRecord)";
		} else {
			methodName = "getNew";
			paramsString = "(" + c.fullName() + ", " + params + ", " + 
			  "ibis.satin.SpawnCounter, ibis.satin.InvocationRecord, int, int, ibis.satin.LocalRecord)";
		}

		ins.insertElementAt(BT_Ins.make(opc_invokestatic, 
					     BT_Class.forName(invocationRecordName(target)).
					     findMethod(methodName, paramsString)), i+5);

		// Store result in outstandingSpawns 
		ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+1), i+6);

		// Now, we call Satin.spawn(outstandingSpawns) 
		
		// push s 
		ins.insertElementAt(BT_Ins.make(opc_getstatic, satinField), i+7);
		
		// push outstandingSpawns 
		ins.insertElementAt(BT_Ins.make(opc_aload, maxLocals+1), i+8);
		
		// and call Satin.spawn 
		ins.insertElementAt(BT_Ins.make(opc_invokevirtual, 
						satinClass. findMethod("spawn", 
							   "(ibis.satin.InvocationRecord)")), i+9);
	}

	/* replace store by pop, load by const push */
	void removeUnusedLocals(BT_Method m, BT_InsVector ins) {
		BT_Ins newIns;
		for(int i=0; i<ins.size(); i++) {
			if(ins.elementAt(i).opcode == opc_iinc) {
				newIns = BT_Ins.make(opc_nop);
				overwriteIns(ins, newIns, i);
				continue;
			}
			
			if(ins.elementAt(i) instanceof BT_LocalIns) {
				BT_LocalIns curr = (BT_LocalIns) ins.elementAt(i);
				if(curr.localNr < m.getCode().maxLocals-1 && !mtab.isLocalUsedInInlet(m, curr.localNr)) {
					switch(curr.opcode) {
					case opc_lstore:
					case opc_lstore_0:
					case opc_lstore_1:
					case opc_lstore_2:
					case opc_lstore_3:
					case opc_dstore:
					case opc_dstore_0:
					case opc_dstore_1:
					case opc_dstore_2:
					case opc_dstore_3:
						newIns = BT_Ins.make(opc_pop2);
//						rewriteJumps(ins, curr, newIns);
						overwriteIns(ins, newIns, i);
						break;
					case opc_istore:
					case opc_istore_0:
					case opc_istore_1:
					case opc_istore_2:
					case opc_istore_3:
					case opc_fstore:
					case opc_fstore_0:
					case opc_fstore_1:
					case opc_fstore_2:
					case opc_fstore_3:
					case opc_astore:
					case opc_astore_0:
					case opc_astore_1:
					case opc_astore_2:
					case opc_astore_3:
						newIns = BT_Ins.make(opc_pop);
//						rewriteJumps(ins, curr, newIns);
						overwriteIns(ins, newIns, i);
						break;
					case opc_aload_0:
					case opc_aload_1:
					case opc_aload_2:
					case opc_aload_3:
					case opc_aload:
						newIns = BT_Ins.make(opc_aconst_null);
//						rewriteJumps(ins, curr, newIns);
						overwriteIns(ins, newIns, i);
						break;
					case opc_iload_0:
					case opc_iload_1:
					case opc_iload_2:
					case opc_iload_3:
					case opc_iload:
						newIns = BT_Ins.make(opc_iconst_0);
//						rewriteJumps(ins, curr, newIns);
						overwriteIns(ins, newIns, i);
						break;
					case opc_fload_0:
					case opc_fload_1:
					case opc_fload_2:
					case opc_fload_3:
					case opc_fload:
						newIns = BT_Ins.make(opc_fconst_0);
//						rewriteJumps(ins, curr, newIns);
						overwriteIns(ins, newIns, i);
						break;
					case opc_lload_0:
					case opc_lload_1:
					case opc_lload_2:
					case opc_lload_3:
					case opc_lload:
						newIns = BT_Ins.make(opc_lconst_0);
//						rewriteJumps(ins, curr, newIns);
						overwriteIns(ins, newIns, i);
						break;
					case opc_dload_0:
					case opc_dload_1:
					case opc_dload_2:
					case opc_dload_3:
					case opc_dload:
						newIns = BT_Ins.make(opc_dconst_0);
//						rewriteJumps(ins, curr, newIns);
						overwriteIns(ins, newIns, i);
						break;
					default:
						System.out.println("unhandled ins in removeUnusedLocals: " + curr);
						System.exit(1);
					}
				}
			}
		}
	}

	void initSpawnTargets(BT_Class c, BT_InsVector ins) {
		for(int i=0; i<idTable.size(); i++) {
			BT_Ins store = getStoreIns(i);

			if(store == null) {
				continue;
			}

			if(isArrayStore(store)) {
				continue;
			}

			switch(store.opcode) {
			case opc_lstore:
			case opc_lstore_0:
			case opc_lstore_1:
			case opc_lstore_2:
			case opc_lstore_3:
				ins.insertElementAt(BT_Ins.make(opc_lconst_0), 0);
				ins.insertElementAt(store, 1);
				break;
			case opc_istore:
			case opc_istore_0:
			case opc_istore_1:
			case opc_istore_2:
			case opc_istore_3:
				ins.insertElementAt(BT_Ins.make(opc_iconst_0), 0);
				ins.insertElementAt(store, 1);
				break;
			case opc_fstore:
			case opc_fstore_0:
			case opc_fstore_1:
			case opc_fstore_2:
			case opc_fstore_3:
				ins.insertElementAt(BT_Ins.make(opc_fconst_0), 0);
				ins.insertElementAt(store, 1);
				break;
			case opc_astore:
			case opc_astore_0:
			case opc_astore_1:
			case opc_astore_2:
			case opc_astore_3:
				ins.insertElementAt(BT_Ins.make(opc_aconst_null), 0);
				ins.insertElementAt(store, 1);
				break;
			case opc_dstore:
			case opc_dstore_0:
			case opc_dstore_1:
			case opc_dstore_2:
			case opc_dstore_3:
				ins.insertElementAt(BT_Ins.make(opc_dconst_0), 0);
				ins.insertElementAt(store, 1);
				break;
			case opc_putfield:
				break; // no need to init... 
			default:
				System.err.println("Unhandled store instruction in initSpawnTargets, opcode = " + store.opcode);
				//				System.exit(1);
			}
		}
	}

	void rewriteMethod(BT_Class c, BT_Method m) {
		int spawnId = 0;

		if(verbose) {
			System.out.println("method " + m + " contains a spawned call, rewriting");
		}
		clearIdTable();

		BT_CodeAttribute code = m.getCode();
		BT_InsVector ins = code.ins;
		int maxLocals = code.maxLocals;
		int insertAllocPos = -1;

		if(verbose) {
			System.out.println("maxLocals = " + maxLocals);
		}

		// optimization:
		// find first spawn, then look if there is a jump before the spawn that jumps over it...
                // this avoids alloccing and deleting spawn counters before a spawn hapens (e.g. with thresholds)
		if(spawnCounterOpt) {
			for(int i=0; i<ins.size(); i++) {
				int opcode = ins.elementAt(i).opcode;
				
				if(opcode == opc_invokevirtual) {
					BT_Method target = ins.elementAt(i).getMethodTarget();
					if(mtab.isSpawnable(target)) {
						for(int j=0; j<i; j++) {
							if(ins.elementAt(j) instanceof BT_JumpIns) {
								BT_Ins jumpTarget = ((BT_JumpIns) ins.elementAt(j)).target;
								int targetPos = ins.indexOf(jumpTarget);
								if(targetPos >= i) {
									insertAllocPos = 0;
								}
							}
						}
						
						if(insertAllocPos == -1) { // no jumps
							insertAllocPos = i;
						}
					break;
					}
				}
			}
		} else {
			insertAllocPos = 0;
		}

		// Allocate a spawn counter at the start of the method, local slot is maxLocals 
		ins.insertElementAt(BT_Ins.make(opc_invokestatic, satinClass.
					       findMethod("newSpawnCounter", "()")), insertAllocPos + 0);
		ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals), insertAllocPos + 1);

		// Allocate and init outstandingSpawns at slot maxLocals+1 
		ins.insertElementAt(BT_Ins.make(opc_aconst_null), insertAllocPos + 2);
		ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+1), insertAllocPos + 3);

		// Allocate and init curr at slot maxLocals+2 
		ins.insertElementAt(BT_Ins.make(opc_aconst_null), insertAllocPos + 4);
		ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+2), insertAllocPos + 5);

		// Allocate and init curr at slot maxLocals+3 
		ins.insertElementAt(BT_Ins.make(opc_aconst_null), insertAllocPos + 6);
		ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals+3), insertAllocPos + 7);

		for(int i=insertAllocPos + 8; i<ins.size(); i++) {
			int opcode = ins.elementAt(i).opcode;
			switch(opcode) {
			case opc_areturn:
			case opc_athrow:
			case opc_return:
			case opc_dreturn:
			case opc_freturn:
			case opc_ireturn:
			case opc_lreturn:
				// Delete the spawn counter before all returns 
				i += insertDeleteSpawncounter(c, m, ins, i, maxLocals);
				break;
			case opc_invokevirtual:
				BT_Method target = ins.elementAt(i).getMethodTarget();

				// Rewrite the sync statement. 
				if(target.equals(satinObjectClass.findMethod("sync", "()"))) {
					rewriteSync(c, m, code, ins, i, maxLocals);
					break;
				} else if(mtab.isSpawnable(target)) {
					rewriteSpawn(c, m, code, ins, target, i, maxLocals, spawnId);
					spawnId++;
					break;
				}

				break;
				}
		}

		initSpawnTargets(c, ins);

		try {
			code.verify();
		} catch (Exception e) {
			System.err.println("verify error: " + e);
			e.printStackTrace();

			for(int i=0; i<code.ins.size(); i++) {
				System.out.println("ins[" + i + "]: " + code.ins.elementAt(i));
			}

			System.exit(1);
		}
	}

	int pushParams(BT_InsVector ins, int pos, BT_Method m) {
		String[] params = mtab.typesOfParams(m);

		for(int i=0, param=0; i<params.length; i++,param++) {
			if(params[i].equals("boolean") ||
			   params[i].equals("byte") ||
			   params[i].equals("short") ||
			   params[i].equals("char") ||
			   params[i].equals("int")) {
				ins.insertElementAt(BT_Ins.make(opc_iload, param), pos+i);
			} else if(params[i].equals("float")) {
				ins.insertElementAt(BT_Ins.make(opc_fload, param), pos+i);
			} else if(params[i].equals("long")) {
				ins.insertElementAt(BT_Ins.make(opc_lload, param), pos+i);
				param++;
			} else if(params[i].equals("double")) {
				ins.insertElementAt(BT_Ins.make(opc_dload, param), pos+i);
				param++;
			} else {
				ins.insertElementAt(BT_Ins.make(opc_aload, param), pos+i);
			}
		}

		return params.length + pos;
	}

	int findPreviousPush(BT_InsVector ins, int pos) {
		int paramsOnStack = ins.elementAt(pos).getStackDiff();
		
		while(ins.elementAt(pos).getStackDiff() == paramsOnStack) {
			pos--;
		}
		
		return pos;
	}

	int rewriteStore(BT_Method m, BT_InsVector ins, int i, int maxLocals, BT_Class localClass) {
		BT_LocalIns curr = (BT_LocalIns)ins.elementAt(i);
		String type = mtab.getLocalType(m, curr);
		String name = mtab.getLocalName(m, curr);
		String fieldName = mtab.generatedLocalName(type, name);

		overwriteIns(ins, BT_Ins.make(opc_aload, maxLocals), i);

		if(type.equals("long") || type.equals("double")) {
			ins.insertElementAt(BT_Ins.make(opc_dup_x2), i+1);
			ins.insertElementAt(BT_Ins.make(opc_pop), i+2);
			ins.insertElementAt(BT_Ins.make(opc_putfield, localClass.findField(fieldName)), i+3);
			return 3; // added three instructions 

		} else {
			ins.insertElementAt(BT_Ins.make(opc_swap), i+1);
			ins.insertElementAt(BT_Ins.make(opc_putfield, localClass.findField(fieldName)), i+2);
			return 2; // added two instructions 
		}
	}

	int rewriteLoad(BT_Method m, BT_InsVector ins, int i, int maxLocals, BT_Class localClass) {
		BT_LocalIns curr = (BT_LocalIns)ins.elementAt(i);
		String type = mtab.getLocalType(m, curr);
		String name = mtab.getLocalName(m, curr);
		String fieldName = mtab.generatedLocalName(type, name);

		overwriteIns(ins, BT_Ins.make(opc_aload, maxLocals), i);
		ins.insertElementAt(BT_Ins.make(opc_getfield, localClass.findField(fieldName)), i+1);

		return 1; // added one instruction 
	}

	void shiftLocals(BT_InsVector insVec, int start, int shift) {
		for(int i=start; i<insVec.size(); i++) {
			BT_Ins ins = insVec.elementAt(i);
			if(ins instanceof BT_LocalIns) {
				((BT_LocalIns) ins).localNr += shift;
			}
		}
	}

	int insertTypecheckCode(BT_Method m, BT_InsVector ins, int pos, int spawnId, int exceptionPos) {
		BT_ExceptionTableEntryVector catches = mtab.getCatchTypes(m, spawnId);

		int startPos = pos;
		int[] jumpTargets = new int[catches.size()+1];

		// optimization: if there is only one type of exception, no need to test.
		if(catches.size() == 1) {
			BT_Class type = catches.elementAt(0).catchType;
			BT_Ins catchTarget = catches.elementAt(0).handlerTarget;

			ins.insertElementAt(BT_Ins.make(opc_aload, exceptionPos), pos); // push the exception
			ins.insertElementAt(BT_Ins.make(opc_checkcast, type), pos+1);
			ins.insertElementAt(BT_Ins.make(opc_goto, catchTarget), pos+2); // jump to the catch block
			return pos+3;
		}

		for(int i=0; i<catches.size(); i++) {
			BT_Class type = catches.elementAt(i).catchType;
			BT_Ins catchTarget = catches.elementAt(i).handlerTarget;
			
			jumpTargets[i] = pos;
			ins.insertElementAt(BT_Ins.make(opc_aload, exceptionPos), pos); // push the exception
			ins.insertElementAt(BT_Ins.make(opc_instanceof, type), pos+1); // test type
			// now, there is a 0 or a 1 on the stack 
			ins.insertElementAt(BT_Ins.make(opc_bipush, 1), pos+2); // needed for compare
			ins.insertElementAt(BT_Ins.make(opc_xxxunusedxxx), pos+3); // replace with compare ne

			ins.insertElementAt(BT_Ins.make(opc_aload, exceptionPos), pos+4); // push the exception
			ins.insertElementAt(BT_Ins.make(opc_checkcast, type), pos+5);
			ins.insertElementAt(BT_Ins.make(opc_goto, catchTarget), pos+6); // jump to the catch block
			pos += 7;
		}
		jumpTargets[catches.size()] = pos;

		int typeCount = 0;
		for(int i=startPos; i<=pos; i++) {
			BT_Ins curr = ins.elementAt(i);
			if(curr.opcode == opc_xxxunusedxxx) {
				// rewrite to compare ne
				typeCount++;
				overwriteIns(ins, new BT_JumpOffsetIns(opc_if_icmpne, -1, 
								      ins.elementAt(jumpTargets[typeCount])), i);
			}
		}

		return pos;
	}

	static int indexOfIns(BT_InsVector ins, BT_Ins target) {
		for (int i=0; i<ins.size(); i++) {
			if(ins.elementAt(i).byteIndex == target.byteIndex) return i;
		}

		return -1;
	}

	void generateExceptionHandlingClone(BT_Class c, BT_Method mOrig) {
		int localsShift = 4; // we have 4 params
		int spawnIdPos = 0;
		int localRecordPos = 1;
		int exceptionPos = 2;
		int parentPos = 3;

		if(!mOrig.isClassMethod()) { // we have an additional 'this' param
			localsShift++; 
			spawnIdPos++;
			localRecordPos++;
			exceptionPos++;
			parentPos++;
		}

//		System.out.println("generateExceptionHandlingClone of " + mOrig);
//		for(int i=0; i<mOrig.getCode().ins.size(); i++) {
//			System.out.println(i + ": " + mOrig.getCode().ins.elementAt(i));
//		}

		String returnType = mOrig.signature.returnType.useName();

		BT_MethodSignature sig = BT_MethodSignature.create(returnType, "(int," + localRecordName(mOrig) +
			",java.lang.Throwable,ibis.satin.InvocationRecord)");

		BT_Method m = BT_Method.createMethod(c, mOrig.flags, sig, 
						     "exceptionHandlingClone_" + mOrig.fullName(), mOrig.getCode());

		BT_InsVector insCopy = (BT_InsVector) mOrig.getCode().ins.clone();
		BT_CodeAttribute code = new BT_CodeAttribute(insCopy.toArray());
		BT_InsVector ins = code.ins; // this only is a shallow copy, so now copy instructions 
		for(int i=0; i<ins.size(); i++) {
			BT_Ins newIns = (BT_Ins)ins.elementAt(i).clone();
			newIns.byteIndex = ((BT_Ins)ins.elementAt(i)).byteIndex;
			ins.setElementAt(newIns, i);
		}

		// now rewrite references to old instructions. 
		for(int i=0; i<ins.size(); i++) {
			if(mOrig.getCode().ins.elementAt(i) instanceof BT_JumpIns) {
				BT_JumpIns orig = (BT_JumpIns)mOrig.getCode().ins.elementAt(i);
				BT_JumpIns curr = (BT_JumpIns)ins.elementAt(i);
//				int index = mOrig.getCode().ins.indexOf(orig.target);
				int index = indexOfIns(mOrig.getCode().ins, orig.target);
//				System.out.println("orig = " + orig + ", ins = " + curr + ", index = " + index);
				BT_Ins newTarget = ins.elementAt(index);
				curr.changeReferencesFromTo(curr.target, newTarget);
			}
		}

//@@@		code.maxLocals = mOrig.getCode().maxLocals;
		m.setCode(code);

		// must add before rewrite, otherwise exception table will be screwed up
		mtab.addCloneToInletTable(mOrig, m);

		if(inletOpt) {
			removeUnusedLocals(mOrig, ins);
		}

		// initialize unused locals to prevent verify errors
//		System.out.println("maxLocals = " + mOrig.getCode().maxLocals);
//		for(int i=0; i<mOrig.getCode().maxLocals-1; i++) {
//			if(!mtab.isLocalUsedInInlet(mOrig, i)) {
////				insertInitLocal(c, mOrig, ins, i);
//				System.out.println("init local " + i);
//			}
//		}

		// now generate code to restore locals, push the exception and jump to the correct catch block
		shiftLocals(ins, 0, localsShift); // add localsShift to all locals (we have localsShift parameters...)

		// at pos 'startPos', the new of the local record starts. delete it, and replace with assignment from param
		int startLocalPos = mOrig.getCode().ins.indexOf(mtab.getStartLocalAlloc(mOrig));
		overwriteIns(ins, BT_Ins.make(opc_aload, localRecordPos), startLocalPos); // load localrecord
		overwriteIns(ins, BT_Ins.make(opc_astore, mOrig.getCode().maxLocals-1+localsShift), startLocalPos+1); // save record

                // Remove allocation of LocalRecord.
		// The nr of instructions to be removed depends on the number of locals used.
		int insCount = mtab.typesOfParams(mOrig).length;
		for(int i=0; i<insCount; i++) {
			ins.removeElementAt(startLocalPos+2);
		}

		int pos = startLocalPos+2;

		// okidoki, now jump 
		int[] spawnIdTable = new int[mtab.nrSpawns(m)+1];
		int nrInlets = 0;
		int startPos = pos;
		for(int i=0; i<mtab.nrSpawns(m); i++) { // loop over all spawnIds in method (i)
			if(mtab.hasInlet(m, i)) {
				spawnIdTable[nrInlets] = pos;
				ins.insertElementAt(BT_Ins.make(opc_iload, spawnIdPos), pos); // push spawnId 
				ins.insertElementAt(BT_Ins.make(opc_bipush, i), pos+1); // push i value 
				ins.insertElementAt(BT_Ins.make(opc_nop), pos+2); // replace with compare ne 
				pos += 3;
				pos = insertTypecheckCode(m, ins, pos, i, exceptionPos);
				nrInlets++;
			}
		}
		spawnIdTable[nrInlets] = pos;

		nrInlets = 0;
		for(int i=startPos; i<=pos; i++) {
			BT_Ins curr = ins.elementAt(i);
			if(curr.opcode == opc_nop) {
				// rewrite to compare ne
				overwriteIns(ins, new BT_JumpOffsetIns(opc_if_icmpne, -1, 
								      ins.elementAt(spawnIdTable[nrInlets+1])), i);
			}
		}

		try {
			code.verify();
		} catch (Exception e) {
			System.err.println("verify error: " + e);
			e.printStackTrace();
			
			for(int j=0; j<code.ins.size(); j++) {
				System.out.println("ins[" + j + "]: " + code.ins.elementAt(j));
			}
			
			System.exit(1);
		}
	}

	static boolean isLocalStore(BT_Ins ins) {
		return (ins instanceof BT_StoreLocalIns);
	}

	static boolean isLocalLoad(BT_Ins ins) {
		return (ins instanceof BT_LoadLocalIns);
	}

	void rewriteInletMethod(BT_Class c, BT_Method m) {
		if(verbose) {
			System.out.println("method " + m + " contains a spawned call and inlet, rewriting to local record");
		}

		BT_Class localClass = BT_Class.forName(localRecordName(m));
		BT_Method localCreator;
		if(m.isClassMethod()) {
			localCreator = localClass.findMethod("getNew", "(" + 
							  m.signature.toExternalArgumentString() + ")");
		} else {
			localCreator = localClass.findMethod("getNew", "(" + c.useName() + "," + 
							  m.signature.toExternalArgumentString() + ")");
		}

		BT_CodeAttribute code = m.getCode();
		BT_InsVector ins = code.ins;
		int maxLocals = code.maxLocals;
		if(verbose) {
			System.out.println("maxLocals = " + maxLocals);
		}

		// Allocate Local class.
		int pos = pushParams(ins, 0, m);
		mtab.setStartLocalAlloc(m, ins.elementAt(0));
		ins.insertElementAt(BT_Ins.make(opc_invokestatic, localCreator), pos);
		ins.insertElementAt(BT_Ins.make(opc_astore, maxLocals), pos+1);
		pos += 2;

		for(int i=pos; i<ins.size(); i++) {
			int opcode = ins.elementAt(i).opcode;
			if(isLocalStore(ins.elementAt(i))) {
				BT_LocalIns curr = (BT_LocalIns) ins.elementAt(i);
				if(!inletOpt || mtab.isLocalUsedInInlet(m, curr.localNr)) {
					if(verbose) {
						System.out.println(m + ": rewriting local " + curr.localNr);
					}
					i += rewriteStore(m, ins, i, maxLocals, localClass);
				} else {
					if(verbose) {
						System.out.println(m + ": NOT rewriting local " + curr.localNr);
					}
				}

			} else if(isLocalLoad(ins.elementAt(i))) {
				BT_LocalIns curr = (BT_LocalIns) ins.elementAt(i);
				if(!inletOpt || mtab.isLocalUsedInInlet(m, curr.localNr)) {
					if(verbose) {
						System.out.println(m + ": rewriting local " + curr.localNr);
					}
					i += rewriteLoad(m, ins, i, maxLocals, localClass);
				} else {
					if(verbose) {
						System.out.println(m + ": NOT rewriting local " + curr.localNr);
					}
				}
			} else {
				switch(opcode) {

				case opc_iinc:
					BT_IIncIns curr = (BT_IIncIns)ins.elementAt(i);
					if(!inletOpt || mtab.isLocalUsedInInlet(m, curr.localNr)) {
						if(verbose) {
							System.out.println(m + ": rewriting local " + curr.localNr);
						}

						int val = ((BT_IIncIns)ins.elementAt(i)).constant;
						String fieldName = mtab.getLocalName(m, curr);
						String fieldType = mtab.getLocalType(m, curr);
						BT_Field f = localClass.findField(mtab.generatedLocalName(fieldType, fieldName));
					
						overwriteIns(ins, BT_Ins.make(opc_aload, maxLocals), i);
						ins.insertElementAt(BT_Ins.make(opc_dup), i+1);
						ins.insertElementAt(BT_Ins.make(opc_getfield, f), i+2);
						ins.insertElementAt(BT_Ins.make(opc_bipush, val), i+3);
						ins.insertElementAt(BT_Ins.make(opc_iadd), i+4);
						ins.insertElementAt(BT_Ins.make(opc_putfield, f), i+5);
						i += 5;
					} else {
						if(verbose) {
							System.out.println(m + ": NOT rewriting local " + curr.localNr);
						}
					}
					break;
				}
			}
		}

		code.maxLocals++;
//		code.print(System.out);

		try {
			code.verify();
		} catch (Exception e) {
			System.err.println("verify error: " + e);
			e.printStackTrace();
			for(int i=0; i<code.ins.size(); i++) {
				System.out.println("ins[" + i + "]: " + code.ins.elementAt(i));
			}
			System.exit(1);
		}
	}

	// returns nr of chars read 
	static int descriptorToType(String d, StringBuffer result) {
		String res = "";
		int index = 0;
		int val = 0;
		int dim = 0;

		while (true) {
			if(d.charAt(index) == '[') {
				index++;
				val++;
				dim++;
			} else {
				break;
			}
		}

		val++;
		char ch = d.charAt(index);
		switch (ch) {
		case 'B': res += "byte";
			break;
		case 'C': res += "char";
			break;
		case 'D': res += "double";
			break;
		case 'F': res += "float";
			break;
		case 'I': res += "int";
			break;
		case 'J': res += "long";
			break;
		case 'L': res += d.substring(index+1, d.indexOf(';'));
			val += d.indexOf(';') - index;
			break;
		case 'S': res += "short";
			break;
		case 'Z': res += "boolean";
			break;
		case 'V': res += "void";
			break;
		default:
			System.err.println("illegal char in descriptor: " + ch);
			System.exit(1);
		}

		for(int i=0; i<dim; i++) {
			res += "[]";
		}
		res = res.replace('/', '.');
		result.append(res);

		return val;
	}

	// rewrite method invocations to spawned method invocations 
	void rewriteMethods(BT_Class c) {
		int size = c.methods.size();
		for(int i=0; i<size; i++) {
			if(mtab.containsSpawnedCall(c.methods.elementAt(i))) {
//				printMethod(c.methods.elementAt(i));
				if(mtab.containsInlet(c.methods.elementAt(i))) {
					rewriteInletMethod(c, c.methods.elementAt(i));
//					printMethod(c.methods.elementAt(i));
				}

				rewriteMethod(c, c.methods.elementAt(i));
//				printMethod(c.methods.elementAt(i));

				if(mtab.containsInlet(c.methods.elementAt(i))) {
					generateExceptionHandlingClone(c, c.methods.elementAt(i));
				}
				if(mtab.containsInlet(c.methods.elementAt(i))) {
					insertAllDeleteLocalRecords(c, c.methods.elementAt(i));
				}
			}
		}

		// now rewrite the aborts (also in generated clones)
		size = c.methods.size();
		for(int j=0; j<size; j++) {
			BT_Method m = c.methods.elementAt(j);
			BT_CodeAttribute code = m.getCode();
			if(code == null) continue;
			BT_InsVector ins = code.ins;
			int maxLocals = code.maxLocals;
			
			for(int i=0; i<ins.size(); i++) {
				int opcode = ins.elementAt(i).opcode;
				switch(opcode) {
				case opc_invokevirtual:
					BT_Method target = ins.elementAt(i).getMethodTarget();
					
				        // Rewrite the abort statement. 
					if(target.equals(satinObjectClass.findMethod("abort", "()"))) {
						rewriteAbort(c, m, code, ins, i, maxLocals);
						break;
					}
				}
			}

			try {
				code.verify();
			} catch (Exception e) {
				System.err.println("verify error: " + e);
				e.printStackTrace();
				
				for(int k=0; k<code.ins.size(); k++) {
					System.out.println("ins[" + k + "]: " + code.ins.elementAt(k));
				}
				
				System.exit(1);
			}
		}
	}

	void removeFile(String name) {
		if(verbose) {
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
			if(verbose) {
				System.out.println("Running: " + command);
			}
		
			Runtime r = Runtime.getRuntime();
			Process p = r.exec(command);
			int res = p.waitFor();
			if(res != 0) {
				System.err.println("Error compiling generated code (" + className + ").");
				System.exit(1);
			}
			if(verbose) {
				System.out.println("Done");
			}
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
	void generateInvocationRecords(BT_Class c, String base) throws IOException {
		for(int i=0; i<c.methods.size(); i++) {
			if(mtab.containsInlet(c.methods.elementAt(i))) {
				if(verbose) {
					System.out.println(c.methods.elementAt(i) + ": code contains an inlet");
				}
				
				writeLocalRecord(c, c.methods.elementAt(i), base, false);
				compileGenerated(localRecordName(c.methods.elementAt(i)));
				
				if(!keep) { // remove generated files 
					removeFile(invocationRecordName(c.methods.elementAt(i)) + ".java");
				}
			}
			
			if(mtab.isSpawnable(c.methods.elementAt(i))) {
				writeInvocationRecord(c, c.methods.elementAt(i), base);
				writeReturnRecord(c, c.methods.elementAt(i), base);
				
				compileGenerated(invocationRecordName(c.methods.elementAt(i)));
				if(!keep) { // remove generated files 
					removeFile(invocationRecordName(c.methods.elementAt(i)) + ".java");
				}
				
				compileGenerated(returnRecordName(c.methods.elementAt(i)));
				if(!keep) { // remove generated files 
					removeFile(returnRecordName(c.methods.elementAt(i)) + ".java");
				}
			}
		}
	}

	int insertReturnPop(BT_Class c, BT_Method m, BT_InsVector ins, int pos) {
		BT_MethodSignature sig = m.getSignature();
		BT_Class returnType = sig.returnType;
		String s = returnType.useName();

		if(s.equals("double") || s.equals("long")) {
			ins.insertElementAt(BT_Ins.make(opc_pop2), pos);
		} else if(s.equals("void")) {
			// do nothing
		} else { 
			ins.insertElementAt(BT_Ins.make(opc_pop), pos);
		}
		
		return 1;
	}

	void regenerateLocalRecord(BT_Class c, String base) {
		for(int i=0; i<c.methods.size(); i++) {
			BT_Method m = c.methods.elementAt(i);

			if(!mtab.isClone(m) && mtab.containsInlet(m)) {
				BT_Method clone = mtab.getExcpetionHandlingClone(m);
				BT_Class localRec = BT_Class.forName(localRecordName(m));
				BT_Method exceptionHandler = localRec.findMethod("handleException",
					"(int, java.lang.Throwable,ibis.satin.InvocationRecord)");
				BT_CodeAttribute code = exceptionHandler.getCode();

				if(verbose) {
					System.out.println(m + ":code contained an inlet, REwriting localrecord, clone = " + clone);
				}

				if(clone.isStatic()) {
					// push spawnId, push localrecord, push exception, push parent then invoke static
					code.removeInstructionAt(code.ins.size()-1); // remove return
					code.insertInstruction(BT_Ins.make(opc_iload, 1));
					code.insertInstruction(BT_Ins.make(opc_aload, 0));
					code.insertInstruction(BT_Ins.make(opc_aload, 2));
					code.insertInstruction(BT_Ins.make(opc_aload, 3));
					code.insertInstruction(BT_Ins.make(opc_invokestatic, clone));
					code.insertInstruction(BT_Ins.make(opc_return));
					insertReturnPop(c, m, code.ins, code.ins.size()-1);
				} else {
					// push this, spawnId, push localrecord, push exception, push parent, then invoke virtual
					String thisName = mtab.getParamName(m, 0);
					String thisType = mtab.getParamType(m, 0);
					String thisFieldName = mtab.generatedLocalName(thisType, thisName);

					code.removeInstructionAt(code.ins.size()-1); // remove return
					code.insertInstruction(BT_Ins.make(opc_aload, 0));
					code.insertInstruction(BT_Ins.make(opc_getfield, 
					   BT_Class.forName(localRecordName(m)).findField(thisFieldName)));

					code.insertInstruction(BT_Ins.make(opc_iload, 1));
					code.insertInstruction(BT_Ins.make(opc_aload, 0));
					code.insertInstruction(BT_Ins.make(opc_aload, 2));
					code.insertInstruction(BT_Ins.make(opc_aload, 3));
					code.insertInstruction(BT_Ins.make(opc_invokevirtual, clone));
					code.insertInstruction(BT_Ins.make(opc_return));
					insertReturnPop(c, m, code.ins, code.ins.size()-1);
				}

				try {
					code.verify();
				} catch (Exception e) {
					System.err.println("verify error: " + e);
					e.printStackTrace();
					
					for(int j=0; j<code.ins.size(); j++) {
						System.out.println("ins[" + j + "]: " + code.ins.elementAt(j));
					}
					
					System.exit(1);
				}
				localRec.write();
			}
		}
	}

	void writeLocalRecord(BT_Class c, BT_Method m, String basename, boolean finalPass) throws IOException {
		String name = localRecordName(m);
		if(verbose) {
			System.out.println("writing localrecord code to " + name + ".java");
		}

		FileOutputStream f = new FileOutputStream(name + ".java");
		BufferedOutputStream b = new BufferedOutputStream(f);
		PrintStream out = new PrintStream(b);

//		PrintStream out = System.err;
		BT_CodeAttribute code = m.getCode();

		out.println("final class " + name + " extends ibis.satin.LocalRecord {");
		out.println("    static " + name + " cache;");

		String[] allLvs = mtab.getAllLocalDecls(m);

		for(int i=0; i<allLvs.length; i++) {
//			if(mtab.isLocalUsedInInlet(m, i)) {
				out.println("    " + allLvs[i]);
//			}
		}
		out.println();

                // generate constructor, all parameters to the call must be copied.
		// locals are not initialized yet, so no need to copy them.
		String[] params = mtab.typesOfParams(m);
		// ctor 
		out.print("    " + name + "(");

		for(int i=0; i<params.length; i++) {
			String paramType = mtab.getParamType(m, i);
			String paramName = mtab.getParamName(m, i);

			out.print(paramType + " " + mtab.generatedLocalName(paramType, paramName));
			if(i != params.length-1) {
				out.print(", ");
			}
		}
		out.println(") {");

		for(int i=0; i<params.length; i++) {
//			if(mtab.isLocalUsedInInlet(m, i)) {
				String paramType = mtab.getParamType(m, i);
				String paramName = mtab.getParamName(m, i);

				out.println("        this." + mtab.generatedLocalName(paramType, paramName) + 
					    " = " + mtab.generatedLocalName(paramType, paramName) + ";");
//			}
		}

		out.println("    }\n");

		// cache
		out.print("    static " + name + " getNew(");

		for(int i=0; i<params.length; i++) {
			String paramType = mtab.getParamType(m, i);
			String paramName = mtab.getParamName(m, i);

			out.print(paramType + " " + mtab.generatedLocalName(paramType, paramName));
			if(i != params.length-1) {
				out.print(", ");
			}
		}
		out.println(") {");

		out.println("        if(cache == null) {");
		out.print("            return new " + name + "(");
		for(int i=0; i<params.length; i++) {
			String paramType = mtab.getParamType(m, i);
			String paramName = mtab.getParamName(m, i);

			out.print( mtab.generatedLocalName(paramType, paramName));
			if(i != params.length-1) {
				out.print(", ");
			}
		}
		out.println(");");
		out.println("        }");

		out.println("        " + name + " result = cache;");
		out.println("        cache = (" + name + ") cache.next;");

		for(int i=0; i<params.length; i++) {
//			if(mtab.isLocalUsedInInlet(m, i)) {
				String paramType = mtab.getParamType(m, i);
				String paramName = mtab.getParamName(m, i);

				out.println("        result." + mtab.generatedLocalName(paramType, paramName) + 
					    " = " + mtab.generatedLocalName(paramType, paramName) + ";");
//			}
		}

		out.println("        result.next = null;");
		out.println("        return result;");
		out.println("    }\n");

		// delete
		out.println("    static void delete(" + name + " curr) {");

		// wipe fields for gc
		String[] ltypes = mtab.getAllLocalTypes(m);
		String[] lnames = mtab.getAllLocalNames(m);

		for(int i=0; i<ltypes.length; i++) {
//			if(mtab.isLocalUsedInInlet(m, i)) {
				if(isRefType(ltypes[i])) {
					out.println("        curr." + lnames[i] + " = null;");
				}
//			}
		}

		out.println("        curr.next = cache;");
		out.println("        cache = curr;");
		out.println("    }\n");

		// generate a method that runs the clone in case of exceptions 
		out.println("    public void handleException(int spawnId, Throwable t, ibis.satin.InvocationRecord parent) {");
		out.println("        if(ibis.satin.Config.INLET_DEBUG) System.out.println(\"handleE: spawnId = \" + spawnId + \", t = \" + t + \", parent = \" + parent + \", this = \" + this);");
		// This will later be replaced with call to exception handler
		out.println("    }");

		out.println("}");
		out.close();
	}

	void writeInvocationRecord(BT_Class c, BT_Method m, String basename) throws IOException {
		String name = invocationRecordName(m);
		if(verbose) {
			System.out.println("writing invocationrecord code to " + name + ".java");
		}

		FileOutputStream f = new FileOutputStream(name + ".java");
		BufferedOutputStream b = new BufferedOutputStream(f);
		PrintStream out = new PrintStream(b);
		//		PrintStream out = System.err;
		String[] params = mtab.typesOfParamsNoThis(m);

		String sig = m.getSignature().toString();
		int start = sig.indexOf(')') + 1;
		StringBuffer returnType = new StringBuffer();
		descriptorToType(sig.substring(start, sig.length()), returnType);

		out.println("import ibis.satin.*;\n");
		out.println("final class " + name + " extends InvocationRecord {");

		// fields 
		out.println("    " + c.className() + " self;");
		for(int i=0; i<params.length; i++) {
			out.println("    " + params[i] + " param" + i + ";");
		}

		// result 
		if(!m.isVoidMethod()) {
			out.println("    transient " + returnType + " result;");
			out.println("    transient int index;");
			out.println("    transient " + returnType + "[] array;");
		}

		if(invocationRecordCache) {
			out.println("    static " + name + " invocationRecordCache;");
		}
		out.println();
		
		// ctor 
		out.print("    " + name + "(");
		out.print(c.className() + " self, ");
		for(int i=0; i<params.length; i++) {
			out.print(params[i] + " param" + i + ", ");
		}
		out.println("SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {");
		out.println("        super(s, next, storeId, spawnId, parentLocals);");
		out.println("        this.self = self;");

		for(int i=0; i<params.length; i++) {
			out.println("        this.param" + i + " = param" + i + ";");
		}

		out.println("    }\n");

		// getNew method 
		out.print("    static " + name + " getNew(");
		out.print(c.className() + " self, ");
		for(int i=0; i<params.length; i++) {
			out.print(params[i] + " param" + i + ", ");
		}
		out.println("SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {");

		if(invocationRecordCache) {
			out.println("        if (invocationRecordCache == null) {");
		}
		out.print("            return new " + name + "(self, ");
		for(int i=0; i<params.length; i++) {
			out.print(" param" + i + ", ");
		}
		out.println("s, next, storeId, spawnId, parentLocals);");
		if(invocationRecordCache) {
			out.println("        }\n");

			out.println("        " + name + " res = invocationRecordCache;");
			out.println("        invocationRecordCache = (" + name + ") res.cacheNext;");
			out.println("        res.self = self;");
			for(int i=0; i<params.length; i++) {
				out.println("        res.param" + i + " = param" + i + ";");
			}
			out.println("        res.spawnCounter = s;");
			out.println("        res.cacheNext = next;");
			out.println("        res.storeId = storeId;");

			out.println("        if(ibis.satin.Config.ABORTS) {");
			out.println("                res.spawnId = spawnId;");
			out.println("                res.parentLocals = parentLocals;");
			out.println("        }");

			out.println("        return res;");
		}
		out.println("    }\n");

		// getNew method for arrays 
		if(!m.isVoidMethod()) {
			out.print("    static " + name + " getNewArray(");
			out.print(returnType + "[] array, int index, ");
			out.print(c.className() + " self, ");
			for(int i=0; i<params.length; i++) {
				out.print(params[i] + " param" + i + ", ");
			}
			out.println("SpawnCounter s, InvocationRecord next, int storeId, int spawnId, LocalRecord parentLocals) {");
			out.print("            " + name + " res = getNew(self, ");
			for(int i=0; i<params.length; i++) {
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
		if(invocationRecordCache) {
			if(!m.isVoidMethod()) {
				out.println("        w.array = null;");
			}
			// Set everything to null, don't keep references live for gc. 
			out.println("        w.clear();");
			out.println("        w.self = null;");
			
			for(int i=0; i<params.length; i++) {
				if(isRefType(params[i])) {
					out.println("        w.param" + i + " = null;");
				}
			}
			out.println("        w.cacheNext = invocationRecordCache;");
			out.println("        invocationRecordCache = w;");
		}
		out.println("    }\n");

                // unused ...
		// delete method (for abort)
/*
		out.println("    public void delete() {");
		if(invocationRecordCache) {
			if(!m.isVoidMethod()) {
				out.println("        array = null;");
			}
			// Set everything to null, don't keep references live for gc. 
			out.println("        clear();");
			out.println("        self = null;");
			
			for(int i=0; i<params.length; i++) {
				if(isRefType(params[i])) {
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
		if(supportAborts) {
			out.println("        try {");
		}
		if(!m.isVoidMethod()) {
			out.print("            result = ");
		}
		out.print("            self." + m.getName() + "(");
		for(int i=0; i<params.length; i++) {
			out.print("param" + i);
			if(i != params.length-1) {
				out.print(", ");
			}
		}
		out.println(");");
		if(supportAborts) {
			out.println("        } catch (Throwable e) {");
			out.println("            if(ibis.satin.Config.INLET_DEBUG) System.out.println(\"caught exception in runlocal: \" + e);");
			out.println("                eek = e;");
			out.println("        }");

			out.println("        if(eek != null) {");
			out.println("            if(ibis.satin.Config.INLET_DEBUG) System.out.println(\"runlocal: calling inlet for: \" + this);");
			out.println("            parentLocals.handleException(spawnId, eek, this);");
			out.println("            if(ibis.satin.Config.INLET_DEBUG) System.out.println(\"runlocal: calling inlet for: \" + this + \" DONE\");");
			out.println("        }");
		}
		out.println("    }\n");

		// runRemote method 
		out.println("    public ibis.satin.ReturnRecord runRemote() {");
		if(supportAborts) {
			out.println("        Throwable eek = null;");
		}
		if(supportAborts) {
			if(!m.isVoidMethod()) {
				out.print("        " + returnType + " result = ");
				out.print(getInitVal(returnType.toString()));
				out.println(";");
			}
		} else {
			if(!m.isVoidMethod()) {
				out.println("        " + returnType + " result;");
			}
		}

		if(supportAborts) {
			out.println("        try {");
		}
		if(!m.isVoidMethod()) {
			out.print("            result = ");
		}
		out.print("            self." + m.getName() + "(");

		for(int i=0; i<params.length; i++) {
			out.print("param" + i);
			if(i != params.length-1) {
				out.print(", ");
			}
		}
		out.println(");");
		if(supportAborts) {
			out.println("        } catch (Throwable e) {");
			out.println("            eek = e;");
			out.println("        }");
		}
		out.print("        return new " + returnRecordName(m));
		if(!m.isVoidMethod()) {
			out.println("(result, eek, stamp);");
		} else {
			out.println("(eek, stamp);");
		}
		out.println("    }");
		out.println("}");

		out.close();
	}

	void writeReturnRecord(BT_Class c, BT_Method m, String basename) throws IOException {
		String name = returnRecordName(m);
		if(verbose) {
			System.out.println("writing returnrecord code to " + name + ".java");
		}

		FileOutputStream f = new FileOutputStream(name + ".java");
		BufferedOutputStream b = new BufferedOutputStream(f);
		PrintStream out = new PrintStream(b);
//		PrintStream out = System.err;

		String sig = m.getSignature().toString();
		int start = sig.indexOf(')') + 1;
		StringBuffer returnType = new StringBuffer();
		descriptorToType(sig.substring(start, sig.length()), returnType);

		out.println("import ibis.satin.*;\n");
		out.println("final class " + name + " extends ReturnRecord {");
		if(!m.isVoidMethod()) {
			out.println("    " + returnType + " result;\n");
		}

		// ctor 
		out.print("    " + name + "(");
		if(!m.isVoidMethod()) {
			out.println(returnType + " result, Throwable eek, int stamp) {");
		} else {
			out.println(" Throwable eek, int stamp) {");
		}

		out.println("        super(eek);");
		if(!m.isVoidMethod()) {
			out.println("        this.result = result;");
		}
		out.println("        this.stamp = stamp;");
		out.println("    }\n");

		out.println("    public void assignTo(InvocationRecord rin) {");
		out.println("        " + invocationRecordName(m) + " r = (" +
			    invocationRecordName(m) + ") rin;");
		if(!m.isVoidMethod()) {
			out.println("        r.result = result;");
		}
		out.println("        r.eek = eek;");
		out.println("    }");
		out.println("}");

		out.close();
	}

	public void start() {
		if (isSatin(c)) {
			if(verbose) {
				System.out.println(c.fullName() + " is a satin class");
			}
		}

		// If we have the main method, rename it to origMain. 
		BT_Method main = null;
		try {
			main = c.findMethod("void", "main", "(java.lang.String[])");
		} catch (java.lang.NoSuchMethodError e) {
			// Ignore.
		}

		if(main != null) {
			if(verbose) {
				System.out.println("the class has main, renaming to origMain");
			}
			main.resetName("origMain");

			BT_Field.createField(c, BT_Item.STATIC, satinClass, "satinClass");
			satinField = c.findField("satinClass");

			generateMain(c, main);
		} else {
			try {
				satinField = mainClass.findField("satinClass");
			} catch (java.lang.NoSuchFieldError e) {
				// no main field!
				// we are probably compiling a non-satin application/library then
				if(verbose) {
					System.err.println("no mainclass, returning");
				}
				return;
			}
		}

		String src = c.getSourceFile();
		int index = src.indexOf(".");
		String base = src.substring(0, index);
		String dst = base + ".class";

		mtab = new MethodTable(c, this, verbose);

		if(verbose) {
			mtab.print(System.out);
		}

		try {
			generateInvocationRecords(c, base);
		} catch (IOException e) {
			System.out.println("IO error: " + e);
			System.exit(1);
		}
		rewriteMethods(c);

		regenerateLocalRecord(c, base);

		// cleanup
		for(int i=0; i<c.methods.size(); i++) {
			if(!keep) { // remove generated files 
				if(mtab.containsInlet(c.methods.elementAt(i))) {
					removeFile(localRecordName(c.methods.elementAt(i)) + ".java");
				}
			}
			
			if(c.methods.elementAt(i).getCode() != null) {
				BT_AttributeVector av = c.methods.elementAt(i).getCode().attributes;
				BT_Attribute a = av.removeAttribute("LocalVariableTable");
				a = av.removeAttribute("LineNumberTable");
			}
		}

		// now overwrite the classfile 
		c.write(dst);

		if(print) {
			c.print(System.out);
		}
	}

	public static void usage() {
		System.err.println("Usage : java Satinc [-v] [-keep] [-print] [-irc-off] [-no-sc-opt]" +
				   "[-compiler \"your compile command\" ] [-no-aborts] [-no-inlet-opt] <classname> [mainClass]");
		System.exit(1);
	}

	public static void main(String[] args) {
		String target = null;
		String mainClass = null;
		boolean verbose = false;
		boolean keep = false;
		boolean print = false;
		boolean invocationRecordCache = true;
		boolean supportAborts = true;
		String compiler = "jikes";
		boolean inletOpt = true;
		boolean spawnCounterOpt = true;

		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-v")) {
				verbose = true;
			} else if(!args[i].startsWith("-")) {
				if(target == null) {
					target = args[i];
				} else if(mainClass == null) {
					mainClass = args[i];
				} else {
					usage();
				}
			} else if(args[i].equals("-compiler")) {
				compiler = args[i+1];
				i++;
			} else if(args[i].equals("-keep")) {
				keep = true;
			} else if(args[i].equals("-print")) {
				print = true;
			} else if(args[i].equals("-irc-off")) {
				invocationRecordCache = false;
			} else if(args[i].equals("-no-aborts")) {
				supportAborts = false;
			} else if(args[i].equals("-no-inlet-opt")) {
				inletOpt = false;
			} else if(args[i].equals("-no-sc-opt")) {
				spawnCounterOpt = false;
			} else {
				usage();
			}
		}

		if(target == null) {
			usage();
		}

		if(mainClass == null) {
			mainClass = target;
		}

		new Satinc(verbose, keep, print, invocationRecordCache, target, mainClass, compiler, supportAborts, inletOpt, spawnCounterOpt, true).start();
	}
}
