package ibis.satin;

import ibis.ipl.IbisError;

//import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import ibis.util.TypedProperties;
//import java.util.ArrayList;

/** This class implements an immutable global tuple space.
 * A tuple consists of a key (a String) and its associated data
 * (a serializable object).
 * Note that the data is <strong>not</strong> immutable, because
 * the {@link #get(String) get()} method does not make a copy. 
 */
public class SatinTupleSpace implements Config {

        // The satin reference can be null if the run is completely sequential.
	// space must be synchronized, adds and dels arrive asynchronously
	private static Satin satin;
	private static HashMap space;
	static boolean use_seq = false;
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
		use_seq =
		    SUPPORT_TUPLE_MULTICAST &&
		    satin != null &&
		    TypedProperties.booleanProperty("satin.tuplespace.ordened");
//		System.out.println("use_seq = " + use_seq);
//		newKeys = new ArrayList();
//		newData = new ArrayList();
	}

    public static void enableActiveTupleOrdening() {
	if(satin == null) return; // sequential run
	if(!SUPPORT_TUPLE_MULTICAST) {
	    System.err.println("Cannot enable active tuple ordening, SUPPORT_TUPLE_MULTICAST is set to false in Config.java");
	    System.exit(1);
	}

	use_seq = true;

	satin.enableActiveTupleOrdening();
//	System.out.println("use_seq = " + use_seq);
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
		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": added key " + key);
		}

		if (satin != null) { // can happen with sequential versions of Satin programs
			satin.broadcastTuple(key, data);
		}
		if (! use_seq || satin == null) {
		    if(data instanceof ActiveTuple) {
			((ActiveTuple)data).handleTuple(key);
		    }
		    else {
			synchronized(space) {
			    space.put(key, data);
			}
		    }
		}

		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": bcast key " + key + " done");
		}
	}

	/** Retrieves an element from the tuple space. 
	    If the element is not in the space yet, 
	    this operation blocks until the element is inserted.
	    @param key the key of the element retrieved.
	    @return the data associated with the key.
	 **/
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

	/** Removes an element from the tuple space.
	 * @param key the key of the tuple to be removed.
	 **/
	public static void remove(String key) {

		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name() + ": removed key " + key);
		}
		if(satin != null) {
			satin.broadcastRemoveTuple(key);
			if(TUPLE_DEBUG) {
				System.err.println("SATIN '" + satin.ident.name() + ": bcast remove key " + key + " done");
			}
		}
		if (! use_seq || satin == null) {
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
