package ibis.repmi;

import ibis.ipl.ReadMessage;
import ibis.ipl.Upcall;

import java.io.IOException;

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
		} catch (ClassNotFoundException ec) { 
			System.out.println(RTS._rank + ": Got an exception in GroupCallHandler !" + ec);
			ec.printStackTrace();
		} catch (IOException e) { 
			System.out.println(RTS._rank + ": Got an exception in GroupCallHandler !" + e);
			e.printStackTrace();
		} 
	}
} 
