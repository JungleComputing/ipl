/*
 * Created on 04.02.2005
 */
package ibis.mpj;

import java.io.*;


import ibis.ipl.*;



/**
 * Implementation of the basic point-to-point communication modes on top of ibis.
 */
public class IbisMPJComm extends Thread {
	private final boolean DEBUG = false;
	
	protected final static int OP_IRECV = 1;
	protected final static int OP_ISEND = 2;
	protected final static int OP_IBSEND = 3;
	protected final static int OP_ISSEND = 4;
	protected final static int OP_IRSEND = 5;
	protected final static int OP_IPROBE = 6;
	protected final static int OP_PROBE = 7;
	
	private boolean buffered = false;
	
	protected Request request;
	protected Object buf;
	protected int offset;
	protected int count;
	protected Datatype datatype;
	protected int dest;
	protected int source;
	protected int tag;
	protected int opCode;
	protected Comm comm;
	protected MPJObject m = null;
	protected int myRank;
	protected Status status = null;
	protected int mode = -1;
	protected int contextId = -1;
    
    protected boolean finished = false;
    

    public IbisMPJComm() {
    	
    }
    
    public IbisMPJComm(Comm comm, int myRank, Object buf, int offset, int count, 
    				   Datatype datatype, int source, int dest, int tag, int mode) {

    	this.buf = buf;
    	this.offset = offset;
    	this.count = count;
    	this.datatype = datatype;
    	this.dest = dest;
    	this.source = source;
    	this.tag = tag;
    	this.mode = mode;
    	this.comm = comm;
    	this.myRank = myRank;
    	this.request = new Request();
    	this.request.setIbisMPJComm(this);
    	this.status = new Status();
    	this.status.setSource(0);
    	this.status.setTag(0);
    	this.status.setCount(0);
    	this.contextId = comm.contextId;
    	this.finished = false;
    
    }

    protected Request getRequest() {
    	return(request);
    }
    
	protected Status getStatus() {
		return(status);
	}
	
	
	private void doIRecvAnySource() throws MPJException {
		int typeSize = 1;
		int realCount = 0;
		int extent = datatype.extent();
		MPJObject obj = null;
		this.status = new Status();
		ReadMessage msg = null;
		//boolean DEBUG = true;

		
		
		if (DEBUG) {
			System.out.println("irecv: " + this.comm.rank() + ": " + MPJ.getMyMPJHostName() + ": ANY_SOURCE");
		}
		
		int src = 0;
		boolean msgReceived = false;
		while (!msgReceived) {

			String mpjHostName = this.comm.group().getMPJHostName(src);
			Connection con = MPJ.getConnection(mpjHostName);
			MPJObjectQueue queue = con.getRecvQueue();
			
			
			if (queue.isLocked()) {
				src = nextSource(src);
				continue;
			}
			else {
				
				// 	first check the object queue and hold the monitor on it
				synchronized(queue) {
					
					// set Lock to inform another irecv using MPJ.ANY_SOURCE
					queue.lock();
					
					obj = null;
					obj = queue.getObject(contextId, tag);
					
					// if queue has not the requested object, try to connect to the receiveport
					if (obj == null) {
						
				
						obj = new MPJObject();
						msg = null;
									
									
						msg = con.pollForMessage();
								
						if (msg == null) {
							src = nextSource(src);

							queue.release();
							if (DEBUG) System.out.println("message was null... go to next rank");
							continue;
						}
						else {
							// 	get message header
							con.receiveHeader(msg, obj.desc);
							
							if(DEBUG) {
								System.out.println("recv tag:       " + obj.getTag());
								System.out.println("recv contextId: " + obj.getContextId());
								System.out.println("recv type:      " + obj.getBaseDatatype());
								System.out.println("recv count:     " + obj.getNumberOfElements());
								System.out.println("recv offset:    " + offset);
								
								System.out.println("supposed count: " + count * datatype.extent());
							}
							
							
							// 	the message was expected
							if (((obj.getTag() == tag) || 
								((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
								(obj.getContextId() == contextId)) {
								
								
								if (!obj.isBuffered()) {
									// 	if count is larger than the array received 
									// and message NOT buffered -> cut count
									if ((count*datatype.extent() > obj.getNumberOfElements()) && (!obj.isBuffered())) {
										count = obj.getNumberOfElements();
										con.receiveData(msg, buf, offset, count * datatype.extent());	
									}
									// 	if count is smaller than the array received
									// 	and message NOT buffered -> copy buffer
									else if(((count*datatype.extent()) < obj.getNumberOfElements()) && (!obj.isBuffered())) {
										
										obj.initBuffer();
										con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());
											
										castBuffer(obj, buf, offset, count*extent);
											
									}
		
									// count is equal to received array size 
									// and message NOT buffered -> receive it directly
									else if (!obj.isBuffered()){
										con.receiveData(msg, buf, offset, count * datatype.extent());
									}
										
										
											
								}							
								// else the message was buffered -> unbuffer it
								else {
									byte[] byteBuf = new byte[obj.getNumberOfElements()+1];
									
									con.receiveData(msg, byteBuf, 0, obj.getNumberOfElements());
									
									realCount = unBuffer(byteBuf, buf, offset, count * extent);
								}
								
								typeSize = datatype.byteSize;
								realCount = count * datatype.extent();
								msgReceived = true;						
							}
							//		the message was NOT expected -> move to queue
							else {
								if(DEBUG) {
									System.out.println("Message was NOT expected -> move to queue");
								}
								obj.initBuffer();
								con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());
								
								queue.addObject(obj);
								msgReceived = false;
								
								src = nextSource(src);
							
							}
						}
					
					}
						// found the object inside the queue
					else {
						if (DEBUG) System.out.println("found the object inside the queue");
						
						typeSize = datatype.getByteSize();
						// 	message was NOT buffered -> normal copy
						if (!obj.isBuffered()) {
							realCount = castBuffer(obj, buf, offset, count*extent);
						}
							
						// 	message was buffered -> unbuffer it
						else {
							realCount = unBuffer(obj.getObjectData(), buf, offset, count * (extent));
						}
						msgReceived = true;
					}
					queue.release();
				}
			}
		}
		
		
		
		status.setSource(src);
		status.setTag(obj.getTag());
		status.setCount(realCount);
		status.setSize(realCount * typeSize);
	}
	
	private int nextSource(int prevSrc) throws MPJException {
		int newSrc = prevSrc++;
		
		if (prevSrc == this.comm.size()) {
			prevSrc = 0;
		}
		
		return (prevSrc);
		
	}
	
	
	private void doIRecvSource(boolean blocking) throws MPJException {
		int typeSize = 1;
		int realCount = 0;
		int extent = datatype.extent();
		MPJObject obj = null;
		this.status = new Status();
		ReadMessage msg = null;
		//boolean DEBUG = true;

		
		String mpjHostName = this.comm.group().getMPJHostName(source);
		Connection con = MPJ.getConnection(mpjHostName);
		MPJObjectQueue queue = con.getRecvQueue();
		
		if (DEBUG) {
			System.out.println("irecv: " + this.comm.rank() + ": " + MPJ.getMyMPJHostName() + ": SOURCE: " + mpjHostName + "; rank: " + source);
		}
			
		// 	first check the object queue and hold the monitor on it
		boolean msgReceived = false;
		while (!msgReceived) {
		
			synchronized(queue) {
				
				// set Lock to inform another irecv using MPJ.ANY_SOURCE
				queue.lock();
			
				obj = null;
				obj = queue.getObject(contextId, tag);
				// found the object inside the queue
				if (obj != null) {
					if(DEBUG) {
						System.out.println("Message was found inside the queue expected -> move out of queue (mytag: " + tag + ")");
					}
					typeSize = datatype.getByteSize();
					// 	message was NOT buffered -> normal copy
					if (!obj.isBuffered()) {
						realCount = castBuffer(obj, buf, offset, count*extent);
					}
				
					// message was buffered -> unbuffer it
					else {
						realCount = unBuffer(obj.getObjectData(), buf, offset, count * (extent));
					}
					if(DEBUG) {
						System.out.println("recv tag:       " + obj.getTag());
						System.out.println("recv contextId: " + obj.getContextId());
						System.out.println("recv type:      " + obj.getBaseDatatype());
						System.out.println("recv count:     " + obj.getNumberOfElements());
						System.out.println("recv offset:    " + offset);
					
						System.out.println("supposed tag:   " + this.tag);
						System.out.println("supposed contID:" + this.contextId);
						System.out.println("supposed count: " + count * datatype.extent());
					}
					msgReceived = true;
			
				}		
				// if queue has not the requested object, try to connect to the receiveport
				else if (obj == null) {

					obj = new MPJObject();
					msg = null;
					
			/*		if(!blocking) {
						msg =con.pollForMessage();
						
						if (msg==null) continue;
					}
					else {*/
						msg = con.getNextMessage();	
					//}
						
					// 	get message header
					con.receiveHeader(msg, obj.desc);
						
					if(DEBUG) {
						System.out.println("recv tag:       " + obj.getTag());
						System.out.println("recv contextId: " + obj.getContextId());
						System.out.println("recv type:      " + obj.getBaseDatatype());
						System.out.println("recv count:     " + obj.getNumberOfElements());
						System.out.println("recv offset:    " + offset);
						
						System.out.println("supposed tag:   " + this.tag);
						System.out.println("supposed contID:" + this.contextId);
						System.out.println("supposed count: " + count * datatype.extent());
						if (obj.isBuffered()) System.out.println("BUFFERED");
						else System.out.println("NOT BUFFERED");
					}
						
					
					// 	the message was expected
					if (((obj.getTag() == tag) || 
						((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
						(obj.getContextId() == contextId)) {
							
						
						if (!obj.isBuffered()) {
							// if count is larger than the array received 
							// and message NOT buffered -> cut count
							if (((count*datatype.extent()) > obj.getNumberOfElements()) && (!obj.isBuffered())) {
								count = obj.getNumberOfElements();
								con.receiveData(msg, buf, offset, count * datatype.extent());	
							}
							// 	if count is smaller than the array received
							// 	and message NOT buffered -> copy buffer
							else if((count*(datatype.extent()+1) < obj.getNumberOfElements()) && (!obj.isBuffered())) {
								obj.initBuffer();
								con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());
								
								castBuffer(obj, buf, offset, count*extent);
							}

							// count is equal to received array size 
							// and message NOT buffered -> receive it directly
							else if (!obj.isBuffered()){
								con.receiveData(msg, buf, offset, count * datatype.extent());
							}
						}							
						// else the message was buffered -> unbuffer it
						else {
							if (DEBUG) {
								System.out.println("Message was expected and buffered -> unbuffer it");
	
							}
							byte[] byteBuf = new byte[obj.getNumberOfElements()];

							con.receiveData(msg, byteBuf, 0, obj.getNumberOfElements());
							realCount = unBuffer(byteBuf, buf, offset, count * extent);
						}
							
						typeSize = datatype.byteSize;
						realCount = count * datatype.extent();
						
						msgReceived = true;						
					}
					// 	the message was NOT expected -> move to queue
					else {
						if(DEBUG) {
							System.out.println("Message was NOT expected -> move to queue");
						}
						obj.initBuffer();
						con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());
							
						queue.addObject(obj);
						msgReceived = false;
						
					}
				
				}
			
				queue.release();
						
			}
		
			
		}			
		
		status.setSource(source);
		status.setTag(obj.getTag());
		status.setCount(realCount);
		status.setSize(realCount * typeSize);
		
	}
		
	
	
	protected void doIrecv(boolean blocking) throws MPJException {
		if (source == MPJ.PROC_NULL) {
			return;
		}
			
		if (source == MPJ.ANY_SOURCE) {
			doIRecvAnySource();
		}
		else {
			doIRecvSource(blocking);
		}
		
	}
    

	private int castBuffer(Object inbuf, Object outbuf, int offset, int count) {
		if ((buf instanceof byte[]) && (datatype != MPJ.OBJECT)) {
			return(((MPJObject)inbuf).cast2ByteArray(buf, offset, count));
		}
		else if (buf instanceof char[]) {
			return(((MPJObject)inbuf).cast2CharArray(buf, offset, count));
		}
		else if (buf instanceof short[]) {
			return(((MPJObject)inbuf).cast2ShortArray(buf, offset, count));
		}
		else if (buf instanceof boolean[]) {
			return(((MPJObject)inbuf).cast2BooleanArray(buf, offset, count));
		}
		else if (buf instanceof int[])  {
			return(((MPJObject)inbuf).cast2IntArray(buf, offset, count));
		}
		else if (buf instanceof long[]) {
			return(((MPJObject)inbuf).cast2LongArray(buf, offset, count));
		}
		else if (buf instanceof float[]) {
			return(((MPJObject)inbuf).cast2FloatArray(buf, offset, count));
		}
		else if (buf instanceof double[]) {
			return(((MPJObject)inbuf).cast2DoubleArray(buf, offset, count));
		}
		else {
			return(((MPJObject)inbuf).cast2ObjectArray(buf, offset, count));
		}
	}
	
	
	
	private int unBuffer(Object inbuf, Object outbuf, int offset, int count) {
		if (outbuf instanceof byte[]) {
			return(unBufferByte((byte[])inbuf, 0, (byte[])outbuf, offset, count));
		}
		else if (outbuf instanceof char[]) {
			return(unBufferChar((byte[])inbuf, 0, (char[])outbuf, offset, count));
		}
		else if (outbuf instanceof short[]) {
			return(unBufferShort((byte[])inbuf, 0, (short[])outbuf, offset, count));
		}
		else if (outbuf instanceof boolean[]) {
			return(unBufferBoolean((byte[])inbuf, 0, (boolean[])outbuf, offset, count));
		}
		else if (outbuf instanceof int[]) {
			return(unBufferInt((byte[])inbuf, 0, (int[])outbuf, offset, count));
		}
		else if (outbuf instanceof long[]) {
			return(unBufferLong((byte[])inbuf, 0, (long[])outbuf, offset, count));
		}
		else if (outbuf instanceof float[]) {
			return(unBufferFloat((byte[])inbuf, 0, (float[])outbuf, offset, count));
		}
		else if (outbuf instanceof double[]) {
			return(unBufferDouble((byte[])inbuf, 0, (double[])outbuf, offset, count));
		}
		else {
			return(unBufferObject((byte[])inbuf, 0, (Object[])outbuf, offset, count));
			
		}

	
	}
	
	
	protected void doIsend() throws MPJException{
		
		if (dest == MPJ.PROC_NULL) return;
		
	//	boolean DEBUG = true;
		
		MPJObject sendObj = new MPJObject(tag, this.contextId, false, datatype.type, count * datatype.extent());
		sendObj.setBuffered(this.buffered);

		
		Connection con = null;
		
		
		if(DEBUG) {
			System.out.println("send tag:       " + sendObj.getTag());
			System.out.println("send contextId: " + sendObj.getContextId());
			System.out.println("send type:      " + sendObj.getBaseDatatype());
			System.out.println("send count:     " + sendObj.getNumberOfElements());
			System.out.println("send offset:    " + offset);
		}

		try {
			String mpjHostName = this.comm.group().getMPJHostName(dest);
			
			if (DEBUG) {
				System.out.println("isend: " + this.comm.rank() + ": " + MPJ.getMyMPJHostName() + ": DEST: " + mpjHostName + "; rank: " + dest + "; tag: " + tag);
			}
			con = MPJ.getConnection(mpjHostName);
			
			con.putMPJObject(sendObj, buf, offset, count * datatype.extent());
			con.sendMPJObject();
			
			
		}
		catch (MPJException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	
	
	
	
	
	protected void doIbsend() throws MPJException {
		if (dest == MPJ.PROC_NULL) {
			return;
		}
	
		
		synchronized (MPJ.bsendCount) {
			MPJ.bsendCount.bsends++;
			MPJ.bsendCount.notifyAll();
		}
		
		byte[] sendBuf = MPJ.getAttachedBuffer();
		int realcount = 0;
		if (sendBuf == null) throw new MPJException("No user space buffer attached to MPJ.");
		
		if (datatype.type == MPJ.BYTE.type) {
			realcount = bufferByte(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
		}
		else if (datatype.type == MPJ.CHAR.type) {
			realcount = bufferChar(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
		}
		else if (datatype.type == MPJ.SHORT.type) {
			realcount = bufferShort(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
		}
		else if (datatype.type == MPJ.BOOLEAN.type) {
			realcount = bufferBoolean(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
		}
		else if (datatype.type == MPJ.INT.type)  {
			realcount = bufferInt(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
		}
		else if (datatype.type == MPJ.LONG.type) {
			realcount = bufferLong(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
		}
		else if (datatype.type == MPJ.FLOAT.type) {				
			realcount = bufferFloat(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
		}
		else if (datatype.type == MPJ.DOUBLE.type) {
			realcount = bufferDouble(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
		}
		else {
			realcount = bufferObject(buf, sendBuf, this.offset, this.count * this.datatype.extent(), 0);
		}

		synchronized (MPJ.bsendCount) {
			MPJ.bsendCount.bsends--;
			MPJ.bsendCount.notifyAll();
		}
		
		
		
		IbisMPJComm myIbisComm = new IbisMPJComm(this.comm, this.myRank, sendBuf, 0, /*this.count * datatype.extent() * datatype.getByteSize()*/ realcount, this.datatype, this.myRank, this.dest, this.tag, OP_ISEND);
		myIbisComm.buffered = true;
		Request request = myIbisComm.getRequest();
		request.ibisMPJCommThread = new Thread(myIbisComm);
		request.ibisMPJCommThread.start();

		
	}
	
	
	
	protected void doIssend() throws MPJException {
		doIsend();
	}
	
	
	
	protected void doIrsend() throws MPJException {
		doIsend();
	}

	private void doProbeStandard() throws MPJException {
		int typeSize = 1;
		int realCount = 0;
		//boolean DEBUG = true;
		
		String mpjHostName = this.comm.group().getMPJHostName(source);
		Connection con = MPJ.getConnection(mpjHostName);
		
		if (DEBUG) {
			System.out.println("iprobe: " + this.comm.rank() + ": " + MPJ.getMyMPJHostName() + ": SOURCE: " + mpjHostName + "; rank: " + source);
		}
		
		
		MPJObject obj = null;
		ReadMessage msg = null;
		MPJObjectQueue queue = con.getRecvQueue();
		
		// first check the object queue and hold the monitor on it
		synchronized(queue) {
			queue.lock();
			this.status = queue.probe(this.contextId, this.tag);
			
			// if queue has not the requested object, try to connect to the receiveport
			if (this.status == null) {

				boolean msgFound = false;
				
				while(!msgFound) {
				
					obj = new MPJObject();
					msg = con.pollForMessage();
		
					if (msg != null) {
						// 	get message header
						con.receiveHeader(msg, obj.desc);
				
						if(DEBUG) {
							System.out.println("iprobe tag:       " + obj.getTag());
							System.out.println("iprobe contextId: " + obj.getContextId());
							System.out.println("iprobe type:      " + obj.getBaseDatatype());
							System.out.println("iprobe count:     " + obj.getNumberOfElements());
							System.out.println("iprobe offset:    " + offset);
						}

						obj.initBuffer();
						con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());
					
						queue.addObject(obj);
					
					
						// 	the message was expected
						if (((obj.getTag() == tag) || 
							((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
							(obj.getContextId() == contextId)) {
							
							this.status = new Status();
							this.status.setSource(source);
							this.status.setTag(obj.getTag());
							this.status.setCount(obj.getNumberOfElements());
							this.status.setSize(obj.getNumberOfElements());
							msgFound = true;
						}
						msg = null;
					}
				
					
					if (this.opCode == OP_IPROBE) {
						msgFound = true;
					}
				}
			
			}
			queue.release();
			
		}
		
	}
	
	
	private void doProbeAnySource() throws MPJException {
		int typeSize = 1;
		int realCount = 0;
		//boolean DEBUG = true;
		
		
		if (DEBUG) {
			System.out.println("iprobe: " + this.comm.rank() + ": " + MPJ.getMyMPJHostName() + ": ANY_SOURCE");
		}
		
		
		MPJObject obj = null;
	
		ReadMessage msg = null;
		
		int src = 0;
		boolean msgFound = false;
		
		while (!msgFound) {
			if (DEBUG) System.out.println("msg not found");
			String mpjHostName = this.comm.group().getMPJHostName(src);
			Connection con = MPJ.getConnection(mpjHostName);
			MPJObjectQueue queue = con.getRecvQueue();
		
			if (queue.isLocked()) {
				src = nextSource(src);
				
				if (DEBUG) System.out.println("queue is locked! src:" + src);
				continue;
			}
			
			// first check the object queue and hold the monitor on it
			synchronized(queue) {
				queue.lock();
				this.status = queue.probe(this.contextId, this.tag);
				
				// if queue has not the requested object, try to connect to the receiveport
				if (this.status == null) {

					obj = new MPJObject();
					if (DEBUG) System.out.println("Polling on " + src);
					msg = con.pollForMessage();
					
					if (msg == null) {
						src = nextSource(src);
						if (DEBUG) System.out.println("msg was null.");	
						this.status = null;
						queue.release();
						continue;
					}
					else {
						
						// 	get message header
						con.receiveHeader(msg, obj.desc);
				
						if(DEBUG) {
							System.out.println("iprobe tag:       " + obj.getTag());
							System.out.println("iprobe contextId: " + obj.getContextId());
							System.out.println("iprobe type:      " + obj.getBaseDatatype());
							System.out.println("iprobe count:     " + obj.getNumberOfElements());
							System.out.println("iprobe offset:    " + offset);
						}

						obj.initBuffer();
						con.receiveData(msg, obj.buffer, 0, obj.getNumberOfElements());
						queue.addObject(obj);
					
						// 	the message was expected
						if (((obj.getTag() == tag) || 
								((tag == MPJ.ANY_TAG) && (obj.getTag() >= 0))) && 
								(obj.getContextId() == contextId)) {
							this.status = new Status();
							this.status.setSource(src);
							this.status.setTag(obj.getTag());
							this.status.setCount(obj.getNumberOfElements());
							this.status.setSize(obj.getNumberOfElements());
							msgFound = true;
							
						}
						msg = null;
					}
				}
				else {
					msgFound = true;
					status.setSource(src);
					queue.release();
					break;
				}
			
				queue.release();
			}			
		}
	}
	
	
	protected void doIprobe() throws MPJException {
		
		
		if (source == MPJ.PROC_NULL) {
			return;
		}
		
		if(source == MPJ.ANY_SOURCE) {
			doProbeAnySource();
		}
		else {
			doProbeStandard();
		}
		
	}
    
	
	protected boolean isFinished() {
		return(this.finished);
	}
	

	
	public void run() {
		this.finished = false;
		if (mode == OP_IRECV) {
			
			try {
				doIrecv(false);
			}
			catch(MPJException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else if (mode == OP_ISEND) {
			try {
				doIsend();
			}
			catch(MPJException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else if (mode == OP_IBSEND) {
			try {
				doIbsend();				
			}
			catch(MPJException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else if (mode == OP_ISSEND) {
			try {
				doIssend();
			}
			catch(MPJException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}
		else if (mode == OP_IRSEND) {
			try {
				doIrsend();
			}
			catch(MPJException e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		this.finished = true;
	}
	
	
	
	
	
	
	
	
	protected static int bufferByte(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((byte[])srcBuf).length) {
				count = ((byte[])srcBuf).length - offset;
			}
			
			if ((position + count) > destBuf.length ) {
				int newCount = destBuf.length - position;
				if (newCount < count) {
					count = newCount;
				}
			}
			System.arraycopy(srcBuf, offset, destBuf, position, count);
			
			return(count+position);
		}
		
		
	}
	
	protected static int bufferChar(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((char[])srcBuf).length) {
				count = ((char[])srcBuf).length - offset;
			}

			if ((position + (count * MPJ.CHAR.getByteSize())) > (destBuf.length)) {
				
				int newCount = (destBuf.length - position) / MPJ.CHAR.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			
			for (int i = 0; i < count; i++) {
				destBuf[position++] = (byte)((((char[])srcBuf)[offset+i] & 0xFF00) >>> 8);
				destBuf[position++] = (byte)((((char[])srcBuf)[offset+i] & 0x00FF));
			}
			return(position);
		}
		
		
	}

	protected static int bufferShort(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((short[])srcBuf).length) {
				count = ((short[])srcBuf).length - offset;
			}

			if ((position + (count * MPJ.SHORT.getByteSize())) > (destBuf.length)) {
				
				int newCount = (destBuf.length - position) / MPJ.SHORT.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			
			for (int i = offset; i < count+offset; i++) {
				destBuf[position++] = (byte)((((short[])srcBuf)[i] & 0xFF00) >>> 8);
				destBuf[position++] = (byte)((((short[])srcBuf)[i] & 0x00FF));
			}
			return(position);
		}
		
		
	}
	
	protected static int bufferBoolean(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((boolean[])srcBuf).length) {
				count = ((boolean[])srcBuf).length - offset;
			}
			
			if ((position + (count * MPJ.BOOLEAN.getByteSize())) > (destBuf.length)) {
				
				int newCount = (destBuf.length - position) / MPJ.BOOLEAN.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			
			for (int i = offset; i < count+offset; i++) {
				if (((boolean[])srcBuf)[i]) {
					destBuf[position] = 1;
				}
				else {
					destBuf[position] = 0;
				}
				position++;
			}
			return(position);
		}
		
		
	}
	
	protected static int bufferInt(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((int[])srcBuf).length) {
				count = ((int[])srcBuf).length - offset;
			}
			if ((position + (count * MPJ.INT.getByteSize())) > (destBuf.length)) {
				
				int newCount = (destBuf.length - position) / MPJ.INT.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {
				destBuf[position] = (byte)((((int[])srcBuf)[i] & 0xFF000000) >>> 24);
				position++;
				destBuf[position] = (byte)((((int[])srcBuf)[i] & 0x00FF0000) >>> 16);
				position++;
				destBuf[position] = (byte)((((int[])srcBuf)[i] & 0x0000FF00) >>> 8);
				position++;
				destBuf[position] = (byte)((((int[])srcBuf)[i] & 0x000000FF));
				position++;
			}
			return(position);
		}
		
		
	}
	
	protected static int bufferLong(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((long[])srcBuf).length) {
				count = ((long[])srcBuf).length - offset;
			}
			if ((position + (count * MPJ.LONG.getByteSize())) > (destBuf.length)) {
				
				int newCount = (destBuf.length - position) / MPJ.LONG.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {
				destBuf[position] = (byte)((((long[])srcBuf)[i] & 0xFF00000000000000L) >>> 56);
				position++;
				destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x00FF000000000000L) >>> 48);
				position++;
				destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x0000FF0000000000L) >>> 40);
				position++;
				destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x000000FF00000000L) >>> 32);
				position++;
				destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x00000000FF000000L) >>> 24);
				position++;
				destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x0000000000FF0000L) >>> 16);
				position++;
				destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x000000000000FF00L) >>> 8);
				position++;
				destBuf[position] = (byte)((((long[])srcBuf)[i] & 0x00000000000000FFL));
				position++;
			}
			return(position);
		}
		
		
	}
	
	protected static int bufferFloat(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((float[])srcBuf).length) {
				count = ((float[])srcBuf).length - offset;
			}
			
			if ((position + (count * MPJ.FLOAT.getByteSize())) > (destBuf.length)) {
				
				int newCount = (destBuf.length - position) / MPJ.FLOAT.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {
				int intBits = Float.floatToIntBits(((float[])srcBuf)[i]);
				destBuf[position] = (byte)((intBits) >>> 24);
				position++;
				destBuf[position] = (byte)((intBits) >>> 16);
				position++;
				destBuf[position] = (byte)((intBits) >>> 8);
				position++;
				destBuf[position] = (byte)((intBits));
				position++;
		    }
			return(position);
		}
		
	}
	
	protected static int bufferDouble(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((double[])srcBuf).length) {
				count = ((double[])srcBuf).length - offset;
			}
			
			if ((position + (count * MPJ.DOUBLE.getByteSize())) > (destBuf.length)) {
				
				int newCount = (destBuf.length - position) / MPJ.DOUBLE.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {
				long longBits = Double.doubleToLongBits(((double[])srcBuf)[i]);
				destBuf[position] = (byte)((longBits) >>> 56);
				position++;
				destBuf[position] = (byte)((longBits) >>> 48);
				position++;
				destBuf[position] = (byte)((longBits) >>> 40);
				position++;
				destBuf[position] = (byte)((longBits) >>> 32);
				position++;
				destBuf[position] = (byte)((longBits) >>> 24);
				position++;
				destBuf[position] = (byte)((longBits) >>> 16);
				position++;
				destBuf[position] = (byte)((longBits) >>> 8);
				position++;
				destBuf[position] = (byte)((longBits));
				position++;
		    }
			return(position);		
		}
		
		
	}
	
	protected static int bufferObject(Object srcBuf, byte[] destBuf, int offset, int count, int position) {
		
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((Object[])srcBuf).length) {
				count = ((Object[])srcBuf).length - offset;
			}
			try {
				ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
				ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream);

				for (int i = offset; i < count+offset; i++) {
					objectOutStream.writeObject(((Object[])srcBuf)[i]);
					
				}

				count = byteOutStream.size();
				
				if ((count+position) <= destBuf.length) {
					System.arraycopy(byteOutStream.toByteArray(), 0, destBuf, position, count);
				}
				
				return(count + position);	
			} 
			catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
				return(0);
			}
		}
		
		
	}
	
	
	protected static int unBufferByte(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((byte[])destBuf).length) {
				count = ((byte[])destBuf).length - offset;
			}
			
			if ((count + position) > srcBuf.length) {
				
				int newCount = srcBuf.length - position;
				if ( newCount < count) {
				    count = newCount;
				}
			}
			System.arraycopy(srcBuf, position, (byte[])destBuf, offset, count);
			
			return(count + position);
		}
	}
	
	protected static int unBufferChar(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((char[])destBuf).length) {
				count = ((char[])destBuf).length - offset;
			}

			if ((position + (count * MPJ.CHAR.getByteSize())) > (srcBuf.length)) {
				
				int newCount = (srcBuf.length - position)/ MPJ.CHAR.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {
				
				((char[])destBuf)[i] = (char)(((srcBuf[position] & 0xFF00) << 8)
											+(srcBuf[position+1] & 0x00FF));
				position += MPJ.CHAR.getByteSize();
			}
			return(position);
		}
		
		
	}



	protected static int unBufferShort(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((short[])destBuf).length) {
				count = ((short[])destBuf).length - offset;
			}

			if (((count * MPJ.SHORT.getByteSize()) + position) > (srcBuf.length)) {
				
				int newCount = (srcBuf.length -position) / MPJ.SHORT.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {
				
				((short[])destBuf)[i] = (short)(((srcBuf[position] & 0xFF00) << 8)
										      +(srcBuf[position+1] & 0x00FF));
				position += MPJ.SHORT.getByteSize();
			}
			return(position);
		}
		
		
	}

	
	
	
	protected static int unBufferBoolean(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((boolean[])destBuf).length) {
				count = ((boolean[])destBuf).length - offset;
			}

			if (((count * MPJ.BOOLEAN.getByteSize()) + position) > (srcBuf.length)) {
				
				int newCount = (srcBuf.length - position) / MPJ.CHAR.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {
				
				if (srcBuf[position] != 0) {
					((boolean[])destBuf)[i] = true;
				}
				else {
					((boolean[])destBuf)[i] = false;
				}
				position++;
			}
			return(position);
		}
		
		
	}

	
	
	protected static int unBufferInt(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((int[])destBuf).length) {
				count = ((int[])destBuf).length - offset;
			}

			if (((count * MPJ.INT.getByteSize()) + position) > (srcBuf.length)) {
				
				int newCount = (srcBuf.length - position) / MPJ.INT.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {
				
			
				((int[])destBuf)[i] = (int)(((srcBuf[position]& 0xff) << 24)
                        				  + ((srcBuf[position+1]& 0xff) << 16)
										  + ((srcBuf[position+2]& 0xff) << 8)
										  + ((srcBuf[position+3]& 0xff)));
				
				position += MPJ.INT.getByteSize();
			}
			return(position);
		}
		
		
	}

	protected static int unBufferLong(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((long[])destBuf).length) {
				count = ((long[])destBuf).length - offset;
			}

			if (((count * MPJ.LONG.getByteSize()) + position) > (srcBuf.length)) {
				
				int newCount = (srcBuf.length - position) / MPJ.LONG.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {

				((long[])destBuf)[i] = (long)(((srcBuf[position]  & 0xff) << 56)
							   	   	       + ((srcBuf[position+1] & 0xff) << 48)
										   + ((srcBuf[position+2] & 0xff) << 40)
										   + ((srcBuf[position+3] & 0xff) << 32)
				                           + ((srcBuf[position+4] & 0xff) << 24)
				                           + ((srcBuf[position+5] & 0xff) << 16)
				                           + ((srcBuf[position+6] & 0xff) << 8)
										   + ((srcBuf[position+7] & 0xff)));
				
				position += MPJ.LONG.getByteSize();
			}
			return(position);
		}
	}

	
	
	
	protected static int unBufferFloat(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((float[])destBuf).length) {
				count = ((float[])destBuf).length - offset;
			}

			if (((count * MPJ.FLOAT.getByteSize()) + position) > (srcBuf.length)) {
				
				int newCount = (srcBuf.length - position) / MPJ.FLOAT.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {
				int intBits = (int)(((srcBuf[position] & 0xff) << 24)
                        		+ ((srcBuf[position+1] & 0xff) << 16)
								+ ((srcBuf[position+2] & 0xff) << 8)
								+ ((srcBuf[position+3] & 0xff)));

				((float[])destBuf)[i] = (float)Float.intBitsToFloat(intBits);
				position += MPJ.FLOAT.getByteSize();
			}
			return(position);
		}
		
		
	}

	
	protected static int unBufferDouble(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
			
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((double[])destBuf).length) {
				count = ((double[])destBuf).length - offset;
			}

			if (((count * MPJ.DOUBLE.getByteSize()) + position)> (srcBuf.length)) {
				
				int newCount = (srcBuf.length - position) / MPJ.DOUBLE.getByteSize();
				if ( newCount < count) {
				    count = newCount;
				}
			}

			for (int i = offset; i < count+offset; i++) {

				
				long longBits = (long)((((long)srcBuf[position] & 0xff) << 56)
						    	   + (((long)srcBuf[position+1] & 0xff) << 48)
								   + (((long)srcBuf[position+2] & 0xff) << 40)
								   + (((long)srcBuf[position+3] & 0xff) << 32)
				                   + (((long)srcBuf[position+4] & 0xff) << 24)
				                   + (((long)srcBuf[position+5] & 0xff) << 16)
				                   + (((long)srcBuf[position+6] & 0xff) << 8)
								   + (((long)srcBuf[position+7] & 0xff)));
				

				((double[])destBuf)[i] = (double)Double.longBitsToDouble(longBits);
				position += MPJ.DOUBLE.getByteSize();
			}
			return(position);
		}
		
		
	}

	
	
	protected static int unBufferObject(byte[] srcBuf, int position, Object destBuf, int offset, int count) {
		if ((count == 0) || (srcBuf == null)) {
			return(0);
		}
		else {
			if ((offset + count) > ((Object[])destBuf).length) {
				count = ((Object[])destBuf).length - offset;
			}

			
			try {
				
				byte[] tmp = new byte[((byte[])srcBuf).length - position];
				System.arraycopy(srcBuf, position, tmp, 0, tmp.length);
				
				ByteArrayInputStream byteInputStream = new ByteArrayInputStream(tmp);
				ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream);
				
			
				for (int i=offset; i < count+offset; i++) {
					((Object[])destBuf)[i] = (Object)objectInputStream.readObject();
					
				}
				position = ((byte[])srcBuf).length - byteInputStream.available();
				
				return(position);
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
				return(0);
			}
		}
	}


   
}
