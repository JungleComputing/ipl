package ibis.gmi;

import ibis.ipl.ReadMessage;

import java.io.IOException;

/**
 * The {@link Forwarder} class serves as a base class for user-defined forwarders.
 * A forwarder is to be defined for the {@link ForwardReply} reply scheme.
 * When this reply scheme is used, all replies of a group method invocation
 * are passed to this forwarder object, through calls to a "forward" method.
 *
 * This class is not abstract, because the user-defined forwarder does not have
 * to supply all "forward" methods (for all different result types). Therefore,
 * default ones are supplied that just throw an exception.
 */
public class Forwarder implements GroupProtocol { 
    /* Set when this forwarder is busy. */
    private boolean inUse = false;

    /* The number of results to be expected. */
    private int numResults = 0;

    /* The number of results received sofar. */
    private int receivedResults = 0;

    /* Ticket for the reply stack. */
    private int ticket;

    /* The stub that did the invocation from which this forwarder gets results. */
    private GroupStub stub;

    /**
     * Invoked when a "void" result is received.
     *
     * @param rank the rank number of the group member from which this reply was received
     * @param size the total number of replies to be expected
     */
    public void forward(int rank, int size) { 
	throw new RuntimeException("void Forwarder.forward(int, int) not implemented");
    } 

    /**
     * Invoked when a "boolean" result is received.
     *
     * @param rank the rank number of the group member from which this reply was received
     * @param size the total number of replies to be expected
     * @param result the replied value
     */
    public void forward(int rank, int size, boolean result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, boolean) not implemented");
    } 

    /**
     * See {@link #forward(int,int,boolean)}, but for a byte result.
     */
    public void forward(int rank, int size, byte result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, byte) not implemented");
    } 

    /**
     * See {@link #forward(int,int,boolean)}, but for a char result.
     */
    public void forward(int rank, int size, char result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, char) not implemented");
    } 

    /**
     * See {@link #forward(int,int,boolean)}, but for a short result.
     */
    public void forward(int rank, int size, short result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, short) not implemented");
    } 

    /**
     * See {@link #forward(int,int,boolean)}, but for an int result.
     */
    public void forward(int rank, int size, int result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, int) not implemented");
    } 

    /**
     * See {@link #forward(int,int,boolean)}, but for a long result.
     */
    public void forward(int rank, int size, long result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, long) not implemented");
    } 

    /**
     * See {@link #forward(int,int,boolean)}, but for a float result.
     */
    public void forward(int rank, int size, float result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, float) not implemented");
    } 

    /**
     * See {@link #forward(int,int,boolean)}, but for a double result.
     */
    public void forward(int rank, int size, double result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, double) not implemented");
    } 

    /**
     * See {@link #forward(int,int,boolean)}, but for an Object result.
     */
    public void forward(int rank, int size, Object result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, Object) not implemented");
    } 

    /**
     * Invoked when the group method invoked caused an exception.
     *
     * @param rank the rank number of the group member from which this reply was received
     * @param size the total number of replies to be expected
     * @param result the exception
     */
    public void forward(int rank, int size, Exception result) { 
	throw new RuntimeException("void Forwarder.forward(int, int, Exception) not implemented");
    } 

    /**
     * Initiate receiving and forwarding a reply for a group method invoked
     * by the {@link GroupStub} "stub". This is done by placing this forwarder
     * on the reply stack of the stub, so that a reply handler can invoke the
     * forwarder, once it receives a reply.
     *
     * @param stub the stub expecting a reply
     * @param numResults the total number of replies to be expected
     * @param ticket the ticket number for the stub's reply stack
     */
    public void startReceiving(GroupStub stub, int numResults, int ticket) { 
	
	/* Assumes stub is locked !!! */

	if (inUse) { 
	    throw new RuntimeException("Forwarder allready in use !");
	} 

	inUse = true;
	this.numResults = numResults;
	this.ticket = ticket;
	this.stub = stub;
	receivedResults = 0;
	
	stub.replyStack.put(ticket, this);
    } 

    /**
     * This is the method invoked by the reply handler. This method will invoke
     * the correct "forward" method.
     *
     * @param r the read message from which the reply is to be read
     */
    protected void receive(ReadMessage r) throws IOException { 

	// stub is synchronized when this is called

	byte result_type;
	int rank;

	rank = r.readInt();		
	result_type = r.readByte();
	    
	switch (result_type) { 
	case RESULT_VOID:
	    forward(rank, numResults);
	    break;
	case RESULT_BOOLEAN:
	    try { 
		boolean result = r.readBoolean();
		forward(rank, numResults, result);
	    } catch (Exception e) {
		forward(rank, numResults, e);				
	    }
	    break;
	case RESULT_BYTE:
	    try {
		byte result = r.readByte();
		forward(rank, numResults, result);
	    } catch (Exception e) {
		forward(rank, numResults, e);				
		    }
	    break;
	case RESULT_SHORT:
	    try {
		short result = r.readShort();
		forward(rank, numResults, result);
	    } catch (Exception e) {
		forward(rank, numResults, e);				
	    }
	    break;
	case RESULT_CHAR:
	    try {
		char result = r.readChar();
		forward(rank, numResults, result);
	    } catch (Exception e) {
		forward(rank, numResults, e);				
	    }
	    break;
	case RESULT_INT:
	    try {
		int result = r.readInt();
		forward(rank, numResults, result);
	    } catch (Exception e) {
		forward(rank, numResults, e);				
	    }
	    break;
	case RESULT_LONG:
	    try {
		long result = r.readLong();
		forward(rank, numResults, result);
	    } catch (Exception e) {
		forward(rank, numResults, e);				
	    }
	    break;
	case RESULT_FLOAT:
	    try {
		float result = r.readFloat();
		forward(rank, numResults, result);
	    } catch (Exception e) {
		forward(rank, numResults, e);				
	    }
	    break;
	case RESULT_DOUBLE:
	    try {
		double result = r.readDouble();
		forward(rank, numResults, result);
	    } catch (Exception e) {
		forward(rank, numResults, e);				
	    }
	    break;
	case RESULT_OBJECT:
	    try {
		Object result = r.readObject();
		forward(rank, numResults, result);
	    } catch (Exception e) {
		forward(rank, numResults, e);
	    }
	    break;
	case RESULT_EXCEPTION:
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
//System.out.println("Freeing position " + ticket);
	    stub.replyStack.freeTicket(ticket);
	    inUse = false;
	} 
    }
} 
