package ibis.satin.impl;

import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;

public abstract class Inlets extends Aborts {
	// trace back from the exception, and execute inlets / empty imlets back to
	// the root
	// during this, prematurely send result messages.
	void handleInlet(InvocationRecord r) {
		InvocationRecord oldParent;
		int oldParentStamp;
		IbisIdentifier oldParentOwner;

		if (r.inletExecuted) {
			// System.err.print("r");
			return;
		}

		if (r.parentLocals == null) {
			// System.err.println("empty inlet in handleInlet");
			handleEmptyInlet(r);
			return;
		}

		onStack.push(r);
		oldParent = parent;
		oldParentStamp = parentStamp;
		oldParentOwner = parentOwner;
		parentStamp = r.stamp;
		parentOwner = r.owner;
		parent = r;

		try {
			if (INLET_DEBUG) {
				System.err.println("SATIN '" + ident.name()
						+ ": calling inlet caused by remote exception");
			}

			r.parentLocals.handleException(r.spawnId, r.eek, r);
			r.inletExecuted = true;

			// restore these, there may be more spawns afterwards...
			parentStamp = oldParentStamp;
			parentOwner = oldParentOwner;
			parent = oldParent;
			onStack.pop();

		} catch (Throwable t) {
			// The inlet has thrown an exception itself.
			// The semantics of this: throw the exception to the parent,
			// And execute the inlet if it has one (might be an empty one).
			// Also, the other children of the parent must be aborted.
			r.inletExecuted = true;

			// restore these, there may be more spawns afterwards...
			parentStamp = oldParentStamp;
			parentOwner = oldParentOwner;
			parent = oldParent;
			onStack.pop();

			if (INLET_DEBUG) {
				System.err.println("Got an exception from exception handler! "
						+ t);
				//						t.printStackTrace();
				System.err.println("r = " + r);
				System.err.println("r.parent = " + r.parent);
			}
			if (r.parent == null) {
				if (INLET_DEBUG) {
					System.err
						.println("An inlet threw an exception, but there is no parent that handles it: "
								+ t);
//					t.printStackTrace();
				}
				if (t instanceof Error) {
					Error te = (Error) t;
					throw te;
				}
				if (t instanceof RuntimeException) {
					RuntimeException tr = (RuntimeException) t;
					throw tr;
				}
				throw new IbisError(t);
//				System.exit(1);
			}

			if (ABORT_STATS) {
				aborts++;
			}

			synchronized (this) {
				// also kill the parent itself.
				// It is either on the stack or on a remote machine.
				// Here, this is OK, the child threw an exception,
				// the parent did not catch it, and must therefore die.
				r.parent.aborted = true;
				r.parent.eek = t; // rethrow exception
				killChildrenOf(r.parent.stamp, r.parent.owner);
			}

			if (!r.parentOwner.equals(ident)) {
				if (INLET_DEBUG || STEAL_DEBUG) {
					System.err.println("SATIN '" + ident.name()
							+ ": prematurely sending exception result");
				}
				sendResult(r.parent, null);
				return;
			}

			// two cases here: empty inlet or normal inlet
			if (r.parent.parentLocals == null) { // empty inlet
				handleEmptyInlet(r.parent);
			} else { // normal inlet
				handleInlet(r.parent);
			}
		}
	}

	// trace back from the exception, and execute inlets / empty imlets back to
	// the root
	// during this, prematurely send result messages.
	private void handleEmptyInlet(InvocationRecord r) {
		// if r does not have parentLocals, this means
		// that the PARENT does not have a try catch block around the spawn.
		// there is thus no inlet to call in the parent.

		if (r.eek == null)
			return;
		if (r.parent == null)
			return;
		//		if(r.parenLocals != null) return;

		if (ASSERTS && r.parentLocals != null) {
			System.err.println("parenlocals is not null in empty inlet");
			System.exit(1);
		}

		if(INLET_DEBUG) {
			out.println("SATIN '" + ident.name() + ": Got exception, empty inlet: "
				+ r.eek + ": " + r.eek.getMessage());
			r.eek.printStackTrace();
		}

		InvocationRecord curr = r;

		synchronized (this) {
			// also kill the parent itself.
			// It is either on the stack or on a remote machine.
			// Here, this is OK, the child threw an exception,
			// the parent did not catch it, and must therefore die.
			r.parent.aborted = true;
			r.parent.eek = r.eek; // rethrow exception
			killChildrenOf(r.parent.stamp, r.parent.owner);
		}

		if (!r.parentOwner.equals(ident)) {
			if (INLET_DEBUG || STEAL_DEBUG) {
				System.err.println("SATIN '" + ident.name()
						+ ": prematurely sending exception result");
			}
			sendResult(r.parent, null);
			return;
		}

		// now the recursion step
		if (r.parent.parentLocals != null) { // parent has inlet
			handleInlet(r.parent);
		} else {
			handleEmptyInlet(r.parent);
		}
	}

	void addToExceptionList(InvocationRecord r) {
		if (ASSERTS) {
			assertLocked(this);
		}
		exceptionList.add(r);
		gotExceptions = true;
		if (INLET_DEBUG) {
			out.println("SATIN '" + ident.name() + ": got remote exception!");
		}
	}

	// both here and in handleEmpty inlets: sendResult NOW if parentOwner is on
	// remote machine
	void handleExceptions() {
		if (ASSERTS && !ABORTS) {
			System.err
					.println("cannot handle inlets, set ABORTS to true in Config.java");
			System.exit(1);
		}

		InvocationRecord r;
		while (true) {
			synchronized (this) {
				r = exceptionList.removeIndex(0);
				if (r == null) {
					gotExceptions = false;
					return;
				}
			}

			if (INLET_DEBUG) {
				out.println("SATIN '" + ident.name()
						+ ": handling remote exception: " + r.eek + ", inv = "
						+ r);
			}

			//  If there is an inlet, call it.
			handleInlet(r);

			r.spawnCounter.value--;
			if (ASSERTS && r.spawnCounter.value < 0) {
				out.println("Just made spawncounter < 0");
				new Exception().printStackTrace();
				System.exit(1);
			}
			if (INLET_DEBUG) {
				out.println("SATIN '" + ident.name()
						+ ": handling remote exception DONE");
			}
		}
	}
}
