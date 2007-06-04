/* $Id$ */

/*
 * Created on 21.01.2005
 */
package ibis.mpj;


import ibis.ipl.IbisIdentifier;
import ibis.io.BufferedArrayInputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.SerializationInput;
import ibis.io.SerializationOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

/**
 * Communicator for point-to-point messages.
 */
public class Comm {

    protected Group group;
    protected int contextId = MPJ.UNDEFINED;
    protected boolean isInterComm = false;
    IbisMPJComm ibisMPJsend = new IbisMPJComm();	
    IbisMPJComm ibisMPJrecv = new IbisMPJComm();

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
            return(MPJ.CONGRUENT);
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

                cpComm.group.table = new Vector<IbisIdentifier>(this.group.table);
                return(cpComm);

            }
            return(null);
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
            return(MPJ.getMyId().toString());
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
        //ibisMPJ. = setIbisMPJComm(this, this.rank(), buf, offset, count, datatype, this.rank(), dest, tag, );

        ibisMPJsend.buf = buf;
        ibisMPJsend.offset = offset;
        ibisMPJsend.count = count;
        ibisMPJsend.datatype = datatype;
        ibisMPJsend.dest = dest;
        ibisMPJsend.source = this.rank();
        ibisMPJsend.tag = tag;
        ibisMPJsend.mode = IbisMPJComm.OP_ISEND;
        ibisMPJsend.comm = this;
        ibisMPJsend.myRank = this.rank();
        ibisMPJsend.request = new Request();
        ibisMPJsend.request.setIbisMPJComm(ibisMPJsend);
        ibisMPJsend.status = new Status();
        ibisMPJsend.status.setSource(0);
        ibisMPJsend.status.setTag(0);
        ibisMPJsend.status.setCount(0);
        ibisMPJsend.contextId = this.contextId;
        ibisMPJsend.finished = false;

        ibisMPJsend.doIsend();

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
        //IbisMPJComm ibisMPJ = new IbisMPJComm(this, this.rank(), buf, offset, count, datatype, source, this.rank(), tag, IbisMPJComm.OP_IRECV);

        ibisMPJrecv.buf = buf;
        ibisMPJrecv.offset = offset;
        ibisMPJrecv.count = count;
        ibisMPJrecv.datatype = datatype;
        ibisMPJrecv.dest = this.rank();
        ibisMPJrecv.source = source;
        ibisMPJrecv.tag = tag;
        ibisMPJrecv.mode = IbisMPJComm.OP_ISEND;
        ibisMPJrecv.comm = this;
        ibisMPJrecv.myRank = this.rank();
        ibisMPJrecv.request = new Request();
        ibisMPJrecv.request.setIbisMPJComm(ibisMPJsend);
        ibisMPJrecv.status = new Status();
        ibisMPJrecv.status.setSource(0);
        ibisMPJrecv.status.setTag(0);
        ibisMPJrecv.status.setCount(0);
        ibisMPJrecv.contextId = this.contextId;
        ibisMPJrecv.finished = false;


        ibisMPJrecv.doIrecv(true);
        return(ibisMPJrecv.getStatus());

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
            return(BufferOps.bufferByte(inbuf, outbuf, offset, incount, position));
        }
        else if (datatype == MPJ.CHAR) {
            return(BufferOps.bufferChar(inbuf, outbuf, offset, incount, position));
        }
        else if (datatype == MPJ.SHORT) {
            return(BufferOps.bufferShort(inbuf, outbuf, offset, incount, position));
        }
        else if (datatype == MPJ.BOOLEAN) {
            return(BufferOps.bufferBoolean(inbuf, outbuf, offset, incount, position));
        }
        else if (datatype == MPJ.INT)  {
            return(BufferOps.bufferInt(inbuf, outbuf, offset, incount, position));
        }
        else if (datatype == MPJ.LONG) {
            return(BufferOps.bufferLong(inbuf, outbuf, offset, incount, position));
        }
        else if (datatype == MPJ.FLOAT) {				
            return(BufferOps.bufferFloat(inbuf, outbuf, offset, incount, position));			
        }
        else if (datatype == MPJ.DOUBLE) {
            return(BufferOps.bufferDouble(inbuf, outbuf, offset, incount, position));
        }
        else if (datatype == MPJ.OBJECT) {
            return(BufferOps.bufferObject(inbuf, outbuf, offset, incount, position));
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
            BufferOps.bufferByte(inbuf, outbuf, offset, incount, 0);
            return(outbuf);
        }
        else if (datatype == MPJ.CHAR) {
            BufferOps.bufferChar(inbuf, outbuf, offset, incount, 0);
            return(outbuf);
        }
        else if (datatype == MPJ.SHORT) {
            BufferOps.bufferShort(inbuf, outbuf, offset, incount, 0);
            return(outbuf);
        }
        else if (datatype == MPJ.BOOLEAN) {
            BufferOps.bufferBoolean(inbuf, outbuf, offset, incount, 0);
            return(outbuf);
        }
        else if (datatype == MPJ.INT)  {
            BufferOps.bufferInt(inbuf, outbuf, offset, incount, 0);
            return(outbuf);
        }
        else if (datatype == MPJ.LONG) {
            BufferOps.bufferLong(inbuf, outbuf, offset, incount, 0);
            return(outbuf);
        }
        else if (datatype == MPJ.FLOAT) {				
            BufferOps.bufferFloat(inbuf, outbuf, offset, incount, 0);
            return(outbuf);
        }
        else if (datatype == MPJ.DOUBLE) {
            BufferOps.bufferDouble(inbuf, outbuf, offset, incount, 0);
            return(outbuf);
        }
        else if (datatype == MPJ.OBJECT) {
            BufferOps.bufferObject(inbuf, outbuf, offset, incount, 0);
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
            return(BufferOps.unBufferByte(inbuf, position, outbuf, offset, outcount));
        }
        else if (datatype == MPJ.CHAR) {
            return(BufferOps.unBufferChar(inbuf, position, outbuf, offset, outcount));
        }
        else if (datatype == MPJ.SHORT) {
            return(BufferOps.unBufferShort(inbuf, position, outbuf, offset, outcount));
        }
        else if (datatype == MPJ.BOOLEAN) {
            return(BufferOps.unBufferBoolean(inbuf, position, outbuf, offset, outcount));
        }
        else if (datatype == MPJ.INT) {
            return(BufferOps.unBufferInt(inbuf, position, outbuf, offset, outcount));
        }
        else if (datatype == MPJ.LONG) {
            return(BufferOps.unBufferLong(inbuf, position, outbuf, offset, outcount));
        }
        else if (datatype == MPJ.FLOAT) {
            return(BufferOps.unBufferFloat(inbuf, position, outbuf, offset, outcount));
        }
        else if (datatype == MPJ.DOUBLE) {
            return(BufferOps.unBufferDouble(inbuf, position, outbuf, offset, outcount));
        }
        else if ((datatype == MPJ.OBJECT)) {
            return(BufferOps.unBufferObject(inbuf, position, outbuf, offset, outcount));
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





    protected void localcopy1type(Object inbuf, int inoffset, Object outbuf, int outoffset, int count, Datatype datatype) throws MPJException {
        if (inbuf instanceof byte[]) {
            System.arraycopy(inbuf, inoffset, outbuf, outoffset, count * datatype.extent());
        }
        else if (inbuf instanceof char[]) {
            System.arraycopy(inbuf, inoffset, outbuf, outoffset, count * datatype.extent());
        }
        else if (inbuf instanceof short[]) {
            System.arraycopy(inbuf, inoffset, outbuf, outoffset, count * datatype.extent());
        }
        else if (inbuf instanceof boolean[]) {
            System.arraycopy(inbuf, inoffset, outbuf, outoffset, count * datatype.extent());
        }
        else if (inbuf instanceof int[])  {
            System.arraycopy(inbuf, inoffset, outbuf, outoffset, count * datatype.extent());
        }
        else if (inbuf instanceof long[]) {
            System.arraycopy(inbuf, inoffset, outbuf, outoffset, count * datatype.extent());
        }
        else if (inbuf instanceof float[]) {
            System.arraycopy(inbuf, inoffset, outbuf, outoffset, count * datatype.extent());
        }
        else if (inbuf instanceof double[]) {
            System.arraycopy(inbuf, inoffset, outbuf, outoffset, count * datatype.extent());
        }
        else {

            if (MPJ.LOCALCOPYIBIS) {
                StoreBuffer stBuf = new StoreBuffer();

                //				StoreArrayInputStream sin = null;
                SerializationOutput mout = null;
                SerializationInput min = null;

                StoreOutputStream store_out = new StoreOutputStream(stBuf);
                StoreInputStream store_in = new StoreInputStream(stBuf);

                ibis.io.DataOutputStream out = new BufferedArrayOutputStream(store_out, 4096);
                ibis.io.DataInputStream in =  new BufferedArrayInputStream(store_in, 4096);

                try {
                    mout = new IbisSerializationOutputStream(out);
                    min = new IbisSerializationInputStream(in);

                    int extent = datatype.extent();

                    mout.writeArray(((Object[])inbuf), inoffset, count * extent);
                    mout.flush();
                    min.readArray((Object[])outbuf, outoffset, count * extent);

                }	
                catch (ClassNotFoundException e) {
                    throw new MPJException(e.getMessage());
                }

                catch (IOException e) {
                    throw new MPJException(e.getMessage());
                }
            }
            else {

                try {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    ObjectOutputStream oout = new ObjectOutputStream(bout);

                    int extent = datatype.extent();

                    for (int i=0; i < count * extent; i++) {
                        oout.writeObject(((Object[])inbuf)[i+inoffset]);
                    }
                    oout.flush();

                    ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
                    ObjectInputStream oin = new ObjectInputStream(bin);

                    for (int i=0; i < count * extent; i++) {
                        ((Object[])outbuf)[i+outoffset] = oin.readObject();
                    }

                    System.gc();
                } catch (Exception e) {
                    throw new MPJException(e.getMessage());
                }
            }


        }	


    }



    protected void localcopy2types(Object inbuf, int inoffset, int incount, Datatype intype, 
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
            System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
            System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());

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
            System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
            System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());
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
            System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
            System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());
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
            System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
            System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());
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
            System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
            System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());
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
            System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
            System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());

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
            System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
            System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());
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
            System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
            System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());
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
            System.arraycopy(inbuf, inoffset, tmpbuf, 0, incount * intype.extent());
            System.arraycopy(tmpbuf, 0, outbuf, outoffset, outcount * outtype.extent());
        }
        else {
            inLength = ((Object[])inbuf).length;
            outLength = ((Object[])outbuf).length;

            while (inLength < inoffset + incount * inExtent) {
                incount--;
            }

            while (outLength < outoffset + outcount * outExtent) {
                outcount--;
            }


            if (MPJ.LOCALCOPYIBIS) {

                StoreBuffer stBuf = new StoreBuffer();

                //				StoreArrayInputStream sin = null;
                SerializationOutput mout = null;
                SerializationInput min = null;

                StoreOutputStream store_out = new StoreOutputStream(stBuf);
                StoreInputStream store_in = new StoreInputStream(stBuf);

                ibis.io.DataOutputStream out = new BufferedArrayOutputStream(store_out, 4096);
                ibis.io.DataInputStream in =  new BufferedArrayInputStream(store_in, 4096);

                try {
                    mout = new IbisSerializationOutputStream(out);
                    min = new IbisSerializationInputStream(in);

                    mout.writeArray(((Object[])inbuf), inoffset, incount * inExtent);
                    mout.flush();
                    min.readArray((Object[])outbuf, outoffset, outcount * outExtent);

                    mout.realClose();
                    min.realClose();

                }	
                catch (ClassNotFoundException e) {
                    throw new MPJException(e.getMessage());
                }

                catch (IOException e) {
                    throw new MPJException(e.getMessage());
                }
            }
            else {

                try {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    ObjectOutputStream oout = new ObjectOutputStream(bout);

                    for (int i=0; i < incount * inExtent; i++) {
                        oout.writeObject(((Object[])inbuf)[i+inoffset]);
                    }
                    oout.flush();

                    ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
                    ObjectInputStream oin = new ObjectInputStream(bin);

                    for (int i=0; i < outcount * outExtent; i++) {
                        ((Object[])outbuf)[i+outoffset] = oin.readObject();
                    }

                    System.gc();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new MPJException(e.getMessage());

                }
            }



        }


    }

}
