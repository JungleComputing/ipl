/*
 * Created on 21.01.2005
 */
package ibis.mpj;


import java.io.*;
import java.util.*;

/**
 * Communicator for point-to-point messages.
 */
public class Comm {
	
	protected Group group;
	protected int contextId = MPJ.UNDEFINED;
	protected boolean isInterComm = false;
	
	
	
	/**
	 * @throws MPJException
	 */
	 public Comm() throws MPJException {
		int contextId = MPJ.getNewContextId();
	
		MPJ.setNewContextId(contextId);
		
		this.contextId = contextId;
		
	}
	
	// Communicator Management
	
	
	/**
	 * Return the rank inside the group of this communicator
	 * @return the rank inside the group of this communicator
	 * @throws MPJException
	 */
	public int rank() throws MPJException {
		return this.group.rank();
	}
	
	/**
	 * Size of the group of this communicator.
	 * 
	 * @return number of processes in the group of this communicator
	 * @throws MPJException
	 */
	public int size() throws MPJException {
		return this.group.size();
	}
	
	
	/**
	 * Return group associated with this communicator.
	 * @return group corresponding to this communicator
	 * @throws MPJException
	 */
	public Group group() throws MPJException {
		return this.group;
	}
	
	

	/**
	 * Compare two communicators.
	 * MPJ.IDENT results if this communicator and comm1 are references to the same object.
	 * MPJ.CONGRUENT results if the underlying groups are identical but the communicators differ by context.
	 * MPJ.SIMILAR results if the group members of both communicators are the same but the rank order differs.
	 * MPJ.UNEQUAL results otherwise
	 * @param comm1 communicator to compare with
	 * @return MPJ.IDENT, MPJ.CONGRUENT of MPJ.UNEQUAL
 	 * @throws MPJException
	 */
	public static int compare(Comm comm1, Comm comm2) throws MPJException {
		
		
	    if (comm1 == comm2) {
			return(MPJ.IDENT);
		}
		else if (Group.compare(comm1.group(), comm2.group()) == MPJ.IDENT) {
			if ((comm1.contextId == MPJ.UNDEFINED) || (comm2.contextId == MPJ.UNDEFINED)) {
				return(MPJ.CONGRUENT);
			}
			
			if (comm1.contextId == comm2.contextId) {
				return(MPJ.IDENT);
			}
			else {
				return(MPJ.CONGRUENT);
			}
		}
		else if (Group.compare(comm1.group(), comm2.group()) == MPJ.SIMILAR) {
			return(MPJ.SIMILAR);
		}
		
		else {
			return(MPJ.UNEQUAL);
		}
	}
	
	
	
	/**
	 * Duplicate this communicator.
	 * The new communicator is "congruent" to the old one, but has a differnt context.
	 * @return copy of this communicator
	 */
	public Object clone() {
		
		try {
			if (!testInter()) {
				Comm cpComm = new Comm();
				
				cpComm.group = new Group();
			
				cpComm.group.table = (Vector)this.group.table.clone();
				return(cpComm);
			
			}
			else {
				return(null);
			}
		}
		catch (MPJException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			return(null);
		}
	}
	
	public void free() throws MPJException {
		// Nothing to do here, we are not using JNI
	}
	
	
	// Inter-communication
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * 
	 * Test if this is an inter-communicator, false otherwise.
	 * 
	 * @return true if this is an inter-communicator
	 * @throws MPJException
	 */
	public boolean testInter() throws MPJException {
		return(false);
		
	}
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * Create an inter communicator.
	 * 
	 * @param localComm local intra communicator
	 * @param localLeader rank of local leader in localComm
	 * @param remoteLeader rank of remote leader in this communicator
	 * @param tag "safe tag"
	 * @return new inter communicator
	 * @throws MPJException
	 */
	public Intercomm createIntercomm(Comm localComm, int localLeader, int remoteLeader, int tag) throws MPJException {
		return(null);
	}
	

	// Caching
	
	/**
	 * Retrieves attribute value by key. Valid key values are MPJ.TAG_UB, MPJ.HOST, MPJ.IO and MPJ.WTIME_IS_GLOBAL
	 * @param keyval one of the key values predefined by MPJ
	 * @return attribute value
	 * @throws MPJException
	 */
	public Object attrGet(int keyval) throws MPJException {
		if (keyval == MPJ.TAG_UB) {
			return(new Integer(Integer.MAX_VALUE));
		}
		else if (keyval == MPJ.HOST) {
			return(new String(MPJ.getMyMPJHostName()));
		}
		else if (keyval == MPJ.IO) {
			return(new Integer(MPJ.ANY_SOURCE));
		}
		else if (keyval == MPJ.WTIME_IS_GLOBAL) {
			return(new Integer(0));
		}
		else {
			throw new MPJException("unknown key value.");
		}
	}
	
	
	// Blocking Send and Receive operations
	
	/**
	 * Blocking send operation.
	 * The data part of the message consists of a sequence of count values, 
	 * each of the type indicated by datatype. The actual argument associated
	 * with buf must be an array. The value of offset is a subscript in this array,
	 * defining the position of the first item of the message.
	 * The elements of buf may have primitive type or class type. If the elements are
	 * objects, they must be serializable objects.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @throws MPJException
	 */
	public void send(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		IbisMPJComm ibisMPJ = new IbisMPJComm(this, this.rank(), buf, offset, count, datatype, this.rank(), dest, tag, IbisMPJComm.OP_ISEND);
		
		ibisMPJ.doIsend();
		
	}
	
	/**
	 * Blocking receive operation.
	 * The actual argument associated with buf must be an array. 
	 * The value offset is a subscript in this array, defining the position into which the first item of the incoming message will be copied.
	 * 
	 * @param buf receive buffer array
	 * @param offset initial offset in receive buffer
	 * @param count number of items in receive buffer
	 * @param datatype datatype of each item in receive buffer
	 * @param source rank of source
	 * @param tag message tag
	 * @return status object
	 * @throws MPJException
	 */
	public Status recv(Object buf, int offset, int count, Datatype datatype, int source, int tag) throws MPJException {
		IbisMPJComm ibisMPJ = new IbisMPJComm(this, this.rank(), buf, offset, count, datatype, source, this.rank(), tag, IbisMPJComm.OP_IRECV);
		
		ibisMPJ.doIrecv(true);
		return(ibisMPJ.getStatus());
		
	}
	
	
	// Communication Modes
	
	/**
	 * Send in buffered mode.
	 * Further comment as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @throws MPJException
	 */
	public void bsend(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		IbisMPJComm ibisMPJ = new IbisMPJComm(this, this.rank(), buf, offset, count, datatype, this.rank(), dest, tag, IbisMPJComm.OP_IBSEND);
		ibisMPJ.doIbsend();
	}
	
	/**
	 * Send in synchronous mode.
	 * Further comments as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @throws MPJException
	 */
	public void ssend(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		IbisMPJComm ibisMPJ = new IbisMPJComm(this, this.rank(), buf, offset, count, datatype, this.rank(), dest, tag, IbisMPJComm.OP_ISSEND);

		ibisMPJ.doIssend();
		
	}
	
	/**
	 * Send in ready mode.
	 * Further comments as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @throws MPJException
	 */
	public void rsend(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		IbisMPJComm ibisMPJ = new IbisMPJComm(this, this.rank(), buf, offset, count, datatype, this.rank(), dest, tag, IbisMPJComm.OP_IRSEND);
		
		ibisMPJ.doIrsend();
		
	}
	
	
	// Nonblocking Modes
	
	/**
	 * Start a standard mode, nonblocking send.
	 * Further comments as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @return communication request
	 * @throws MPJException
	 */
	public Request isend(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		IbisMPJComm ibisMPJComm = new IbisMPJComm(this, this.group.rank(), buf, offset, count, datatype, this.group.rank(), dest, tag, IbisMPJComm.OP_ISEND);

		Request request = ibisMPJComm.getRequest();
		request.ibisMPJCommThread = new Thread(ibisMPJComm);
		request.ibisMPJCommThread.start();

		
		return(request);
		
	} 
	
	/**
	 * Start a buffered mode, nonblocking send.
	 * Further comments as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @return communication request
	 * @throws MPJException
	 */
	public Request ibsend(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		IbisMPJComm ibisMPJComm = new IbisMPJComm(this, this.group.rank(), buf, offset, count, datatype, this.group.rank(), dest, tag, IbisMPJComm.OP_IBSEND);

		Request request = ibisMPJComm.getRequest();
		request.ibisMPJCommThread = new Thread(ibisMPJComm);
		request.ibisMPJCommThread.start();

		
		return(request);
	}
	
	/**
	 * Start a synchronous mode, nonblocking send.
	 * Further comments as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @return communication request
	 * @throws MPJException
	 */
	public Request issend(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		IbisMPJComm ibisMPJComm = new IbisMPJComm(this, this.group.rank(), buf, offset, count, datatype, this.group.rank(), dest, tag, IbisMPJComm.OP_ISSEND);

		Request request = ibisMPJComm.getRequest();
		request.ibisMPJCommThread = new Thread(ibisMPJComm);
		request.ibisMPJCommThread.start();

		
		return(request);
	}

	/**
	 * Start a ready mode, nonblocking send.
	 * Further comments as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @return communication request
	 * @throws MPJException
	 */
	public Request irsend(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		IbisMPJComm ibisMPJComm = new IbisMPJComm(this, this.group.rank(), buf, offset, count, datatype, this.group.rank(), dest, tag, IbisMPJComm.OP_IRSEND);

		Request request = ibisMPJComm.getRequest();
		request.ibisMPJCommThread = new Thread(ibisMPJComm);
		request.ibisMPJCommThread.start();

		
		return(request);
	}
	
	
	
	/**
	 * Start a nonblocking receive.
	 * Further comments as for recv.
	 * 
	 * @param buf receiver buffer array
	 * @param offset initial offset in receive buffer
	 * @param count number of items in receive buffer
	 * @param datatype datatype of each item in receive buffer
	 * @param source rank of source
	 * @param tag message tag
	 * @return communication request
	 * @throws MPJException
	 */
	public Request irecv(Object buf, int offset, int count, Datatype datatype, int source, int tag) throws MPJException {
		IbisMPJComm ibisMPJComm = new IbisMPJComm(this, this.group.rank(), buf, offset, count, datatype, source, this.group.rank(), tag, IbisMPJComm.OP_IRECV);

		Request request = ibisMPJComm.getRequest();
		request.ibisMPJCommThread = new Thread(ibisMPJComm);
		request.ibisMPJCommThread.start();
		
		
		return(request);		
	}


	// Probe and Cancel
	
	/**
	 * Check if there is an incoming message matching the pattern specified.
	 * If such a message is currently available, a status object similar to the return value of 
	 * a matching recv operation is returned. Otherwise a null reference is returned.
	 * 
	 * @param source source rank
	 * @param tag tag value
	 * @return status object or null reference
	 * @throws MPJException
	 */
	public Status iprobe(int source, int tag) throws MPJException {
		
		IbisMPJComm ibisMPJ = new IbisMPJComm(this, this.rank(), null, 0, 0, null, source, this.rank(), tag, IbisMPJComm.OP_IPROBE);
		ibisMPJ.doIprobe();
		
		return(ibisMPJ.getStatus());
		
		
	}
	
	

	/**
	 * Wait until there is an incoming message matching the pattern specified.
	 * Return a status object similar to the return value of a matching recv operation.
	 * 
	 * @param source source rank
	 * @param tag tag value
	 * @return status object or null reference
	 * @throws MPJException
	 */
	public Status probe(int source, int tag) throws MPJException {
		
		IbisMPJComm ibisMPJ = new IbisMPJComm(this, this.rank(), null, 0, 0, null, source, this.rank(), tag, IbisMPJComm.OP_PROBE);
		
		ibisMPJ.doIprobe();
		
		return(ibisMPJ.getStatus());
	}
	
	
	// Persistent communication requests
	// since ALL connections are persistent, these methods only exist 
	// due to compatibility purposes (incl. class Prequest) 
	// using Blocking send/recv operations
	
	/**
	 * Creates a persistent communication request for a standard mode send.
	 * Further comments as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @return peristent communication request
	 * @throws MPJException
	 */
	public Prequest sendInit(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		Prequest preq = new Prequest();
		preq.buf = buf;
		preq.offset = offset;
		preq.count = count;
		preq.datatype = datatype;
		preq.rank = dest;
		preq.tag = tag;
		preq.mode = Prequest.OP_SEND;
		preq.comm = this;
		return(preq);
	}

	
	/**
	 * Creates a persistent communication request for a buffered mode send.
	 * Further comment as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @return peristent communication request
	 * @throws MPJException
	 */
	public Prequest bsendInit(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		Prequest preq = new Prequest();
		preq.buf = buf;
		preq.offset = offset;
		preq.count = count;
		preq.datatype = datatype;
		preq.rank = dest;
		preq.tag = tag;
		preq.mode = Prequest.OP_BSEND;
		preq.comm = this;
		return(preq);
	}


	/**
	 * Creates a persitent communication request for a synchronous mode send.
	 * Further comments as for send.
	 * 
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @return peristent communication request
	 * @throws MPJException
	 */
	public Prequest ssendInit(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		Prequest preq = new Prequest();
		preq.buf = buf;
		preq.offset = offset;
		preq.count = count;
		preq.datatype = datatype;
		preq.rank = dest;
		preq.tag = tag;
		preq.mode = Prequest.OP_SSEND;
		preq.comm = this;
		return(preq);
	}

	/**
	 * Creates a persistent communication request for a ready mode send.
	 * Further comments as for send.
	 *  
	 * @param buf send buffer array
	 * @param offset initial offset in send buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param tag message tag
	 * @return peristent communication request
	 * @throws MPJException
	 */
	public Prequest rsendInit(Object buf, int offset, int count, Datatype datatype, int dest, int tag) throws MPJException {
		Prequest preq = new Prequest();
		preq.buf = buf;
		preq.offset = offset;
		preq.count = count;
		preq.datatype = datatype;
		preq.rank = dest;
		preq.tag = tag;
		preq.mode = Prequest.OP_RSEND;
		preq.comm = this;
		return(preq);
	}


	/**
	 * Creates a persistent communication request for a receive operation.
	 * Further comments as for recv.
	 * 
	 * @param buf receive buffer array
	 * @param offset initial offset in receive buffer
	 * @param count number of items in receive buffer
	 * @param datatype datatype of each item in receive buffer
	 * @param source rank of source
	 * @param tag message tag
	 * @return peristent communication request
	 * @throws MPJException
	 */
	public Prequest recvInit(Object buf, int offset, int count, Datatype datatype, int source, int tag) throws MPJException {
		Prequest preq = new Prequest();
		preq.buf = buf;
		preq.offset = offset;
		preq.count = count;
		preq.datatype = datatype;
		preq.rank = source;
		preq.tag = tag;
		preq.mode = Prequest.OP_RECV;
		preq.comm = this;
		return(preq);
	}
	
	
	// Send-receive
	
	/**
	 * Execute a blocking send and receive operation.
	 * Further comments as for send and recv.
	 * 
	 * @param sendbuf send buffer array
	 * @param sendoffset initial offset in send buffer
	 * @param sendcount number of items to send
	 * @param sendtype datatype of each item in send buffer
	 * @param dest rank of destination
	 * @param sendtag send tag
	 * @param recvbuf receive buffer array
	 * @param recvoffset initial offset in receive buffer
	 * @param recvcount number of items in receive buffer
	 * @param recvtype datatype of each item in recveive buffer
	 * @param source rank of source
	 * @param recvtag receive tag
	 * @return status object
	 * @throws MPJException
	 */ 
	public Status sendrecv(Object sendbuf, int sendoffset, int sendcount, Datatype sendtype, int dest, int sendtag,
						   Object recvbuf, int recvoffset, int recvcount, Datatype recvtype, int source, int recvtag) throws MPJException {
		
		Request req = this.irecv(recvbuf, recvoffset, recvcount, recvtype, source, recvtag);
		this.send(sendbuf, sendoffset, sendcount, sendtype, dest, sendtag);
		return(req.Wait());
	}
	
	
	
	/**
	 * Execute a blocking send and receive operation, receiving message into send buffer.
	 * Further comments as for send and recv.
	 * 
	 * @param buf buffer array
	 * @param offset initial offset in buffer
	 * @param count number of items to send
	 * @param datatype datatype of each item in buffer
	 * @param dest rank of destination
	 * @param sendtag send tag
	 * @param source rank of source
	 * @param recvtag receive tag
	 * @return status object
	 * @throws MPJException
	 */
	public Status sendrecvReplace(Object buf, int offset, int count, Datatype datatype, int dest, int sendtag, int source, int recvtag) throws MPJException {
		
		Object tempBuf = null;
		if (buf instanceof byte[]) {
			tempBuf = new byte[((byte[])buf).length];
			System.arraycopy(buf, offset, tempBuf, offset, count * datatype.extent());
		}
		else if (buf instanceof char[]) {
			tempBuf = new char[((char[])buf).length];		
			System.arraycopy(buf, offset, tempBuf, offset, count * datatype.extent());
		}
		else if (buf instanceof short[]) {
			tempBuf = new short[((short[])buf).length];		
			System.arraycopy(buf, offset, tempBuf, offset, count * datatype.extent());
		}
		else if (buf instanceof boolean[]) {
			tempBuf = new boolean[((boolean[])buf).length];		
			System.arraycopy(buf, offset, tempBuf, offset, count * datatype.extent());
		}
		else if (buf instanceof int[])  {
			tempBuf = new int[((int[])buf).length];		
			System.arraycopy(buf, offset, tempBuf, offset, count * datatype.extent());
		}
		else if (buf instanceof long[]) {
			tempBuf = new long[((long[])buf).length];		
			System.arraycopy(buf, offset, tempBuf, offset, count * datatype.extent());
		}
		else if (buf instanceof float[]) {
			tempBuf = new float[((float[])tempBuf).length];		
			System.arraycopy(buf, offset, tempBuf, offset, count * datatype.extent());
		}
		else if (buf instanceof double[]) {
			tempBuf = new double[((double[])buf).length];		
			System.arraycopy(buf, offset, tempBuf, offset, count * datatype.extent());
		}
		else {
			tempBuf = new Object[((Object[])buf).length];		
			System.arraycopy(buf, offset, tempBuf, offset, count * datatype.extent());
		}
		
		Request req = this.irecv(buf, offset, count, datatype, source, recvtag);
		this.send(tempBuf, offset, count, datatype, dest, sendtag);
		return(req.Wait());

	}

	
	// Pack and unpack
	
	/**
	 * Packs message in send buffer inbuf into space specified in outbuf.
	 * The return value is the output value of position-the initial value incremented by the
	 * number of byte written.
	 * 
	 * @param inbuf input buffer array
	 * @param offset initial offset in input buffer
	 * @param incount number of items in input buffer
	 * @param datatype datatype of each item in input buffer
	 * @param outbuf output buffer
	 * @param position initial position in output buffer
	 * @return final position in output buffer
	 * @throws MPJException
	 */
	public int pack(Object inbuf, int offset, int incount, Datatype datatype, byte[] outbuf, int position) throws MPJException {
		if (datatype == MPJ.BYTE) {
			return(IbisMPJComm.bufferByte(inbuf, outbuf, offset, incount, position));
		}
		else if (datatype == MPJ.CHAR) {
			return(IbisMPJComm.bufferChar(inbuf, outbuf, offset, incount, position));
		}
		else if (datatype == MPJ.SHORT) {
			return(IbisMPJComm.bufferShort(inbuf, outbuf, offset, incount, position));
		}
		else if (datatype == MPJ.BOOLEAN) {
			return(IbisMPJComm.bufferBoolean(inbuf, outbuf, offset, incount, position));
		}
		else if (datatype == MPJ.INT)  {
			return(IbisMPJComm.bufferInt(inbuf, outbuf, offset, incount, position));
		}
		else if (datatype == MPJ.LONG) {
			return(IbisMPJComm.bufferLong(inbuf, outbuf, offset, incount, position));
		}
		else if (datatype == MPJ.FLOAT) {				
			return(IbisMPJComm.bufferFloat(inbuf, outbuf, offset, incount, position));			
		}
		else if (datatype == MPJ.DOUBLE) {
			return(IbisMPJComm.bufferDouble(inbuf, outbuf, offset, incount, position));
		}
		else if (datatype == MPJ.OBJECT) {
			return(IbisMPJComm.bufferObject(inbuf, outbuf, offset, incount, position));
		}
		return(0);
	}
	
	
	/**
	 * Packs message in send buffer inbuf into buffer space allocated internally
	 * @param inbuf input buffer array
	 * @param offset initial offset in input buffer
	 * @param incount number of items in input buffer
	 * @param datatype datatype of each item in input buffer
	 * @return output buffer
	 * @throws MPJException
	 */
	public byte[] pack(Object inbuf, int offset, int incount, Datatype datatype) throws MPJException {
		byte[] outbuf = new byte[incount * datatype.getByteSize()];
		
		if (datatype == MPJ.BYTE) {
			IbisMPJComm.bufferByte(inbuf, outbuf, offset, incount, 0);
			return(outbuf);
		}
		else if (datatype == MPJ.CHAR) {
			IbisMPJComm.bufferChar(inbuf, outbuf, offset, incount, 0);
			return(outbuf);
		}
		else if (datatype == MPJ.SHORT) {
			IbisMPJComm.bufferShort(inbuf, outbuf, offset, incount, 0);
			return(outbuf);
		}
		else if (datatype == MPJ.BOOLEAN) {
			IbisMPJComm.bufferBoolean(inbuf, outbuf, offset, incount, 0);
			return(outbuf);
		}
		else if (datatype == MPJ.INT)  {
			IbisMPJComm.bufferInt(inbuf, outbuf, offset, incount, 0);
			return(outbuf);
		}
		else if (datatype == MPJ.LONG) {
			IbisMPJComm.bufferLong(inbuf, outbuf, offset, incount, 0);
			return(outbuf);
		}
		else if (datatype == MPJ.FLOAT) {				
			IbisMPJComm.bufferFloat(inbuf, outbuf, offset, incount, 0);
			return(outbuf);
		}
		else if (datatype == MPJ.DOUBLE) {
			IbisMPJComm.bufferDouble(inbuf, outbuf, offset, incount, 0);
			return(outbuf);
		}
		else if (datatype == MPJ.OBJECT) {
			IbisMPJComm.bufferObject(inbuf, outbuf, offset, incount, 0);
			return(outbuf);
		}
		return(null);
	}

	
	/**
	 * Unpacks message in receive buffer outbuf into space specified in inbuf.
	 * The return value is the output value of position - the internal value incremented by the number
	 * of bytes read.
	 * @param inbuf input buffer
	 * @param position initial position in input buffer
	 * @param outbuf output buffer array
	 * @param offset initial offset in output buffer
	 * @param outcount number of items in output buffer
	 * @param datatype datatype of each item in output buffer
	 * @return final position in input buffer
	 * @throws MPJException
	 */
	public int unpack(byte[] inbuf, int position, Object outbuf, int offset, int outcount, Datatype datatype) throws MPJException {
	    if (datatype == MPJ.BYTE) {
	    	return(IbisMPJComm.unBufferByte(inbuf, position, outbuf, offset, outcount));
	    }
	    else if (datatype == MPJ.CHAR) {
	    	return(IbisMPJComm.unBufferChar(inbuf, position, outbuf, offset, outcount));
	    }
	    else if (datatype == MPJ.SHORT) {
	    	return(IbisMPJComm.unBufferShort(inbuf, position, outbuf, offset, outcount));
	    }
	    else if (datatype == MPJ.BOOLEAN) {
	    	return(IbisMPJComm.unBufferBoolean(inbuf, position, outbuf, offset, outcount));
	    }
	    else if (datatype == MPJ.INT) {
	    	return(IbisMPJComm.unBufferInt(inbuf, position, outbuf, offset, outcount));
	    }
	    else if (datatype == MPJ.LONG) {
	    	return(IbisMPJComm.unBufferLong(inbuf, position, outbuf, offset, outcount));
	    }
	    else if (datatype == MPJ.FLOAT) {
	    	return(IbisMPJComm.unBufferFloat(inbuf, position, outbuf, offset, outcount));
	    }
	    else if (datatype == MPJ.DOUBLE) {
	    	return(IbisMPJComm.unBufferDouble(inbuf, position, outbuf, offset, outcount));
	    }
	    else if ((datatype == MPJ.OBJECT)) {
	    	return(IbisMPJComm.unBufferObject(inbuf, position, outbuf, offset, outcount));
	    }
	    return(0);
	}
	
	/**
	 * Returns an upper bound on the increment of position effected by pack.
	 * It is an error to call this function if the base type of datatype is MPJ.OBJECT.
	 * @param incount number of items in input buffer
	 * @param datatype datatype of each item in input buffer
	 * @return upper bound on size of packed message 
	 * @throws MPJException
	 */
	public int packSize(int incount, Datatype datatype) throws MPJException {
		if (datatype.type == MPJ.OBJECT.type)
			throw new MPJException ("MPJ.OBJECT is not allowed here.");
		
		return(datatype.getByteSize() * incount);
	}
	
	// Process Topologies
	
	/**
	 * <strong> NOT IMPLEMENTED YET. </strong>
	 * 
	 * Returns the type of topology associated with the communicator.
	 * The return value will be one of MPJ.GRAPH, MPJ.CART or MPJ.UNDEFINED.
	 * 
	 * @return topology type of communicator
	 * @throws MPJException
	 */
	public int topoTest() throws MPJException {
		return(0);
	}
	
	
	// Environmental Management
	
/*	public static void errorhandlerSet(Errhandler errhandler) throws MPJException {
		
	}

	public static Errhandler errorhandlerGet() throws MPJException {
		return(null);
	}
	*/
	public void abort(int errorcode) throws MPJException {
	  	throw new MPJException("Abort called. Errorcode: " + errorcode);
	}



	public Object copyBuffer(Object inbuf, int offset, int count, Datatype datatype) throws MPJException {
		count = count * datatype.extent();
		
		if (inbuf instanceof byte[]) {
	    	byte[] ibuf = (byte[])inbuf;
			byte[] obuf = new byte[count];
			
			if ((offset + count) > ((byte[])inbuf).length ) {
				count = ((byte[])inbuf).length - offset;
			}
			
			for (int i = offset; i<offset+count; i++) {
				obuf[i-offset] = ibuf[i];
			}
			
			return(obuf);
	    }
	    else if (inbuf instanceof char[]) {
	    	char[] ibuf = (char[])inbuf;
			char[] obuf = new char[count];

			if ((offset + count) > ((char[])inbuf).length ) {
				count = ((char[])inbuf).length - offset;
			}

			for (int i = offset; i<offset+count; i++) {
				obuf[i-offset] = ibuf[i];
			}
			return(obuf);
	    }
	    else if (inbuf instanceof short[]) {
	    	short[] ibuf = (short[])inbuf;
			short[] obuf = new short[count];
			
			if ((offset + count) > ((short[])inbuf).length ) {
				count = ((short[])inbuf).length - offset;
			}

			for (int i = offset; i<offset+count; i++) {
				obuf[i-offset] = ibuf[i];
			}
			return(obuf);
	    }
	    else if (inbuf instanceof boolean[]) {
	    	boolean[] ibuf = (boolean[])inbuf;
			boolean[] obuf = new boolean[count];
			
			if ((offset + count) > ((boolean[])inbuf).length ) {
				count = ((boolean[])inbuf).length - offset;
			}

			for (int i = offset; i<offset+count; i++) {
				obuf[i-offset] = ibuf[i];
			}
			return(obuf);
	    }
	    else if (inbuf instanceof int[]) {
	    	int[] ibuf = (int[])inbuf;
			int[] obuf = new int[count];
			
			if ((offset + count) > ((int[])inbuf).length ) {
				count = ((int[])inbuf).length - offset;
			}

			for (int i = offset; i<offset+count; i++) {
				obuf[i-offset] = ibuf[i];
			}
			return(obuf);
	    }
	    else if (inbuf instanceof long[]) {
	    	long[] ibuf = (long[])inbuf;
			long[] obuf = new long[count];
			
			if ((offset + count) > ((long[])inbuf).length ) {
				count = ((long[])inbuf).length - offset;
			}

			for (int i = offset; i<offset+count; i++) {
				obuf[i-offset] = ibuf[i];
			}
			return(obuf);
	    }
	    else if (inbuf instanceof float[]) {
	    	float[] ibuf = (float[])inbuf;
			float[] obuf = new float[count];
			
			if ((offset + count) > ((float[])inbuf).length ) {
				count = ((float[])inbuf).length - offset;
			}

			for (int i = offset; i<offset+count; i++) {
				obuf[i-offset] = ibuf[i];
			}
			return(obuf);
	    }
	    else if (inbuf instanceof double[]) {
	    	double[] ibuf = (double[])inbuf;
			double[] obuf = new double[count];
			
			if ((offset + count) > ((double[])inbuf).length ) {
				count = ((double[])inbuf).length - offset;
			}

			for (int i = offset; i<offset+count; i++) {
				obuf[i-offset] = ibuf[i];
			}
			return(obuf);
	    }
	    else if (datatype == MPJ.OBJECT) {

	    	ByteArrayOutputStream byteStream = null;
	    	ObjectOutputStream objectStream = null;

	    	Object[] ibuf = (Object[])inbuf;

	    	try {
	    		byteStream = new ByteArrayOutputStream();
	    		objectStream = new ObjectOutputStream(byteStream);
	    	
				if ((offset + count) > ((Object[])inbuf).length ) {
					count = ((Object[])inbuf).length - offset;
				}

	    		
	    		for (int i = offset; i < (offset + count); i++) {
	    		    objectStream.writeObject(ibuf[i]);
	    		}
	    		
	    		
	    		return(byteStream.toByteArray());
	    	} 
	    	catch (IOException e) {
	    		System.err.println(e.getMessage());
	    		throw new MPJException(e.getMessage());
	    		

	    	}
	    }
	    else {
	    	throw new MPJException("copyBuffer: Buffer instance type not found.");
	    }
		
		
	}



	protected void localcopy(Object inbuf, int inoffset, int incount, Datatype intype, 
							 Object outbuf, int outoffset, int outcount, Datatype outtype) throws MPJException {
		
	
		int inExtent = intype.extent();
		int outExtent = outtype.extent();
		int inLength = 0;
		int outLength = 0;
		Object tmpbuf = null;
		
		
		if (inbuf instanceof int[]) {
			if (!(outbuf instanceof int[])) {
				throw new MPJException("inbuf and outbuf must be the same type.");
			}
			
			
			inLength = ((int[])inbuf).length;
			outLength = ((int[])outbuf).length;
			
			tmpbuf = new int[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		
		}
	
		
		
		
		if (inbuf instanceof byte[]) {
			if (!(outbuf instanceof byte[])) {
				throw new MPJException("inbuf and outbuf must be of the same type.");
			}
			
			
			inLength = ((byte[])inbuf).length;
			outLength = ((byte[])outbuf).length;
			
			tmpbuf = new byte[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		}
		else if (inbuf instanceof char[]) {
			if (!(outbuf instanceof char[])) {
				throw new MPJException("inbuf and outbuf must be of the same type.");
			}
			
			inLength = ((char[])inbuf).length;
			outLength = ((char[])outbuf).length;
			
			tmpbuf = new char[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		}
		else if (inbuf instanceof short[]) {
			if (!(outbuf instanceof short[])) {
				throw new MPJException("inbuf and outbuf must be of the same type.");
			}
			
			
			inLength = ((short[])inbuf).length;
			outLength = ((short[])outbuf).length;
			
			tmpbuf = new short[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		}
		else if (inbuf instanceof boolean[]) {
			if (!(outbuf instanceof boolean[])) {
				throw new MPJException("inbuf and outbuf must be of the same type.");
			}
			
			
			inLength = ((boolean[])inbuf).length;
			outLength = ((boolean[])outbuf).length;
			
			tmpbuf = new boolean[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		}
		else if (inbuf instanceof int[])  {
			if (!(outbuf instanceof int[])) {
				throw new MPJException("inbuf and outbuf must be of the same type.");
			}
			
			
			inLength = ((int[])inbuf).length;
			outLength = ((int[])outbuf).length;
			
			tmpbuf = new int[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		
		}
		else if (inbuf instanceof long[]) {
			if (!(outbuf instanceof long[])) {
				throw new MPJException("inbuf and outbuf must be of the same type.");
			}
			
			
			inLength = ((long[])inbuf).length;
			outLength = ((long[])outbuf).length;
			
			tmpbuf = new long[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		}
		else if (inbuf instanceof float[]) {
			if (!(outbuf instanceof float[])) {
				throw new MPJException("inbuf and outbuf must be of the same type.");
			}
			
			inLength = ((float[])inbuf).length;
			outLength = ((float[])outbuf).length;
			
			tmpbuf = new float[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		}
		else if (inbuf instanceof double[]) {
			if (!(outbuf instanceof double[])) {
				throw new MPJException("inbuf and outbuf must be of the same type.");
			}
			
			inLength = ((double[])inbuf).length;
			outLength = ((double[])outbuf).length;
			
			tmpbuf = new double[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		}
		else {
			inLength = ((Object[])inbuf).length;
			outLength = ((Object[])outbuf).length;
			
			tmpbuf = new Object[incount * inExtent];
			while (inLength < inoffset + incount * inExtent) {
				incount--;
			}
			
			while (outLength < outoffset + outcount * outExtent) {
				outcount--;
			}
		}

		System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
		System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());

	}

}
