package ibis.group;

import ibis.ipl.*;
import ibis.util.SpecialStack;

public class Forwarder { 

	private boolean inUse = false;

	public void forward(int rank, int size) { 
		throw new RuntimeException("void Forwarder.forward(int, int) not implemented");
	} 

	public void forward(int rank, int size, boolean result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, boolean) not implemented");
	} 

	public void forward(int rank, int size, byte result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, byte) not implemented");
	} 

	public void forward(int rank, int size, char result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, char) not implemented");
	} 

	public void forward(int rank, int size, short result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, short) not implemented");
	} 

	public void forward(int rank, int size, int result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, int) not implemented");
	} 

	public void forward(int rank, int size, long result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, long) not implemented");
	} 

	public void forward(int rank, int size, float result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, float) not implemented");
	} 

	public void forward(int rank, int size, double result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, double) not implemented");
	} 

	public void forward(int rank, int size, Object result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, Object) not implemented");
	} 

	public void forward(int rank, int size, Exception result) { 
		throw new RuntimeException("void Forwarder.forward(int, int, Exception) not implemented");
	} 


	/* =============================================================================================== */

	synchronized void resetUse() { 		
		inUse = false;
	} 

	private int numResults = 0, receivedResults = 0;
	private SpecialStack replyStack;
	private int ticket;
	private GroupStub stub;
	
	public void startReceiving(GroupStub stub, int numResults, SpecialStack replyStack, int ticket) { 
		
		// assumes stub is locked !!!

		if (inUse) { 
			throw new RuntimeException("Forwarder allready in use !");
		} 

		// start receiving here. Thread will terminate automatically when 
		// "numResults" results are received.	

		inUse = true;
		this.numResults = numResults;
		this.replyStack = replyStack;
		this.ticket = ticket;
		this.stub = stub;
		
//		System.out.println("startReceiving " + numResults + " " + ticket);		
		stub.replyStack.putData(ticket, this);

//		ForwarderReceiveThread thread = Group.getForwarderReceiveThread();
//		thread.startReceiving(this, stub, replyStack, ticket, numResults);	
	} 

	void receive(ReadMessage r) throws IbisException, IbisIOException { 

		// stub is synchronized when this is called

		byte result_type;
		int rank;

		rank = r.readInt();		
		result_type = r.readByte();
			
		switch (result_type) { 
		case Group.RESULT_VOID:
			forward(rank, numResults);
			break;
		case Group.RESULT_BOOLEAN:
			try { 
				boolean result = r.readBoolean();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);				
			}
			break;
		case Group.RESULT_BYTE:
			try {
				byte result = r.readByte();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);				
					}
			break;
		case Group.RESULT_SHORT:
			try {
				short result = r.readShort();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);				
			}
			break;
		case Group.RESULT_CHAR:
			try {
				char result = r.readChar();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);				
			}
			break;
		case Group.RESULT_INT:
			try {
				int result = r.readInt();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);				
			}
			break;
		case Group.RESULT_LONG:
			try {
				long result = r.readLong();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);				
			}
			break;
		case Group.RESULT_FLOAT:
			try {
				float result = r.readFloat();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);				
			}
					break;
		case Group.RESULT_DOUBLE:
			try {
				double result = r.readDouble();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);				
			}
			break;
		case Group.RESULT_OBJECT:
			try {
				Object result = r.readObject();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);
			}
			break;
		case Group.RESULT_EXCEPTION:
			try {
				Exception result = (Exception) r.readObject();
				forward(rank, numResults, result);
			} catch (Exception e) {
				forward(rank, numResults, e);
			}
			break;
		}	
		receivedResults++;
//System.out.println("Received Results " + receivedResults);
		if (receivedResults == numResults) { 
//			System.out.println("Freeing position " + ticket);
			stub.replyStack.freePosition(ticket);
		} 
	}
} 










