package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

/** A stack of invocation records. */

final class IRStack implements Config {
	private InvocationRecord[] l = new InvocationRecord[500];

	private int count = 0;

	Satin s;

	IRStack(Satin s) {
		this.s = s;
	}

	int size() {
		return count;
	}

	boolean contains(InvocationRecord r) {
		InvocationRecord curr;

		for (int i = 0; i < count; i++) {
			curr = l[i];
			if (curr.equals(r))
				return true;
		}

		return false;
	}

	void push(InvocationRecord r) {
		if (count >= l.length) {
			InvocationRecord[] nl = new InvocationRecord[l.length * 2];
			System.arraycopy(l, 0, nl, 0, l.length);
			l = nl;
		}

		l[count] = r;
		count++;
	}

	void pop() {
		if (ASSERTS && count <= 0) {
			System.err.println("popping from empty IR stack!");
			new Exception().printStackTrace();
			System.exit(1);
		}

		count--;
		l[count] = null;
	}

	/*
	 * Observation: mostly, the jobs on the stack are children of eachother. In
	 * the normal case, there is no need to run isDescendentOf all the time.
	 */
	void oldkillChildrenOf(int targetStamp, IbisIdentifier targetOwner) {
		InvocationRecord curr;

		for (int i = 0; i < count; i++) {
			curr = l[i];
			if (Satin.isDescendentOf(curr, targetStamp, targetOwner)) {
				curr.aborted = true;
				s.abortedJobs++;

				if (ABORT_DEBUG) {
					System.err.println("found child on the stack: "
							+ curr.stamp + ", it depends on " + targetStamp);
				}
			}
		}
	}

	void killChildrenOf(int targetStamp, IbisIdentifier targetOwner) {
		InvocationRecord curr;

		for (int i = 0; i < count; i++) {
			curr = l[i];
			if(curr.aborted) continue; // already handled.

			if ((curr.parent != null && curr.parent.aborted)
					|| Satin.isDescendentOf(curr, targetStamp, targetOwner)) {
				//				if(curr.parent != null && curr.parent.aborted)
				// System.err.print("#");

				curr.aborted = true;
				s.abortedJobs++;

				if (ABORT_DEBUG) {
					System.err.println("found child on the stack: "
							+ curr.stamp + ", it depends on " + targetStamp);
				}
			}
		}
	}

	/**
	 * Used for fault tolerance Aborts all descendents of the given job, stores
	 * their finished children in the global result table
	 */

	void killAndStoreChildrenOf(int targetStamp, IbisIdentifier targetOwner) {
		InvocationRecord curr;

		for (int i = 0; i < count; i++) {
			curr = l[i];

			if ((curr.parent != null && curr.parent.aborted)
					|| Satin.isDescendentOf(curr, targetStamp, targetOwner)) {

				if (curr.aborted)
					continue;

				curr.aborted = true;
				if (FT_STATS) {
					s.killedOrphans++;
				}
				//update the global result table
				InvocationRecord child = curr.finishedChild;
				while (child != null) {
					s.globalResultTable.storeResult(child);
					child = child.finishedSibling;
				}

			}
		}
	}

	/**
	 * Used for fault tolerance. Abort every job that was spawned on targetOwner
	 * or is a child of a job spawned on targetOwner
	 */

	void killSubtreeOf(IbisIdentifier targetOwner) {
		InvocationRecord curr;

		for (int i = 0; i < count; i++) {
			curr = l[i];

			if ((curr.parent != null && curr.parent.aborted)
					|| Satin.isDescendentOf1(curr, targetOwner)
					|| curr.owner.equals(targetOwner)) {
				//				if(curr.parent != null && curr.parent.aborted)
				// System.err.print("#");

				curr.aborted = true;

			}
		}
	}

	/**
	 * Used for fault tolerance Aborts every job that was spawned on targetOwner
	 * or is a child of a job spawned on targetOwner store all finished children
	 * of the aborted jobs in the global result table
	 */

	void killAndStoreSubtreeOf(IbisIdentifier targetOwner) {
		InvocationRecord curr;

		for (int i = 0; i < count; i++) {
			curr = l[i];

			if ((curr.parent != null && curr.parent.aborted)
					|| Satin.isDescendentOf1(curr, targetOwner)
					|| curr.owner.equals(targetOwner)) {

				if (curr.aborted)
					continue;

				curr.aborted = true;
				if (FT_STATS) {
					s.killedOrphans++;
				}
				//update the global result table
				InvocationRecord child = curr.finishedChild;
				while (child != null) {
					s.globalResultTable.storeResult(child);
					child = child.finishedSibling;
				}

			}
		}
	}
	
	
	/* Used for fault tolerance.
	 * Stores pointers to the invocation records of orphan jobs in the
	 * global result table
	 **/
	void storeOrphansOf(IbisIdentifier crashedIbis) {
		if (ASSERTS) {
			Satin.assertLocked(s);
		}
		
		InvocationRecord curr;
		
		for (int i=0; i<count; i++) {
			curr = l[i];
			
			if (curr.owner != null && 
			    curr.owner.equals(crashedIbis) &&
			    !curr.orphan) {
				s.globalResultTable.storeLock(curr);
				curr.orphan = true;
				
			}
		}
	}
								

	/**
	 * Used for malleability. After receiving a delete() signal, store all the
	 * finished work in the global result table
	 */

	void storeAll() {
		InvocationRecord curr, child;

		for (int i = 0; i < count; i++) {
			curr = l[i];
			child = curr.finishedChild;
			while (child != null) {
				s.globalResultTable.storeResult(child);
				child = child.finishedSibling;
			}
		}
	}

	void pushAll(Victim victim) {
		InvocationRecord curr, child;
		Map toPush = new Hashtable();

		for (int i = 0; i < count; i++) {
			curr = l[i];
			child = curr.finishedChild;
			while (child != null) {
				GlobalResultTable.Key key = new GlobalResultTable.Key(child);
				GlobalResultTable.Value value = new GlobalResultTable.Value(GlobalResultTable.Value.TYPE_RESULT, child);
				toPush.put(key, value);
				child = child.finishedSibling;
			}
		}
		
		if (toPush.size() > 0) {		    
			SendPort sp = victim.s;
			try {
				WriteMessage m = sp.newMessage();
				m.writeByte(Protocol.RESULT_PUSH);
				m.writeObject(toPush);
				long numBytes = m.finish();
				System.err.println("SATIN '" + s.ident.name() + "': " + numBytes + " bytes pushed");
			} catch (IOException e) {
				System.err.println("SATIN '" + s.ident.name() + "': error pushing results " + e);
			}
		} else {
			System.err.println("to push is 0");
		}
	}

	void killAll() {
		for (int i = 0; i < count; i++) {
			l[i].aborted = true;
		}
	}

	void print(java.io.PrintStream out) {
		out.println("=IRStack " + s.ident.name() + ":=============");
		for (int i = 0; i < count; i++) {
			ParameterRecord pr = l[i].getParameterRecord();
			out.println("stack [" + i + "] = " + pr);
		}
		out.println("=========end of IRStack:===========");
	}

	int numStolenJobs() {
		int numStolen = 0;
		for (int i = 0; i < count; i++) {
			if (!l[i].owner.equals(s.ident)) {
				numStolen++;
			}
		}
		return numStolen;
	}
}
