package ibis.satin;

import ibis.ipl.IbisError;

//import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
//import java.util.ArrayList;

/** This class implements an immutable global tupe space. **/
public class SatinTupleSpace implements Config {

	// space must be synchronized, adds and dels arrive asynchronously
	private static Satin satin;
	private static HashMap space;
//	private static ArrayList newKeys;
//	private static ArrayList newData;

	static {
		satin = Satin.this_satin;

		if(satin != null && !satin.closed) {
			System.err.println("The tuple space currently only works with a closed world. Try running with -satin-closed");
			System.exit(1);
//			throw new IbisError("The tuple space currently only works with a closed world. Try running with -satin-closed");
		}

		space = new HashMap();
//		newKeys = new ArrayList();
//		newData = new ArrayList();
	}

	/** Adds an element with the specified key to the global tuple space. 
          * If a tuple with this key already exists, it is overwritten
          * with the new element. The propagation to other processors can
          * take an arbitrary amount of time, but it is guaranteed that
          * after multiple updates by the same processor, eventually all
          * processors will have the latest value.
          * <p>
          * However, if multiple processors 
          * update the value of the same key, the value
          * of an updated key can be different on different processors.
          * @param key The key of the new tuple.
          * @param data The data associated with the key.
           **/
	public static void add(String key, Serializable data) {
		synchronized(space) {
			space.put(key, data);
//			newKeys.add(key);
//			newData.add(data);
		}

		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": added key " + key);
		}

		if (satin != null) { // can happen with sequential versions of Satin programs
			satin.broadcastTuple(key, data);
		}

		if(data instanceof ActiveTuple) {
			((ActiveTuple)data).handleTuple(key);
		}

		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": bcast key " + key + " done");
		}
	}

	/** Retrieves an element from the tuple space. 
	    If the element is not in the space yet, 
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
	public static void remove(String key) {
		synchronized(space) {
			if(ASSERTS && !space.containsKey(key)) {
				throw new IbisError("Key " + key + " is not in the tuple space");
			}

			space.remove(key);
			
			// also remove it from the new lists (if there)
//			int index = newKeys.indexOf(key);
//			if(index != -1) {
//				newData.remove(index);
//			}
		}

		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": removed key " + key);
		}
		if(satin != null) {
			satin.broadcastRemoveTuple(key);
			if(TUPLE_DEBUG) {
				System.err.println("SATIN '" + satin.ident.name() + ": bcast remove key " + key + " done");
			}
		}
	}

	/** Get a "new" element from the tuple space.
	    New is defined as: an element in the tuple space that has
	    not been retrieved with a getNew call before on this
	    machine.
	    This call does not work for active tuples.
	**/
/*
  public static Tuple getNew() {
  synchronized(space) {
  if(newKeys.size() == 0) {
  return null;
  }

  Tuple res = new Tuple();
  res.key = (String ) newKeys.remove(0);
  res.data = (Serializable) newData.remove(0);

  return res;
  }
  }
*/
	protected static void remoteAdd(String key, Serializable data) {
		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": remote add of key " + key);
		}

		synchronized(space) {
			space.put(key, data);
//			newKeys.add(key);
//			newData.add(data);
			space.notifyAll();
		}
	}

	protected static void remoteDel(String key) {
		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": remote del of key " + key);
		}
		synchronized(space) {
			space.remove(key);

			// also remove it from the new lists (if there)
//			int index = newKeys.indexOf(key);
//			if(index != -1) {
//				newData.remove(index);
//			}
		}
	}
}
