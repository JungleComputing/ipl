package manta.repmi;

import manta.ibis.*;

final class CallHandler implements Protocol, Upcall { 
	
	public void upcall(ReadMessage m) { 

		byte opcode;

		try { 
			opcode = m.readByte();
			
			switch (opcode) { 

			case NEW_OBJECT:
				if (RTS.DEBUG) System.out.println(RTS._rank + ": Got a REGISTRY");
				RTS.newObject(m);
				break;
				
			case INVOCATION:
				if (RTS.DEBUG) System.out.println(RTS._rank + ": Got an INVOCATION");
				RTS.findSkeleton(m.readInt()).handleMessage(m);
				break;

			default: 
				System.out.println(RTS._rank + ": Got an illegal opcode !");
			} 
		} catch (IbisIOException e) { 
			System.out.println(RTS._rank + ": Got an exception in GroupCallHandler !" + e);
			e.printStackTrace();
		} 
	}
} 
