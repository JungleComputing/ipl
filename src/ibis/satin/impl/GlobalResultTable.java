package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.Upcall;
import ibis.ipl.WriteMessage;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class GlobalResultTable implements Upcall, Config {

	static class Key implements java.io.Serializable {
		int stamp;
		ParameterRecord parameters;
		
		Key(InvocationRecord r) {
			if (Satin.this_satin.branchingFactor > 0) {
				this.stamp = r.stamp;
				this.parameters = null;
			} else {
				this.stamp = -1;
				this.parameters = r.getParameterRecord();
			}
		}
		
		public boolean equals(Object other) {
			Key otherKey = (Key) other;
			if (Satin.this_satin.branchingFactor > 0) {
				return this.stamp == otherKey.stamp;
			} else {
				if (other == null) return false;
				return this.parameters.equals(otherKey.parameters);
			}
		}
		
		public int hashCode() {
			if (Satin.this_satin.branchingFactor > 0) {
				return this.stamp;
			} else {
				return this.parameters.hashCode();
			}
		} 
		
		public String toString() {
			if (Satin.this_satin.branchingFactor > 0) {
				return Integer.toString(stamp);
			} else {
				return parameters.toString();
			}
		}
	}
	
	static class Value implements java.io.Serializable {
		static final int TYPE_LOCK	= 0;
		static final int TYPE_RESULT	= 1;
		static final int TYPE_POINTER	= 2;
		int type;		    
		transient IbisIdentifier sendTo;
		ReturnRecord result;
		IbisIdentifier owner;
		
		Value(int type, InvocationRecord r) {
			this.type = type;
			this.owner = Satin.this_satin.ident;
			if (type == TYPE_RESULT) {
				result = r.getReturnRecord();
			}
		}
		
		public String toString() {
			String str = "";
			switch (type) {
				case TYPE_LOCK:
					str += "(LOCK,sendTo:" + sendTo + ")";
					break;
				case TYPE_RESULT:
					str += "(RESULT,result:" + result + ")";
					break;
				case TYPE_POINTER:
					str += "(POINTER,owner:" + owner + ")";
					break;
				default:
					System.err.println("SATIN '" + Satin.this_satin.ident.name() + "': illegal type in value");
			}
			return str;
		}
	}

	private Satin satin;

	private Map entries;

	/* used for communication with other replicas of the table */

	//private SendPort send;
	private ReceivePort receive;

	//a quick net ibis bug fix
	private Map sends = new Hashtable();

	private int numReplicas = 0;

	public int numResultUpdates = 0;
	public int numLockUpdates = 0;	
	public int numLookups = 0;
	public int numLookupsSucceded = 0;

	public int maxNumEntries = 0;

	public int numRemoteLookups = 0;
	
	public final static int max = 20;
	
	private Value pointerValue = new Value(Value.TYPE_POINTER, null);

	GlobalResultTable(Satin sat) {

		satin = sat;
		entries = new Hashtable();
		try {
			receive = satin.globalResultTablePortType.createReceivePort(
					"satin global result table receive port on "
							+ satin.ident.name(), this);
			//send = portType.createSendPort("satin global result table send
			// port on " +
			//    satin.ident.name());
			receive.enableUpcalls();
			receive.enableConnections();

		} catch (IOException e) {
			System.err.println("SATIN '" + satin.ident.name()
					+ "': Global result table - unable to create ports - "
					+ e.getMessage());
			e.printStackTrace();
		}
	}

	Value lookup(InvocationRecord r, boolean stats) {
		Key key = new Key(r);
		return lookup(key, stats);
	}
	
	Value lookup(Key key, boolean stats) {
		if (GRT_TIMING) {
			satin.lookupTimer.start();
		}
	
		if (ASSERTS) {
			Satin.assertLocked(satin);
		}
		
		Value value = (Value) entries.get(key);

		if (GRT_DEBUG) {
			if (value != null) {
				System.err.println("SATIN '" + satin.ident.name()
						+ "': lookup successful " + key);
			}
		}			
		if (GRT_STATS && stats) {
			if (value != null) {
				if (value.type == Value.TYPE_POINTER) {
//					if (satin.allIbises.contains(value.owner)) {
					if (!satin.deadIbises.contains(value.owner)) {
						numLookupsSucceded++;
						numRemoteLookups++;
					}
				} else {
					numLookupsSucceded++;
				}
			}
			numLookups++;
		}
		
		if (GRT_TIMING) {
			satin.lookupTimer.stop();
		}
		
		return value;
	}
	
	
	void storeResult(InvocationRecord r) {
		Key key = new Key(r);
		Value value = new Value(Value.TYPE_RESULT, r);
		update(key, value);
	}
		
	void storeLock(InvocationRecord r) {
		Key key = new Key(r);
		Value value = new Value(Value.TYPE_LOCK, null);
		update(key, value);
	}
	

	void update(Key key, Value value) {
	
		if (GRT_TIMING) {
			satin.updateTimer.start();
		}

		if (ASSERTS) {
			Satin.assertLocked(satin);
		}

		Object oldValue = entries.get(key);
		/* if (entries.size() < max) */entries.put(key, value);
		if (GRT_STATS) {
			if (entries.size() > maxNumEntries)
				maxNumEntries = entries.size();
		}

		if (numReplicas > 0 && oldValue == null) {
			if (GRT_DEBUG) {
				System.err.println("SATIN '" + satin.ident.name()
    				    	    + "': sending update: " + key + "," + value);
			}
			
			//send an update message
			Iterator sendIter = sends.values().iterator();
			long size = 0;
			int i = 0;
			while (sendIter.hasNext()) {
				try {
					SendPort send = (SendPort) sendIter.next();
					if (GRT_TIMING) {
						satin.tableSerializationTimer.start();
					}
					WriteMessage m = send.newMessage();
					m.writeObject(key);
					if (GLOBAL_RESULT_TABLE_REPLICATED) {
						m.writeObject(value);						
					} else {
						m.writeObject(pointerValue);
						//m.writeObject(satin.ident);
					}
					size = m.finish();
					
					if (GRT_TIMING) {
						satin.tableSerializationTimer.stop();
					}
					/*System.err.println("SATIN '" + satin.ident.name() + "': " + size 
					+ " sent in " + satin.tableSerializationTimer.lastTimeVal()
					+ " to " + send.connectedTo()[0].ibis().name());*/
					
				} catch (IOException e) {
					//always happens after a crash
				}
			}
			

			//send an update message
			/*
			 * try { WriteMessage m = send.newMessage(); m.writeObject(key);
			 * m.writeObject(value); m.finish(); } catch (IOException
			 * e) { //always happens after the crash }
			 */
			if (GRT_DEBUG) {
				System.err.println("SATIN '" + satin.ident.name()
					+ "': update sent: " + key + "," + value);
			}
			 
		}
		if (GRT_STATS) {
			if (value.type == Value.TYPE_RESULT) {
				numResultUpdates++;
			}
			if (value.type == Value.TYPE_LOCK) {
				numLockUpdates++;
			}
		}
		if (GRT_DEBUG) {
			System.err.println("SATIN '" + satin.ident.name()
				+ "': update complete: " + key + "," + value);
		}
		
		if (GRT_TIMING) {
			satin.updateTimer.stop();
		}		
		
	}


	//returns ready to send contents of the table
	Map getContents() {
		if (ASSERTS) {
			Satin.assertLocked(satin);
		}

		if (GLOBAL_RESULT_TABLE_REPLICATED) {
			return (Map) ((Hashtable) entries).clone();
		} else {
			//replace "real" results with pointer values
			Map newEntries = new Hashtable();
			Iterator iter = entries.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry element = (Map.Entry) iter.next();
				Value value = (Value) element.getValue();
				Key key = (Key) element.getKey();
				switch (value.type) {
					case Value.TYPE_RESULT:
					case Value.TYPE_LOCK:
						newEntries.put(key, pointerValue);
						break;
					case Value.TYPE_POINTER:
						newEntries.put(key, value);
						break;
					default:
						System.err.println("SATIN '" + satin.ident.name()
							+ "': EEK invalid value type in getContents()");
				}
			}
			return newEntries;
		}
	}

	void addContents(Map contents) {
		if (ASSERTS) {
			Satin.assertLocked(satin);
		}
		
//		System.err.println("adding contents");

		entries.putAll(contents);

		if (GRT_STATS) {
			if (entries.size() > maxNumEntries)
				maxNumEntries = entries.size();
		}

	}

	void addReplica(IbisIdentifier ident) {
		if (ASSERTS) {
			Satin.assertLocked(satin);
		}

		try {
			SendPort send = satin.globalResultTablePortType
					.createSendPort("satin global result table send port on "
							+ satin.ident.name() + System.currentTimeMillis());
			sends.put(ident, send);
			ReceivePortIdentifier r = null;
			r = satin.lookup("satin global result table receive port on "
					+ ident.name());
			if (Satin.connect(send, r, satin.connectTimeout)) {
				numReplicas++;
			} else {
				System.err.println("SATN '" + satin.ident.name()
					+ "': Transpositon table - unable to add new replica");
			}
		} catch (IOException e) {
			System.err.println("SATN '" + satin.ident.name()
					+ "': Transpositon table - unable to add new replica");
			e.printStackTrace();
		}

	}

	void removeReplica(IbisIdentifier ident) {
		if (ASSERTS) {
			Satin.assertLocked(satin);
		}

		if (sends.remove(ident) != null) {
			numReplicas--;
		}

	}

	void exit() {
		try {
			synchronized (satin) {
				if (numReplicas > 0) {
					Iterator sendIter = sends.values().iterator();
					while (sendIter.hasNext()) {
						SendPort send = (SendPort) sendIter.next();
						send.close();
					}
				}
				//		send.close();
			}
			receive.close();
		} catch (IOException e) {
			System.err.println("SATIN '" + satin.ident.name()
					+ "': Unable to free global result table ports");
			e.printStackTrace();
		}
	}

	public void upcall(ReadMessage m) {
		if (GRT_TIMING) {
			satin.handleUpdateTimer.start();
		}
		try {
			
			if (GRT_TIMING) {
				satin.tableDeserializationTimer.start();
			}
			Key key = (Key) m.readObject();
			Value value = (Value) m.readObject();
			//IbisIdentifier ident = (IbisIdentifier) m.readObject();
			//Value value = new Value(Value.TYPE_POINTER, null);
			//value.owner = ident;
			if (GRT_TIMING) {
				satin.tableDeserializationTimer.stop();
			}


			synchronized (satin) {
				/* if (entries.size() < max) */entries.put(key, value);
				if (GRT_STATS) {
					if (entries.size() > maxNumEntries)
						maxNumEntries = entries.size();
				}
			}

			if (GRT_DEBUG) {
				System.err.println("SATIN '" + satin.ident.name()
						+ "': upcall finished:" + key + "," + value + "," + entries.size());
			}

		} catch (IOException e) {
			System.err.println("SATIN '" + satin.ident.name()
					+ "': Global result table - error reading message");
			e.printStackTrace();
		} catch (ClassNotFoundException e1) {
			System.err.println("SATIN '" + satin.ident.name()
					+ "': Global result table - error reading message");
			e1.printStackTrace();
		}
		if (GRT_TIMING) {
			satin.handleUpdateTimer.stop();
		}
	}

	public void print(java.io.PrintStream out) {
		synchronized (satin) {
			out.println("=GRT: " + satin.ident.name() + "=");
			int i = 0;
			Iterator iter = entries.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				out.println("GRT[" + i + "]= " + entry.getKey() + ";"
						+ entry.getValue());
				i++;
			}
			out.println("=end of GRT " + satin.ident.name() + "=");
		}
	}
}
