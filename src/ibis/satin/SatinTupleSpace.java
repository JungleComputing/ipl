package ibis.satin;

import ibis.ipl.IbisError;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/** This class implements an immutable global tupe space. **/
public class SatinTupleSpace implements Config {

	// space must be synchronized, adds and dels arrive asynchronously
	private static Satin satin;
	private static HashMap space;

	static {
		satin = Satin.this_satin;
		if(satin == null) {
			throw new IbisError("Internal error: Satin not initialized");
		}

		space = new HashMap();
	}

	/** add an element to the global tuple space. The key must be unique. **/
	public static void add(String key, Serializable data) {
		synchronized(space) {
			if(ASSERTS && space.containsKey(key)) {
				throw new IbisError("Key " + key + " is already in the tuple space");
			}

			space.put(key, data);
		}
		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": added key " + key);
		}
		satin.broadcastTuple(key, data);
		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": bcast key " + key + " done");
		}
	}

	/** Retrieves an element from the tuple space. If the element is not in the space yet, 
	    this operation blocks until the element is inserted. **/
	public static Serializable get(String key) {
		Serializable data = null;

//		if(TUPLE_DEBUG) {
//			System.err.println("SATIN '" + satin.ident.name() + ": get key " + key);
//		}

		synchronized(space) {
			while (data == null) {
				data = (Serializable) space.get(key);
				if(data == null) {
					try {
						if(TUPLE_DEBUG) {
							System.err.println("SATIN '" + satin.ident.name() + ": get key " 
									   + key + " waiting");
						}

						space.wait();
						if(TUPLE_DEBUG) {
							System.err.println("SATIN '" + satin.ident.name() + ": get key " 
									   + key + " waiting DONE");
						}

					} catch (Exception e) {
						// Ignore.
					}
				}
			}

//			if(TUPLE_DEBUG) {
//				System.err.println("SATIN '" + satin.ident.name() + ": get key " + key + " DONE");
//			}

			return data;
		}
	}

	/** Remove an element from the tuple space. **/
	public static void remove(String key) throws IOException {
		synchronized(space) {
			if(ASSERTS && !space.containsKey(key)) {
				throw new IbisError("Key " + key + " is not in the tuple space");
			}

			space.remove(key);
		}

		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": removed key " + key);
		}
		satin.broadcastRemoveTuple(key);
		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": bcast remove key " + key + " done");
		}
	}

	protected static void remoteAdd(String key, Serializable data) {
		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": remote add of key " + key);
		}

		synchronized(space) {
			space.put(key, data);
			space.notifyAll();
		}
	}

	protected static void remoteDel(String key) {
		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": remote del of key " + key);
		}
		synchronized(space) {
			space.remove(key);
		}
	}
}
