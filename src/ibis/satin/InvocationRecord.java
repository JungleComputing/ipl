package ibis.satin;
import ibis.ipl.IbisIdentifier;
import java.io.IOException;

public abstract class InvocationRecord implements java.io.Serializable, Config {
	/* Of all fields, only the owner and the stamp must be sent over the network. Parents too .*/

	public IbisIdentifier owner;
	public int stamp; /* Used to locate this invocation record, when a remote job result comes in. */

	public IbisIdentifier parentOwner;
	public int parentStamp;

	public transient InvocationRecord parent;

	protected transient SpawnCounter spawnCounter;
	public transient Throwable eek;
	public transient InvocationRecord cacheNext; /* Used to link the records in the cache. */
	public transient boolean aborted;

	/* These are used to link the records in the JobQueue. Not needed when Dijkstra is used. */
//	public transient InvocationRecord qprev;
//	public transient InvocationRecord qnext;

	public transient int storeId; /* An id for the store where the result of the spawn must go. */
	public transient int spawnId; /* An id for the spawn in the code. Needed to run the correct inlet. */
	public transient LocalRecord parentLocals;
	public transient IbisIdentifier stealer;


	protected InvocationRecord(SpawnCounter spawnCounter, InvocationRecord cacheNext,
				   int storeId, int spawnId, LocalRecord parentLocals) {
		this.storeId = storeId;
		this.cacheNext = cacheNext;
		this.spawnCounter = spawnCounter;
		if(ABORTS) {
			this.spawnId = spawnId;
			this.parentLocals = parentLocals;
		}
	}

	/* @@@ many of these are not necesary, but usefull for debugging */
	final public void clear() {
		owner = null;
		stamp = -2;
		spawnCounter = null;

//		qprev = null;
//		qnext = null;

		storeId = -2;
		stealer = null;

		if(ABORTS) {
			eek = null;
			parentOwner = null;
			parentStamp = -2;
			parent = null;
			aborted = false;
			spawnId = -2;
			parentLocals = null;
		}
	}

	final public boolean equals(InvocationRecord other) {
		if(other == this) return true;
		return stamp == other.stamp && owner.equals(other.owner);
	}

	final public boolean equals(Object o) {
		if(o == this) return true;
		if(o instanceof InvocationRecord) {
			InvocationRecord other = (InvocationRecord) o;
			return stamp == other.stamp && owner.equals(other.owner);
		} else {
			if(Config.ASSERTS) {
				System.out.println("warning: weird equals in Invocationrecord");
			}
			return false;
		}
	}

	final public int hashCode() {
		return stamp;
	}

	public String toString() {
		String result = "(Invocation record: stamp = " + stamp;
			result += ", owner = " + (owner == null ? "NULL" : "" + owner);
			result += ", spawnCounter = " + (spawnCounter == null ? "NULL" : "" + spawnCounter.value);
			result += ", parentStamp = " + parentStamp;
			result += ", parentOwner = " + (parentOwner == null ? "NULL" : "" + parentOwner);
			result += ", parentLocals = " + (parentLocals == null ? "NULL" : "" + parentLocals) + ")";

			return result;
	}

/*
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		System.err.print("w");
		out.writeObject(owner);
		out.writeObject(parentOwner);
		out.writeInt(stamp);
		out.writeInt(parentStamp);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		System.err.print("r");
		owner = (IbisIdentifier) in.readObject();
		parentOwner = (IbisIdentifier) in.readObject();
		stamp = in.readInt();
		parentStamp = in.readInt();
	}
*/
	public abstract void runLocal();
	public abstract ReturnRecord runRemote();
}
